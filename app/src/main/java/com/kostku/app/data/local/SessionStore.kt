package com.kostku.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

enum class UserRole { ADMIN, TENANT }

data class Session(
    val role: UserRole,
    val userId: Long,
    val name: String,
    /** Info tambahan, mis. "Kamar 102" untuk penghuni. */
    val label: String? = null
)

/**
 * Menyimpan sesi login (token API + data pengguna) dan status panduan di SharedPreferences.
 *
 * File: /data/data/com.kostku.app/shared_prefs/kostku_session.xml (MODE_PRIVATE, hanya bisa
 * dibaca aplikasi ini; android:allowBackup="false" sehingga tidak ikut dicadangkan).
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private object Keys {
        const val TOKEN = "token"
        const val TOKEN_EXPIRES_AT = "token_expires_at"
        const val ROLE = "role"
        const val USER_ID = "user_id"
        const val NAME = "name"
        const val LABEL = "label"
        const val WELCOME_COMPLETED = "welcome_completed"
        const val ADMIN_TOUR_COMPLETED = "admin_tour_completed"
        const val TENANT_TOUR_COMPLETED = "tenant_tour_completed"
    }

    /** Token Bearer untuk API; dibaca oleh OkHttp setiap kali mengirim permintaan. */
    fun token(): String? = prefs.getString(Keys.TOKEN, null)

    private fun readSession(): Session? {
        if (token().isNullOrBlank()) return null
        val role = prefs.getString(Keys.ROLE, null)?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
        val id = prefs.getLong(Keys.USER_ID, -1L)
        val name = prefs.getString(Keys.NAME, null)
        return if (role != null && id > 0 && name != null) {
            Session(role, id, name, prefs.getString(Keys.LABEL, null))
        } else null
    }

    /** Mengalirkan nilai terbaru setiap kali isi SharedPreferences berubah. */
    private fun <T> observe(read: () -> T): Flow<T> = callbackFlow {
        trySend(read())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    val session: Flow<Session?> = observe { readSession() }

    /** Sesi saat ini (tanpa Flow), mis. untuk layanan notifikasi yang berjalan di latar belakang. */
    fun currentSession(): Session? = readSession()

    val isWelcomeCompleted: Flow<Boolean> = observe { prefs.getBoolean(Keys.WELCOME_COMPLETED, false) }
    val isAdminTourCompleted: Flow<Boolean> = observe { prefs.getBoolean(Keys.ADMIN_TOUR_COMPLETED, false) }
    val isTenantTourCompleted: Flow<Boolean> = observe { prefs.getBoolean(Keys.TENANT_TOUR_COMPLETED, false) }

    fun setWelcomeCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(Keys.WELCOME_COMPLETED, completed).apply()
    }

    fun setAdminTourCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(Keys.ADMIN_TOUR_COMPLETED, completed).apply()
    }

    fun setTenantTourCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(Keys.TENANT_TOUR_COMPLETED, completed).apply()
    }

    /** Disimpan setelah login berhasil. */
    fun save(session: Session, token: String, expiresAt: String?) {
        prefs.edit()
            .putString(Keys.TOKEN, token)
            .putString(Keys.TOKEN_EXPIRES_AT, expiresAt)
            .putString(Keys.ROLE, session.role.name)
            .putLong(Keys.USER_ID, session.userId)
            .putString(Keys.NAME, session.name)
            .putString(Keys.LABEL, session.label)
            .apply()
    }

    /** Logout: hapus token dan data pengguna, tetapi status panduan tetap disimpan. */
    fun clear() {
        prefs.edit()
            .remove(Keys.TOKEN)
            .remove(Keys.TOKEN_EXPIRES_AT)
            .remove(Keys.ROLE)
            .remove(Keys.USER_ID)
            .remove(Keys.NAME)
            .remove(Keys.LABEL)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "kostku_session"
    }
}
