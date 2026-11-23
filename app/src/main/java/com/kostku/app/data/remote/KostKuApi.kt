package com.kostku.app.data.remote

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Daftar endpoint KostKu REST API (basis: BuildConfig.API_BASE_URL, mis. https://.../api/v1/).
 * Alamat ditulis TANPA garis miring di depan supaya digabung dengan basis URL.
 *
 * Endpoint yang tidak mengembalikan data (logout, hapus, dsb.) memakai ApiEnvelope<JsonElement>.
 */
interface KostKuApi {

    // ---------- Publik ----------
    /** Nama & nomor WhatsApp pengelola untuk tombol "Hubungi pengelola" (tanpa login). */
    @GET("public/contact")
    suspend fun contact(): Response<ApiEnvelope<ContactDto>>

    // ---------- Auth ----------
    @POST("auth/login-admin")
    suspend fun loginAdmin(@Body body: LoginAdminRequest): Response<ApiEnvelope<LoginData>>

    @POST("auth/register-admin")
    suspend fun registerAdmin(@Body body: RegisterAdminRequest): Response<ApiEnvelope<LoginData>>

    @POST("auth/login-tenant")
    suspend fun loginTenant(@Body body: LoginTenantRequest): Response<ApiEnvelope<LoginData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiEnvelope<JsonElement>>

    @GET("me")
    suspend fun me(): Response<ApiEnvelope<UserDto>>

    // ---------- Kamar ----------
    @GET("rooms")
    suspend fun rooms(): Response<ApiEnvelope<List<RoomListDto>>>

    @GET("rooms/{id}")
    suspend fun room(@Path("id") id: Long): Response<ApiEnvelope<RoomDetailDto>>

    @PUT("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: Long, @Body body: UpdateRoomRequest): Response<ApiEnvelope<RoomDetailDto>>

    @Multipart
    @POST("rooms/{id}/photos")
    suspend fun uploadRoomPhoto(
        @Path("id") roomId: Long,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody
    ): Response<ApiEnvelope<PhotoDto>>

    @DELETE("photos/{id}")
    suspend fun deletePhoto(@Path("id") id: Long): Response<ApiEnvelope<JsonElement>>

    @GET("my/room")
    suspend fun myRoom(): Response<ApiEnvelope<RoomDetailDto>>

    @GET("my/profile")
    suspend fun myProfile(): Response<ApiEnvelope<TenantDto>>

    @PUT("my/profile")
    suspend fun updateMyProfile(@Body body: UpdateMyProfileRequest): Response<ApiEnvelope<TenantDto>>

    // ---------- Penghuni ----------
    @GET("tenants")
    suspend fun tenants(): Response<ApiEnvelope<List<TenantDto>>>

    @GET("tenants/{id}")
    suspend fun tenant(@Path("id") id: Long): Response<ApiEnvelope<TenantDto>>

    @POST("tenants")
    suspend fun createTenant(@Body body: CreateTenantRequest): Response<ApiEnvelope<TenantDto>>

    @PUT("tenants/{id}")
    suspend fun updateTenant(@Path("id") id: Long, @Body body: UpdateTenantRequest): Response<ApiEnvelope<TenantDto>>

    /** Mengakhiri sewa (penghuni dinonaktifkan, kamar kembali kosong). */
    @DELETE("tenants/{id}")
    suspend fun endTenancy(@Path("id") id: Long): Response<ApiEnvelope<JsonElement>>

    // ---------- Pembayaran ----------
    /**
     * Admin: tagihan satu bulan (bawaan bulan ini), atau semua periode jika [period] = "all".
     * Penghuni: semua tagihan miliknya.
     */
    @GET("payments")
    suspend fun payments(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
        @Query("status") status: String? = null,
        @Query("period") period: String? = null,
        /** "pending" = admin: semua bukti transfer yang menunggu verifikasi (semua periode) */
        @Query("transfer") transfer: String? = null
    ): Response<ApiEnvelope<List<PaymentDto>>>

    @GET("payments/{id}/receipt")
    suspend fun receipt(@Path("id") id: Long): Response<ApiEnvelope<ReceiptDto>>

    @POST("payments/{id}/confirm-cash")
    suspend fun confirmCash(@Path("id") id: Long): Response<ApiEnvelope<PaymentDto>>

    @POST("payments/{id}/remind")
    suspend fun remind(@Path("id") id: Long): Response<ApiEnvelope<ReminderDto>>

    /** Admin mencatat lunas: method = "Tunai" | "Transfer"; proof = foto bukti (opsional, transfer). */
    @Multipart
    @POST("payments/{id}/confirm")
    suspend fun confirmPayment(
        @Path("id") id: Long,
        @Part("method") method: RequestBody,
        @Part proof: MultipartBody.Part?
    ): Response<ApiEnvelope<PaymentDto>>

    /** Penghuni mengirim bukti transfer. */
    @Multipart
    @POST("payments/{id}/transfer")
    suspend fun submitTransfer(
        @Path("id") id: Long,
        @Part proof: MultipartBody.Part,
        @Part("note") note: RequestBody
    ): Response<ApiEnvelope<PaymentDto>>

    @POST("payments/{id}/verify-transfer")
    suspend fun verifyTransfer(@Path("id") id: Long): Response<ApiEnvelope<PaymentDto>>

    @POST("payments/{id}/reject-transfer")
    suspend fun rejectTransfer(@Path("id") id: Long, @Body body: RejectTransferRequest): Response<ApiEnvelope<PaymentDto>>

    // ---------- Pengaturan metode pembayaran ----------
    @GET("payment-settings")
    suspend fun paymentSettings(): Response<ApiEnvelope<PaymentSettingsDto>>

    @PUT("payment-settings")
    suspend fun updatePaymentSettings(@Body body: PaymentSettingsDto): Response<ApiEnvelope<PaymentSettingsDto>>

    // ---------- Pengumuman ----------
    @GET("announcements")
    suspend fun announcements(): Response<ApiEnvelope<List<AnnouncementDto>>>

    @POST("announcements")
    suspend fun createAnnouncement(@Body body: AnnouncementRequest): Response<ApiEnvelope<AnnouncementDto>>

    @DELETE("announcements/{id}")
    suspend fun deleteAnnouncement(@Path("id") id: Long): Response<ApiEnvelope<JsonElement>>

    // ---------- Laporan kerusakan ----------
    @GET("reports")
    suspend fun reports(): Response<ApiEnvelope<List<ReportDto>>>

    /** Penghuni mengirim laporan; [photo] boleh null (tanpa foto). */
    @Multipart
    @POST("reports")
    suspend fun createReport(
        @Part("category") category: RequestBody,
        @Part("description") description: RequestBody,
        @Part photo: MultipartBody.Part?
    ): Response<ApiEnvelope<ReportDto>>

    /** Admin mengubah status laporan. */
    @PUT("reports/{id}")
    suspend fun updateReportStatus(@Path("id") id: Long, @Body body: ReportStatusRequest): Response<ApiEnvelope<ReportDto>>

    // ---------- Notifikasi push (penghuni) ----------
    @POST("devices")
    suspend fun registerDevice(@Body body: DeviceRequest): Response<ApiEnvelope<JsonElement>>

    @POST("devices/unregister")
    suspend fun unregisterDevice(@Body body: DeviceRequest): Response<ApiEnvelope<JsonElement>>

    @GET("notifications")
    suspend fun notifications(): Response<ApiEnvelope<NotificationListDto>>

    @POST("notifications/read-all")
    suspend fun readAllNotifications(): Response<ApiEnvelope<JsonElement>>

    @POST("notifications/{id}/read")
    suspend fun readNotification(@Path("id") id: Long): Response<ApiEnvelope<JsonElement>>
}
