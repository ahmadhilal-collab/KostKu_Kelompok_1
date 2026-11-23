package com.kostku.app.data.repository

import com.kostku.app.BuildConfig
import com.kostku.app.data.local.AppDatabase
import com.kostku.app.data.local.SessionStore
import com.kostku.app.data.remote.ApiClient
import com.kostku.app.data.remote.LoginAdminRequest
import com.kostku.app.data.remote.LoginData
import com.kostku.app.data.remote.LoginTenantRequest
import com.kostku.app.data.remote.RegisterAdminRequest
import com.kostku.app.data.remote.toSession
import com.kostku.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Login, daftar, dan logout lewat KostKu REST API. Token disimpan di SharedPreferences. */
class AuthRepository(
    private val client: ApiClient,
    private val sessionStore: SessionStore,
    private val database: AppDatabase,
    private val pushRepository: PushRepository
) {

    suspend fun registerAdmin(
        name: String,
        phone: String,
        password: String,
        secretCode: String
    ): Result<Unit> = runCatching {
        val data = client.call {
            this.registerAdmin(
                RegisterAdminRequest(
                    name = name.trim(),
                    phone = PhoneUtils.normalize(phone),
                    password = password,
                    secretCode = secretCode.trim()
                )
            )
        }
        startSession(data)
    }

    suspend fun loginAdmin(identifier: String, password: String): Result<Unit> = runCatching {
        val data = client.call { this.loginAdmin(LoginAdminRequest(identifier.trim(), password)) }
        startSession(data)
    }

    suspend fun loginTenant(name: String, nik: String, phone: String): Result<Unit> = runCatching {
        val data = client.call {
            this.loginTenant(LoginTenantRequest(name.trim(), nik.trim(), PhoneUtils.normalize(phone)))
        }
        startSession(data)
    }

    /** Logout: lepas HP dari notifikasi, cabut token di server (jika online), lalu hapus sesi dan data lokal. */
    suspend fun logout() {
        if (sessionStore.currentSession() != null) {
            pushRepository.unregisterDevice() // admin maupun penghuni
        }
        runCatching { client.callUnit { this.logout() } }
        endSession()
    }

    /** Dipanggil juga saat server menjawab 401 (token kedaluwarsa/dicabut). */
    suspend fun endSession() {
        sessionStore.clear()
        clearLocalData()
    }

    /**
     * Nomor WhatsApp pengelola untuk tombol "Hubungi pengelola" di halaman login penghuni.
     * Diambil dari server (nomor akun admin pertama); jika offline, pakai BuildConfig.OWNER_WHATSAPP.
     */
    suspend fun ownerPhone(): String? =
        runCatching { client.call { this.contact() }.phone }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: BuildConfig.OWNER_WHATSAPP.takeIf { it.isNotBlank() }

    private suspend fun startSession(data: LoginData) {
        // Data milik akun sebelumnya (jika ada) dibuang supaya tidak tercampur.
        clearLocalData()
        sessionStore.save(data.user.toSession(), data.token, data.expiresAt)
    }

    private suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
