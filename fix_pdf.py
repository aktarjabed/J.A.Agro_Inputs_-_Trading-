with open('app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()


# 1. Update write to use explicit FileOutputStream and remove warning
content = content.replace(
    '        val file = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.pdf")\n        pdfDocument.writeTo(file.outputStream())\n        pdfDocument.close()',
    '        val file = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.pdf")\n        java.io.FileOutputStream(file).use { out ->\n            pdfDocument.writeTo(out)\n        }\n        pdfDocument.close()'
)

# 2. Fix the drawTerms unused variable warning
content = content.replace(
    '        var yPosition = drawPaymentDetails(canvas, invoice, drawAmountInWords(canvas, invoice.totalAmount, drawTotals(canvas, invoice, currentY)))\n\n        drawTerms(canvas, yPosition)\n\n        drawFooter(canvas, invoice)',
    '        var yPosition = drawPaymentDetails(canvas, invoice, drawAmountInWords(canvas, invoice.totalAmount, drawTotals(canvas, invoice, currentY)))\n\n        yPosition = drawTerms(canvas, yPosition)\n\n        drawFooter(canvas)'
)
content = content.replace('private fun drawFooter(canvas: Canvas, invoice: Invoice) {', 'private fun drawFooter(canvas: Canvas) {')

# 3. Increase space reserved for totals to 350f
content = content.replace('if (currentY + 250f > PAGE_HEIGHT - 100f) {', 'if (currentY + 350f > PAGE_HEIGHT - 100f) {')

# 4. Use Text Wrapping via StaticLayout for Amount In Words
old_amount = """    private fun drawAmountInWords(canvas: Canvas, amount: Double, startY: Float): Float {
        // As it is critical functionality keeping the actual logic present, although modifying visual layout if needed
        var y = startY
        val amountInWords = com.aktarjabed.inbusiness.utils.AmountInWordsConverter.convertAmountToWords(amount)
        val text = "Amount in Words: $amountInWords"
        canvas.drawText(text, MARGIN, y, boldPaint)
        return y + 30f
    }"""

new_amount = """    private fun drawAmountInWords(canvas: Canvas, amount: Double, startY: Float): Float {
        var y = startY
        val amountInWords = com.aktarjabed.inbusiness.utils.AmountInWordsConverter.convertAmountToWords(amount)
        val text = "Amount in Words: $amountInWords"

        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val words = text.split(" ")
        var currentLine = ""
        val lines = mutableListOf<String>()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (boldPaint.measureText(testLine) < maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        for (line in lines) {
            canvas.drawText(line, MARGIN, y, boldPaint)
            y += 20f
        }
        return y + 10f
    }"""
content = content.replace(old_amount, new_amount)

# 5. Fix unused import/parameter
content = content.replace('import androidx.core.content.FileProvider', '')
content = content.replace('import com.aktarjabed.inbusiness.domain.context.BusinessContext', '')


with open('app/src/main/java/com/aktarjabed/inbusiness/utils/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
