with open('./app/src/main/java/com/aktarjabed/inbusiness/domain/usecase/CreateInvoiceUseCase.kt', 'w') as f:
    f.write("""package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(
        userId: String,
        businessId: String,
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        totalAmount: Double,
        taxAmount: Double,
        totalCgst: Double,
        totalSgst: Double,
        totalIgst: Double,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null
    ): InvoiceCreationResult {
        return invoiceRepository.createInvoice(
            userId = userId,
            businessId = businessId,
            customerName = customerName,
            customerGSTIN = customerGSTIN,
            buyerAddress = buyerAddress,
            supplyType = supplyType,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            totalCgst = totalCgst,
            totalSgst = totalSgst,
            totalIgst = totalIgst,
            items = items,
            idempotencyKey = idempotencyKey
        )
    }
}
""")
