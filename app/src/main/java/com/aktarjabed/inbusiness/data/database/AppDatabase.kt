package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.converters.Converters
import com.aktarjabed.inbusiness.data.dao.*
import com.aktarjabed.inbusiness.data.entities.*
import com.aktarjabed.inbusiness.security.KeyProvider
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        BusinessData::class,
        Invoice::class,
        InvoiceItem::class,
        CalculationResult::class,
        UserQuotaEntity::class,
        InvoiceSequence::class,
        Product::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun userQuotaDao(): UserQuotaDao
    abstract fun productDao(): ProductDao

    companion object {
        private const val DATABASE_NAME = "inbusiness_ultra.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, keyProvider)
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure foreign keys are turned off during migration
                db.execSQL("PRAGMA foreign_keys=OFF")

                // Create the invoice sequence table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_sequences` (
                        `businessId` TEXT NOT null,
                        `currentNumber` INTEGER NOT null,
                        PRIMARY KEY(`businessId`)
                    )
                """)

                // Drop and recreate invoices table with composite unique index
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoices_new` (
                        `id` TEXT NOT null,
                        `businessId` TEXT NOT null,
                        `invoiceNumber` TEXT NOT null,
                        `customerId` TEXT NOT null,
                        `customerName` TEXT NOT null,
                        `customerGSTIN` TEXT,
                        `totalAmount` REAL NOT null,
                        `taxAmount` REAL NOT null,
                        `createdAt` INTEGER NOT null,
                        `updatedAt` INTEGER NOT null,
                        `irn` TEXT,
                        `ackNo` TEXT,
                        `ackDate` INTEGER,
                        `qrCodeData` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("INSERT INTO invoices_new SELECT * FROM invoices")
                db.execSQL("DROP TABLE invoices")
                db.execSQL("ALTER TABLE invoices_new RENAME TO invoices")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_businessId_invoiceNumber` ON `invoices` (`businessId`, `invoiceNumber`)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN idempotencyKey TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_idempotencyKey` ON `invoices` (`idempotencyKey`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_businessId_name_brand_category_unitType_batchNumber` ON `products` (`businessId`, `name`, `brand`, `category`, `unitType`, `batchNumber`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN buyerAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalCgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalSgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalIgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN supplyType TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE invoice_items ADD COLUMN gstPercentage REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN taxAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN totalAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN productId INTEGER DEFAULT NULL")
            }
        }

        private fun buildDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            val passphrase = keyProvider.getDatabasePassphrase()
            val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
            val factory = SupportFactory(passphraseBytes)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(DatabaseCallback())
                .build()
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }
}
