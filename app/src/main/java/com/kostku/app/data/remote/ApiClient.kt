package com.kostku.app.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.kostku.app.BuildConfig
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Kesalahan dari API, dengan pesan yang siap ditampilkan ke pengguna. */
class ApiException(
    message: String,
    /** Kode HTTP; 0 = tidak tersambung ke server. */
    val code: Int,
    /** Pesan per kolom formulir (hanya untuk kode 422/403/409 tertentu). */
    val fieldErrors: Map<String, String> = emptyMap()
) : Exception(message)

/**
 * Membuat Retrofit + OkHttp.
 *
 * @param tokenProvider mengambil token yang tersimpan (dari SharedPreferences).
 * @param onUnauthorized dipanggil jika server menjawab 401 pada permintaan yang memakai token
 *   (token kedaluwarsa / dicabut) → aplikasi kembali ke halaman login.
 */
class ApiClient(
    tokenProvider: () -> String?,
    onUnauthorized: () -> Unit
) {
    val gson: Gson = GsonBuilder().create()

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider()
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        if (response.code == 401 && request.header("Authorization") != null) {
            onUnauthorized()
        }
        response
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS) // unggah foto
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                // Log permintaan di Logcat (filter: "okhttp") hanya saat debug. Token disensor.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader("Authorization")
                    }
                )
            }
        }
        .build()

    val api: KostKuApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(KostKuApi::class.java)

    /** Menjalankan permintaan dan mengembalikan isi "data". Melempar [ApiException] jika gagal. */
    suspend fun <T> call(block: suspend KostKuApi.() -> Response<ApiEnvelope<T>>): T {
        val envelope = execute(block)
        return envelope.data ?: throw ApiException("Respons server tidak lengkap.", 200)
    }

    /** Untuk endpoint yang tidak mengembalikan data (hapus, logout, dll.). */
    suspend fun callUnit(block: suspend KostKuApi.() -> Response<ApiEnvelope<JsonElement>>) {
        execute(block)
    }

    private suspend fun <T> execute(block: suspend KostKuApi.() -> Response<ApiEnvelope<T>>): ApiEnvelope<T> {
        val response = try {
            api.block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ApiException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", 0)
        } catch (e: RuntimeException) {
            // Biasanya JSON tidak sesuai (mis. server mengirim halaman HTML galat).
            throw ApiException("Respons server tidak dapat dibaca. Coba lagi nanti.", 0)
        }
        val body = response.body()
        if (response.isSuccessful && body != null && body.success) return body
        throw parseError(response)
    }

    private fun parseError(response: Response<*>): ApiException {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        val type = object : TypeToken<ApiEnvelope<JsonElement>>() {}.type
        val parsed: ApiEnvelope<JsonElement>? = raw?.let { runCatching { gson.fromJson<ApiEnvelope<JsonElement>>(it, type) }.getOrNull() }
        val message = parsed?.message?.takeIf { it.isNotBlank() } ?: when (response.code()) {
            401 -> "Sesi berakhir. Silakan login kembali."
            403 -> "Anda tidak memiliki akses ke fitur ini."
            404 -> "Data tidak ditemukan."
            429 -> "Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi."
            in 500..599 -> "Server sedang bermasalah (kode ${response.code()}). Coba lagi nanti."
            else -> "Permintaan gagal (kode ${response.code()})."
        }
        return ApiException(message, response.code(), parsed?.errors.orEmpty())
    }
}
