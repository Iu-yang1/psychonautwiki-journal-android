package com.isaakhanimann.journal.ui.tabs.search

import com.isaakhanimann.journal.data.substances.classes.availableRoutesForIngestion
import com.isaakhanimann.journal.data.substances.classes.searchFormulations
import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.repositories.SearchRepository
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    experienceRepo: ExperienceRepository,
    private val substanceRepo: SubstanceRepository,
    private val searchRepository: SearchRepository
) : AndroidViewModel(application) {

    val sortMode = searchRepository.sortMode
    fun setSortMode(mode: String) = searchRepository.setSortMode(mode)

    val selectedRoutes = searchRepository.selectedRoutes
    val selectedFormulations = searchRepository.selectedFormulations
    val includeReferences = searchRepository.includeReferences
    val selectedDomains = searchRepository.selectedDomains
    val selectedMechanisms = searchRepository.selectedMechanisms
    val taxonomy = substanceRepo.taxonomy
    val domainOptions = taxonomy.nodes.filter { it.kind == "domain" }
    val categoryOptions = taxonomy.nodes.filter { it.kind == "category" }
    val mechanismOptions = taxonomy.nodes.filter { it.kind == "mechanism" }
    fun categoryIsInSelectedDomain(id: String): Boolean = selectedDomains.value.isEmpty() ||
        taxonomy.tags(listOf(id)).any { it in selectedDomains.value }
    fun toggleDomain(id: String) {
        isShowingCustomSubstancesFlow.value = false
        val selected = selectedDomains.value
        selectedDomains.value = if (id in selected) selected - id else selected + id
        searchRepository.selectedCategories.value = searchRepository.selectedCategories.value.filter(::categoryIsInSelectedDomain).toSet()
    }
    fun clearFilters() {
        isShowingCustomSubstancesFlow.value = false
        selectedDomains.value = emptySet()
        searchRepository.selectedCategories.value = emptySet()
        selectedMechanisms.value = emptySet()
        selectedRoutes.value = emptySet()
        selectedFormulations.value = emptySet()
    }
    val favorites = searchRepository.favorites
    val routeOptions = substanceRepo.getAllSubstances().flatMap { it.availableRoutesForIngestion() }.distinct().map { it.name }
    val formulationOptions = substanceRepo.getAllSubstances().flatMap { it.searchFormulations() }.distinct().sorted()
    val errorMessage = MutableStateFlow<String?>(null)
    fun toggleFavorite(name: String) {
        viewModelScope.launch {
            try { substanceRepo.getSubstance(name)?.let { searchRepository.toggleFavorite(it.id) }; errorMessage.value = null }
            catch (exception: Exception) {
                if (exception is kotlinx.coroutines.CancellationException) throw exception
                errorMessage.value = exception.message ?: "无法更新收藏"
            }
        }
    }

    private val _searchTextFlow = MutableStateFlow("")
    val searchTextFlow = _searchTextFlow.asStateFlow()

    private val filtersFlow = searchRepository.selectedCategories.map { it.toList() }

    private val customChipName: String get() = getApplication<Application>().getString(R.string.custom)

    fun toggleCustomOnly() {
        val enabled = !isShowingCustomSubstancesFlow.value
        if (enabled) clearFilters()
        isShowingCustomSubstancesFlow.value = enabled
    }

    fun onFilterTapped(filterName: String) {
        viewModelScope.launch {
            if (filterName == customChipName) {
                toggleCustomOnly()
                return@launch
            }
            isShowingCustomSubstancesFlow.value = false
            val filters = searchRepository.selectedCategories.value.toMutableSet()
            if (filters.contains(filterName)) {
                filters.remove(filterName)
            } else {
                filters.add(filterName)
            }
            searchRepository.selectedCategories.value = filters
        }
    }

    val isShowingCustomSubstancesFlow = searchRepository.customOnly

    val chipCategoriesFlow: StateFlow<List<CategoryChipModel>> = combine(filtersFlow, substanceRepo.getAllSubstancesFlow()) { filters, _ ->
        substanceRepo.getAllCategories().filter { category -> taxonomy.node(category.name)?.let { it.kind == "category" && it.id == category.name } == true }.map { category ->
            val isActive = filters.contains(category.name)
            CategoryChipModel(
                chipName = category.name,
                color = category.color,
                isActive = isActive,
                displayName = category.displayName ?: localizedCategoryDisplayName(category.name)
            )
        }
    }.combine(isShowingCustomSubstancesFlow) { chips, isShowingCustom ->
        val newChips = chips.toMutableList()
        newChips.add(
            0,
            CategoryChipModel(
                chipName = customChipName,
                color = customColor,
                isActive = isShowingCustom
            )
        )
        return@combine newChips
    }.stateIn(
        initialValue = emptyList(),
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    val filteredSubstancesFlow = searchRepository.observe(searchTextFlow, filtersFlow, selectedRoutes, selectedFormulations, includeReferences, selectedDomains, selectedMechanisms)
        .combine(isShowingCustomSubstancesFlow) { matches, customOnly -> if (customOnly) emptyList() else matches }
        .map { matches -> matches.map { it.toSubstanceModel() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterSubstances(searchText: String) {
        _searchTextFlow.value = searchText
    }

    private val customSubstancesFlow = experienceRepo.getCustomSubstancesFlow()
    private val hasBuiltinFilters = combine(searchRepository.selectedCategories, selectedDomains, selectedMechanisms, selectedRoutes, selectedFormulations) { categories, domains, mechanisms, routes, forms ->
        listOf(categories, domains, mechanisms, routes, forms).any { it.isNotEmpty() }
    }

    val filteredCustomSubstancesFlow =
        combine(customSubstancesFlow, searchTextFlow, hasBuiltinFilters, isShowingCustomSubstancesFlow) { customSubstances, searchText, filtered, onlyCustom ->
            if (filtered && !onlyCustom) emptyList() else customSubstances.filter { custom ->
                custom.name.contains(
                    other = searchText,
                    ignoreCase = true
                )
            }
        }.stateIn(
            initialValue = emptyList(),
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )
}

val customColor = Color.Cyan

data class CategoryChipModel(
    val chipName: String,
    val color: Color,
    val isActive: Boolean,
    val displayName: String = localizedCategoryDisplayName(chipName)
)

data class CategoryModel(
    val name: String,
    val color: Color,
    val displayName: String = localizedCategoryDisplayName(name)
)
