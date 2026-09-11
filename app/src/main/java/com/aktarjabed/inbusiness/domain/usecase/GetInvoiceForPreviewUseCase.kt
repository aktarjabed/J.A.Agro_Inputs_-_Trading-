package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetInvoiceForPreviewUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val businessRepository: BusinessRepository,
    private val businessContext: BusinessContext
) {
    suspend operator fun invoke(invoiceId: String): PreviewResult {
        val invoice = invoiceRepository.getInvoiceById(invoiceId)
            ?: return PreviewResult.Error("Invoice not found or access denied")

        val items = invoiceRepository.getInvoiceItems(invoiceId)

        return PreviewResult.Success(invoice, items)
    }
}

sealed class PreviewResult {
    data class Success(val invoice: Invoice, val items: List<InvoiceItem>) : PreviewResult()
    data class Error(val message: String) : PreviewResult()
}
