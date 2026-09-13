with open('app/src/main/java/com/aktarjabed/inbusiness/domain/quota/QuotaGate.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '        }\n    \n    private suspend fun createFirstQuota',
    '        }\n    }\n    \n    private suspend fun createFirstQuota'
)

with open('app/src/main/java/com/aktarjabed/inbusiness/domain/quota/QuotaGate.kt', 'w') as f:
    f.write(content)
