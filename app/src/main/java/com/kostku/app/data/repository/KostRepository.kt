package com.kostku.app.data.repository

import androidx.room.withTransaction
import com.kostku.app.data.local.AnnouncementEntity
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.BuildConfig
import com.kostku.app.data.local.AppDatabase
import com.kostku.app.data.local.SessionStore
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RepairReportEntity
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.local.RoomPhotoEntity
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.local.TenantEntity
import com.kostku.app.data.remote.AnnouncementRequest
import com.kostku.app.data.remote.ApiClient
import com.kostku.app.data.remote.ApiDates
import com.kostku.app.data.remote.CreateTenantRequest
import com.kostku.app.data.remote.InboxItem
import com.kostku.app.data.remote.PaymentSettings
import com.kostku.app.data.remote.RejectTransferRequest
import com.kostku.app.data.remote.UpdateMyProfileRequest
import com.kostku.app.data.remote.UpdateTenantRequest
import com.kostku.app.data.remote.toModel
import com.kostku.app.data.remote.PaymentDto
import com.kostku.app.data.remote.ReportDto
import com.kostku.app.data.remote.ReportStatusRequest
import com.kostku.app.data.remote.RoomDetailDto
import com.kostku.app.data.remote.UpdateRoomRequest
import com.kostku.app.data.remote.apiValue
import com.kostku.app.data.remote.partialTenant
import com.kostku.app.data.remote.toEntity
import com.kostku.app.data.remote.toItem
import com.kostku.app.util.Fmt
import com.kostku.app.util.PhoneUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.YearMonth

class OperationException(message: String) : Exception(message)

data class RoomCardData(val room: RoomEntity, val tenant: TenantEntity?, val payment: PaymentEntity?)
data class TenantWithRoom(val tenant: TenantEntity, val room: RoomEntity?)
data class ReportWithInfo(val report: RepairReportEntity, val roomNumber: Int?, val tenantName: String?)
data class PaidInfo(val payment: PaymentEntity, val roomNumber: Int?, val tenantName: String?)

data class ReceiptData(
    val receiptNo: String,
    val tenantName: String,
    val roomNumber: Int,
    val floor: Int,
    val periodLabel: String,
    val paidDate: String,
    val method: String,
    val amount: Long,
    val receivedBy: String
)

/**
 * Sumber data utama aplikasi.
 *
 * Server (KostKu REST API) adalah sumber kebenaran. Database Room di HP hanya salinan (cache):
 *  - fungsi `observe…` membaca Room → layar langsung tampil dan otomatis ikut berubah;
 *  - fungsi `sync…` mengambil data terbaru dari API lalu menyimpannya ke Room;
 *  - operasi tulis (tambah/ubah/hapus) dikirim ke API dulu, baru Room diperbarui dari jawabannya.
 */
class KostRepository(
    private val db: AppDatabase,
    private val client: ApiClient,
    private val sessionStore: SessionStore
) {

    /** Pengaturan metode pembayaran terakhir dari server (rekening transfer, metode aktif). */
    private val _paymentSettings = MutableStateFlow(PaymentSettings.DEFAULT)
    val paymentSettings: StateFlow<PaymentSettings> = _paymentSettings.asStateFlow()

    private val roomDao = db.roomDao()
    private val tenantDao = db.tenantDao()
    private val paymentDao = db.paymentDao()
    private val photoDao = db.roomPhotoDao()
    private val announcementDao = db.announcementDao()
    private val reportDao = db.repairReportDao()

    // =====================================================================
    //  Membaca dari Room (dipakai layar; tidak berubah dari versi lokal)
    // =====================================================================

    fun observeRooms(): Flow<List<RoomEntity>> = roomDao.observeAll()
    fun observeRoom(id: Long): Flow<RoomEntity?> = roomDao.observeById(id)
    fun observeAvailableRooms(): Flow<List<RoomEntity>> = roomDao.observeAvailable()
    fun observeAllPhotos(): Flow<List<RoomPhotoEntity>> = photoDao.observeAll()

    fun observeRoomCards(ym: YearMonth): Flow<List<RoomCardData>> = combine(
        roomDao.observeAll(),
        tenantDao.observeActive(),
        paymentDao.observeByPeriod(ym.year, ym.monthValue)
    ) { rooms, tenants, payments ->
        val tenantByRoom = tenants.associateBy { it.roomId }
        val paymentByTenant = payments.associateBy { it.tenantId }
        rooms.map { room ->
            val tenant = tenantByRoom[room.id]
            RoomCardData(room, tenant, tenant?.let { paymentByTenant[it.id] })
        }
    }

    fun observeTenant(id: Long): Flow<TenantEntity?> = tenantDao.observeById(id)

    fun observeTenantsWithRoom(): Flow<List<TenantWithRoom>> = combine(
        tenantDao.observeActive(), roomDao.observeAll()
    ) { tenants, rooms ->
        val byId = rooms.associateBy { it.id }
        tenants.map { TenantWithRoom(it, byId[it.roomId]) }.sortedBy { it.room?.number ?: Int.MAX_VALUE }
    }

    fun observePaymentsByTenant(tenantId: Long): Flow<List<PaymentEntity>> = paymentDao.observeByTenant(tenantId)

    fun observeAllPayments(): Flow<List<PaymentEntity>> = paymentDao.observeAll()

    /** Semua penghuni, termasuk yang sudah keluar (untuk nama pada rekap & riwayat kamar). */
    fun observeAllTenants(): Flow<List<TenantEntity>> = tenantDao.observeAll()

    fun observePaidInfo(): Flow<List<PaidInfo>> = combine(
        paymentDao.observePaid(), tenantDao.observeAll(), roomDao.observeAll()
    ) { paid, tenants, rooms ->
        val tById = tenants.associateBy { it.id }
        val rById = rooms.associateBy { it.id }
        paid.map { PaidInfo(it, rById[it.roomId]?.number, tById[it.tenantId]?.name) }
    }

    fun observeAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.observeAll()

    fun observeReports(): Flow<List<ReportWithInfo>> = combine(
        reportDao.observeAll(), roomDao.observeAll(), tenantDao.observeAll()
    ) { reports, rooms, tenants ->
        val rById = rooms.associateBy { it.id }
        val tById = tenants.associateBy { it.id }
        reports.map { ReportWithInfo(it, rById[it.roomId]?.number, tById[it.tenantId]?.name) }
    }

    fun observeReportsByTenant(tenantId: Long): Flow<List<RepairReportEntity>> =
        reportDao.observeByTenant(tenantId)

    // =====================================================================
    //  Sinkronisasi: API → Room
    // =====================================================================

    /**
     * Admin: ambil kamar, penghuni, tagihan bulan [period], pengumuman, dan laporan.
     * @param includeHistory true = ambil tagihan SEMUA periode sekaligus (untuk daftar nota lunas).
     */
    suspend fun syncAdmin(period: YearMonth, includeHistory: Boolean = false) = coroutineScope {
        val roomsReq = async { client.call { this.rooms() } }
        val tenantsReq = async { client.call { this.tenants() } }
        val paymentsReq = async {
            if (includeHistory) client.call { this.payments(period = "all") }
            else client.call { this.payments(period.year, period.monthValue) }
        }
        val announcementsReq = async { client.call { this.announcements() } }
        val reportsReq = async { client.call { this.reports() } }
        // Bukti transfer yang menunggu verifikasi bisa untuk bulan mana pun.
        val pendingReq = async { runCatching { client.call { this.payments(transfer = "pending") } }.getOrDefault(emptyList()) }
        val settingsReq = async { runCatching { client.call { this.paymentSettings() } }.getOrNull() }

        val rooms = roomsReq.await()
        val tenants = tenantsReq.await()
        val payments = paymentsReq.await()
        val announcements = announcementsReq.await()
        val reports = reportsReq.await()
        val pending = pendingReq.await()

        // Kelompokkan per bulan. Bulan berjalan selalu disimpan, walau belum ada tagihan.
        val byPeriod = payments.groupBy { YearMonth.of(it.periodYear, it.periodMonth) }
            .toMutableMap()
            .apply { putIfAbsent(period, emptyList()) }

        db.withTransaction {
            // 1. Kamar (hapus yang sudah tidak ada di server lebih dulu)
            roomDao.deleteAllExcept(rooms.map { it.id })
            roomDao.upsertAll(rooms.map { it.toEntity() })

            // 2. Penghuni aktif (data lengkap), sisanya ditandai tidak aktif
            tenantDao.upsertAll(tenants.map { it.toEntity() })
            tenantDao.deactivateAllExcept(tenants.map { it.id })

            // 3. Tagihan per bulan
            byPeriod.forEach { (ym, list) -> savePeriod(ym, list) }
            rememberTenantNames(pending.map { Triple(it.tenantId, it.tenantName, it.roomId) })
            paymentDao.upsertAll(pending.map { it.toEntity(receivedBy = paymentDao.getById(it.id)?.receivedBy) })

            // 4. Pengumuman & laporan
            announcementDao.deleteAllExcept(announcements.map { it.id })
            announcementDao.upsertAll(announcements.map { it.toEntity() })
            saveReports(reports)
        }
        settingsReq.await()?.let { _paymentSettings.value = it.toModel() }

        // 5. Foto. Backend baru mengirim semua foto di daftar kamar; backend lama hanya jumlahnya,
        //    sehingga kamar yang jumlah fotonya berubah diambil ulang satu per satu.
        if (rooms.all { it.photos != null }) {
            db.withTransaction {
                rooms.forEach { r ->
                    photoDao.deleteByRoom(r.id)
                    photoDao.upsertAll(r.photos.orEmpty().map { it.toEntity(r.id) })
                }
            }
        } else {
            val changed = rooms.filter { it.photoCount != photoDao.countByRoom(it.id) }
            changed.map { r -> async { client.call { this.room(r.id) } } }.awaitAll()
                .forEach { saveRoomDetail(it) }
        }
    }

    /** Admin: detail satu penghuni + seluruh riwayat tagihannya. */
    suspend fun syncTenant(tenantId: Long) {
        val t = client.call { this.tenant(tenantId) }
        val payments = t.payments.orEmpty()
        db.withTransaction {
            tenantDao.upsert(t.toEntity())
            paymentDao.deleteTenantExcept(t.id, payments.map { it.id })
            paymentDao.upsertAll(payments.map { it.toEntity() })
        }
    }

    /** Admin: detail satu kamar (foto, penghuni). */
    suspend fun syncRoom(roomId: Long) {
        saveRoomDetail(client.call { this.room(roomId) })
    }

    /** Penghuni: kamar sendiri, semua tagihan, pengumuman, dan laporan miliknya. */
    suspend fun syncTenantSelf(tenantId: Long) = coroutineScope {
        val roomReq = async { client.call { this.myRoom() } }
        val paymentsReq = async { client.call { this.payments() } }
        val announcementsReq = async { client.call { this.announcements() } }
        val reportsReq = async { client.call { this.reports() } }
        val settingsReq = async { runCatching { client.call { this.paymentSettings() } }.getOrNull() }

        val room = roomReq.await()
        val payments = paymentsReq.await()
        val announcements = announcementsReq.await()
        val reports = reportsReq.await()
        settingsReq.await()?.let { _paymentSettings.value = it.toModel() }

        db.withTransaction {
            saveRoomDetail(room)
            // Kamar ini milik penghuni yang sedang login, jadi pastikan datanya tercatat.
            val self = room.tenant
            if (self != null && self.id == tenantId) {
                tenantDao.upsert(
                    partialTenant(
                        id = self.id, name = self.name, roomId = room.id, phone = self.phone,
                        entryDate = self.entryDate, monthlyRate = room.monthlyRate, active = true
                    )
                )
            }
            paymentDao.deleteTenantExcept(tenantId, payments.map { it.id })
            paymentDao.upsertAll(payments.map { it.toEntity() })

            announcementDao.deleteAllExcept(announcements.map { it.id })
            announcementDao.upsertAll(announcements.map { it.toEntity() })
            saveReports(reports)
        }
    }

    private suspend fun savePeriod(ym: YearMonth, list: List<PaymentDto>) {
        rememberTenantNames(list.map { Triple(it.tenantId, it.tenantName, it.roomId) })
        paymentDao.deletePeriodExcept(ym.year, ym.monthValue, list.map { it.id })
        // receivedBy yang sudah diketahui (mis. dari nota) tidak dihapus.
        val known = list.associate { it.id to paymentDao.getById(it.id)?.receivedBy }
        paymentDao.upsertAll(list.map { it.toEntity(receivedBy = known[it.id]) })
    }

    private suspend fun saveReports(list: List<ReportDto>) {
        rememberTenantNames(list.map { Triple(it.tenantId, it.tenantName, it.roomId) })
        reportDao.deleteAllExcept(list.map { it.id })
        reportDao.upsertAll(list.map { it.toEntity() })
    }

    private suspend fun saveRoomDetail(room: RoomDetailDto) {
        db.withTransaction {
            roomDao.upsert(room.toEntity())
            photoDao.deleteByRoom(room.id)
            photoDao.upsertAll(room.photos.orEmpty().map { it.toEntity(room.id) })
        }
    }

    /** Nama penghuni lama (sudah keluar) tetap tersimpan supaya nota/laporan lama tetap bernama. */
    private suspend fun rememberTenantNames(refs: List<Triple<Long, String?, Long>>) {
        val partial = refs.filter { it.second != null }.distinctBy { it.first }
            .map { (id, name, roomId) -> partialTenant(id = id, name = name!!, roomId = roomId) }
        if (partial.isNotEmpty()) tenantDao.insertIfMissing(partial)
    }

    // =====================================================================
    //  Operasi tulis: kirim ke API, lalu perbarui Room dari jawaban server
    // =====================================================================

    suspend fun addPhoto(roomId: Long, path: String, caption: String) {
        val file = File(path)
        val part = MultipartBody.Part.createFormData(
            "photo", file.name, file.asRequestBody("image/jpeg".toMediaType())
        )
        client.call { this.uploadRoomPhoto(roomId, part, caption.trim().toRequestBody(TEXT)) }
        file.delete() // salinan sementara di HP tidak diperlukan lagi
        syncRoom(roomId)
    }

    suspend fun deletePhoto(id: Long) {
        client.callUnit { this.deletePhoto(id) }
        photoDao.delete(id)
    }

    suspend fun setRoomMaintenance(roomId: Long, maintenance: Boolean): Result<Unit> = runCatching {
        val body = if (maintenance) {
            UpdateRoomRequest(RoomStatus.MAINTENANCE.apiValue, "Perbaikan fasilitas")
        } else {
            UpdateRoomRequest(RoomStatus.EMPTY.apiValue)
        }
        val updated = client.call { this.updateRoom(roomId, body) }
        roomDao.upsert(updated.toEntity())
    }

    suspend fun addTenant(
        name: String, nik: String, phone: String, roomId: Long, entryDate: Long, rate: Long
    ): Result<Long> = runCatching {
        val created = client.call {
            this.createTenant(
                CreateTenantRequest(
                    name = name.trim(),
                    nik = nik,
                    phone = PhoneUtils.normalize(phone),
                    roomId = roomId,
                    entryDate = ApiDates.millisToDate(entryDate),
                    monthlyRate = rate
                )
            )
        }
        tenantDao.upsert(created.toEntity())
        // Kamar berubah jadi terisi dan tagihan bulan ini dibuat oleh server.
        runCatching { syncAdmin(YearMonth.now()) }
        created.id
    }

    suspend fun endTenancy(tenantId: Long): Result<Unit> = runCatching {
        client.callUnit { this.endTenancy(tenantId) }
        runCatching { syncAdmin(YearMonth.now()) }.onFailure {
            // Jika gagal memuat ulang, setidaknya tandai tidak aktif secara lokal.
            tenantDao.getById(tenantId)?.let { t -> tenantDao.update(t.copy(active = false)) }
        }
    }

    /**
     * Admin mencatat tagihan lunas.
     * @param method "Tunai" atau "Transfer"
     * @param proofPath foto bukti transfer di HP (opsional, hanya untuk Transfer)
     */
    suspend fun confirmPayment(
        paymentId: Long, method: String, proofPath: String?, receivedBy: String
    ): Result<PaymentEntity> = runCatching {
        val file = proofPath?.let { File(it) }?.takeIf { method == METHOD_TRANSFER && it.exists() }
        val part = file?.let { MultipartBody.Part.createFormData("proof", it.name, it.asRequestBody(JPEG)) }
        val dto = client.call { this.confirmPayment(paymentId, method.toRequestBody(TEXT), part) }
        file?.delete()
        val entity = dto.toEntity(receivedBy = receivedBy)
        paymentDao.upsert(entity)
        entity
    }

    /** Admin menyetujui bukti transfer → lunas + nota. */
    suspend fun verifyTransfer(paymentId: Long, receivedBy: String): Result<PaymentEntity> = runCatching {
        val entity = client.call { this.verifyTransfer(paymentId) }.toEntity(receivedBy = receivedBy)
        paymentDao.upsert(entity)
        entity
    }

    /** Admin menolak bukti transfer dengan alasan; penghuni bisa mengirim ulang. */
    suspend fun rejectTransfer(paymentId: Long, reason: String): Result<Unit> = runCatching {
        val dto = client.call { this.rejectTransfer(paymentId, RejectTransferRequest(reason.trim())) }
        paymentDao.upsert(dto.toEntity())
    }

    /** Penghuni mengirim foto bukti transfer. */
    suspend fun submitTransfer(paymentId: Long, proofPath: String, note: String): Result<PaymentEntity> = runCatching {
        val file = File(proofPath)
        val part = MultipartBody.Part.createFormData("proof", file.name, file.asRequestBody(JPEG))
        val entity = client.call { this.submitTransfer(paymentId, part, note.trim().toRequestBody(TEXT)) }.toEntity()
        file.delete()
        paymentDao.upsert(entity)
        entity
    }

    /** Admin menyimpan rekening tujuan dan metode yang aktif. */
    suspend fun savePaymentSettings(settings: PaymentSettings): Result<PaymentSettings> = runCatching {
        val saved = client.call { this.updatePaymentSettings(settings.toDto()) }.toModel()
        _paymentSettings.value = saved
        saved
    }

    /** Alamat foto bukti transfer. [version] mencegah Coil memakai foto lama dari cache setelah diganti. */
    fun proofUrl(payment: PaymentEntity): String =
        BuildConfig.API_BASE_URL + "payments/${payment.id}/proof?v=${payment.transferSubmittedAt ?: payment.paidAt ?: 0L}"

    /** Header login untuk memuat foto bukti (bukti transfer tidak bisa dibuka tanpa login). */
    fun authHeader(): String? = sessionStore.token()?.let { "Bearer $it" }

    // ---- Data penghuni ----

    /** Admin mengubah data penghuni. */
    suspend fun updateTenant(
        tenantId: Long, name: String, nik: String, phone: String, entryDate: Long, rate: Long
    ): Result<TenantEntity> = runCatching {
        val dto = client.call {
            this.updateTenant(
                tenantId,
                UpdateTenantRequest(name.trim(), nik, PhoneUtils.normalize(phone), ApiDates.millisToDate(entryDate), rate)
            )
        }
        val entity = dto.toEntity()
        tenantDao.upsert(entity)
        roomDao.getById(entity.roomId)?.let { roomDao.upsert(it.copy(monthlyRate = rate)) }
        entity
    }

    /** Penghuni: data diri lengkap dari server. */
    suspend fun fetchMyProfile(): Result<TenantEntity> = runCatching {
        val entity = client.call { this.myProfile() }.toEntity()
        tenantDao.upsert(entity)
        entity
    }

    /** Penghuni mengubah nama & nomor HP. Nama di sesi ikut diperbarui. */
    suspend fun updateMyProfile(name: String, phone: String): Result<TenantEntity> = runCatching {
        val entity = client.call { this.updateMyProfile(UpdateMyProfileRequest(name.trim(), PhoneUtils.normalize(phone))) }.toEntity()
        tenantDao.upsert(entity)
        sessionStore.updateName(entity.name)
        entity
    }

    /** Nama & nomor pengelola (untuk layar bayar tunai). */
    suspend fun fetchContact(): com.kostku.app.data.remote.ContactDto? =
        runCatching { client.call { this.contact() } }.getOrNull()

    /** Mencatat pengingat di server (server juga mengirim notifikasi ke penghuni). */
    suspend fun markReminderSent(paymentId: Long) {
        client.call { this.remind(paymentId) }
        paymentDao.getById(paymentId)?.let {
            paymentDao.update(it.copy(reminderSentAt = System.currentTimeMillis()))
        }
    }

    /** Data nota diambil dari server (berisi nama admin penerima). */
    suspend fun buildReceipt(paymentId: Long): ReceiptData? {
        val remote = runCatching { client.call { this.receipt(paymentId) } }.getOrNull()
        if (remote != null) {
            return ReceiptData(
                receiptNo = remote.receiptNo ?: "-",
                tenantName = remote.tenantName ?: "-",
                roomNumber = remote.roomNumber,
                floor = remote.floor,
                periodLabel = remote.periodLabel ?: "-",
                paidDate = ApiDates.isoToMillis(remote.paidAt)?.let { Fmt.date(it) } ?: "-",
                method = remote.method ?: "Tunai",
                amount = remote.amount,
                receivedBy = remote.receivedBy ?: "-"
            )
        }
        // Tanpa internet: susun dari salinan lokal.
        val p = paymentDao.getById(paymentId) ?: return null
        if (p.status != PaymentStatus.PAID) return null
        val tenant = tenantDao.getById(p.tenantId)
        val room = roomDao.getById(p.roomId)
        return ReceiptData(
            receiptNo = p.receiptNo ?: "-",
            tenantName = tenant?.name ?: "-",
            roomNumber = room?.number ?: 0,
            floor = room?.floor ?: 0,
            periodLabel = Fmt.month(p.periodYear, p.periodMonth),
            paidDate = p.paidAt?.let { Fmt.date(it) } ?: "-",
            method = p.method ?: "Tunai",
            amount = p.amount,
            receivedBy = p.receivedBy ?: "-"
        )
    }

    suspend fun publishAnnouncement(
        title: String, body: String, target: AnnouncementTarget, publishedAt: Long
    ) {
        val created = client.call {
            this.createAnnouncement(
                AnnouncementRequest(
                    title = title.trim(),
                    body = body.trim(),
                    target = target.apiValue,
                    publishedAt = ApiDates.millisToIso(publishedAt)
                )
            )
        }
        announcementDao.upsertAll(listOf(created.toEntity()))
    }

    suspend fun deleteAnnouncement(id: Long) {
        client.callUnit { this.deleteAnnouncement(id) }
        announcementDao.delete(id)
    }

    suspend fun submitReport(category: String, description: String, photoPath: String?) {
        val file = photoPath?.let { File(it) }?.takeIf { it.exists() }
        val photo = file?.let {
            MultipartBody.Part.createFormData("photo", it.name, it.asRequestBody("image/jpeg".toMediaType()))
        }
        val created = client.call {
            this.createReport(category.toRequestBody(TEXT), description.trim().toRequestBody(TEXT), photo)
        }
        file?.delete()
        reportDao.upsert(created.toEntity())
    }

    suspend fun updateReportStatus(report: RepairReportEntity, status: ReportStatus) {
        val updated = client.call { this.updateReportStatus(report.id, ReportStatusRequest(status.apiValue)) }
        reportDao.upsert(updated.toEntity())
    }

    // =====================================================================
    //  Kotak masuk notifikasi penghuni (Tahap H)
    // =====================================================================

    suspend fun fetchNotifications(): List<InboxItem> =
        client.call { this.notifications() }.items.map { it.toItem() }

    suspend fun markAllNotificationsRead() {
        client.callUnit { this.readAllNotifications() }
    }

    companion object {
        const val METHOD_CASH = "Tunai"
        const val METHOD_TRANSFER = "Transfer"
        private val TEXT = "text/plain".toMediaType()
        private val JPEG = "image/jpeg".toMediaType()
    }
}
