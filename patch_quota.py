import re

# Update UserQuotaDao.kt
with open('app/src/main/java/com/aktarjabed/inbusiness/data/dao/UserQuotaDao.kt', 'r') as f:
    dao_content = f.read()

# Replace incrementUsage and reset methods with a single consumeQuota
old_dao_query = """    @Query(\"\"\"
        UPDATE user_quota
        SET dailyUsed = dailyUsed + 1,
            monthlyUsed = monthlyUsed + 1,
            updatedAt = :timestamp
        WHERE userId = :userId AND dailyUsed < :dailyCap AND monthlyUsed < :monthlyCap
    \"\"\")
    suspend fun incrementUsage(userId: String, dailyCap: Int, monthlyCap: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query(\"\"\"
        UPDATE user_quota
        SET dailyUsed = 0,
            lastResetEpochDay = :today,
            updatedAt = :timestamp
        WHERE userId = :userId
    \"\"\")
    suspend fun resetDaily(userId: String, today: Long, timestamp: Long = System.currentTimeMillis())

    @Query(\"\"\"
        UPDATE user_quota
        SET monthlyUsed = 0,
            lastMonthlyResetEpochDay = :monthStart,
            updatedAt = :timestamp
        WHERE userId = :userId
    \"\"\")
    suspend fun resetMonthly(userId: String, monthStart: Long, timestamp: Long = System.currentTimeMillis())"""

new_dao_query = """    @Query(\"\"\"
        UPDATE user_quota
        SET
            dailyUsed = CASE
                WHEN lastResetEpochDay < :today THEN 1
                ELSE dailyUsed + 1
            END,
            lastResetEpochDay = :today,
            monthlyUsed = CASE
                WHEN lastMonthlyResetEpochDay < :monthStart THEN 1
                ELSE monthlyUsed + 1
            END,
            lastMonthlyResetEpochDay = :monthStart,
            updatedAt = :timestamp
        WHERE userId = :userId
        AND (
            (lastResetEpochDay < :today OR dailyUsed < :dailyCap)
            AND
            (lastMonthlyResetEpochDay < :monthStart OR monthlyUsed < :monthlyCap)
        )
    \"\"\")
    suspend fun consumeQuotaAtomic(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query(\"\"\"
        UPDATE user_quota
        SET
            dailyUsed = CASE
                WHEN lastResetEpochDay < :today THEN 0
                ELSE dailyUsed
            END,
            lastResetEpochDay = :today,
            monthlyUsed = CASE
                WHEN lastMonthlyResetEpochDay < :monthStart THEN 0
                ELSE monthlyUsed
            END,
            lastMonthlyResetEpochDay = :monthStart,
            updatedAt = :timestamp
        WHERE userId = :userId
        AND (lastResetEpochDay < :today OR lastMonthlyResetEpochDay < :monthStart)
    \"\"\")
    suspend fun syncQuotaPeriods(userId: String, today: Long, monthStart: Long, timestamp: Long = System.currentTimeMillis())"""

dao_content = dao_content.replace(old_dao_query, new_dao_query)

# Update getQuotaStatus to handle rollover implicitly
old_status_query = """    @Query(\"\"\"
        SELECT CASE
            WHEN monthlyUsed >= :monthlyCap AND dailyUsed >= :dailyCap THEN 'BOTH_EXCEEDED'
            WHEN monthlyUsed >= :monthlyCap THEN 'MONTHLY_EXCEEDED'
            WHEN dailyUsed >= :dailyCap THEN 'DAILY_EXCEEDED'
            ELSE 'AVAILABLE'
        END
        FROM user_quota
        WHERE userId = :userId
    \"\"\")
    suspend fun getQuotaStatus(userId: String, dailyCap: Int, monthlyCap: Int): String?"""

new_status_query = """    @Query(\"\"\"
        SELECT CASE
            WHEN (lastMonthlyResetEpochDay >= :monthStart AND monthlyUsed >= :monthlyCap) AND (lastResetEpochDay >= :today AND dailyUsed >= :dailyCap) THEN 'BOTH_EXCEEDED'
            WHEN (lastMonthlyResetEpochDay >= :monthStart AND monthlyUsed >= :monthlyCap) THEN 'MONTHLY_EXCEEDED'
            WHEN (lastResetEpochDay >= :today AND dailyUsed >= :dailyCap) THEN 'DAILY_EXCEEDED'
            ELSE 'AVAILABLE'
        END
        FROM user_quota
        WHERE userId = :userId
    \"\"\")
    suspend fun getQuotaStatus(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int): String?"""
dao_content = dao_content.replace(old_status_query, new_status_query)

with open('app/src/main/java/com/aktarjabed/inbusiness/data/dao/UserQuotaDao.kt', 'w') as f:
    f.write(dao_content)

# Update QuotaGate.kt
with open('app/src/main/java/com/aktarjabed/inbusiness/domain/quota/QuotaGate.kt', 'r') as f:
    gate_content = f.read()

# Remove withContext(Dispatchers.IO)
gate_content = gate_content.replace('suspend fun assertQuota(userId: String, consume: Boolean = true): QuotaVerdict = withContext(Dispatchers.IO) {', 'suspend fun assertQuota(userId: String, consume: Boolean = true): QuotaVerdict {')
gate_content = gate_content.replace('return@withContext', 'return')

# Update createFirstQuota to re-read
old_create = """    private suspend fun createFirstQuota(userId: String, today: Long): UserQuotaEntity {
        val deviceTier = deviceClassifier.getDeviceTier(context)

        val entity = UserQuotaEntity(
            userId = userId,
            tier = "FREE",  // Everyone starts FREE
            dailyUsed = 0,
            lastResetEpochDay = today,
            monthlyUsed = 0,
            lastMonthlyResetEpochDay = clock.monthStartEpochDay(),
            watermark = true,
            retentionDays = if (isLaunchPeriod()) 60 else 30,
            freeExpiryEpochDay = today + 365,  // 1 year expiry
            deviceTier = deviceTier.name
        )

        dao.insertIfAbsent(entity)
        Log.i(TAG, "Created quota for $userId: tier=${deviceTier.name}")
        return entity
    }"""

new_create = """    private suspend fun createFirstQuota(userId: String, today: Long): UserQuotaEntity {
        val deviceTier = deviceClassifier.getDeviceTier(context)

        val entity = UserQuotaEntity(
            userId = userId,
            tier = "FREE",  // Everyone starts FREE
            dailyUsed = 0,
            lastResetEpochDay = today,
            monthlyUsed = 0,
            lastMonthlyResetEpochDay = clock.monthStartEpochDay(),
            watermark = true,
            retentionDays = if (isLaunchPeriod()) 60 else 30,
            freeExpiryEpochDay = today + 365,  // 1 year expiry
            deviceTier = deviceTier.name
        )

        dao.insertIfAbsent(entity)
        Log.i(TAG, "Created quota for $userId: tier=${deviceTier.name}")
        // Strictly read the persisted state even if it collided and was IGNORED
        return dao.getQuota(userId) ?: entity
    }"""
gate_content = gate_content.replace(old_create, new_create)


old_logic = """        val today = clock.todayEpochDay()
        var entity = dao.getQuota(userId) ?: createFirstQuota(userId, today)

        // Roll daily
        var needsUpdate = false
        if (entity.lastResetEpochDay != today) {
            dao.resetDaily(userId, today)
            needsUpdate = true
        }

        // Roll monthly
        val monthStart = clock.monthStartEpochDay()
        if (entity.lastMonthlyResetEpochDay != monthStart) {
            dao.resetMonthly(userId, monthStart)
            needsUpdate = true
        }

        if (needsUpdate) {
            entity = dao.getQuota(userId) ?: entity
        }

        // Check expiry
        val freeExpiry = entity.freeExpiryEpochDay
        if (freeExpiry != null && today > freeExpiry) {
            return QuotaVerdict.FreeExpired
        }

        // Get daily limit (hardcoded for Stage 1)
        val dailyCap = getDailyLimit(entity.tier) + getLaunchBonus(entity.tier)
        val monthlyCap = getMonthlyLimit(entity.tier)

        if (consume) {
            val rows = dao.incrementUsage(userId, dailyCap, monthlyCap)
            if (rows > 0) {
                val freshEntity = dao.getQuota(userId)!!
                QuotaVerdict.Allowed(dailyCap - freshEntity.dailyUsed)
            } else {
                val status = dao.getQuotaStatus(userId, dailyCap, monthlyCap)
                if (status == "MONTHLY_EXCEEDED" || status == "BOTH_EXCEEDED") {
                    Log.d(TAG, "Monthly cap hit (concurrent/SQL): monthly cap $monthlyCap")
                    QuotaVerdict.MonthlyCap
                } else {
                    Log.d(TAG, "Daily cap hit (concurrent/SQL): daily cap $dailyCap")
                    QuotaVerdict.DailyCap(dailyCap)
                }
            }
        } else {
            val status = dao.getQuotaStatus(userId, dailyCap, monthlyCap)
            if (status == "MONTHLY_EXCEEDED" || status == "BOTH_EXCEEDED") {
                QuotaVerdict.MonthlyCap
            } else if (status == "DAILY_EXCEEDED") {
                QuotaVerdict.DailyCap(dailyCap)
            } else {
                val peekEntity = dao.getQuota(userId) ?: entity
                QuotaVerdict.Allowed(dailyCap - peekEntity.dailyUsed)
            }
        }"""


new_logic = """        val today = clock.todayEpochDay()
        val monthStart = clock.monthStartEpochDay()
        val entity = dao.getQuota(userId) ?: createFirstQuota(userId, today)

        // Check expiry
        val freeExpiry = entity.freeExpiryEpochDay
        if (freeExpiry != null && today > freeExpiry) {
            return QuotaVerdict.FreeExpired
        }

        val dailyCap = getDailyLimit(entity.tier) + getLaunchBonus(entity.tier)
        val monthlyCap = getMonthlyLimit(entity.tier)

        if (consume) {
            val rows = dao.consumeQuotaAtomic(userId, today, monthStart, dailyCap, monthlyCap)
            if (rows > 0) {
                val freshEntity = dao.getQuota(userId)!!
                return QuotaVerdict.Allowed(dailyCap - freshEntity.dailyUsed)
            } else {
                val status = dao.getQuotaStatus(userId, today, monthStart, dailyCap, monthlyCap)
                if (status == "MONTHLY_EXCEEDED" || status == "BOTH_EXCEEDED") {
                    Log.d(TAG, "Monthly cap hit (concurrent/SQL): monthly cap $monthlyCap")
                    return QuotaVerdict.MonthlyCap
                } else {
                    Log.d(TAG, "Daily cap hit (concurrent/SQL): daily cap $dailyCap")
                    return QuotaVerdict.DailyCap(dailyCap)
                }
            }
        } else {
            // Non-consuming peek: just sync periods if needed so we don't return stale views
            dao.syncQuotaPeriods(userId, today, monthStart)
            val status = dao.getQuotaStatus(userId, today, monthStart, dailyCap, monthlyCap)
            if (status == "MONTHLY_EXCEEDED" || status == "BOTH_EXCEEDED") {
                return QuotaVerdict.MonthlyCap
            } else if (status == "DAILY_EXCEEDED") {
                return QuotaVerdict.DailyCap(dailyCap)
            } else {
                val peekEntity = dao.getQuota(userId) ?: entity
                return QuotaVerdict.Allowed(dailyCap - peekEntity.dailyUsed)
            }
        }"""
gate_content = gate_content.replace(old_logic, new_logic)


# One last fix for withContext brackets issue if any
if 'suspend fun assertQuota(userId: String, consume: Boolean = true): QuotaVerdict {' in gate_content:
    gate_content = gate_content.replace('}\n\n    private suspend fun createFirstQuota', '\n    private suspend fun createFirstQuota')


with open('app/src/main/java/com/aktarjabed/inbusiness/domain/quota/QuotaGate.kt', 'w') as f:
    f.write(gate_content)
