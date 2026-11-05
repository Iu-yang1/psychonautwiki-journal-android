package com.isaakhanimann.journal.ui.tabs.hrt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.data.hrt.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface HormoneChartState {
    data object Loading : HormoneChartState
    data class Ready(val data: HormoneChartData) : HormoneChartState
    data class Failed(val message: String) : HormoneChartState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HormoneSimulationViewModel @Inject constructor(repo: HrtRepository, val references: HormoneReferenceRepository) : ViewModel() {
    private val request = MutableStateFlow(HormoneChartRequest())
    private val refresh = MutableStateFlow(0)
    fun configure(consumer: String?, historyDays: Int, forecastDays: Int, analyteId: String = "E2", gelReference: String = "compare", bridgeSublingualValerate: Boolean = true) { request.value = HormoneChartRequest(consumer, historyDays, forecastDays, analyteId, gelReference, bridgeSublingualValerate) }
    fun retry() { refresh.value += 1 }
    private val log = combine(repo.database.experienceDao().getIngestionsSortedDescendingFlow(), repo.dao.links(), repo.dao.details(), repo.dao.settings(), repo.dao.plans()) { records, links, details, settings, plans ->
        HormoneJournalData(records, links, details, settings, plans)
    }
    private val journal = combine(log, repo.dao.labs(), repo.substances.getAllSubstancesFlow()) { data, labs, substances -> data.copy(labs = labs, substances = substances) }
    private val clock = flow { while (true) { emit(System.currentTimeMillis()); delay(60000) } }
    private val service = HormoneChartService(references.catalog)
    val state: StateFlow<HormoneChartState> = combine(journal, request, clock, refresh) { data, options, now, _ -> Triple(data, options, now) }
        .transformLatest { (data, options, now) ->
            emit(HormoneChartState.Loading)
            try {
                val chart = withContext(Dispatchers.Default) {
                    val cancellation = currentCoroutineContext()
                    service.calculate(data, options, now) { cancellation.ensureActive() }
                }
                emit(HormoneChartState.Ready(chart))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                emit(HormoneChartState.Failed(exception.message ?: "无法计算参考曲线"))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HormoneChartState.Loading)
}
