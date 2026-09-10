package com.aktarjabed.inbusiness.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInWordsConverterTest {

    @Test
    fun testZero() {
        assertEquals("Rupees Zero Only", AmountInWordsConverter.convertAmountToWords(0.0))
    }

    @Test
    fun testSimple() {
        assertEquals("Rupees One Hundred Five Only", AmountInWordsConverter.convertAmountToWords(105.0))
    }

    @Test
    fun testPaise() {
        assertEquals("Rupees Zero and Fifty Paise Only", AmountInWordsConverter.convertAmountToWords(0.50))
        assertEquals("Rupees One Hundred Five and Fifty Paise Only", AmountInWordsConverter.convertAmountToWords(105.50))
    }

    @Test
    fun testThousands() {
        assertEquals("Rupees Eight Thousand Eight Hundred Six Only", AmountInWordsConverter.convertAmountToWords(8806.0))
    }

    @Test
    fun testLakhsAndCrores() {
        assertEquals("Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six Only", AmountInWordsConverter.convertAmountToWords(123456.0))
        assertEquals("Rupees One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight Only", AmountInWordsConverter.convertAmountToWords(12345678.0))
    }
}
