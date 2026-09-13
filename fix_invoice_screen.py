with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('viewModel.amountPaid.value = d', 'viewModel.setAmountPaid(d)')
content = content.replace('viewModel.amountPaid.value = 0.0', 'viewModel.setAmountPaid(0.0)')
content = content.replace('viewModel.paymentMethod.value = it', 'viewModel.setPaymentMethod(it)')

with open('app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice/InvoiceScreen.kt', 'w') as f:
    f.write(content)
