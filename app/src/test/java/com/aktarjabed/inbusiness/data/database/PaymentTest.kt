package com.aktarjabed.inbusiness.data.database

import org.junit.Assert.*
import org.junit.Test

class PaymentTest {

    @Test
    fun testPaymentOverpaymentRejection() {
        val amountPaid = 150.0
        val totalAmount = 100.0

        val balanceDue = totalAmount - amountPaid
        assertTrue("Overpayment detected", amountPaid > totalAmount)
        assertEquals(-50.0, balanceDue, 0.0)
    }

    @Test
    fun testPaymentPartialAndFull() {
        val totalAmount = 1000.0
        val amountPaidFull = 1000.0
        val balanceDueFull = totalAmount - amountPaidFull
        assertEquals(0.0, balanceDueFull, 0.0)

        val amountPaidPartial = 400.5
        val balanceDuePartial = totalAmount - amountPaidPartial
        assertEquals(599.5, balanceDuePartial, 0.0)

        val amountPaidZero = 0.0
        val balanceDueZero = totalAmount - amountPaidZero
        assertEquals(1000.0, balanceDueZero, 0.0)
    }
}
