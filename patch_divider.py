with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Divider(', 'HorizontalDivider(')

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewScreen.kt', 'w') as f:
    f.write(content)
