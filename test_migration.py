with open('app/src/androidTest/java/com/aktarjabed/inbusiness/data/database/MigrationTest.kt', 'r') as f:
    content = f.read()

new_test = """
    @Test
    fun migrate11To12() {
        val db = helper.createDatabase(TEST_DB, 11)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 12, true, AppDatabase.Companion.MIGRATION_11_12)
        migratedDb.close()
    }

    @Test
    fun migrate12To13() {
        val db = helper.createDatabase(TEST_DB, 12)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 13, true, AppDatabase.Companion.MIGRATION_12_13)
        migratedDb.close()
    }

    @Test
    fun migrate11To13() {
        val db = helper.createDatabase(TEST_DB, 11)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 13, true, AppDatabase.Companion.MIGRATION_11_12, AppDatabase.Companion.MIGRATION_12_13)
        migratedDb.close()
    }
"""

import re
content = re.sub(r'    @Test\n    fun migrate11To12\(\) \{.*?\n    \}', new_test, content, flags=re.DOTALL)

with open('app/src/androidTest/java/com/aktarjabed/inbusiness/data/database/MigrationTest.kt', 'w') as f:
    f.write(content)
