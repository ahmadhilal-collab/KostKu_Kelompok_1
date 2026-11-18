package com.kostku.app

import android.app.Application
import com.kostku.app.push.PushNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KostKuApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Data tidak lagi diisi contoh di HP; semuanya diambil dari server KostKu API.
        container = AppContainer(this, appScope)
        // Saluran notifikasi Android 8+ (Tagihan, pengumuman, status laporan).
        PushNotifications.createChannel(this)
    }
}
