with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('val grandTotal by viewModel.grandTotal.collectAsState()\n', '')
content = content.replace('Text(text = "Total: ₹${"%.2f".format(grandTotal)}")', 'Text(text = "Total: ₹${"%.2f".format(calculationResult?.totalAmount ?: 0.0)}")')


with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'w') as f:
    f.write(content)
