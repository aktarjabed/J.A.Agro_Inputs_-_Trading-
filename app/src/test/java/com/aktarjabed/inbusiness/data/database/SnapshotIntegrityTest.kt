package com.aktarjabed.inbusiness.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product

class SnapshotIntegrityTest {

    @Test
    fun testSnapshotIntegrity() {
        val originalProduct = Product(
            id = 1L,
            businessId = "biz-1",
            name = "Fertilizer",
            brand = "Brand",
            category = "Cat",
            unitType = "Kg",
            pricePerUnit = 100.0,
            availableStock = 50.0
        )

        // Simulating the snapshotted item at the time of invoice creation
        val snapshotItem = InvoiceItem(
            id = "item-1",
            invoiceId = "inv-1",
            description = originalProduct.name,
            quantity = 5.0,
            pricePerUnit = originalProduct.pricePerUnit,
            unitType = originalProduct.unitType,
            subTotal = 500.0,
            gstPercentage = 5.0,
            taxAmount = 25.0,
            totalAmount = 525.0,
            productId = originalProduct.id
        )

        // Sometime later, the product's price and name change
        val updatedProduct = originalProduct.copy(
            name = "Fertilizer V2",
            pricePerUnit = 150.0
        )

        // The snapshot should remain absolutely unchanged
        assertEquals("Fertilizer", snapshotItem.description)
        assertEquals(100.0, snapshotItem.pricePerUnit, 0.0)
        assertEquals(5.0, snapshotItem.quantity, 0.0)
        assertEquals(525.0, snapshotItem.totalAmount, 0.0)
    }

    @Test
    fun testAdHocItem() {
        // An ad-hoc item has no product link
        val adHocItem = InvoiceItem(
            id = "item-2",
            invoiceId = "inv-1",
            description = "Custom Labour",
            quantity = 1.0,
            pricePerUnit = 500.0,
            unitType = "Day",
            subTotal = 500.0,
            gstPercentage = 0.0,
            taxAmount = 0.0,
            totalAmount = 500.0,
            productId = null
        )

        assertNull(adHocItem.productId)
        assertEquals("Custom Labour", adHocItem.description)
        assertEquals(500.0, adHocItem.totalAmount, 0.0)
    }
}
