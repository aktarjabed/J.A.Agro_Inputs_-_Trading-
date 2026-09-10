with open('./app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'r') as f:
    content = f.read()

import re
content = content.replace('import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult\nimport com.aktarjabed.inbusiness.domain.invoice.SupplyType',
                          'import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult\nimport com.aktarjabed.inbusiness.domain.invoice.SupplyType')

with open('./app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'w') as f:
    f.write(content)
