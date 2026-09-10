package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(
        userId: String,
        businessId: String,
        customerName: String,
        totalAmount: Double,
        taxRate: Double,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null
    ): QuotaVerdict {
        return invoiceRepository.createInvoice(
            userId = userId,
            businessId = businessId,
            customerName = customerName,
            totalAmount = totalAmount,
            taxRate = taxRate,
            items = items,
            idempotencyKey = idempotencyKey
        )
    }
}
