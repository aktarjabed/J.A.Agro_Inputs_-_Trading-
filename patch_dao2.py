with open('./app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'r') as f:
    content = f.read()

import re
content = content.replace('UPDATE invoice_sequence SET currentNumber = currentNumber + 1 WHERE businessId = :businessId', 'UPDATE invoice_sequences SET currentNumber = currentNumber + 1 WHERE businessId = :businessId')

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/dao/InvoiceDao.kt', 'w') as f:
    f.write(content)
