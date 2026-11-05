package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.isaakhanimann.journal.data.hrt.*
import com.isaakhanimann.journal.data.substances.repositories.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HrtViewModel @Inject constructor(
    val repo: HrtRepository,
    val notificationScheduler: com.isaakhanimann.journal.notifications.HrtPlanNotifications,
    private val legacyNotifications: com.isaakhanimann.journal.notifications.IngestionReminderNotificationManager,
    val search: SearchRepository,
    val references: HormoneReferenceRepository,
    val legacyAlarms: com.isaakhanimann.journal.notifications.LegacyReminderCoordinator,
) : ViewModel() {
    val medications = repo.substances.getAllSubstances().filter { "hrt-related" in it.categories }
    fun matchingMedications(query: String) = search.getMatchingSubstances(query, emptyList(), emptyList())
        .map { it.substance }.filter { "hrt-related" in it.categories }
    val plans = repo.dao.plans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repo.dao.settings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val legacyReminders = repo.database.experienceDao().getAllRemindersFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stock = repo.dao.stock().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movements = repo.dao.movements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val labs = repo.dao.labs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val links = repo.dao.links().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val details = repo.dao.details().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = repo.database.experienceDao().getIngestionsSortedDescendingFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val consumer = MutableStateFlow<String?>(null)
    val dashboard = combine(events, plans, labs, links, consumer) { events, plans, labs, links, subject ->
        val now = System.currentTimeMillis()
        val personalLabs = labs.filter { it.consumer == subject && it.time <= now }
        val personalEvents = events.filter { it.consumerName == subject }.map { it.id }.toSet()
        HrtDashboard(
            consumers = (listOf<String?>(null) + events.map { it.consumerName } + plans.map { it.consumer } + labs.map { it.consumer }).distinct(),
            estradiol = personalLabs.filter { LabTrends.analyteKey(it.analyte) == "E2" }.maxByOrNull { it.time },
            testosterone = personalLabs.filter { LabTrends.analyteKey(it.analyte) == "总睾酮" }.maxByOrNull { it.time },
            pending = links.count { it.medicationId == null && it.ingestionId in personalEvents },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HrtDashboard())
    val reviewRecords = combine(events, links, consumer) { events, links, subject ->
        val linkMap = links.associateBy { it.ingestionId }
        val ids = medications.map { it.id }.toSet()
        events.filter { it.consumerName == subject && (linkMap[it.id]?.medicationId == null || linkMap[it.id]?.medicationId in ids) }
            .map { event -> HrtReviewRecord(event, linkMap[event.id], medications.singleOrNull { it.id == linkMap[event.id]?.medicationId }?.hrtCategoryLabel()) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var message by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    init {
        viewModelScope.launch {
            combine(repo.database.experienceDao().getIngestionsSortedDescendingFlow(), repo.database.experienceDao().getCustomSubstancesFlow()) { events, customs -> events to customs }.collect { (events, customs) ->
                withContext(Dispatchers.Default) { repo.identify(events, customs.map { it.name }.toSet()) }
            }
        }
    }
    fun run(action: suspend () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            try { action(); message = "已保存" }
            catch (exception: Exception) {
                if (exception is kotlinx.coroutines.CancellationException) throw exception
                errorMessage = exception.message ?: "保存失败"
                message = errorMessage
            }
        }
    }
    fun savePlan(plan: HrtPlan, rule: PlanSchedule, onSaved: () -> Unit) = run {
        repo.savePlan(plan, rule)
        onSaved()
    }

    fun migrateReminder(reminder: com.isaakhanimann.journal.data.room.experiences.entities.IngestionReminder, plan: HrtPlan, rule: PlanSchedule, onSaved: () -> Unit) = run {
        repo.database.withTransaction {
            require(reminder.isEnabled && repo.database.experienceDao().getReminderById(reminder.id) == reminder) { "旧提醒已变更，请刷新" }
            repo.savePlan(plan, rule)
            repo.database.experienceDao().update(reminder.copy(isEnabled = false))
        }
        legacyNotifications.cancelReminder(reminder.id)
        onSaved()
    }
}

data class HrtDashboard(val consumers: List<String?> = listOf(null), val estradiol: HrtLab? = null, val testosterone: HrtLab? = null, val pending: Int = 0)
data class HrtReviewRecord(val event: com.isaakhanimann.journal.data.room.experiences.entities.Ingestion, val link: MedicationLink?, val category: String? = null)
