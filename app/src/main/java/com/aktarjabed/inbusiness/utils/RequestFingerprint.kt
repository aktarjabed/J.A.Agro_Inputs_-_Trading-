package com.aktarjabed.inbusiness.utils

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import java.security.MessageDigest
import java.util.Locale

object RequestFingerprint {
    fun generate(
        businessId: String,
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
        items: List<InvoiceItem>,
        amountPaid: Double,
        paymentMethod: String
    ): String {
        val payload = StringBuilder()

        appendField(payload, businessId)
        appendField(payload, sellerName.trim().lowercase(Locale.ROOT))
        appendField(payload, sellerAddress.trim().lowercase(Locale.ROOT))
        appendField(payload, sellerGSTIN?.trim()?.lowercase(Locale.ROOT) ?: "")
        appendField(payload, customerName.trim().lowercase(Locale.ROOT))
        appendField(payload, customerGSTIN?.trim()?.lowercase(Locale.ROOT) ?: "")
        appendField(payload, buyerAddress.trim().lowercase(Locale.ROOT))
        appendField(payload, supplyType.name)
        appendField(payload, String.format(Locale.US, "%.2f", subtotal))
        appendField(payload, String.format(Locale.US, "%.2f", totalAmount))
        appendField(payload, String.format(Locale.US, "%.2f", taxAmount))
        appendField(payload, String.format(Locale.US, "%.2f", amountPaid))
        appendField(payload, paymentMethod)

        for (item in items) {
            appendField(payload, item.productId?.toString() ?: "")
            appendField(payload, item.description.trim().lowercase(Locale.ROOT))
            appendField(payload, String.format(Locale.US, "%.2f", item.quantity))
            appendField(payload, String.format(Locale.US, "%.2f", item.pricePerUnit))
            appendField(payload, String.format(Locale.US, "%.2f", item.gstPercentage))
            appendField(payload, item.unitType.trim().lowercase(Locale.ROOT))
        }

        return hash(payload.toString())
    }

    private fun appendField(sb: StringBuilder, value: String) {
        sb.append(value.length).append(":").append(value)
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
