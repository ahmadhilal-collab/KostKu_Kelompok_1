package com.kostku.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kostku.app.KostKuApp
import com.kostku.app.data.local.UserRole
import kotlinx.coroutines.launch

/** Menerima pesan dari Firebase Cloud Messaging. Didaftarkan di AndroidManifest.xml. */
class KostKuMessagingService : FirebaseMessagingService() {

    private val container get() = (application as KostKuApp).container

    /** Token FCM HP ini berganti (jarang terjadi) → daftarkan ulang ke server jika penghuni sedang login. */
    override fun onNewToken(token: String) {
        if (container.sessionStore.currentSession()?.role == UserRole.TENANT) {
            container.appScope.launch { runCatching { container.pushRepository.registerDevice(token) } }
        }
    }

    /** Dipanggil saat aplikasi sedang terbuka (latar depan). */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "KostKu"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val id = message.data["notification_id"]?.toIntOrNull() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        PushNotifications.show(this, title, body, id)
        // Beri tahu layar penghuni supaya langsung memuat data terbaru (tagihan, laporan, dll.).
        container.pushRepository.notifyReceived()
    }
}
