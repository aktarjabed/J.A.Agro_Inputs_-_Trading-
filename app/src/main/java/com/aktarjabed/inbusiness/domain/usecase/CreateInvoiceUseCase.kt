package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val businessContext: BusinessContext
) {
    suspend operator fun invoke(
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult {
        return invoiceRepository.createInvoice(
            customerName = customerName,
            customerGSTIN = customerGSTIN,
            buyerAddress = buyerAddress,
            supplyType = supplyType,
            items = items,
            idempotencyKey = idempotencyKey,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod
        )
    }
}
