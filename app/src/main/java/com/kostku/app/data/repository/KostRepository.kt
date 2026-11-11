package com.kostku.app.data.repository

import androidx.room.withTransaction
import com.kostku.app.data.local.AnnouncementEntity
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.data.local.AppDatabase
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RepairReportEntity
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.local.RoomPhotoEntity
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.local.TenantEntity
import com.kostku.app.util.Fmt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

class KostRepository(private val db: AppDatabase) {

    private val roomDao = db.roomDao()
    private val tenantDao = db.tenantDao()
    private val paymentDao = db.paymentDao()

    // ---------- Kamar ----------
    fun observeRooms(): Flow<List<RoomEntity>> = roomDao.observeAll()
    fun observeRoom(id: Long): Flow<RoomEntity?> = roomDao.observeById(id)
    fun observeAvailableRooms(): Flow<List<RoomEntity>> = roomDao.observeAvailable()
    fun observeAllPhotos(): Flow<List<RoomPhotoEntity>> = db.roomPhotoDao().observeAll()

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

    suspend fun addPhoto(roomId: Long, path: String, caption: String) {
        db.roomPhotoDao().insert(RoomPhotoEntity(roomId = roomId, uri = path, caption = caption.trim()))
    }

    suspend fun deletePhoto(id: Long) = db.roomPhotoDao().delete(id)

    suspend fun setRoomMaintenance(roomId: Long, maintenance: Boolean): Result<Unit> = runCatching {
        val room = roomDao.getById(roomId) ?: throw OperationException("Kamar tidak ditemukan.")
        if (maintenance) {
            if (tenantDao.findActiveByRoom(roomId) != null) {
                throw OperationException("Kamar masih dihuni. Akhiri sewa penghuni terlebih dahulu.")
            }
            roomDao.update(room.copy(status = RoomStatus.MAINTENANCE, maintenanceNote = "Perbaikan fasilitas"))
        } else {
            roomDao.update(room.copy(status = RoomStatus.EMPTY, maintenanceNote = null))
        }
    }

    // ---------- Penghuni ----------
    fun observeTenant(id: Long): Flow<TenantEntity?> = tenantDao.observeById(id)

    fun observeTenantsWithRoom(): Flow<List<TenantWithRoom>> = combine(
        tenantDao.observeActive(), roomDao.observeAll()
    ) { tenants, rooms ->
        val byId = rooms.associateBy { it.id }
        tenants.map { TenantWithRoom(it, byId[it.roomId]) }.sortedBy { it.room?.number ?: Int.MAX_VALUE }
    }

    suspend fun addTenant(
        name: String, nik: String, phone: String, roomId: Long, entryDate: Long, rate: Long
    ): Result<Long> = runCatching {
        db.withTransaction {
            if (tenantDao.findActiveByNik(nik) != null) {
                throw OperationException("NIK ini sudah terdaftar sebagai penghuni aktif.")
            }
            val room = roomDao.getById(roomId) ?: throw OperationException("Kamar tidak ditemukan.")
            if (room.status != RoomStatus.EMPTY) throw OperationException("Kamar ${room.number} sedang tidak tersedia.")
            val id = tenantDao.insert(
                TenantEntity(
                    name = name.trim(), nik = nik, phone = phone, roomId = roomId,
                    entryDate = entryDate, monthlyRate = rate
                )
            )
            roomDao.update(room.copy(status = RoomStatus.OCCUPIED, monthlyRate = rate, maintenanceNote = null))
            tenantDao.getById(id)?.let { createBillIfMissing(it, YearMonth.now()) }
            id
        }
    }

    suspend fun endTenancy(tenantId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val tenant = tenantDao.getById(tenantId) ?: throw OperationException("Penghuni tidak ditemukan.")
            tenantDao.update(tenant.copy(active = false))
            roomDao.getById(tenant.roomId)?.let { roomDao.update(it.copy(status = RoomStatus.EMPTY)) }
            paymentDao.deleteUnpaidByTenant(tenantId)
        }
    }

    // ---------- Pembayaran ----------
    fun observePaymentsByTenant(tenantId: Long): Flow<List<PaymentEntity>> = paymentDao.observeByTenant(tenantId)

    fun observePaidInfo(): Flow<List<PaidInfo>> = combine(
        paymentDao.observePaid(), tenantDao.observeAll(), roomDao.observeAll()
    ) { paid, tenants, rooms ->
        val tById = tenants.associateBy { it.id }
        val rById = rooms.associateBy { it.id }
        paid.map { PaidInfo(it, rById[it.roomId]?.number, tById[it.tenantId]?.name) }
    }

    /** Pastikan setiap penghuni aktif punya tagihan untuk bulan [ym] (jatuh tempo tanggal 10). */
    suspend fun ensureBills(ym: YearMonth) {
        tenantDao.getActive().forEach { createBillIfMissing(it, ym) }
    }

    private suspend fun createBillIfMissing(tenant: TenantEntity, ym: YearMonth) {
        if (paymentDao.find(tenant.id, ym.year, ym.monthValue) != null) return
        val due = Fmt.startOfDayMillis(ym.atDay(10)) + (23L * 60 + 59) * 60_000L
        paymentDao.insert(
            PaymentEntity(
                tenantId = tenant.id, roomId = tenant.roomId,
                periodYear = ym.year, periodMonth = ym.monthValue,
                amount = tenant.monthlyRate, dueDate = due
            )
        )
    }

    suspend fun confirmCashPayment(paymentId: Long, receivedBy: String): Result<PaymentEntity> = runCatching {
        val payment = paymentDao.getById(paymentId) ?: throw OperationException("Tagihan tidak ditemukan.")
        if (payment.status == PaymentStatus.PAID) throw OperationException("Tagihan ini sudah lunas.")
        val number = roomDao.getById(payment.roomId)?.number ?: 0
        val updated = payment.copy(
            status = PaymentStatus.PAID,
            method = "Tunai",
            paidAt = System.currentTimeMillis(),
            receiptNo = "NK-%d%02d-%d".format(payment.periodYear, payment.periodMonth, number),
            receivedBy = receivedBy
        )
        paymentDao.update(updated)
        updated
    }

    suspend fun markReminderSent(paymentId: Long) {
        paymentDao.getById(paymentId)?.let { paymentDao.update(it.copy(reminderSentAt = System.currentTimeMillis())) }
    }

    suspend fun buildReceipt(paymentId: Long): ReceiptData? {
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

    // ---------- Pengumuman ----------
    fun observeAnnouncements(): Flow<List<AnnouncementEntity>> = db.announcementDao().observeAll()

    suspend fun publishAnnouncement(
        title: String, body: String, target: AnnouncementTarget, publishedAt: Long, author: String
    ) {
        db.announcementDao().insert(
            AnnouncementEntity(
                title = title.trim(), body = body.trim(), target = target,
                publishedAt = publishedAt, author = author
            )
        )
    }

    suspend fun deleteAnnouncement(id: Long) = db.announcementDao().delete(id)

    // ---------- Laporan kerusakan ----------
    fun observeReports(): Flow<List<ReportWithInfo>> = combine(
        db.repairReportDao().observeAll(), roomDao.observeAll(), tenantDao.observeAll()
    ) { reports, rooms, tenants ->
        val rById = rooms.associateBy { it.id }
        val tById = tenants.associateBy { it.id }
        reports.map { ReportWithInfo(it, rById[it.roomId]?.number, tById[it.tenantId]?.name) }
    }

    fun observeReportsByTenant(tenantId: Long): Flow<List<RepairReportEntity>> =
        db.repairReportDao().observeByTenant(tenantId)

    suspend fun submitReport(tenantId: Long, roomId: Long, category: String, description: String, photoPath: String?) {
        db.repairReportDao().insert(
            RepairReportEntity(
                tenantId = tenantId, roomId = roomId, category = category,
                description = description.trim(), photoUri = photoPath
            )
        )
    }

    suspend fun updateReportStatus(report: RepairReportEntity, status: ReportStatus) {
        db.repairReportDao().update(report.copy(status = status))
    }
}
