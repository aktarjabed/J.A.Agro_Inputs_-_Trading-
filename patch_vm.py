with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'val nextInvoiceNumber = generateInvoiceNumberPreview\(currentBusinessId\)',
                 r'val nextInvoiceNumber = "Preview Number"', content)

content = re.sub(r'private suspend fun generateInvoiceNumberPreview\(businessId: String\): String \{[\s\S]*?\}',
                 r'', content)

content = content.replace('private val invoiceDao: InvoiceDao,', '')
content = content.replace('import com.aktarjabed.inbusiness.data.dao.InvoiceDao\n', '')

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceViewModel.kt', 'w') as f:
    f.write(content)
