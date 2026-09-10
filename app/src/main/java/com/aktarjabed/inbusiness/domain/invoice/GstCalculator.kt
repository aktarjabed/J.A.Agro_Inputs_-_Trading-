package com.aktarjabed.inbusiness.domain.invoice

enum class SupplyType {
    INTRA_STATE,
    INTER_STATE,
    UNKNOWN
}

object GstCalculator {
    /**
     * Determines SupplyType by comparing the first two digits (state code) of GSTINs.
     * Fallbacks to UNKNOWN if either GSTIN is missing, too short, or manual override is desired.
     */
    fun determineSupplyType(sellerGstin: String?, buyerGstin: String?): SupplyType {
        if (sellerGstin.isNullOrBlank() || buyerGstin.isNullOrBlank()) {
            return SupplyType.UNKNOWN
        }

        if (sellerGstin.length < 2 || buyerGstin.length < 2) {
            return SupplyType.UNKNOWN
        }

        val sellerState = sellerGstin.substring(0, 2)
        val buyerState = buyerGstin.substring(0, 2)

        return if (sellerState == buyerState) {
            SupplyType.INTRA_STATE
        } else {
            SupplyType.INTER_STATE
        }
    }

    /**
     * Calculates tax amounts for a single item based on its subtotal (qty * price) and GST percentage.
     * Rounding to 2 decimal places is commonly expected for currency.
     */
fun calculateItemTaxes(
        quantity: Double,
        unitPrice: Double,
        gstPercentage: Double,
        supplyType: SupplyType
    ): ItemTaxResult {
        // subTotal = round(quantity * rate, 2)
        val rawSubtotal = quantity * unitPrice
        val subtotal = Math.round(rawSubtotal * 100.0) / 100.0

        // taxAmount = round(unroundedLineSubtotal * gst% / 100, 2)
        val rawTaxAmount = (rawSubtotal * gstPercentage) / 100.0
        val taxAmount = Math.round(rawTaxAmount * 100.0) / 100.0

        val totalAmount = subtotal + taxAmount

        val (cgst, sgst, igst) = when (supplyType) {
            SupplyType.INTRA_STATE -> {
                // cgst = round(taxAmount / 2, 2)
                val cgstRaw = taxAmount / 2.0
                val cgstRounded = Math.round(cgstRaw * 100.0) / 100.0
                // sgst = taxAmount - cgst
                val sgstRounded = taxAmount - cgstRounded
                Triple(cgstRounded, sgstRounded, 0.0)
            }
            SupplyType.INTER_STATE -> Triple(0.0, 0.0, taxAmount)
            SupplyType.UNKNOWN -> Triple(0.0, 0.0, 0.0) // Must be resolved before final calculation
        }

        return ItemTaxResult(
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = igst
        )
    }

    data class ItemTaxResult(
        val subtotal: Double,
        val taxAmount: Double,
        val totalAmount: Double,
        val cgstAmount: Double,
        val sgstAmount: Double,
        val igstAmount: Double
    )
}
