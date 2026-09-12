package com.aktarjabed.inbusiness.domain.invoice

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class CalculateInvoiceTotalsUseCase @Inject constructor() {
    operator fun invoke(
        items: List<InvoiceItem>,
        supplyType: SupplyType,
        amountPaid: Double
    ): InvoiceCalculationResult {
        if (items.isEmpty()) {
            throw IllegalArgumentException("Invoice must have at least one item")
        }

        var totalSubtotal = BigDecimal.ZERO
        var totalTax = BigDecimal.ZERO
        var totalAmount = BigDecimal.ZERO
        var totalCgst = BigDecimal.ZERO
        var totalSgst = BigDecimal.ZERO
        var totalIgst = BigDecimal.ZERO

        val processedItems = items.map { item ->

            require(item.quantity.isFinite()) { "Quantity must be finite" }
            require(item.pricePerUnit.isFinite()) { "Price per unit must be finite" }
            require(item.gstPercentage.isFinite()) { "GST percentage must be finite" }
            if (item.description.isBlank()) throw IllegalArgumentException("Item description cannot be empty or blank")
            if (item.quantity <= 0) throw IllegalArgumentException("Quantity must be greater than zero")
            if (item.pricePerUnit < 0) throw IllegalArgumentException("Price per unit cannot be negative")
            if (item.gstPercentage < 0) throw IllegalArgumentException("GST percentage cannot be negative")

            val taxResult = GstCalculator.calculateItemTaxes(
                quantity = item.quantity,
                unitPrice = item.pricePerUnit,
                gstPercentage = item.gstPercentage,
                supplyType = supplyType
            )

            totalSubtotal = totalSubtotal.add(BigDecimal.valueOf(taxResult.subtotal))
            totalTax = totalTax.add(BigDecimal.valueOf(taxResult.taxAmount))
            totalAmount = totalAmount.add(BigDecimal.valueOf(taxResult.totalAmount))
            totalCgst = totalCgst.add(BigDecimal.valueOf(taxResult.cgstAmount))
            totalSgst = totalSgst.add(BigDecimal.valueOf(taxResult.sgstAmount))
            totalIgst = totalIgst.add(BigDecimal.valueOf(taxResult.igstAmount))

            item.copy(
                subTotal = taxResult.subtotal,
                taxAmount = taxResult.taxAmount,
                totalAmount = taxResult.totalAmount
            )
        }

        if (amountPaid < 0) throw IllegalArgumentException("Amount paid cannot be negative")

        val totalAmountDouble = totalAmount.setScale(2, RoundingMode.HALF_UP).toDouble()
        if (amountPaid > totalAmountDouble) {
            throw IllegalArgumentException("Amount paid cannot exceed total amount")
        }

        val balanceDue = BigDecimal.valueOf(totalAmountDouble).subtract(BigDecimal.valueOf(amountPaid)).setScale(2, RoundingMode.HALF_UP).toDouble()

        return InvoiceCalculationResult(
            processedItems = processedItems,
            subtotal = totalSubtotal.setScale(2, RoundingMode.HALF_UP).toDouble(),
            taxAmount = totalTax.setScale(2, RoundingMode.HALF_UP).toDouble(),
            totalAmount = totalAmountDouble,
            totalCgst = totalCgst.setScale(2, RoundingMode.HALF_UP).toDouble(),
            totalSgst = totalSgst.setScale(2, RoundingMode.HALF_UP).toDouble(),
            totalIgst = totalIgst.setScale(2, RoundingMode.HALF_UP).toDouble(),
            amountPaid = amountPaid,
            balanceDue = balanceDue
        )
    }
}

data class InvoiceCalculationResult(
    val processedItems: List<InvoiceItem>,
    val subtotal: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val totalCgst: Double,
    val totalSgst: Double,
    val totalIgst: Double,
    val amountPaid: Double,
    val balanceDue: Double
)
