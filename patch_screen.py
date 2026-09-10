with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'viewModel\.createInvoice\(customerName, amount, tax\)',
                 r'viewModel.createInvoice()', content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'w') as f:
    f.write(content)
