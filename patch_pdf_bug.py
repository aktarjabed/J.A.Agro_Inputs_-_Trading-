with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('businessDao.getBusinessById(currentBusinessId)', 'businessDao.getBusinessDataById(currentBusinessId)')

with open('./app/src/main/java/com/aktarjabed/inbusiness/presentation/screens/invoice_preview/InvoicePreviewViewModel.kt', 'w') as f:
    f.write(content)

with open('./app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

content = content.replace('business.businessName', 'business.name')

with open('./app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
