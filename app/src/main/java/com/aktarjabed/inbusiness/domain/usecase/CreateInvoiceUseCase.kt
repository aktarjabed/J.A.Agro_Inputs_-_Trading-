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
        sellerName: String,
        sellerAddress: String,
        sellerGSTIN: String?,
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        subtotal: Double,
        totalAmount: Double,
        taxAmount: Double,
        totalCgst: Double,
        totalSgst: Double,
        totalIgst: Double,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult {
        return invoiceRepository.createInvoice(
            sellerName = sellerName,
            sellerAddress = sellerAddress,
            sellerGSTIN = sellerGSTIN,
            customerName = customerName,
            customerGSTIN = customerGSTIN,
            buyerAddress = buyerAddress,
            supplyType = supplyType,
            subtotal = subtotal,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            totalCgst = totalCgst,
            totalSgst = totalSgst,
            totalIgst = totalIgst,
            items = items,
            idempotencyKey = idempotencyKey,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod
        )
    }
}
