package com.kostku.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.kostku.app.data.repository.MonthlyRecap
import com.kostku.app.data.repository.RoomPayState
import com.kostku.app.data.repository.isTransfer
import java.io.File
import java.io.FileOutputStream

/** PDF rekap pembayaran satu bulan (ukuran A4, otomatis lanjut ke halaman berikutnya). */
object RecapPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 40f
    private const val GREEN = 0xFF104B38.toInt()
    private const val GREEN_SOFT = 0xFFEDF6F0.toInt()
    private const val INK = 0xFF17352B.toInt()
    private const val MUTED = 0xFF75847C.toInt()
    private const val LINE = 0xFFE3EAE5.toInt()
    private const val RED = 0xFFC94C4C.toInt()
    private const val AMBER = 0xFFB97A16.toInt()

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /** @param floorLabel mis. "Semua lantai" atau "Lantai 1" */
    fun create(context: Context, recap: MonthlyRecap, floorLabel: String, adminName: String): File {
        val doc = PdfDocument()
        val right = W - M
        val periodLabel = Fmt.month(recap.period.year, recap.period.monthValue)
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: Canvas
        var y = 0f

        fun header() {
            c.drawText("REKAP PEMBAYARAN BULANAN", M, 56f, paint(16f, GREEN, true))
            c.drawText("New Afra Kost • Jl. Perintis Kemerdekaan 4", M, 74f, paint(10f, MUTED))
            val p = paint(11f, INK, true)
            val t = "$periodLabel • $floorLabel"
            c.drawText(t, right - p.measureText(t), 56f, p)
            c.drawLine(M, 86f, right, 86f, Paint().apply { color = LINE; strokeWidth = 1f })
        }

        fun tableHeader() {
            c.drawRect(M, y - 13f, right, y + 7f, Paint().apply { color = GREEN_SOFT })
            val h = paint(9.5f, GREEN, true)
            c.drawText("KAMAR", M + 6f, y, h)
            c.drawText("PENGHUNI", M + 62f, y, h)
            c.drawText("JUMLAH", M + 230f, y, h)
            c.drawText("STATUS", M + 310f, y, h)
            c.drawText("METODE / TANGGAL", M + 390f, y, h)
            y += 22f
        }

        fun newPage() {
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page.canvas
            header()
            y = 112f
        }

        fun finishPage() {
            val p = paint(8.5f, MUTED)
            c.drawText("Dibuat oleh aplikasi KostKu • ${Fmt.date(System.currentTimeMillis())} • $adminName", M, H - 28f, p)
            val n = "Halaman $pageNo"
            c.drawText(n, right - p.measureText(n), H - 28f, p)
            doc.finishPage(page)
        }

        newPage()

        // Ringkasan
        val box = Paint().apply { color = GREEN_SOFT }
        c.drawRoundRect(M, y, right, y + 112f, 10f, 10f, box)
        c.drawText("Total diterima", M + 14f, y + 22f, paint(10f, MUTED))
        c.drawText(Fmt.rupiah(recap.paidAmount), M + 14f, y + 50f, paint(22f, GREEN, true))
        val pct = String.format(java.util.Locale("id", "ID"), "%.1f%%", recap.paidPercent)
        c.drawText("${recap.paidCount} lunas dari ${recap.billCount} tagihan ($pct)", M + 14f, y + 70f, paint(10f, INK))
        c.drawText(
            "${recap.roomCount} kamar • ${recap.maintenanceCount} perbaikan • ${recap.emptyCount} kosong (tidak ditagih)",
            M + 14f, y + 88f, paint(9f, MUTED)
        )
        fun stat(label: String, value: String, x: Float, row: Int, color: Int = INK) {
            c.drawText(label, x, y + 22f + row * 30f, paint(9.5f, MUTED))
            c.drawText(value, x, y + 36f + row * 30f, paint(11f, color, true))
        }
        val col = M + 300f
        stat("Target tagihan", Fmt.rupiah(recap.targetAmount), col, 0)
        stat("Belum dibayar (${recap.unpaidCount})", Fmt.rupiah(recap.unpaidAmount), col + 130f, 0, RED)
        stat("Tunai", Fmt.rupiah(recap.cashAmount), col, 1)
        stat("Transfer", Fmt.rupiah(recap.transferAmount), col + 130f, 1)
        if (recap.pendingCount > 0) {
            c.drawText("${recap.pendingCount} bukti transfer menunggu verifikasi", col, y + 102f, paint(9f, AMBER, true))
        }
        y += 140f

        c.drawText("Daftar pembayaran kamar", M, y, paint(12f, INK, true))
        y += 22f
        tableHeader()

        val rowLine = Paint().apply { color = LINE; strokeWidth = 0.7f }
        recap.rows.forEach { r ->
            if (y > H - 70f) {
                finishPage()
                newPage()
                tableHeader()
            }
            val p = r.payment
            c.drawText(r.roomNumber.toString(), M + 6f, y, paint(10f, INK, true))
            val name = r.tenantName.let { if (it.length > 26) it.take(25) + "…" else it }
            c.drawText(name, M + 62f, y, paint(10f, INK))
            c.drawText(Fmt.rupiah(p.amount), M + 230f, y, paint(10f, INK))
            val (status, color) = when (r.state) {
                RoomPayState.PAID -> "Lunas" to GREEN
                RoomPayState.PENDING -> "Verifikasi" to AMBER
                RoomPayState.REJECTED -> "Bukti ditolak" to RED
                else -> "Belum" to RED
            }
            c.drawText(status, M + 310f, y, paint(10f, color, true))
            val info = if (p.isPaid) {
                "${if (p.isTransfer()) "Transfer" else "Tunai"} • ${p.paidAt?.let { Fmt.shortDate(it) } ?: "-"}"
            } else {
                "Jatuh tempo ${Fmt.shortDate(p.dueDate)}"
            }
            c.drawText(info, M + 390f, y, paint(9.5f, MUTED))
            c.drawLine(M, y + 8f, right, y + 8f, rowLine)
            y += 22f
        }
        if (recap.rows.isEmpty()) {
            c.drawText("Belum ada tagihan pada periode ini.", M + 6f, y, paint(10f, MUTED))
        }
        finishPage()

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val suffix = floorLabel.lowercase().replace(' ', '-')
        val file = File(dir, "Rekap-${recap.period}-$suffix.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
