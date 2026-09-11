package com.aktarjabed.inbusiness.presentation.screens.invoice_preview

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.domain.usecase.GetInvoiceForPreviewUseCase
import com.aktarjabed.inbusiness.domain.usecase.PreviewResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InvoicePreviewUiState {
    object Loading : InvoicePreviewUiState()
    data class Success(val invoice: Invoice, val items: List<InvoiceItem>) : InvoicePreviewUiState()
    data class Error(val message: String) : InvoicePreviewUiState()
}

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    private val getInvoiceForPreviewUseCase: GetInvoiceForPreviewUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _uiState = MutableStateFlow<InvoicePreviewUiState>(InvoicePreviewUiState.Loading)
    val uiState: StateFlow<InvoicePreviewUiState> = _uiState.asStateFlow()

    init {
        loadInvoice()
    }

    private fun loadInvoice() {
        viewModelScope.launch {
            try {
                val result = getInvoiceForPreviewUseCase(invoiceId)
                when(result) {
                    is PreviewResult.Success -> {
                        _uiState.value = InvoicePreviewUiState.Success(result.invoice, result.items)
                    }
                    is PreviewResult.Error -> {
                        _uiState.value = InvoicePreviewUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e("InvoicePreviewViewModel", "Failed to load invoice", e)
                _uiState.value = InvoicePreviewUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
