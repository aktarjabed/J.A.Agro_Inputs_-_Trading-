package com.aktarjabed.inbusiness.utils

import kotlin.math.roundToLong

object AmountInWordsConverter {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convertAmountToWords(amount: Double): String {
        require(amount.isFinite()) { "Amount must be finite" }
        require(amount >= 0.0) { "Amount cannot be negative" }

        val totalPaise = (amount * 100).roundToLong()
        val rupees = totalPaise / 100
        val paise = totalPaise % 100

        val rupeesPart = if (rupees == 0L) {
            "Zero"
        } else {
            convert(rupees).trim()
        }

        return if (paise > 0L) {
            "Rupees $rupeesPart and ${convert(paise).trim()} Paise Only"
        } else {
            "Rupees $rupeesPart Only"
        }
    }

    private fun convert(n: Long): String {
        if (n == 0L) return ""
        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + (if (n % 10 != 0L) " " + units[(n % 10).toInt()] else "")
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred" + (if (n % 100 != 0L) " " + convert(n % 100) else "")
        if (n < 100000) return convert(n / 1000) + " Thousand" + (if (n % 1000 != 0L) " " + convert(n % 1000) else "")
        if (n < 10000000) return convert(n / 100000) + " Lakh" + (if (n % 100000 != 0L) " " + convert(n % 100000) else "")
        return convert(n / 10000000) + " Crore" + (if (n % 10000000 != 0L) " " + convert(n % 10000000) else "")
    }
}
