package com.kostku.app.data.remote

import com.google.gson.annotations.SerializedName

/*
 * Model data (DTO) yang sama persis dengan JSON dari KostKu REST API (PHP).
 * Semua respons server dibungkus seperti ini:
 *   sukses : {"success":true,  "message":"...", "data": ...}
 *   gagal  : {"success":false, "message":"...", "errors": {"field":"pesan"}}
 *
 * Catatan: Gson tidak memeriksa null-safety Kotlin, jadi kolom yang bisa kosong dari
 * server dibuat nullable (pakai tanda ?).
 */

data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("errors") val errors: Map<String, String>? = null
)

// ───────────────────────── Auth ─────────────────────────

data class LoginAdminRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String
)

data class RegisterAdminRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("secret_code") val secretCode: String
)

data class LoginTenantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("phone") val phone: String
)

data class UserDto(
    @SerializedName("id") val id: Long,
    /** "admin" atau "tenant" */
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String,
    /** Contoh "Kamar 102" untuk penghuni, null untuk admin. */
    @SerializedName("label") val label: String?
)

/** GET /public/contact (null jika belum ada admin terdaftar) */
data class ContactDto(
    @SerializedName("name") val name: String?,
    @SerializedName("phone") val phone: String?
)

data class LoginData(
    @SerializedName("token") val token: String,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("user") val user: UserDto
)

// ───────────────────────── Kamar ─────────────────────────

data class RoomTenantBrief(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("entry_date") val entryDate: String?
)

/** Satu item dari GET /rooms */
data class RoomListDto(
    @SerializedName("id") val id: Long,
    @SerializedName("number") val number: Int,
    @SerializedName("floor") val floor: Int,
    /** occupied | empty | maintenance */
    @SerializedName("status") val status: String,
    @SerializedName("monthly_rate") val monthlyRate: Long,
    @SerializedName("facilities") val facilities: String?,
    @SerializedName("maintenance_note") val maintenanceNote: String?,
    @SerializedName("cover_photo") val coverPhoto: String?,
    @SerializedName("photo_count") val photoCount: Int,
    /** Semua foto kamar (backend versi baru). Null pada backend lama → foto diambil per kamar. */
    @SerializedName("photos") val photos: List<PhotoDto>?,
    @SerializedName("tenant") val tenant: RoomTenantBrief?
)

data class PhotoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("url") val url: String,
    @SerializedName("caption") val caption: String?,
    @SerializedName("created_at") val createdAt: String?
)

/** GET /rooms/{id}, GET /my/room, PUT /rooms/{id} (tanpa photos/tenant) */
data class RoomDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("number") val number: Int,
    @SerializedName("floor") val floor: Int,
    @SerializedName("status") val status: String,
    @SerializedName("monthly_rate") val monthlyRate: Long,
    @SerializedName("facilities") val facilities: String?,
    @SerializedName("maintenance_note") val maintenanceNote: String?,
    @SerializedName("photos") val photos: List<PhotoDto>?,
    @SerializedName("tenant") val tenant: RoomTenantBrief?
)

data class UpdateRoomRequest(
    /** empty | maintenance */
    @SerializedName("status") val status: String,
    @SerializedName("maintenance_note") val maintenanceNote: String? = null
)

// ───────────────────────── Penghuni ─────────────────────────

data class TenantDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("room_id") val roomId: Long,
    @SerializedName("room_number") val roomNumber: Int?,
    @SerializedName("floor") val floor: Int?,
    /** YYYY-MM-DD */
    @SerializedName("entry_date") val entryDate: String,
    @SerializedName("monthly_rate") val monthlyRate: Long,
    @SerializedName("active") val active: Boolean,
    /** Hanya ada di GET /tenants/{id} */
    @SerializedName("payments") val payments: List<PaymentDto>?
)

data class CreateTenantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("nik") val nik: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("room_id") val roomId: Long,
    /** YYYY-MM-DD */
    @SerializedName("entry_date") val entryDate: String,
    @SerializedName("monthly_rate") val monthlyRate: Long
)

// ───────────────────────── Pembayaran ─────────────────────────

data class PaymentDto(
    @SerializedName("id") val id: Long,
    @SerializedName("tenant_id") val tenantId: Long,
    @SerializedName("tenant_name") val tenantName: String?,
    @SerializedName("room_id") val roomId: Long,
    @SerializedName("room_number") val roomNumber: Int?,
    @SerializedName("period_year") val periodYear: Int,
    @SerializedName("period_month") val periodMonth: Int,
    @SerializedName("amount") val amount: Long,
    /** paid | unpaid */
    @SerializedName("status") val status: String,
    @SerializedName("method") val method: String?,
    @SerializedName("receipt_no") val receiptNo: String?,
    /** YYYY-MM-DD */
    @SerializedName("due_date") val dueDate: String,
    /** ISO-8601, mis. 2026-11-17T08:24:00+08:00 */
    @SerializedName("paid_at") val paidAt: String?,
    @SerializedName("reminder_sent_at") val reminderSentAt: String?
)

data class ReceiptDto(
    @SerializedName("receipt_no") val receiptNo: String?,
    @SerializedName("tenant_name") val tenantName: String?,
    @SerializedName("room_number") val roomNumber: Int,
    @SerializedName("floor") val floor: Int,
    @SerializedName("period_label") val periodLabel: String?,
    @SerializedName("paid_at") val paidAt: String?,
    @SerializedName("method") val method: String?,
    @SerializedName("amount") val amount: Long,
    @SerializedName("received_by") val receivedBy: String?
)

data class ReminderDto(
    @SerializedName("message") val message: String,
    @SerializedName("phone") val phone: String
)

// ───────────────────────── Pengumuman ─────────────────────────

data class AnnouncementDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    /** all | floor_1 | floor_2 */
    @SerializedName("target") val target: String,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("author") val author: String?
)

data class AnnouncementRequest(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("target") val target: String,
    /** ISO-8601 dengan zona waktu, mis. 2026-11-20T00:00:00+08:00 */
    @SerializedName("published_at") val publishedAt: String
)

// ───────────────────────── Laporan kerusakan ─────────────────────────

data class ReportDto(
    @SerializedName("id") val id: Long,
    @SerializedName("tenant_id") val tenantId: Long,
    @SerializedName("tenant_name") val tenantName: String?,
    @SerializedName("room_id") val roomId: Long,
    @SerializedName("room_number") val roomNumber: Int?,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("photo_url") val photoUrl: String?,
    /** sent | in_progress | done */
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String?
)

data class ReportStatusRequest(
    @SerializedName("status") val status: String
)

// ───────────────────────── Notifikasi (Tahap H) ─────────────────────────

data class DeviceRequest(
    @SerializedName("fcm_token") val fcmToken: String,
    @SerializedName("platform") val platform: String? = null
)

data class NotificationDto(
    @SerializedName("id") val id: Long,
    /** payment_paid | payment_reminder | repair_status | due:<id>:<tahap> | announcement:<id> */
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("sent_at") val sentAt: String?,
    @SerializedName("read") val read: Boolean
)

/** GET /notifications */
data class NotificationListDto(
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("items") val items: List<NotificationDto>
)
