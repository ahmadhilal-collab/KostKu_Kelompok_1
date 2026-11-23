package com.kostku.app.data.repository

import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.local.TenantEntity
import java.time.YearMonth

/*
 * Perhitungan rekap pembayaran (murni Kotlin, tanpa Android) supaya mudah diuji.
 * Dipakai layar Rekap per bulan, Rekap per kamar, dan Riwayat pembayaran kamar.
 */

/** Status satu kamar pada satu bulan. */
enum class RoomPayState { PAID, PENDING, REJECTED, UNPAID, EMPTY, MAINTENANCE }

fun PaymentEntity.payState(): RoomPayState = when {
    isPaid -> RoomPayState.PAID
    isAwaitingVerification -> RoomPayState.PENDING
    isTransferRejected -> RoomPayState.REJECTED
    else -> RoomPayState.UNPAID
}

data class RecapRow(
    val room: RoomEntity?,
    val roomNumber: Int,
    val tenantName: String,
    val payment: PaymentEntity
) {
    val state: RoomPayState get() = payment.payState()
}

data class MonthlyRecap(
    val period: YearMonth,
    val rows: List<RecapRow>,
    /** Jumlah kamar pada filter lantai, dan yang sedang perbaikan / kosong (tidak ditagih). */
    val roomCount: Int,
    val maintenanceCount: Int,
    val emptyCount: Int
) {
    val billCount: Int get() = rows.size
    val paidRows: List<RecapRow> get() = rows.filter { it.payment.isPaid }
    val unpaidRows: List<RecapRow> get() = rows.filter { !it.payment.isPaid }
    val paidCount: Int get() = paidRows.size
    val unpaidCount: Int get() = unpaidRows.size
    val pendingCount: Int get() = rows.count { it.state == RoomPayState.PENDING }
    val targetAmount: Long get() = rows.sumOf { it.payment.amount }
    val paidAmount: Long get() = paidRows.sumOf { it.payment.amount }
    val unpaidAmount: Long get() = targetAmount - paidAmount
    val cashAmount: Long get() = paidRows.filter { !it.payment.isTransfer() }.sumOf { it.payment.amount }
    val transferAmount: Long get() = paidRows.filter { it.payment.isTransfer() }.sumOf { it.payment.amount }
    /** Persentase tagihan lunas (0–100). */
    val paidPercent: Double get() = if (billCount == 0) 0.0 else paidCount * 100.0 / billCount
}

fun PaymentEntity.isTransfer(): Boolean = method.equals(KostRepository.METHOD_TRANSFER, ignoreCase = true)

object Recap {

    /** Bulan yang punya data tagihan (terbaru dulu); bulan berjalan selalu ada. */
    fun availableMonths(payments: List<PaymentEntity>, current: YearMonth): List<YearMonth> =
        (payments.map { YearMonth.of(it.periodYear, it.periodMonth) } + current)
            .distinct()
            .sortedDescending()

    /** @param floor null = semua lantai */
    fun monthly(
        period: YearMonth,
        floor: Int?,
        rooms: List<RoomEntity>,
        tenants: List<TenantEntity>,
        payments: List<PaymentEntity>
    ): MonthlyRecap {
        val roomById = rooms.associateBy { it.id }
        val tenantById = tenants.associateBy { it.id }
        val rows = payments
            .filter { it.periodYear == period.year && it.periodMonth == period.monthValue }
            .mapNotNull { p ->
                val room = roomById[p.roomId]
                if (floor != null && room != null && room.floor != floor) return@mapNotNull null
                RecapRow(
                    room = room,
                    roomNumber = room?.number ?: 0,
                    tenantName = tenantById[p.tenantId]?.name ?: "-",
                    payment = p
                )
            }
            .sortedBy { it.roomNumber }
        val floorRooms = rooms.filter { floor == null || it.floor == floor }
        val billedRooms = rows.map { it.payment.roomId }.toSet()
        return MonthlyRecap(
            period = period,
            rows = rows,
            roomCount = floorRooms.size,
            maintenanceCount = floorRooms.count { it.status == RoomStatus.MAINTENANCE && it.id !in billedRooms },
            emptyCount = floorRooms.count { it.status == RoomStatus.EMPTY && it.id !in billedRooms }
        )
    }

    data class RoomMonthRow(
        val room: RoomEntity,
        val tenantName: String?,
        val payment: PaymentEntity?
    ) {
        val state: RoomPayState
            get() = payment?.payState() ?: when (room.status) {
                RoomStatus.MAINTENANCE -> RoomPayState.MAINTENANCE
                else -> RoomPayState.EMPTY
            }
    }

    /** Semua kamar (filter lantai) beserta status pembayarannya pada [period]. */
    fun byRoom(
        period: YearMonth,
        floor: Int?,
        rooms: List<RoomEntity>,
        tenants: List<TenantEntity>,
        payments: List<PaymentEntity>
    ): List<RoomMonthRow> {
        val tenantById = tenants.associateBy { it.id }
        val activeByRoom = tenants.filter { it.active }.associateBy { it.roomId }
        val paymentByRoom = payments
            .filter { it.periodYear == period.year && it.periodMonth == period.monthValue }
            .associateBy { it.roomId }
        return rooms
            .filter { floor == null || it.floor == floor }
            .sortedBy { it.number }
            .map { room ->
                val pay = paymentByRoom[room.id]
                val name = pay?.let { tenantById[it.tenantId]?.name } ?: activeByRoom[room.id]?.name
                RoomMonthRow(room, name, pay)
            }
    }

    data class HistoryRow(val payment: PaymentEntity, val tenantName: String)

    /** Riwayat semua tagihan satu kamar (lintas penghuni), terbaru dulu. */
    fun roomHistory(roomId: Long, tenants: List<TenantEntity>, payments: List<PaymentEntity>): List<HistoryRow> {
        val tenantById = tenants.associateBy { it.id }
        return payments
            .filter { it.roomId == roomId }
            .sortedWith(compareByDescending<PaymentEntity> { it.periodYear }.thenByDescending { it.periodMonth })
            .map { HistoryRow(it, tenantById[it.tenantId]?.name ?: "-") }
    }
}
