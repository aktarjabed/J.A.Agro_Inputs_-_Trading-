with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'r') as f:
    content = f.read()

import re

# Add migration to indices logic for uniqueness
migration_4_5 = """        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN idempotencyKey TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_idempotencyKey` ON `invoices` (`idempotencyKey`)")
            }
        }"""

content = re.sub(r'        val MIGRATION_4_5 = object : Migration\(4, 5\) \{[\s\S]*?\}', migration_4_5, content)


with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'w') as f:
    f.write(content)
