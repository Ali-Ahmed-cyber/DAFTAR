package com.daftar.app.core.print

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.daftar.app.core.model.MoneyMath
import com.daftar.app.data.activity.entities.ActivityProfileEntity
import com.daftar.app.data.activity.entities.InvoiceEntity
import com.daftar.app.data.activity.entities.InvoiceLineEntity
import java.io.File
import java.io.FileOutputStream

object InvoicePdfPrinter {

    fun generateAndShareInvoicePdf(
        context: Context,
        profile: ActivityProfileEntity?,
        invoice: InvoiceEntity,
        lines: List<InvoiceLineEntity>,
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val mutedPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.2f
        }

        // Monochrome Print Variant Geometric Emblem (Top Left)
        canvas.drawRect(36f, 36f, 80f, 80f, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        })
        canvas.drawLine(58f, 44f, 58f, 72f, linePaint)

        val companyName = profile?.nameAr ?: "دفتر | DAFTAR"
        canvas.drawText("DAFTAR | $companyName", 96f, 56f, titlePaint)
        canvas.drawText("المحاسب الذكي المستقل — وثيقة مطبوعة", 96f, 74f, mutedPaint)

        canvas.drawLine(36f, 94f, 559f, 94f, linePaint)

        var y = 120f
        canvas.drawText("Invoice #: ${invoice.invoiceNumber} (${invoice.invoiceType})", 36f, y, titlePaint)
        y += 22f
        canvas.drawText("Date: ${invoice.issueDateIso}   |   Status: ${invoice.status}   |   Method: ${invoice.paymentMethod}", 36f, y, bodyPaint)
        y += 18f
        canvas.drawText("Currency: ${invoice.currencyCode}   |   FX Rate Locked: ${invoice.exchangeRateAtTx.toPlainString()}", 36f, y, mutedPaint)
        y += 18f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 20f

        canvas.drawText("Item", 36f, y, titlePaint)
        canvas.drawText("Qty", 300f, y, titlePaint)
        canvas.drawText("Unit Price", 370f, y, titlePaint)
        canvas.drawText("Total", 480f, y, titlePaint)
        y += 12f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 20f

        val dec = profile?.decimalPlaces ?: 2
        lines.forEach { l ->
            canvas.drawText(l.itemNameSnapshot.take(36), 36f, y, bodyPaint)
            canvas.drawText(l.quantity.toPlainString(), 300f, y, bodyPaint)
            canvas.drawText(MoneyMath.format(l.unitPriceOriginal, dec), 370f, y, bodyPaint)
            canvas.drawText(MoneyMath.format(l.lineTotalOriginal, dec), 480f, y, bodyPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 22f
        canvas.drawText("Subtotal: ${MoneyMath.format(invoice.subtotalOriginal, dec, invoice.currencyCode)}", 330f, y, bodyPaint)
        y += 18f
        canvas.drawText("Discount: ${MoneyMath.format(invoice.discountOriginal, dec, invoice.currencyCode)}", 330f, y, bodyPaint)
        y += 18f
        canvas.drawText("Tax: ${MoneyMath.format(invoice.taxOriginal, dec, invoice.currencyCode)}", 330f, y, bodyPaint)
        y += 22f
        canvas.drawText("TOTAL: ${MoneyMath.format(invoice.totalOriginal, dec, invoice.currencyCode)}", 330f, y, titlePaint)
        y += 18f
        canvas.drawText("Base Equivalent: ${MoneyMath.format(invoice.totalBase, dec, profile?.baseCurrency ?: "SDG")}", 330f, y, mutedPaint)

        doc.finishPage(page)

        val outDir = File(context.cacheDir, "shared_invoices").apply { mkdirs() }
        val pdfFile = File(outDir, "${invoice.invoiceNumber}.pdf")
        FileOutputStream(pdfFile).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DAFTAR Invoice ${invoice.invoiceNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, invoice.invoiceNumber).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return pdfFile
    }
}
