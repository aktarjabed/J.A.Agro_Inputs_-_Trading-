with open('app/src/test/java/com/aktarjabed/inbusiness/domain/quota/QuotaGateTest.kt', 'r') as f:
    content = f.read()

# Replace old methods with new ones
old_methods = """        override suspend fun incrementUsage(userId: String, dailyCap: Int, monthlyCap: Int, timestamp: Long): Int {
            val q = quotas[userId]
            if (q != null) {
                if (q.dailyUsed < dailyCap && q.monthlyUsed < monthlyCap) {
                    quotas[userId] = q.copy(
                        dailyUsed = q.dailyUsed + 1,
                        monthlyUsed = q.monthlyUsed + 1
                    )
                    return 1
                }
            }
            return 0
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

        override suspend fun getQuotaStatus(userId: String, dailyCap: Int, monthlyCap: Int): String? {
            val q = quotas[userId] ?: return "AVAILABLE"
            if (q.monthlyUsed >= monthlyCap && q.dailyUsed >= dailyCap) return "BOTH_EXCEEDED"
            if (q.monthlyUsed >= monthlyCap) return "MONTHLY_EXCEEDED"
            if (q.dailyUsed >= dailyCap) return "DAILY_EXCEEDED"
            return "AVAILABLE"
        }"""

new_methods = """        override suspend fun consumeQuotaAtomic(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int, timestamp: Long): Int {
            val q = quotas[userId] ?: return 0

            var newDaily = q.dailyUsed
            if (q.lastResetEpochDay < today) newDaily = 0

            var newMonthly = q.monthlyUsed
            if (q.lastMonthlyResetEpochDay < monthStart) newMonthly = 0

            if (newDaily < dailyCap && newMonthly < monthlyCap) {
                quotas[userId] = q.copy(
                    dailyUsed = newDaily + 1,
                    lastResetEpochDay = today,
                    monthlyUsed = newMonthly + 1,
                    lastMonthlyResetEpochDay = monthStart
                )
                return 1
            }
            return 0
        }

        override suspend fun syncQuotaPeriods(userId: String, today: Long, monthStart: Long, timestamp: Long) {
            val q = quotas[userId] ?: return

            var newDaily = q.dailyUsed
            if (q.lastResetEpochDay < today) newDaily = 0

            var newMonthly = q.monthlyUsed
            if (q.lastMonthlyResetEpochDay < monthStart) newMonthly = 0

            quotas[userId] = q.copy(
                dailyUsed = newDaily,
                lastResetEpochDay = today,
                monthlyUsed = newMonthly,
                lastMonthlyResetEpochDay = monthStart
            )
        }

        override suspend fun getQuotaStatus(userId: String, today: Long, monthStart: Long, dailyCap: Int, monthlyCap: Int): String? {
            val q = quotas[userId] ?: return "AVAILABLE"

            var currentDaily = q.dailyUsed
            if (q.lastResetEpochDay < today) currentDaily = 0

            var currentMonthly = q.monthlyUsed
            if (q.lastMonthlyResetEpochDay < monthStart) currentMonthly = 0

            if (currentMonthly >= monthlyCap && currentDaily >= dailyCap) return "BOTH_EXCEEDED"
            if (currentMonthly >= monthlyCap) return "MONTHLY_EXCEEDED"
            if (currentDaily >= dailyCap) return "DAILY_EXCEEDED"
            return "AVAILABLE"
        }"""
content = content.replace(old_methods, new_methods)

with open('app/src/test/java/com/aktarjabed/inbusiness/domain/quota/QuotaGateTest.kt', 'w') as f:
    f.write(content)
