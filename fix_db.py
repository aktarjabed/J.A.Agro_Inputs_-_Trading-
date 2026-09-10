with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'r') as f:
    content = f.read()

import re

# Bump version
content = content.replace('version = 6,', 'version = 7,')

# Add migration
migration_6_7 = """        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(\"\"\"
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
                \"\"\")
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
        }"""

content = re.sub(r'        val MIGRATION_5_6 = object : Migration\(5, 6\) \{[\s\S]*?\}', migration_6_7, content)

# Add migration to builder
content = content.replace('.addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)', '.addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)')

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'w') as f:
    f.write(content)
