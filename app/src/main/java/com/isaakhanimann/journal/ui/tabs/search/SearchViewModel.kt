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

    val selectedRoutes = MutableStateFlow(emptySet<String>())
    val selectedFormulations = MutableStateFlow(emptySet<String>())
    val includeReferences = MutableStateFlow(false)
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

    private val filtersFlow = MutableStateFlow(emptyList<String>())

    private val customChipName: String = application.getString(R.string.custom)

    fun onFilterTapped(filterName: String) {
        viewModelScope.launch {
            if (filterName == customChipName) {
                isShowingCustomSubstancesFlow.emit(isShowingCustomSubstancesFlow.value.not())
                return@launch
            }
            val filters = filtersFlow.value.toMutableList()
            if (filters.contains(filterName)) {
                filters.remove(filterName)
            } else {
                filters.add(filterName)
            }
            filtersFlow.emit(filters)
        }
    }

    private val isShowingCustomSubstancesFlow = MutableStateFlow(false)

    val chipCategoriesFlow: StateFlow<List<CategoryChipModel>> = filtersFlow.map { filters ->
        substanceRepo.getAllCategories().map { category ->
            val isActive = filters.contains(category.name)
            CategoryChipModel(
                chipName = category.name,
                color = category.color,
                isActive = isActive
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

    val filteredSubstancesFlow = searchRepository.observe(searchTextFlow, filtersFlow, selectedRoutes, selectedFormulations, includeReferences)
        .map { matches -> matches.map { it.toSubstanceModel() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterSubstances(searchText: String) {
        _searchTextFlow.value = searchText
    }

    private val customSubstancesFlow = experienceRepo.getCustomSubstancesFlow()

    val filteredCustomSubstancesFlow =
        customSubstancesFlow.combine(searchTextFlow) { customSubstances, searchText ->
            customSubstances.filter { custom ->
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
