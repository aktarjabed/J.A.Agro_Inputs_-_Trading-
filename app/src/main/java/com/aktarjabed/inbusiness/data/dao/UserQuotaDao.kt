package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.UserQuotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserQuotaDao {

    @Query("SELECT * FROM user_quota WHERE userId = :userId LIMIT 1")
    suspend fun getQuota(userId: String): UserQuotaEntity?

    @Query("SELECT * FROM user_quota WHERE userId = :userId LIMIT 1")
    fun getQuotaFlow(userId: String): Flow<UserQuotaEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(quota: UserQuotaEntity)

    @Query("""
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
    """)
    suspend fun consumeQuotaAtomic(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query("""
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
    """)
    suspend fun syncQuotaPeriods(userId: String, today: Long, monthStart: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT CASE
            WHEN (lastMonthlyResetEpochDay >= :monthStart AND monthlyUsed >= :monthlyCap) AND (lastResetEpochDay >= :today AND dailyUsed >= :dailyCap) THEN 'BOTH_EXCEEDED'
            WHEN (lastMonthlyResetEpochDay >= :monthStart AND monthlyUsed >= :monthlyCap) THEN 'MONTHLY_EXCEEDED'
            WHEN (lastResetEpochDay >= :today AND dailyUsed >= :dailyCap) THEN 'DAILY_EXCEEDED'
            ELSE 'AVAILABLE'
        END
        FROM user_quota
        WHERE userId = :userId
    """)
    suspend fun getQuotaStatus(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int): String?
}