with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewViewModel.kt', 'r') as f:
    content = f.read()

import re

# Add BusinessData fetching since it is needed for the PDF Generator
content = content.replace('import com.aktarjabed.inbusiness.data.entities.InvoiceItem',
                          'import com.aktarjabed.inbusiness.data.entities.InvoiceItem\nimport com.aktarjabed.inbusiness.data.entities.BusinessData\nimport com.aktarjabed.inbusiness.data.dao.BusinessDao')

content = content.replace('data class Success(val invoice: Invoice, val items: List<InvoiceItem>) : InvoicePreviewUiState()',
                          'data class Success(val business: BusinessData, val invoice: Invoice, val items: List<InvoiceItem>) : InvoicePreviewUiState()')

content = content.replace('private val invoiceDao: InvoiceDao,',
                          'private val invoiceDao: InvoiceDao,\n    private val businessDao: BusinessDao,')

content = content.replace('val items = invoiceDao.getInvoiceItems(invoiceId)\n                _uiState.value = InvoicePreviewUiState.Success(invoice, items)',
                          'val items = invoiceDao.getInvoiceItems(invoiceId)\n                val business = businessDao.getBusinessById(currentBusinessId) ?: return@launch\n                _uiState.value = InvoicePreviewUiState.Success(business, invoice, items)')

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewViewModel.kt', 'w') as f:
    f.write(content)
