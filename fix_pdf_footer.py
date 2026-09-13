with open('app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

content = content.replace('drawFooter(canvas, invoice)', 'drawFooter(canvas)')

with open('app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
