with open('./app/src/main/java/com/aktarjabed/inbusiness/data/entities/InvoiceItem.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    val amount: Double = 0\.0\n\)',
                 r'    val amount: Double = 0.0,\n    val gstPercentage: Double = 0.0,\n    val taxAmount: Double = 0.0,\n    val totalAmount: Double = 0.0,\n    val productId: Long? = null\n)', content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/entities/InvoiceItem.kt', 'w') as f:
    f.write(content)
