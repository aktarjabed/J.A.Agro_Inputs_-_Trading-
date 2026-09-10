package com.aktarjabed.inbusiness.domain.invoice

import org.junit.Assert.assertEquals
import org.junit.Test

class GstCalculatorTest {

    @Test
    fun testIntraStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(5.03, result.cgstAmount, 0.0001)
        assertEquals(5.02, result.sgstAmount, 0.0001)
        assertEquals(0.0, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }

    @Test
    fun testInterStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(0.0, result.cgstAmount, 0.0001)
        assertEquals(0.0, result.sgstAmount, 0.0001)
        assertEquals(10.05, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }
}
