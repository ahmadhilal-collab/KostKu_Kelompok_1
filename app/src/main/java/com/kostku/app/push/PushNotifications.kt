package com.kostku.app.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kostku.app.MainActivity
import com.kostku.app.R

/** Saluran dan tampilan notifikasi KostKu. */
object PushNotifications {
    /** Harus sama dengan ANDROID_CHANNEL di server (src/Fcm.php) dan meta-data di AndroidManifest. */
    const val CHANNEL_ID = "kostku_umum"

    /** Dipanggil sekali saat aplikasi dibuka (KostKuApp.onCreate). Aman dipanggil berulang. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Notifikasi KostKu",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Tagihan, bukti transfer, pengumuman, dan laporan perbaikan"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Menampilkan notifikasi saat aplikasi sedang terbuka. (Saat aplikasi tertutup/di latar belakang,
     * Android menampilkannya sendiri memakai channel & ikon dari AndroidManifest.)
     */
    @SuppressLint("MissingPermission") // izin sudah diperiksa lewat canPost()
    fun show(context: Context, title: String, body: String, id: Int, data: Map<String, String> = emptyMap()) {
        if (!canPost(context)) return
        // Data FCM (type, scope, ref_id, notification_id) ikut dibawa supaya ketukan membuka layar terkait.
        val intent = Intent(context, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        for (key in listOf(PushNavigation.EXTRA_TYPE, PushNavigation.EXTRA_SCOPE, PushNavigation.EXTRA_REF_ID, PushNavigation.EXTRA_NOTIFICATION_ID)) {
            data[key]?.let { intent.putExtra(key, it) }
        }
        val openApp = PendingIntent.getActivity(
            context,
            id, // tiap notifikasi punya PendingIntent sendiri (extras tidak saling menimpa)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_kostku)
            .setColor(ContextCompat.getColor(context, R.color.kostku_green))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
