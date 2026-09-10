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
        business: BusinessData,
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
        yPosition = drawHeader(canvas, business, invoice, yPosition)

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

                // Redraw table header on new page
                yPosition = drawTableHeader(canvas, yPosition)
            }

            yPosition = drawItemRow(canvas, item, yPosition)
        }

        // Draw line after items
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, boldPaint)
        yPosition += 20f

        // Check if totals fit
        if (yPosition > PAGE_HEIGHT - MARGIN - 150) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = MARGIN
        }

        // Totals
        yPosition = drawTotals(canvas, invoice, yPosition)

        // Footer
        drawFooter(canvas, business)

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

    private fun drawHeader(canvas: Canvas, business: BusinessData, invoice: Invoice, startY: Float): Float {
        var y = startY

        // Title
        canvas.drawText("TAX INVOICE", PAGE_WIDTH / 2f - titlePaint.measureText("TAX INVOICE") / 2, y, titlePaint)
        y += 40f

        // Seller Info (Left)
        canvas.drawText(business.name, MARGIN, y, boldPaint)
        y += 20f
        if (!business.gstin.isNullOrBlank()) {
            canvas.drawText("GSTIN: ${business.gstin}", MARGIN, y, textPaint)
            y += 20f
        }
        if (business.address.isNotBlank()) {
            canvas.drawText(business.address, MARGIN, y, textPaint)
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
        val y = startY

        // Truncate description if too long to prevent overlap
        var desc = item.description
        if (textPaint.measureText(desc) > 190f) {
            desc = desc.take(25) + "..."
        }

        canvas.drawText(desc, MARGIN + 5f, y, textPaint)
        canvas.drawText(item.quantity.toString(), 250f, y, textPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", item.pricePerUnit), 300f, y, textPaint)
        canvas.drawText("${item.gstPercentage}%", 380f, y, textPaint)
        canvas.drawText(String.format(Locale.US, "%.2f", item.taxAmount), 440f, y, textPaint)

        val totalStr = String.format(Locale.US, "%.2f", item.totalAmount)
        canvas.drawText(totalStr, PAGE_WIDTH - MARGIN - textPaint.measureText(totalStr) - 5f, y, textPaint)

        return y + 20f
    }

    private fun drawTotals(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY
        val rightMargin = PAGE_WIDTH - MARGIN - 5f

        val subtotal = invoice.totalAmount - invoice.taxAmount

        val subtotalStr = "Subtotal: ${String.format(Locale.US, "%.2f", subtotal)}"
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

        return y + 40f
    }

    private fun drawFooter(canvas: Canvas, business: BusinessData) {
        val y = PAGE_HEIGHT - 60f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, textPaint)

        canvas.drawText("For ${business.name}", PAGE_WIDTH - MARGIN - boldPaint.measureText("For ${business.name}"), y + 20f, boldPaint)
        canvas.drawText("Authorized Signatory", PAGE_WIDTH - MARGIN - textPaint.measureText("Authorized Signatory"), y + 40f, smallTextPaint)
    }
}
