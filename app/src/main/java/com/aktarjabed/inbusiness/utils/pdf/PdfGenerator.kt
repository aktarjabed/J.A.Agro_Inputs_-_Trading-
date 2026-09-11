package com.aktarjabed.inbusiness.utils.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 Width in PostScript points
        private const val PAGE_HEIGHT = 842 // A4 Height
        private const val MARGIN = 40f
    }

    private val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f
        color = Color.BLACK
    }

    private val boldPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        color = Color.BLACK
    }

    private val textPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 12f
        color = Color.BLACK
    }

    private val smallTextPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10f
        color = Color.DKGRAY
    }

    fun generateInvoicePdf(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var yPosition = MARGIN

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        yPosition = drawHeader(canvas, invoice, yPosition)

        // Items Table Header
        yPosition = drawTableHeader(canvas, yPosition)

        // Items
        for (item in items) {
            // Check if we need a new page
            if (yPosition > PAGE_HEIGHT - MARGIN - 100) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN

                // Redraw seller and buyer header on new page
                yPosition = drawHeader(canvas, invoice, yPosition)

                // Redraw table header on new page
                yPosition = drawTableHeader(canvas, yPosition)
            }

            yPosition = drawItemRow(canvas, item, yPosition)
        }

        // Draw line after items
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, boldPaint)
        yPosition += 20f

        // Check if totals fit (increased margin to accommodate new sections)
        if (yPosition > PAGE_HEIGHT - MARGIN - 250) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = MARGIN

            // Redraw header for context on the new page
            yPosition = drawHeader(canvas, invoice, yPosition)
        }

        // Totals
        yPosition = drawTotals(canvas, invoice, yPosition)

        // Amount in words
        yPosition = drawAmountInWords(canvas, invoice.totalAmount, yPosition)

        // Payment Details
        yPosition = drawPaymentDetails(canvas, invoice, yPosition)

        // Terms
        yPosition = drawTerms(canvas, yPosition)

        // Footer
        drawFooter(canvas, invoice)

        pdfDocument.finishPage(page)

        // Save to FileProvider cache directory
        val cachePath = File(context.cacheDir, "invoices")
        cachePath.mkdirs()
        // Ensure unique filename to prevent overwrite
        val file = File(cachePath, "Invoice_${invoice.invoiceNumber}_${System.currentTimeMillis()}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawHeader(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY

        // Title
        canvas.drawText("TAX INVOICE", PAGE_WIDTH / 2f - titlePaint.measureText("TAX INVOICE") / 2, y, titlePaint)
        y += 40f

        // Seller Info (Left) - use persisted snapshot from invoice
        val sellerName = invoice.sellerName
        val sellerGstin = invoice.sellerGSTIN
        val sellerAddress = invoice.sellerAddress

        if (sellerName.isNotBlank()) {
            canvas.drawText(sellerName, MARGIN, y, boldPaint)
            y += 20f
        }
        if (!sellerGstin.isNullOrBlank()) {
            canvas.drawText("GSTIN: ${sellerGstin}", MARGIN, y, textPaint)
            y += 20f
        }
        if (sellerAddress.isNotBlank()) {
            canvas.drawText(sellerAddress, MARGIN, y, textPaint)
            y += 20f
        }

        // Invoice Info (Right)
        var rightY = startY + 40f
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneId.systemDefault())
        val rightMargin = PAGE_WIDTH - MARGIN

        val invNoText = "Invoice No: ${invoice.invoiceNumber}"
        canvas.drawText(invNoText, rightMargin - boldPaint.measureText(invNoText), rightY, boldPaint)
        rightY += 20f

        val dateText = "Date: ${dateFormatter.format(invoice.createdAt)}"
        canvas.drawText(dateText, rightMargin - textPaint.measureText(dateText), rightY, textPaint)
        rightY += 20f

        val supplyText = "Supply: ${invoice.supplyType}"
        canvas.drawText(supplyText, rightMargin - textPaint.measureText(supplyText), rightY, textPaint)

        y = maxOf(y, rightY) + 30f

        // Buyer Info
        canvas.drawText("Billed To:", MARGIN, y, boldPaint)
        y += 20f
        canvas.drawText(invoice.customerName, MARGIN, y, textPaint)
        y += 20f
        if (!invoice.customerGSTIN.isNullOrBlank()) {
            canvas.drawText("GSTIN: ${invoice.customerGSTIN}", MARGIN, y, textPaint)
            y += 20f
        }
        if (invoice.buyerAddress.isNotBlank()) {
            canvas.drawText(invoice.buyerAddress, MARGIN, y, textPaint)
            y += 20f
        }

        return y + 20f
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float): Float {
        val y = startY
        canvas.drawRect(MARGIN, y - 15f, PAGE_WIDTH - MARGIN, y + 10f, Paint().apply { color = Color.LTGRAY })

        canvas.drawText("Description", MARGIN + 5f, y, boldPaint)
        canvas.drawText("Qty", 250f, y, boldPaint)
        canvas.drawText("Price", 300f, y, boldPaint)
        canvas.drawText("GST %", 380f, y, boldPaint)
        canvas.drawText("Tax", 440f, y, boldPaint)
        canvas.drawText("Total", PAGE_WIDTH - MARGIN - boldPaint.measureText("Total") - 5f, y, boldPaint)

        return y + 30f
    }

    private fun drawItemRow(canvas: Canvas, item: InvoiceItem, startY: Float): Float {
        var y = startY
        val colDescWidth = 190f

        // Text wrapping for description
        val words = item.description.split(" ")
        var currentLine = ""
        val lines = mutableListOf<String>()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) < colDescWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Draw first line of description and the other columns
        if (lines.isNotEmpty()) {
            canvas.drawText(lines[0], MARGIN + 5f, y, textPaint)
        }

        val qtyText = if(item.unitType.isNotBlank()) "${item.quantity} ${item.unitType}" else item.quantity.toString()
        canvas.drawText(qtyText, 250f, y, textPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", item.pricePerUnit), 300f, y, textPaint)
        canvas.drawText("${item.gstPercentage}%", 380f, y, textPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", item.taxAmount), 440f, y, textPaint)

        val totalStr = String.format(Locale.US, "%.2f", item.totalAmount)
        canvas.drawText(totalStr, PAGE_WIDTH - MARGIN - textPaint.measureText(totalStr) - 5f, y, textPaint)

        y += 20f

        // Draw remaining lines of description
        for (i in 1 until lines.size) {
            canvas.drawText(lines[i], MARGIN + 5f, y, textPaint)
            y += 20f
        }

        return y
    }

    private fun drawTotals(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY
        val rightMargin = PAGE_WIDTH - MARGIN - 5f

        val subtotalStr = "Subtotal: Rs. ${String.format(Locale.US, "%.2f", invoice.subtotal)}"
        canvas.drawText(subtotalStr, rightMargin - textPaint.measureText(subtotalStr), y, textPaint)
        y += 20f

        if (invoice.totalCgst > 0) {
            val cgstStr = "CGST: ${String.format(Locale.US, "%.2f", invoice.totalCgst)}"
            canvas.drawText(cgstStr, rightMargin - textPaint.measureText(cgstStr), y, textPaint)
            y += 20f
        }

        if (invoice.totalSgst > 0) {
            val sgstStr = "SGST: ${String.format(Locale.US, "%.2f", invoice.totalSgst)}"
            canvas.drawText(sgstStr, rightMargin - textPaint.measureText(sgstStr), y, textPaint)
            y += 20f
        }

        if (invoice.totalIgst > 0) {
            val igstStr = "IGST: ${String.format(Locale.US, "%.2f", invoice.totalIgst)}"
            canvas.drawText(igstStr, rightMargin - textPaint.measureText(igstStr), y, textPaint)
            y += 20f
        }

        y += 10f
        val grandTotalStr = "Grand Total: Rs. ${String.format(Locale.US, "%.2f", invoice.totalAmount)}"
        canvas.drawText(grandTotalStr, rightMargin - boldPaint.measureText(grandTotalStr), y, boldPaint)

        return y + 30f
    }

    private fun drawAmountInWords(canvas: Canvas, amount: Double, startY: Float): Float {
        var y = startY
        val amountInWords = com.aktarjabed.inbusiness.utils.AmountInWordsConverter.convertAmountToWords(amount)
        val text = "Amount in Words: $amountInWords"
        canvas.drawText(text, MARGIN, y, boldPaint)
        return y + 30f
    }

    private fun drawPaymentDetails(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY

        canvas.drawText("Payment Details", MARGIN, y, boldPaint)
        y += 20f

        val amountPaidStr = "Amount Paid: Rs. ${String.format(Locale.US, "%.2f", invoice.amountPaid)}"
        canvas.drawText(amountPaidStr, MARGIN, y, textPaint)
        y += 20f

        val balanceDueStr = "Balance Due: Rs. ${String.format(Locale.US, "%.2f", invoice.balanceDue)}"
        canvas.drawText(balanceDueStr, MARGIN, y, textPaint)
        y += 20f

        val paymentMethodStr = "Payment Method: ${invoice.paymentMethod}"
        canvas.drawText(paymentMethodStr, MARGIN, y, textPaint)

        return y + 30f
    }

    private fun drawTerms(canvas: Canvas, startY: Float): Float {
        var y = startY
        canvas.drawText("Terms & Conditions:", MARGIN, y, boldPaint)
        y += 20f
        canvas.drawText("1. Goods once sold will not be taken back.", MARGIN, y, smallTextPaint)
        y += 15f
        canvas.drawText("2. Interest @ 18% p.a. will be charged if payment is delayed.", MARGIN, y, smallTextPaint)
        return y + 20f
    }

    private fun drawFooter(canvas: Canvas, invoice: Invoice) {
        val y = PAGE_HEIGHT - 60f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, textPaint)

        val sellerName = invoice.sellerName
        if (sellerName.isNotBlank()) {
            canvas.drawText("For $sellerName", PAGE_WIDTH - MARGIN - boldPaint.measureText("For $sellerName"), y + 20f, boldPaint)
        }
        canvas.drawText("Authorized Signatory", PAGE_WIDTH - MARGIN - textPaint.measureText("Authorized Signatory"), y + 40f, smallTextPaint)
    }
}
