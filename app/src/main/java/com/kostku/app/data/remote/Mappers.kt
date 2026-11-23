package com.kostku.app.data.remote

import com.kostku.app.data.local.AnnouncementEntity
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RepairReportEntity
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.local.RoomPhotoEntity
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.local.Session
import com.kostku.app.data.local.TenantEntity
import com.kostku.app.data.local.TransferStatus
import com.kostku.app.data.local.UserRole
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Mengubah JSON dari API menjadi entity Room yang sudah dipakai layar-layar aplikasi. */
object ApiDates {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** "2026-11-17T08:24:00+08:00" → epoch millis */
    fun isoToMillis(iso: String?): Long? =
        iso?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

    /** "2026-11-17" → awal hari (00.00) */
    fun dateToMillis(date: String?): Long? =
        date?.let { runCatching { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() }

    /** Jatuh tempo "2026-11-10" → 10 Nov pukul 23.59 (sama seperti perhitungan aplikasi sebelumnya). */
    fun dueToMillis(date: String?): Long? =
        dateToMillis(date)?.plus((23L * 60 + 59) * 60_000L)

    /** epoch millis → "2026-11-17" */
    fun millisToDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

    /** epoch millis → "2026-11-17T08:24:00+08:00" */
    fun millisToIso(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

private inline fun <reified E : Enum<E>> enumOf(value: String?, fallback: E): E =
    value?.let { v -> enumValues<E>().firstOrNull { it.name.equals(v, ignoreCase = true) } } ?: fallback

fun UserDto.toSession(): Session = Session(
    role = if (role == "admin") UserRole.ADMIN else UserRole.TENANT,
    userId = id,
    name = name,
    label = label
)

fun roomEntity(
    id: Long, number: Int, floor: Int, status: String, monthlyRate: Long,
    facilities: String?, maintenanceNote: String?
): RoomEntity = RoomEntity(
    id = id,
    number = number,
    floor = floor,
    status = enumOf(status, RoomStatus.EMPTY),
    monthlyRate = monthlyRate,
    facilities = facilities ?: "",
    maintenanceNote = maintenanceNote
)

fun RoomListDto.toEntity(): RoomEntity =
    roomEntity(id, number, floor, status, monthlyRate, facilities, maintenanceNote)

fun RoomDetailDto.toEntity(): RoomEntity =
    roomEntity(id, number, floor, status, monthlyRate, facilities, maintenanceNote)

fun PhotoDto.toEntity(roomId: Long): RoomPhotoEntity = RoomPhotoEntity(
    id = id,
    roomId = roomId,
    uri = url,
    caption = caption.orEmpty(),
    createdAt = ApiDates.isoToMillis(createdAt) ?: 0L
)

fun TenantDto.toEntity(): TenantEntity = TenantEntity(
    id = id,
    name = name,
    nik = nik,
    phone = phone,
    roomId = roomId,
    entryDate = ApiDates.dateToMillis(entryDate) ?: 0L,
    monthlyRate = monthlyRate,
    active = active
)

/**
 * Data penghuni yang hanya diketahui sebagian (mis. dari /my/room atau dari nama pada tagihan lama).
 * Dipakai supaya nama tetap tampil walau data lengkapnya tidak tersedia di layar tersebut.
 */
fun partialTenant(
    id: Long, name: String, roomId: Long, phone: String? = null,
    entryDate: String? = null, monthlyRate: Long = 0L, active: Boolean = false
): TenantEntity = TenantEntity(
    id = id,
    name = name,
    nik = "",
    phone = phone.orEmpty(),
    roomId = roomId,
    entryDate = ApiDates.dateToMillis(entryDate) ?: 0L,
    monthlyRate = monthlyRate,
    active = active
)

fun PaymentDto.toEntity(receivedBy: String? = null): PaymentEntity = PaymentEntity(
    id = id,
    tenantId = tenantId,
    roomId = roomId,
    periodYear = periodYear,
    periodMonth = periodMonth,
    amount = amount,
    status = enumOf(status, PaymentStatus.UNPAID),
    method = method,
    paidAt = ApiDates.isoToMillis(paidAt),
    receiptNo = receiptNo,
    receivedBy = receivedBy,
    dueDate = ApiDates.dueToMillis(dueDate) ?: 0L,
    reminderSentAt = ApiDates.isoToMillis(reminderSentAt),
    transferStatus = enumOf(transferStatus, TransferStatus.NONE),
    transferNote = transferNote,
    transferSubmittedAt = ApiDates.isoToMillis(transferSubmittedAt),
    rejectReason = rejectReason,
    hasProof = hasProof == true
)

/** Pengaturan metode pembayaran (rekening tujuan + metode yang aktif). */
data class PaymentSettings(
    val transferEnabled: Boolean,
    val cashEnabled: Boolean,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val transferNote: String
) {
    val hasAccount: Boolean get() = bankName.isNotBlank() && accountNumber.isNotBlank()

    fun toDto() = PaymentSettingsDto(transferEnabled, cashEnabled, bankName, accountNumber, accountHolder, transferNote)

    companion object {
        /** Dipakai sebelum data dari server termuat. */
        val DEFAULT = PaymentSettings(false, true, "", "", "", "")
    }
}

fun PaymentSettingsDto.toModel(): PaymentSettings = PaymentSettings(
    transferEnabled = transferEnabled,
    cashEnabled = cashEnabled,
    bankName = bankName.orEmpty(),
    accountNumber = accountNumber.orEmpty(),
    accountHolder = accountHolder.orEmpty(),
    transferNote = transferNote.orEmpty()
)

fun AnnouncementDto.toEntity(): AnnouncementEntity = AnnouncementEntity(
    id = id,
    title = title,
    body = body,
    target = enumOf(target, AnnouncementTarget.ALL),
    publishedAt = ApiDates.isoToMillis(publishedAt) ?: 0L,
    author = author ?: "Pengelola"
)

fun ReportDto.toEntity(): RepairReportEntity = RepairReportEntity(
    id = id,
    tenantId = tenantId,
    roomId = roomId,
    category = category,
    description = description,
    photoUri = photoUrl,
    status = enumOf(status, ReportStatus.SENT),
    createdAt = ApiDates.isoToMillis(createdAt) ?: 0L
)

/** Satu notifikasi di kotak masuk penghuni (tidak disimpan di Room; selalu diambil dari server). */
data class InboxItem(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val sentAt: Long,
    val read: Boolean
) {
    /** Notifikasi pengumuman sudah tampil sebagai kartu pengumuman, jadi tidak ditampilkan dua kali. */
    val isAnnouncement: Boolean get() = type.startsWith("announcement")
    val isPayment: Boolean get() = type.startsWith("payment") || type.startsWith("due:")
    val isRepair: Boolean get() = type.startsWith("repair")
}

fun NotificationDto.toItem(): InboxItem = InboxItem(
    id = id,
    type = type,
    title = title,
    body = body,
    sentAt = ApiDates.isoToMillis(sentAt) ?: 0L,
    read = read
)

/** RoomStatus/AnnouncementTarget/ReportStatus → teks yang diharapkan server (huruf kecil). */
val RoomStatus.apiValue: String get() = name.lowercase()
val AnnouncementTarget.apiValue: String get() = name.lowercase()
val ReportStatus.apiValue: String get() = name.lowercase()
