package com.kostku.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kostku.app.KostKuApp
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RepairReportEntity
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.data.local.TenantEntity
import com.kostku.app.data.repository.KostRepository
import com.kostku.app.data.repository.ReceiptData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class AdminViewModel(
    private val repo: KostRepository,
    val adminName: String
) : ViewModel() {

    val period: YearMonth = YearMonth.now()

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val roomCards = repo.observeRoomCards(period).state(emptyList())
    val tenants = repo.observeTenantsWithRoom().state(emptyList())
    val availableRooms = repo.observeAvailableRooms().state(emptyList())
    val announcements = repo.observeAnnouncements().state(emptyList())
    val reports = repo.observeReports().state(emptyList())
    val paidReceipts = repo.observePaidInfo().state(emptyList())
    val photosByRoom = repo.observeAllPhotos().map { list -> list.groupBy { it.roomId } }.state(emptyMap())

    /** Pesan untuk snackbar (berhasil / gagal), ditampilkan oleh AdminShell. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** true saat sedang mengambil data dari server. */
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private var syncJob: Job? = null
    private var historyLoaded = false
    /** Ada permintaan refresh saat sinkron lain sedang berjalan → jalankan sekali lagi sesudahnya. */
    private var refreshAgain = false
    /** Pesan gagal hanya ditampilkan sekali sampai sinkron berhasil lagi (tidak berulang tiap 30 detik). */
    private var lastSyncOk = true

    /**
     * Ambil data terbaru dari server. Dipanggil AdminShell saat aplikasi tampil lalu tiap 30 detik,
     * dan oleh ViewModel ini setelah setiap perubahan. Sinkron pertama juga mengambil tagihan semua
     * periode untuk daftar nota.
     */
    fun refresh() {
        if (syncJob?.isActive == true) {
            refreshAgain = true
            return
        }
        syncJob = viewModelScope.launch {
            _syncing.value = true
            try {
                do {
                    refreshAgain = false
                    try {
                        repo.syncAdmin(period, includeHistory = !historyLoaded)
                        historyLoaded = true
                        lastSyncOk = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (lastSyncOk) _messages.tryEmit(e.message ?: "Gagal memuat data dari server")
                        lastSyncOk = false
                    }
                } while (refreshAgain)
            } finally {
                _syncing.value = false
            }
        }
    }

    /** Hasil operasi tulis: jika berhasil, segarkan data supaya semua layar ikut terbarui. */
    private fun <T> Result<T>.alsoRefresh(): Result<T> = onSuccess { refresh() }

    fun refreshTenant(tenantId: Long) =
        launchOp(successMessage = null, refreshAfter = false) { repo.syncTenant(tenantId) }

    fun observeTenant(id: Long): Flow<TenantEntity?> = repo.observeTenant(id)
    fun observePayments(tenantId: Long): Flow<List<PaymentEntity>> = repo.observePaymentsByTenant(tenantId)

    fun addPhoto(roomId: Long, path: String, caption: String, roomNumber: Int) =
        launchOp("Foto kamar $roomNumber tersimpan") { repo.addPhoto(roomId, path, caption) }

    fun deletePhoto(id: Long) = launchOp("Foto dihapus") { repo.deletePhoto(id) }

    fun setMaintenance(roomId: Long, maintenance: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(repo.setRoomMaintenance(roomId, maintenance).alsoRefresh()) }
    }

    fun addTenant(
        name: String, nik: String, phone: String, roomId: Long, entryDate: Long, rate: Long,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch { onResult(repo.addTenant(name, nik, phone, roomId, entryDate, rate)) }
    }

    fun endTenancy(tenantId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(repo.endTenancy(tenantId)) }
    }

    fun confirmPayment(paymentId: Long, onResult: (Result<PaymentEntity>) -> Unit) {
        viewModelScope.launch { onResult(repo.confirmCashPayment(paymentId, adminName).alsoRefresh()) }
    }

    fun markReminderSent(paymentId: Long) = launchOp(successMessage = null) { repo.markReminderSent(paymentId) }

    fun publishAnnouncement(
        title: String, body: String, target: AnnouncementTarget, publishedAt: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(runCatching { repo.publishAnnouncement(title, body, target, publishedAt) }.alsoRefresh())
        }
    }

    fun deleteAnnouncement(id: Long) = launchOp("Pengumuman dihapus") { repo.deleteAnnouncement(id) }

    fun updateReportStatus(report: RepairReportEntity, status: ReportStatus) =
        launchOp(if (status == ReportStatus.DONE) "Laporan ditandai selesai" else "Laporan ditandai diproses") {
            repo.updateReportStatus(report, status)
        }

    suspend fun loadReceipt(paymentId: Long): ReceiptData? = repo.buildReceipt(paymentId)

    /** Menjalankan operasi ke server lalu menampilkan pesan berhasil/gagal di snackbar. */
    private fun launchOp(successMessage: String?, refreshAfter: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                successMessage?.let { _messages.tryEmit(it) }
                if (refreshAfter) refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "Terjadi kesalahan. Coba lagi.")
            }
        }
    }

    companion object {
        fun factory(adminName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KostKuApp
                AdminViewModel(app.container.kostRepository, adminName)
            }
        }
    }
}

fun PaymentEntity?.isPaid(): Boolean = this?.status == PaymentStatus.PAID
