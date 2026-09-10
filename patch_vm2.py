with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    "\n    }', r'', content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'w') as f:
    f.write(content)
