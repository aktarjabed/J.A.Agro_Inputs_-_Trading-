package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.models.FinancialMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val repo: BusinessRepository
) : ViewModel() {

    private val _businessData = MutableStateFlow(BusinessData())
    val businessData: StateFlow<BusinessData> = _businessData.asStateFlow()

    private val _financialMetrics = MutableStateFlow(FinancialMetrics())
    val financialMetrics: StateFlow<FinancialMetrics> = _financialMetrics.asStateFlow()

    private val _savedScenarios = MutableStateFlow<List<BusinessData>>(emptyList())
    val savedScenarios: StateFlow<List<BusinessData>> = _savedScenarios.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMsg = MutableSharedFlow<String>()
    val errorMsg: SharedFlow<String> = _errorMsg.asSharedFlow()

    init {
        loadSavedScenarios()
        calculateMetrics() // initial calc
    }

    fun updateBusinessData(data: BusinessData) {
        _businessData.value = data
        calculateMetrics()
    }

    private fun calculateMetrics() {
        _financialMetrics.value = repo.calculateFinancialMetrics(_businessData.value)
    }

    fun saveScenario(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val data = _businessData.value.copy(
                    id = UUID.randomUUID().toString(),
                    scenarioName = name.ifBlank { "Scenario ${System.currentTimeMillis()}" }
                )
                repo.saveBusinessData(data)
                val metrics = _financialMetrics.value
                repo.saveCalculationResult(
                    CalculationResult(
                        id = UUID.randomUUID().toString(),
                        businessDataId = data.id,
                        grossProfit = metrics.grossProfit,
                        ebitda = metrics.ebitda,
                        netProfit = metrics.netProfit,
                        gstPayable = metrics.gstPayable,
                        breakEvenPoint = metrics.breakEvenPoint,
                        cashFlow = metrics.cashFlow,
                        grossMargin = metrics.grossMargin,
                        netMargin = metrics.netMargin,
                        operatingMargin = metrics.operatingMargin,
                        roi = metrics.roi
                    )
                )
                loadSavedScenarios()
            }.onFailure {
                _errorMsg.emit("Failed to save: ${it.localizedMessage}")
            }
            _isLoading.value = false
        }
    }

    fun loadScenario(data: BusinessData) {
        _businessData.value = data
        calculateMetrics()
    }

    fun deleteScenario(data: BusinessData) {
        viewModelScope.launch {
            runCatching {
                repo.deleteBusinessData(data)
                repo.deleteAllCalculationResults(data.id)
                loadSavedScenarios()
            }.onFailure {
                _errorMsg.emit("Delete failed: ${it.localizedMessage}")
            }
        }
    }

    private fun loadSavedScenarios() {
        viewModelScope.launch {
            repo.getAllBusinessData().collect { _savedScenarios.value = it }
        }
    }
}
