package com.kostku.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kostku.app.R
import com.kostku.app.data.repository.ReceiptData
import java.io.File
import java.io.FileOutputStream

object ReceiptPdf {
    private const val GREEN = 0xFF064E2E.toInt()
    private const val GREEN_SOFT = 0xFFE3F1EA.toInt()
    private const val INK = 0xFF0F2A20.toInt()
    private const val MUTED = 0xFF5B6B63.toInt()
    private const val LINE = 0xFFE2E8E4.toInt()

    fun create(context: Context, r: ReceiptData): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(420, 620, 1).create())
        val c = page.canvas

        fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        val margin = 32f
        val right = 420f - margin

        // Header: logo + nama kost
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_kostku)
        val logoH = 44
        val logoW = (logo.width.toFloat() / logo.height * logoH).toInt()
        c.drawBitmap(Bitmap.createScaledBitmap(logo, logoW, logoH, true), margin, 28f, null)
        c.drawText("KostKu", margin + logoW + 10f, 54f, paint(20f, GREEN, true))
        c.drawText("New Afra Kost", margin, 96f, paint(13f, INK, true))
        c.drawText("Jl. Perintis Kemerdekaan 4", margin, 112f, paint(11f, MUTED))
        val noP = paint(11f, MUTED)
        c.drawText("Nota: ${r.receiptNo}", right - noP.measureText("Nota: ${r.receiptNo}"), 112f, noP)

        val divider = Paint().apply { color = LINE; strokeWidth = 1f }
        c.drawLine(margin, 126f, right, 126f, divider)

        // Judul
        val title = paint(18f, GREEN, true)
        c.drawText("PEMBAYARAN LUNAS", margin, 156f, title)
        c.drawText("Terima kasih, pembayaran telah diterima.", margin, 174f, paint(11f, MUTED))

        // Baris detail
        var y = 208f
        fun row(label: String, value: String) {
            c.drawText(label, margin, y, paint(11f, MUTED))
            val p = paint(11f, INK, true)
            c.drawText(value, right - p.measureText(value), y, p)
            y += 24f
        }
        row("Nama penghuni", r.tenantName)
        row("Kamar", "${r.roomNumber} • Lantai ${r.floor}")
        row("Periode sewa", r.periodLabel)
        row("Tanggal bayar", r.paidDate)
        row("Metode", r.method)
        c.drawLine(margin, y - 8f, right, y - 8f, divider)

        // Total
        y += 14f
        c.drawText("Total dibayar (All-In)", margin, y, paint(11f, MUTED))
        val badge = Paint().apply { color = GREEN_SOFT }
        c.drawRoundRect(right - 60f, y - 13f, right, y + 5f, 6f, 6f, badge)
        c.drawText("LUNAS", right - 50f, y, paint(10f, GREEN, true))
        y += 38f
        c.drawText(Fmt.rupiah(r.amount), margin, y, paint(30f, INK, true))
        y += 22f
        c.drawText(Fmt.terbilang(r.amount), margin, y, Paint(paint(11f, MUTED)).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) })
        y += 26f
        c.drawLine(margin, y, right, y, divider)
        y += 26f
        c.drawText("Diterima oleh", margin, y, paint(11f, MUTED))
        val by = "${r.receivedBy} • Pengelola"
        val byP = paint(11f, INK, true)
        c.drawText(by, right - byP.measureText(by), y, byP)

        c.drawText("Nota ini sah dan dibuat otomatis oleh aplikasi KostKu.", margin, 590f, paint(9f, MUTED))

        doc.finishPage(page)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "Nota-${r.receiptNo}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Simpan ke folder Unduhan (Android 10+ lewat MediaStore; di bawahnya ke folder Unduhan milik aplikasi). */
    fun saveToDownloads(context: Context, file: File): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            true
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching false
            file.copyTo(File(dir, file.name), overwrite = true)
            true
        }
    }.getOrDefault(false)
}
