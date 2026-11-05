package com.isaakhanimann.journal.data.substances.repositories

import android.content.Context
import androidx.room.withTransaction
import com.isaakhanimann.journal.applicationScope
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.room.AppDatabase
import com.isaakhanimann.journal.data.substances.classes.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val substanceRepo: SubstanceRepository,
    private val database: AppDatabase,
) : SearchRepositoryInterface {
    private val preferences = context.getSharedPreferences("medication_search", 0)
    private val modes = setOf("default", "recent", "name")
    val sortMode = MutableStateFlow(preferences.getString("sort", "default")?.takeIf { it in modes } ?: "default")
    val favorites = database.hrtDao().settings().map { settings -> settings.filter { it.id.startsWith("favorite:") && it.payload == "true" }.map { it.id.removePrefix("favorite:") }.toSet() }
        .distinctUntilChanged().stateIn(applicationScope, SharingStarted.Eagerly, emptySet())

    private val clock = flow { while (true) { emit(System.currentTimeMillis()); delay(60000) } }
    private val dataRanking = combine(
        database.experienceDao().getIngestionsSortedDescendingFlow(),
        database.hrtDao().plans(), database.hrtDao().settings(), database.hrtDao().links(),
        database.experienceDao().getCustomSubstancesFlow()
    ) { events, plans, settings, links, custom ->
        val catalog = substanceRepo.getAllSubstances().map { IdentityEntry(it.id, it.name, it.commonNames, it.availableRoutesForIngestion().map { route -> route.name }.toSet()) }
        val linkMap = links.associateBy { it.ingestionId }
        val customNames = custom.map { it.name }.toSet()
        val index = MedicationIdentityIndex(catalog, customNames)
        val recent = events.sortedByDescending { it.time }.mapNotNull { event ->
            if (event.id in linkMap) linkMap.getValue(event.id).medicationId
            else index.resolve(event.substanceName, event.administrationRoute.name)
        }.distinct()
        Triple(plans, settings, recent)
    }.flowOn(Dispatchers.Default)
    val ranking = combine(dataRanking, clock) { (plans, settings, recent), now ->
        MedicationSearchRanking(
            settings.filter { it.id.startsWith("favorite:") && it.payload == "true" }.map { it.id.removePrefix("favorite:") }.toSet(),
            plans.filter { !it.paused && it.start <= now && (PlanSchedule.read(it, settings).end ?: Long.MAX_VALUE) >= now }.map { it.medicationId }.toSet(),
            recent,
        )
    }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(applicationScope, SharingStarted.Eagerly, MedicationSearchRanking())

    private class PreparedCatalog(val substances: List<SubstanceWithCategories>, val locale: Locale) {
        val byId = substances.associateBy { it.substance.id }
        val index = MedicationSearchIndex(substances.map { item ->
            val sub = item.substance
            MedicationSearchEntry(sub.id, sub.name, sub.commonNames, sub.categories.toSet(), sub.availableRoutesForIngestion().map { it.name }.toSet(),
                sub.searchFormulations(), sub.referenceSearchText(), sub.localizedSortName(locale))
        }, locale)
    }
    @Volatile private var prepared: PreparedCatalog? = null
    @Synchronized private fun prepare(substances: List<SubstanceWithCategories>): PreparedCatalog {
        val locale = context.resources.configuration.locales[0]
        return prepared?.takeIf { it.substances === substances && it.locale == locale } ?: PreparedCatalog(substances, locale).also { prepared = it }
    }
    init {
        applicationScope.launch(Dispatchers.Default) {
            substanceRepo.getAllSubstancesWithCategoriesFlow().collect { prepare(it) }
        }
    }

    fun setSortMode(mode: String) {
        require(mode in modes)
        preferences.edit().putString("sort", mode).apply()
        sortMode.value = mode
    }

    suspend fun toggleFavorite(id: String) = database.withTransaction {
        require(substanceRepo.getAllSubstances().any { it.id == id })
        val key = "favorite:$id"
        val old = database.hrtDao().setting(key)?.payload == "true"
        database.hrtDao().save(HrtModelSetting(key, (!old).toString()))
    }

    fun observe(text: Flow<String>, categories: Flow<List<String>> = flowOf(emptyList()), routes: Flow<Set<String>> = flowOf(emptySet()), formulations: Flow<Set<String>> = flowOf(emptySet()), references: Flow<Boolean> = flowOf(false)): Flow<List<SubstanceWithCategories>> {
        val requests = combine(text, categories, routes, formulations, references) { query, category, route, formulation, include ->
            MedicationSearchRequest(query, category.toSet(), route, formulation, include)
        }
        return combine(requests, sortMode, ranking, substanceRepo.getAllSubstancesWithCategoriesFlow()) { request, mode, context, substances ->
            Triple(request.copy(sort = mode), context, substances)
        }.mapLatest { (request, context, substances) ->
            withContext(Dispatchers.Default) {
                val cancellation = currentCoroutineContext()
                val catalog = prepare(substances)
                catalog.index.rank(request, context) { cancellation.ensureActive() }.map { catalog.byId.getValue(it) }
            }
        }.distinctUntilChanged()
    }

    override fun getMatchingSubstances(searchText: String, filterCategories: List<String>, recentlyUsedSubstanceNamesSorted: List<String>): List<SubstanceWithCategories> {
        val recent = recentlyUsedSubstanceNamesSorted.mapNotNull { substanceRepo.getSubstance(it)?.id }
        val context = ranking.value
        return search(substanceRepo.getAllSubstancesWithCategories(), MedicationSearchRequest(searchText, filterCategories.toSet(), sort = sortMode.value),
            if (recent.isEmpty()) context else context.copy(recentIds = (recent + context.recentIds).distinct()))
    }

    fun getMatchingSubstancesFlow(searchText: String): Flow<List<SubstanceWithCategories>> = observe(flowOf(searchText))

    private fun search(substances: List<SubstanceWithCategories>, request: MedicationSearchRequest, ranking: MedicationSearchRanking): List<SubstanceWithCategories> {
        val catalog = prepare(substances)
        return catalog.index.rank(request, ranking).map { catalog.byId.getValue(it) }
    }

}

internal fun Substance.referenceSearchText(): String = listOfNotNull(
        summary, effectsSummary, dosageRemark, generalRisks, longtermRisks,
        clinicalInfo?.let { (it.atcCodes + it.drugClass + it.indications + it.contraindications + it.majorWarnings + it.majorInteractions + it.monitoring).joinToString(" ") },
        endocrineInfo?.let { (it.hormoneClass + it.mechanisms + it.affectedHormones + it.monitoringLabs + it.assayCaveats).joinToString(" ") },
        sourceRefs.joinToString(" ") { it.title },
        timeCourse.joinToString(" ") { (it.notes + listOfNotNull(it.formulation)).joinToString(" ") },
        doseUseReferences.joinToString(" ") { listOfNotNull(it.indication, it.note, it.amountText, it.scheduleText).joinToString(" ") },
    ).joinToString(" ")
