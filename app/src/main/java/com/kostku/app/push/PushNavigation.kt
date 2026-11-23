package com.kostku.app.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tujuan layar dari notifikasi yang diketuk.
 * @param scope "admin" atau "tenant" (akun penerima)
 * @param type jenis notifikasi dari server, mis. transfer_submitted, repair_new, payment_paid
 * @param refId id tagihan / laporan yang dirujuk (notifikasi admin)
 */
data class PushTarget(
    val scope: String,
    val type: String,
    val refId: Long?,
    val notificationId: Long?
)

/**
 * Menyimpan notifikasi yang diketuk sampai layar admin/penghuni siap membukanya.
 * MainActivity mengisi dari Intent; AdminShell/TenantShell membaca lalu memanggil [consume].
 */
object PushNavigation {
    const val EXTRA_SCOPE = "scope"
    const val EXTRA_TYPE = "type"
    const val EXTRA_REF_ID = "ref_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    private val _target = MutableStateFlow<PushTarget?>(null)
    val target: StateFlow<PushTarget?> = _target.asStateFlow()

    /**
     * Saat aplikasi tertutup, Android membuka MainActivity dengan data FCM sebagai extras (semuanya teks).
     * Saat aplikasi terbuka, PushNotifications.show() mengisi extras yang sama.
     */
    fun handle(intent: Intent?) {
        val extras = intent?.extras ?: return
        val type = extras.getString(EXTRA_TYPE)?.takeIf { it.isNotBlank() } ?: return
        _target.value = PushTarget(
            scope = extras.getString(EXTRA_SCOPE) ?: "tenant",
            type = type,
            refId = extras.getString(EXTRA_REF_ID)?.toLongOrNull()?.takeIf { it > 0 },
            notificationId = extras.getString(EXTRA_NOTIFICATION_ID)?.toLongOrNull()
        )
        // Jangan diproses ulang saat Activity dibuat ulang (mis. layar diputar).
        intent.removeExtra(EXTRA_TYPE)
    }

    fun consume() {
        _target.value = null
    }
}
