package com.kostku.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Intents {
    /** Buka chat WhatsApp ke [phone] dengan pesan terisi. Mengembalikan false jika tidak ada aplikasi yang bisa membukanya. */
    fun openWhatsApp(context: Context, phone: String, message: String): Boolean {
        val uri = Uri.parse("https://wa.me/${PhoneUtils.toInternational(phone)}?text=${Uri.encode(message)}")
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun dial(context: Context, phone: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PhoneUtils.normalize(phone)}")))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    fun sharePdf(context: Context, file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(send, "Bagikan nota"))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun reminderText(tenantName: String, roomNumber: Int, periodLabel: String, amount: Long, dueDate: String): String =
        "Halo $tenantName, ini pengingat dari pengelola New Afra Kost. " +
            "Tagihan kost bulan $periodLabel untuk Kamar $roomNumber sebesar ${Fmt.rupiah(amount)} " +
            "jatuh tempo pada $dueDate. Mohon segera melakukan pembayaran. Terima kasih."
}
