package com.kostku.app

import android.content.Context
import com.kostku.app.data.local.AppDatabase
import com.kostku.app.data.local.SessionStore
import com.kostku.app.data.remote.ApiClient
import com.kostku.app.data.repository.AuthRepository
import com.kostku.app.data.repository.KostRepository
import com.kostku.app.data.repository.PushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Dependency container sederhana (tanpa Hilt) supaya mudah dipahami. */
class AppContainer(context: Context, val appScope: CoroutineScope) {
    /** Salinan data dari server (cache) supaya layar tetap tampil cepat dan saat offline. */
    val database: AppDatabase = AppDatabase.build(context)

    /** Token login + data sesi di SharedPreferences. */
    val sessionStore: SessionStore = SessionStore(context)

    /** Retrofit + OkHttp. Jika token ditolak server (401), sesi diakhiri → kembali ke login. */
    val apiClient: ApiClient = ApiClient(
        tokenProvider = { sessionStore.token() },
        onUnauthorized = { appScope.launch { authRepository.endSession() } }
    )

    /** Token notifikasi (Firebase Cloud Messaging) HP ini. */
    val pushRepository: PushRepository = PushRepository(apiClient)

    val authRepository: AuthRepository = AuthRepository(
        client = apiClient,
        sessionStore = sessionStore,
        database = database,
        pushRepository = pushRepository
    )
    val kostRepository: KostRepository = KostRepository(database, apiClient, sessionStore)
}
