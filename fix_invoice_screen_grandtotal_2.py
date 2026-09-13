with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('val formattedTotal = String.format(java.util.Locale.US, "%.2f", grandTotal)', 'val formattedTotal = String.format(java.util.Locale.US, "%.2f", calculationResult?.totalAmount ?: 0.0)')

with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'w') as f:
    f.write(content)
