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
        assert(cursor.moveToFirst())
        val idempotencyKeyIndex = cursor.getColumnIndex("idempotencyKey")
        assert(cursor.isNull(idempotencyKeyIndex))
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
        assert(cursor.moveToFirst())
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("pricePerUnit")) == 50.0)
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("subTotal")) == 100.0)
        assert(cursor.getString(cursor.getColumnIndexOrThrow("unitType")) == "")
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("gstPercentage")) == 5.0)
        cursor.close()
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
        assert(invCursor.moveToFirst())
        assert(invCursor.getDouble(invCursor.getColumnIndexOrThrow("amountPaid")) == 0.0)
        assert(invCursor.getDouble(invCursor.getColumnIndexOrThrow("balanceDue")) == 0.0)
        assert(invCursor.getString(invCursor.getColumnIndexOrThrow("paymentMethod")) == "NONE")
        invCursor.close()

        val seqCursor = db.query("SELECT lastSequenceNumber FROM invoice_sequence WHERE businessId = 'biz-1'")
        assert(seqCursor.moveToFirst())
        assert(seqCursor.getInt(seqCursor.getColumnIndexOrThrow("lastSequenceNumber")) == 42)
        seqCursor.close()
    }
}
