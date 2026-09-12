package com.aktarjabed.inbusiness.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4To5() {
        var db = helper.createDatabase(TEST_DB, 4)

        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        val cursor = db.query("SELECT * FROM invoices")
        org.junit.Assert.assertTrue(cursor.moveToFirst())
        val idempotencyKeyIndex = cursor.getColumnIndex("idempotencyKey")
        org.junit.Assert.assertTrue(cursor.isNull(idempotencyKeyIndex))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        var db = helper.createDatabase(TEST_DB, 7)

        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        db.execSQL("""
            INSERT INTO invoice_items (id, invoiceId, description, quantity, unitPrice, taxRate, amount, gstPercentage, taxAmount, totalAmount, productId)
            VALUES ('item-1', 'inv-1', 'Test Item', 2.0, 50.0, 5.0, 100.0, 0.0, 5.0, 105.0, 123)
        """)

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8)

        val cursor = db.query("SELECT * FROM invoice_items WHERE id = 'item-1'")
        org.junit.Assert.assertTrue(cursor.moveToFirst())
        org.junit.Assert.assertEquals(50.0, cursor.getDouble(cursor.getColumnIndexOrThrow("pricePerUnit")), 0.001)
        org.junit.Assert.assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("subTotal")), 0.001)
        org.junit.Assert.assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("unitType")))
        org.junit.Assert.assertEquals(5.0, cursor.getDouble(cursor.getColumnIndexOrThrow("gstPercentage")), 0.001)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate9To10To11() {
        var db = helper.createDatabase(TEST_DB, 9)

        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt, idempotencyKey)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000, 'test-key')
        """)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 10, true, AppDatabase.MIGRATION_9_10)

        var invCursor = db.query("SELECT requestFingerprint, sellerName, sellerAddress, sellerGSTIN, subtotal FROM invoices WHERE id = 'inv-1'")
        org.junit.Assert.assertTrue(invCursor.moveToFirst())
        org.junit.Assert.assertEquals("", invCursor.getString(invCursor.getColumnIndexOrThrow("sellerName")))
        org.junit.Assert.assertEquals("", invCursor.getString(invCursor.getColumnIndexOrThrow("sellerAddress")))
        org.junit.Assert.assertTrue(invCursor.isNull(invCursor.getColumnIndexOrThrow("sellerGSTIN")))
        org.junit.Assert.assertEquals(0.0, invCursor.getDouble(invCursor.getColumnIndexOrThrow("subtotal")), 0.001)
        invCursor.close()

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 11, true, AppDatabase.MIGRATION_10_11)

        invCursor = db.query("SELECT * FROM invoices WHERE id = 'inv-1'")
        org.junit.Assert.assertTrue(invCursor.moveToFirst())
        invCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        var db = helper.createDatabase(TEST_DB, 8)
        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS invoice_sequences (businessId TEXT NOT NULL PRIMARY KEY, currentNumber INTEGER NOT NULL)
        """)
        db.execSQL("INSERT INTO invoice_sequences (businessId, currentNumber) VALUES ('biz-1', 42)")

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, AppDatabase.MIGRATION_8_9)

        val invCursor = db.query("SELECT amountPaid, balanceDue, paymentMethod FROM invoices WHERE id = 'inv-1'")
        org.junit.Assert.assertTrue(invCursor.moveToFirst())
        org.junit.Assert.assertEquals(0.0, invCursor.getDouble(invCursor.getColumnIndexOrThrow("amountPaid")), 0.001)
        org.junit.Assert.assertEquals(0.0, invCursor.getDouble(invCursor.getColumnIndexOrThrow("balanceDue")), 0.001)
        org.junit.Assert.assertEquals("NONE", invCursor.getString(invCursor.getColumnIndexOrThrow("paymentMethod")))
        invCursor.close()

        val seqCursor = db.query("SELECT lastSequenceNumber FROM invoice_sequence WHERE businessId = 'biz-1'")
        org.junit.Assert.assertTrue(seqCursor.moveToFirst())
        org.junit.Assert.assertEquals(42, seqCursor.getInt(seqCursor.getColumnIndexOrThrow("lastSequenceNumber")))
        seqCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate11To12() {
        var db = helper.createDatabase(TEST_DB, 11)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `products` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `businessId` TEXT NOT NULL,
                `name` TEXT NOT NULL COLLATE NOCASE,
                `brand` TEXT NOT NULL COLLATE NOCASE,
                `category` TEXT NOT NULL COLLATE NOCASE,
                `unitType` TEXT NOT NULL COLLATE NOCASE,
                `pricePerUnit` REAL NOT NULL,
                `availableStock` REAL NOT NULL,
                `batchNumber` TEXT NOT NULL,
                `isWholesaleOnly` INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            INSERT INTO products (id, businessId, name, brand, category, unitType, pricePerUnit, availableStock, batchNumber, isWholesaleOnly)
            VALUES (1, 'biz-1', 'Test Product', 'Brand', 'Category', 'Unit', 100.0, 10.0, 'Batch-1', 0)
        """)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 12, true, AppDatabase.MIGRATION_11_12)

        val cursor = db.query("SELECT gstPercentage FROM products WHERE id = 1")
        org.junit.Assert.assertTrue(cursor.moveToFirst())
        org.junit.Assert.assertEquals(0.0, cursor.getDouble(cursor.getColumnIndexOrThrow("gstPercentage")), 0.001)
        cursor.close()
    }
}
