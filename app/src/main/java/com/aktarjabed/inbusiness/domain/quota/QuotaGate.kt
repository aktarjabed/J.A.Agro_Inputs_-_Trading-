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

    suspend fun assertQuota(userId: String, consume: Boolean = true): QuotaVerdict {
        val today = clock.todayEpochDay()
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

        dao.insertIfAbsent(entity)
        Log.i(TAG, "Created quota for $userId: tier=${deviceTier.name}")
        // Strictly read the persisted state even if it collided and was IGNORED
        return dao.getQuota(userId) ?: entity
    }

    // Hardcoded for Stage 1 (will be Remote Config in Stage 4)
    private fun getDailyLimit(tier: String): Int {
        return when (tier) {
            "FREE" -> 2
            "BASIC", "PRO", "ENTERPRISE" -> Int.MAX_VALUE
            else -> 2
        }
    }

    private fun getMonthlyLimit(tier: String): Int {
        return when (tier) {
            "FREE" -> 60
            "BASIC", "PRO", "ENTERPRISE" -> Int.MAX_VALUE
            else -> 60
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