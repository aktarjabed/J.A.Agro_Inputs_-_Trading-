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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(quota: UserQuotaEntity)

    @Query("""
        UPDATE user_quota
        SET dailyUsed = dailyUsed + 1,
            monthlyUsed = monthlyUsed + 1,
            updatedAt = :timestamp
        WHERE userId = :userId AND dailyUsed < :dailyCap AND monthlyUsed < :monthlyCap
    """)
    suspend fun incrementUsage(userId: String, dailyCap: Int, monthlyCap: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query("""
        UPDATE user_quota
        SET dailyUsed = 0,
            lastResetEpochDay = :today,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun resetDaily(userId: String, today: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE user_quota
        SET monthlyUsed = 0,
            lastMonthlyResetEpochDay = :monthStart,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun resetMonthly(userId: String, monthStart: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT CASE
            WHEN monthlyUsed >= :monthlyCap AND dailyUsed >= :dailyCap THEN 'BOTH_EXCEEDED'
            WHEN monthlyUsed >= :monthlyCap THEN 'MONTHLY_EXCEEDED'
            WHEN dailyUsed >= :dailyCap THEN 'DAILY_EXCEEDED'
            ELSE 'AVAILABLE'
        END
        FROM user_quota
        WHERE userId = :userId
    """)
    suspend fun getQuotaStatus(userId: String, dailyCap: Int, monthlyCap: Int): String?
}