with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace('        val MIGRATION_4_5 = object : Migration(4, 5) {\n            override fun migrate(db: SupportSQLiteDatabase) {\n                db.execSQL("ALTER TABLE invoices ADD COLUMN idempotencyKey TEXT")\n                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_idempotencyKey` ON `invoices` (`idempotencyKey`)")\n            }\n        }\n        }', '        val MIGRATION_4_5 = object : Migration(4, 5) {\n            override fun migrate(db: SupportSQLiteDatabase) {\n                db.execSQL("ALTER TABLE invoices ADD COLUMN idempotencyKey TEXT")\n                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_idempotencyKey` ON `invoices` (`idempotencyKey`)")\n            }\n        }')

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/database/AppDatabase.kt', 'w') as f:
    f.write(content)
