package com.aktarjabed.inbusiness.domain.quota

import android.content.Context
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.LocalDate

class QuotaGateTest {

    // Fakes
    class FakeUserQuotaDao : UserQuotaDao {
        val quotas = mutableMapOf<String, UserQuotaEntity>()

        override suspend fun getQuota(userId: String): UserQuotaEntity? {
            return quotas[userId]
        }

        override fun getQuotaFlow(userId: String): Flow<UserQuotaEntity?> {
            return flowOf(quotas[userId])
        }

        override suspend fun insertOrReplace(quota: UserQuotaEntity) {
            quotas[quota.userId] = quota
        }

        override suspend fun incrementUsage(userId: String, timestamp: Long) {
            val q = quotas[userId]
            if (q != null) {
                quotas[userId] = q.copy(
                    dailyUsed = q.dailyUsed + 1,
                    monthlyUsed = q.monthlyUsed + 1
                )
            }
        }

        override suspend fun resetDaily(userId: String, today: Long, timestamp: Long) {
            val q = quotas[userId]
            if (q != null) {
                quotas[userId] = q.copy(dailyUsed = 0, lastResetEpochDay = today)
            }
        }

        override suspend fun resetMonthly(userId: String, monthStart: Long, timestamp: Long) {
            val q = quotas[userId]
            if (q != null) {
                quotas[userId] = q.copy(monthlyUsed = 0, lastMonthlyResetEpochDay = monthStart)
            }
        }
    }

    class TestSystemClock : SystemClock() {
        var forcedToday: LocalDate = LocalDate.of(2027, 1, 1) // Force after launch period

        override fun todayEpochDay(): Long = forcedToday.toEpochDay()
        override fun today(): LocalDate = forcedToday
        override fun monthStartEpochDay(): Long = forcedToday.withDayOfMonth(1).toEpochDay()
    }

    @Test
    fun testQuotaConsumptionLogic() = runBlocking {
        val dao = FakeUserQuotaDao()
        val clock = TestSystemClock()

        // Setup existing quota
        val userId = "test_user"
        val today = clock.todayEpochDay()
        val initialQuota = UserQuotaEntity(
            userId = userId,
            tier = "FREE",
            dailyUsed = 0,
            lastResetEpochDay = today,
            monthlyUsed = 0,
            lastMonthlyResetEpochDay = clock.monthStartEpochDay(),
            watermark = true,
            retentionDays = 30,
            freeExpiryEpochDay = today + 365,
            deviceTier = "LOW_END"
        )
        dao.insertOrReplace(initialQuota)

        val mockContext = mock(Context::class.java)
        val quotaGate = QuotaGate(dao, DeviceClassifier(), clock, mockContext)

        // 1. Simulate "Check Quota" (Entering screen) with consume=false
        val verdict1 = quotaGate.assertQuota(userId, consume = false)

        // Assert allowed
        assertTrue(verdict1 is QuotaVerdict.Allowed)

        // Check side effect: dailyUsed should REMAIN 0
        val quotaAfterCheck = dao.getQuota(userId)!!
        assertEquals("Usage should NOT increment after check", 0, quotaAfterCheck.dailyUsed)

        // 2. Simulate "Create Invoice" (Clicking button) with consume=true
        val verdict2 = quotaGate.assertQuota(userId, consume = true)

        // Assert allowed
        assertTrue(verdict2 is QuotaVerdict.Allowed)

        // Check side effect: dailyUsed should be 1
        val quotaAfterCreate = dao.getQuota(userId)!!
        assertEquals("Usage should increment after create", 1, quotaAfterCreate.dailyUsed)

        // 3. Do another check
        val verdict3 = quotaGate.assertQuota(userId, consume = false)
        assertTrue(verdict3 is QuotaVerdict.Allowed)
        assertEquals(1, dao.getQuota(userId)!!.dailyUsed)

        // 4. Create another
        val verdict4 = quotaGate.assertQuota(userId, consume = true)
        assertTrue(verdict4 is QuotaVerdict.Allowed)
        assertEquals(2, dao.getQuota(userId)!!.dailyUsed)

        // 5. Try to create 3rd (Should be blocked since launch bonus is 0 and daily limit is 2)
        val verdict5 = quotaGate.assertQuota(userId, consume = true)
        assertTrue("Should be blocked on 3rd attempt", verdict5 is QuotaVerdict.DailyCap)
    }
}
