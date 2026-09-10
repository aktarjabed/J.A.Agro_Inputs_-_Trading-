with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewScreen.kt', 'r') as f:
    content = f.read()

import re

imports_addition = """import android.content.Intent
import androidx.core.content.FileProvider
import com.aktarjabed.inbusiness.utils.pdf.PdfGenerator
import kotlinx.coroutines.launch"""

content = content.replace('import com.aktarjabed.inbusiness.data.entities.InvoiceItem',
                          'import com.aktarjabed.inbusiness.data.entities.InvoiceItem\n' + imports_addition)

share_logic = """
                            val coroutineScope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val generator = PdfGenerator(context)
                                        val file = generator.generateInvoicePdf(state.business, state.invoice, state.items)
                                        if (file != null) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
                                        }
                                    }
                                },
"""

content = content.replace('Button(\n                            onClick = { \n                                // TODO: PDF Generation and Share logic \n                            },',
                          share_logic)

content = content.replace('@Composable\nfun InvoicePreviewScreen', '@Composable\nfun InvoicePreviewScreen')

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewScreen.kt', 'w') as f:
    f.write(content)
