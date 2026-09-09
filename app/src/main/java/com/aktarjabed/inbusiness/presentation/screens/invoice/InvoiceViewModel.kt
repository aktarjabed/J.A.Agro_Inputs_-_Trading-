package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate,
    private val invoiceDao: InvoiceDao, // Used for peek/generate invoice number preview if needed
    private val invoiceRepository: InvoiceRepository,
    private val businessContext: BusinessContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Initial)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    fun checkQuotaAndPrepare() {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading

            try {
                val currentUserId = businessContext.currentUserId.first()
                val currentBusinessId = businessContext.activeBusinessId.first()

                // Peek without consuming
                val verdict = quotaGate.assertQuota(currentUserId, consume = false)

                when (verdict) {
                    is QuotaVerdict.Allowed -> {
                        val nextInvoiceNumber = generateInvoiceNumberPreview(currentBusinessId)
                        _uiState.value = InvoiceUiState.CreateAllowed(
                            remainingToday = verdict.remaining,
                            invoiceNumber = nextInvoiceNumber
                        )
                        Log.d(TAG, "Quota check passed. Remaining: \${verdict.remaining}")
                    }

                    is QuotaVerdict.DailyCap -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Daily quota exceeded. Limit: \${verdict.limit}")
                    }

                    is QuotaVerdict.MonthlyCap -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Monthly quota exceeded")
                    }

                    is QuotaVerdict.FreeExpired -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Free tier expired")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking quota", e)
                _uiState.value = InvoiceUiState.Error("Failed to check quota: \${e.message}")
            }
        }
    }

    fun createInvoice(
        customerName: String,
        totalAmount: Double,
        taxRate: Double,
        items: List<InvoiceItem> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading

            try {
                val currentUserId = businessContext.currentUserId.first()
                val currentBusinessId = businessContext.activeBusinessId.first()

                val finalVerdict = invoiceRepository.createInvoice(
                    userId = currentUserId,
                    businessId = currentBusinessId,
                    customerName = customerName,
                    totalAmount = totalAmount,
                    taxRate = taxRate,
                    items = items
                )

                if (finalVerdict !is QuotaVerdict.Allowed) {
                    _uiState.value = InvoiceUiState.QuotaBlocked(finalVerdict)
                    return@launch
                }

                _uiState.value = InvoiceUiState.Success(
                    invoiceId = "Generated internally",
                    message = "Invoice created successfully"
                )

                Log.i(TAG, "Invoice created. Remaining quota: \${finalVerdict.remaining}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invoice", e)
                _uiState.value = InvoiceUiState.Error("Failed to create invoice: \${e.message}")
            }
        }
    }

    private suspend fun generateInvoiceNumberPreview(businessId: String): String {
        val currentSeq = invoiceDao.getInvoiceSequence(businessId)
        val nextSeqNumber = (currentSeq?.lastSequenceNumber ?: 0) + 1
        return "INV-\${String.format(\"%05d\", nextSeqNumber)}"
    }

    fun resetState() {
        _uiState.value = InvoiceUiState.Initial
    }

    companion object {
        private const val TAG = "InvoiceViewModel"
    }
}
