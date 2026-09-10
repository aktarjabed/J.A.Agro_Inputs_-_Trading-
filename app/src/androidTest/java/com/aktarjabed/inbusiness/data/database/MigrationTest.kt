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
        // Unfortunately, versions prior to 4 are not in the schemas folder since exportSchema was not enabled previously.
        // If 3.json was not generated, Room's MigrationTestHelper cannot create a DB at version 3 for testing.
        // We will attempt to start at version 4, migrate to 5, and validate.

        var db = helper.createDatabase(TEST_DB, 4)

        // Insert some dummy data to verify it survives migration
        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        // Prepare for the next version
        db.close()

        // Re-open the database with version 5 and provide the MIGRATION_4_5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        // Check if the data is still there and schema constraints match
        val cursor = db.query("SELECT * FROM invoices")
        assert(cursor.moveToFirst())
        val idempotencyKeyIndex = cursor.getColumnIndex("idempotencyKey")
        assert(cursor.isNull(idempotencyKeyIndex)) // Initialized to null for existing rows
        cursor.close()

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        var db = helper.createDatabase(TEST_DB, 7)

        // Insert invoice so foreign key succeeds
        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        // Insert old invoice item format (v7 schema)
        db.execSQL("""
            INSERT INTO invoice_items (id, invoiceId, description, quantity, unitPrice, taxRate, amount, gstPercentage, taxAmount, totalAmount, productId)
            VALUES ('item-1', 'inv-1', 'Test Item', 2.0, 50.0, 5.0, 100.0, 0.0, 5.0, 105.0, 123)
        """)

        db.close()

        // Migrate to 8
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8)

        val cursor = db.query("SELECT * FROM invoice_items WHERE id = 'item-1'")
        assert(cursor.moveToFirst())

        // Assert field renames and value mappings
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("pricePerUnit")) == 50.0)
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("subTotal")) == 100.0)
        assert(cursor.getString(cursor.getColumnIndexOrThrow("unitType")) == "")
        // GST percentage should be taken from taxRate since gstPercentage was 0.0
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("gstPercentage")) == 5.0)

        cursor.close()
    }

}

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        var db = helper.createDatabase(TEST_DB, 7)

        // Insert invoice so foreign key succeeds
        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, customerId, customerName, totalAmount, taxAmount, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 'cust-1', 'Test Customer', 100.0, 10.0, 1000000, 1000000)
        """)

        // Insert old invoice item format (v7 schema)
        db.execSQL("""
            INSERT INTO invoice_items (id, invoiceId, description, quantity, unitPrice, taxRate, amount, gstPercentage, taxAmount, totalAmount, productId)
            VALUES ('item-1', 'inv-1', 'Test Item', 2.0, 50.0, 5.0, 100.0, 0.0, 5.0, 105.0, 123)
        """)

        db.close()

        // Migrate to 8
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, AppDatabase.MIGRATION_7_8)

        val cursor = db.query("SELECT * FROM invoice_items WHERE id = 'item-1'")
        assert(cursor.moveToFirst())

        // Assert field renames and value mappings
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("pricePerUnit")) == 50.0)
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("subTotal")) == 100.0)
        assert(cursor.getString(cursor.getColumnIndexOrThrow("unitType")) == "")
        // GST percentage should be taken from taxRate since gstPercentage was 0.0
        assert(cursor.getDouble(cursor.getColumnIndexOrThrow("gstPercentage")) == 5.0)

        cursor.close()
    }

}

// Wait, I shouldn't append blindly. Let's write a python script.
