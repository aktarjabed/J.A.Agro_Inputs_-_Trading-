package com.aktarjabed.inbusiness.domain.quota

import android.content.Context
import android.util.Log
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaGate @Inject constructor(
    private val dao: UserQuotaDao,
    private val deviceClassifier: DeviceClassifier,
    private val clock: SystemClock,
    @ApplicationContext private val context: Context
) {

    suspend fun assertQuota(userId: String, consume: Boolean = true): QuotaVerdict = withContext(Dispatchers.IO) {
        val today = clock.todayEpochDay()
        val entity = dao.getQuota(userId) ?: createFirstQuota(userId, today)

        // Roll daily
        if (entity.lastResetEpochDay != today) {
            dao.resetDaily(userId, today)
            return@withContext assertQuota(userId, consume)
        }

        // Roll monthly
        val monthStart = clock.monthStartEpochDay()
        if (entity.lastMonthlyResetEpochDay != monthStart) {
            dao.resetMonthly(userId, monthStart)
            return@withContext assertQuota(userId, consume)
        }

        // Check expiry
        if (entity.freeExpiryEpochDay != null && today > entity.freeExpiryEpochDay) {
            return@withContext QuotaVerdict.FreeExpired
        }

        // Get daily limit (hardcoded for Stage 1)
        val dailyCap = getDailyLimit(entity.tier) + getLaunchBonus(entity.tier)

        // Check quotas
        if (entity.tier == "FREE" && entity.monthlyUsed >= 60) {
            Log.d(TAG, "Monthly cap hit: ${entity.monthlyUsed}/60")
            return@withContext QuotaVerdict.MonthlyCap
        }

        if (consume) {
            val rows = dao.incrementUsage(userId, dailyCap)
            if (rows > 0) {
                QuotaVerdict.Allowed(dailyCap - entity.dailyUsed - 1)
            } else {
                Log.d(TAG, "Daily cap hit (concurrent/SQL): ${entity.dailyUsed}/$dailyCap")
                QuotaVerdict.DailyCap(dailyCap)
            }
        } else {
            if (entity.dailyUsed >= dailyCap) {
                Log.d(TAG, "Daily cap hit (peek): ${entity.dailyUsed}/$dailyCap")
                QuotaVerdict.DailyCap(dailyCap)
            } else {
                QuotaVerdict.Allowed(dailyCap - entity.dailyUsed)
            }
        }
    }

    private suspend fun createFirstQuota(userId: String, today: Long): UserQuotaEntity {
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

        dao.insertOrReplace(entity)
        Log.i(TAG, "Created quota for $userId: tier=${deviceTier.name}")
        return entity
    }

    // Hardcoded for Stage 1 (will be Remote Config in Stage 4)
    private fun getDailyLimit(tier: String): Int {
        return when (tier) {
            "FREE" -> 2
            "BASIC", "PRO", "ENTERPRISE" -> Int.MAX_VALUE
            else -> 2
        }
    }

    private fun getLaunchBonus(tier: String): Int {
        if (tier != "FREE") return 0
        return if (isLaunchPeriod()) 1 else 0
    }

    // Hardcoded launch end date (will be Remote Config in Stage 4)
    private fun isLaunchPeriod(): Boolean {
        val launchEnd = LocalDate.of(2026, 11, 20)
        return clock.today() <= launchEnd
    }

    companion object {
        private const val TAG = "QuotaGate"
    }
}