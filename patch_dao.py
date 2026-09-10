with open('./app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'suspend fun getInvoiceSequence\(businessId: String\): InvoiceSequence\?',
                 r'suspend fun getInvoiceSequence(businessId: String): InvoiceSequence?\n\n    @Query("UPDATE invoice_sequence SET currentNumber = currentNumber + 1 WHERE businessId = :businessId")\n    suspend fun incrementSequence(businessId: String): Int\n\n    @Insert(onConflict = OnConflictStrategy.IGNORE)\n    suspend fun insertSequence(sequence: InvoiceSequence): Long', content)

content = content.replace('@Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insertInvoice(invoice: Invoice)', '@Insert(onConflict = OnConflictStrategy.ABORT)\n    suspend fun insertInvoice(invoice: Invoice)')

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'w') as f:
    f.write(content)
