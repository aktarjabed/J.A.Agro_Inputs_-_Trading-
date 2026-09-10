package com.aktarjabed.inbusiness.domain.invoice

import java.math.BigDecimal
import java.math.RoundingMode

enum class SupplyType {
    INTRA_STATE,
    INTER_STATE,
    UNKNOWN
}

object GstCalculator {
    /**
     * Regex for validating Indian GSTIN format.
     */
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

    fun isValidGstin(gstin: String?): Boolean {
        if (gstin.isNullOrBlank()) return false
        return GSTIN_REGEX.matches(gstin.uppercase())
    }

    /**
     * Determines SupplyType by comparing the first two digits (state code) of GSTINs.
     * Fallbacks to UNKNOWN if either GSTIN is missing, invalid, or manual override is desired.
     */
    fun determineSupplyType(sellerGstin: String?, buyerGstin: String?): SupplyType {
        if (sellerGstin.isNullOrBlank() || buyerGstin.isNullOrBlank()) {
            return SupplyType.UNKNOWN
        }

        if (!isValidGstin(sellerGstin) || !isValidGstin(buyerGstin)) {
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
        val qty = BigDecimal.valueOf(quantity)
        val price = BigDecimal.valueOf(unitPrice)
        val gstPct = BigDecimal.valueOf(gstPercentage)

        // subTotal = round(quantity * rate, 2)
        val rawSubtotal = qty.multiply(price)
        val subtotal = rawSubtotal.setScale(2, RoundingMode.HALF_UP)

        // taxAmount = round(unroundedLineSubtotal * gst% / 100, 2)
        val rawTaxAmount = rawSubtotal.multiply(gstPct).divide(BigDecimal.valueOf(100.0))
        val taxAmount = rawTaxAmount.setScale(2, RoundingMode.HALF_UP)

        val totalAmount = subtotal.add(taxAmount)

        val (cgst, sgst, igst) = when (supplyType) {
            SupplyType.INTRA_STATE -> {
                // cgst = round(taxAmount / 2, 2)
                val cgstRaw = taxAmount.divide(BigDecimal.valueOf(2.0))
                val cgstRounded = cgstRaw.setScale(2, RoundingMode.HALF_UP)
                // sgst = taxAmount - cgst
                val sgstRounded = taxAmount.subtract(cgstRounded)
                Triple(cgstRounded.toDouble(), sgstRounded.toDouble(), 0.0)
            }
            SupplyType.INTER_STATE -> Triple(0.0, 0.0, taxAmount.toDouble())
            SupplyType.UNKNOWN -> throw IllegalArgumentException("SupplyType cannot be UNKNOWN during tax calculation")
        }

        return ItemTaxResult(
            subtotal = subtotal.toDouble(),
            taxAmount = taxAmount.toDouble(),
            totalAmount = totalAmount.toDouble(),
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
