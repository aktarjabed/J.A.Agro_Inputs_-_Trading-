with open('./app/src/main/java/com/aktarjabed/inbusiness/data/entities/Invoice.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    val customerGSTIN: String\? = null,\n    val totalAmount: Double = 0\.0,\n    val taxAmount: Double = 0\.0,\n    val createdAt: Instant = Instant\.now\(\),',
                 r'    val customerGSTIN: String? = null,\n    val buyerAddress: String = "",\n    val totalAmount: Double = 0.0,\n    val taxAmount: Double = 0.0,\n    val totalCgst: Double = 0.0,\n    val totalSgst: Double = 0.0,\n    val totalIgst: Double = 0.0,\n    val supplyType: String = "",\n    val createdAt: Instant = Instant.now(),', content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/entities/Invoice.kt', 'w') as f:
    f.write(content)
