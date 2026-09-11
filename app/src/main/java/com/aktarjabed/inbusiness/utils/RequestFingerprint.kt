package com.aktarjabed.inbusiness.utils

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import java.security.MessageDigest
import java.util.Locale

object RequestFingerprint {
    fun generate(
        businessId: String,
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

        payload.append(businessId).append("|")
        payload.append(customerName.trim().lowercase(Locale.ROOT)).append("|")
        payload.append(customerGSTIN?.trim()?.lowercase(Locale.ROOT) ?: "").append("|")
        payload.append(buyerAddress.trim().lowercase(Locale.ROOT)).append("|")
        payload.append(supplyType.name).append("|")
        payload.append(String.format(Locale.US, "%.2f", subtotal)).append("|")
        payload.append(String.format(Locale.US, "%.2f", totalAmount)).append("|")
        payload.append(String.format(Locale.US, "%.2f", taxAmount)).append("|")
        payload.append(String.format(Locale.US, "%.2f", amountPaid)).append("|")
        payload.append(paymentMethod).append("|")

        for (item in items) {
            payload.append(item.productId ?: "").append(";")
            payload.append(item.description.trim().lowercase(Locale.ROOT)).append(";")
            payload.append(String.format(Locale.US, "%.2f", item.quantity)).append(";")
            payload.append(String.format(Locale.US, "%.2f", item.pricePerUnit)).append(";")
            payload.append(String.format(Locale.US, "%.2f", item.gstPercentage)).append(";")
            payload.append(item.unitType.trim().lowercase(Locale.ROOT)).append("|")
        }

        return hash(payload.toString())
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
