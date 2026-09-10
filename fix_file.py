with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'Text\("₹\$\{"%\.2f"\.format\(item\.taxResult\?\.totalAmount \?: 0\.0\)\}", fontWeight = FontWeight\.Bold\)',
                 r'Text(text = String.format(java.util.Locale.US, "₹%.2f", item.taxResult?.totalAmount ?: 0.0), fontWeight = FontWeight.Bold)', content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'w') as f:
    f.write(content)
