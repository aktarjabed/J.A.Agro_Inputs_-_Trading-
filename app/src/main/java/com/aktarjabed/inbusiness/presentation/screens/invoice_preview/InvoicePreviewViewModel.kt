package com.aktarjabed.inbusiness.presentation.screens.invoice_preview

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InvoicePreviewUiState {
    object Loading : InvoicePreviewUiState()
    data class Success(val business: BusinessData, val invoice: Invoice, val items: List<InvoiceItem>) : InvoicePreviewUiState()
    data class Error(val message: String) : InvoicePreviewUiState()
}

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val businessDao: BusinessDao,
    private val businessContext: BusinessContext,
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
                val currentBusinessId = businessContext.activeBusinessId.first()
                val invoice = invoiceDao.getInvoiceById(invoiceId, currentBusinessId)

                if (invoice == null) {
                    _uiState.value = InvoicePreviewUiState.Error("Invoice not found")
                    return@launch
                }

                val items = invoiceDao.getInvoiceItems(invoiceId)
                val business = businessDao.getBusinessDataById(currentBusinessId) ?: return@launch
                _uiState.value = InvoicePreviewUiState.Success(business, invoice, items)
            } catch (e: Exception) {
                Log.e("InvoicePreviewViewModel", "Failed to load invoice", e)
                _uiState.value = InvoicePreviewUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
