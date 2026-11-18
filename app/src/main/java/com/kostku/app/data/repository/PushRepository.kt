package com.kostku.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.kostku.app.data.remote.ApiClient
import com.kostku.app.data.remote.DeviceRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Mendaftarkan / melepas token FCM HP ini di server (POST /devices, /devices/unregister). */
class PushRepository(private val client: ApiClient) {

    private val _received = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /** Berbunyi setiap ada push masuk saat aplikasi terbuka → layar penghuni memuat ulang data. */
    val received: SharedFlow<Unit> = _received.asSharedFlow()

    fun notifyReceived() {
        _received.tryEmit(Unit)
    }

    /** Token FCM HP ini; null jika Firebase belum siap (mis. Google Play Services tidak ada). */
    suspend fun currentToken(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Penghuni: daftarkan HP ini supaya menerima notifikasi. Aman dipanggil berulang. */
    suspend fun registerDevice(token: String? = null) {
        val fcmToken = token ?: currentToken() ?: return
        client.callUnit { this.registerDevice(DeviceRequest(fcmToken, "android")) }
    }

    /** Saat logout: HP ini berhenti menerima notifikasi akun tersebut. Gagal pun tidak masalah. */
    suspend fun unregisterDevice() {
        val fcmToken = currentToken() ?: return
        runCatching { client.callUnit { this.unregisterDevice(DeviceRequest(fcmToken)) } }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val error = task.exception
            if (task.isSuccessful) cont.resume(task.result) else cont.resumeWithException(error ?: IllegalStateException("FCM"))
        }
    }
}
