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
class ProductMigrationTest {
    private val TEST_DB = "migration-test-product"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // Create database at version 5
        var db = helper.createDatabase(TEST_DB, 5)

        // Close the database
        db.close()

        // Re-open the database with version 6 and provide MIGRATION_5_6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6)

        // Check if the products table exists and we can insert into it
        db.execSQL("""
            INSERT INTO products (businessId, name, brand, category, unitType, pricePerUnit, availableStock, batchNumber, isWholesaleOnly)
            VALUES ('biz-1', 'Product A', 'Brand A', 'Cat A', 'kg', 10.5, 100.0, 'B1', 0)
        """)

        val cursor = db.query("SELECT * FROM products")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndexOrThrow("name")) == "Product A")
        cursor.close()
    }
}
