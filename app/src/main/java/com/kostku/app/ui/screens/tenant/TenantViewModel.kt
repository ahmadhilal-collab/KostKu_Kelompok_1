package com.kostku.app.ui.screens.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kostku.app.KostKuApp
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.local.TenantEntity
import com.kostku.app.data.remote.ContactDto
import com.kostku.app.data.remote.PaymentSettings
import com.kostku.app.data.remote.InboxItem
import com.kostku.app.data.repository.KostRepository
import com.kostku.app.data.repository.PushRepository
import com.kostku.app.data.repository.ReceiptData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TenantViewModel(
    private val repo: KostRepository,
    private val push: PushRepository,
    private val tenantId: Long
) : ViewModel() {

    val period: YearMonth = YearMonth.now()

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val room: StateFlow<RoomEntity?> = repo.observeTenant(tenantId)
        .flatMapLatest { t -> if (t == null) flowOf<RoomEntity?>(null) else repo.observeRoom(t.roomId) }
        .state(null)

    val payments = repo.observePaymentsByTenant(tenantId).state(emptyList())

    /** Rekening tujuan dan metode pembayaran yang diaktifkan pengelola. */
    val paymentSettings: StateFlow<PaymentSettings> = repo.paymentSettings

    /** Data diri penghuni (nama, NIK, HP, dll.). */
    val profile: StateFlow<TenantEntity?> = repo.observeTenant(tenantId).state(null)

    /** Nama & nomor pengelola (untuk pembayaran tunai). */
    private val _contact = MutableStateFlow<ContactDto?>(null)
    val contact: StateFlow<ContactDto?> = _contact.asStateFlow()

    /** Pengumuman yang sudah terbit dan ditujukan untuk lantai penghuni ini. */
    val announcements = combine(repo.observeAnnouncements(), room) { list, r ->
        val now = System.currentTimeMillis()
        list.filter { a ->
            a.publishedAt <= now && when (a.target) {
                AnnouncementTarget.ALL -> true
                AnnouncementTarget.FLOOR_1 -> r?.floor == 1
                AnnouncementTarget.FLOOR_2 -> r?.floor == 2
            }
        }
    }.state(emptyList())

    val reports = repo.observeReportsByTenant(tenantId).state(emptyList())

    /** Kotak masuk notifikasi dari server (pembayaran diterima, pengingat, status laporan, dll.). */
    private val _notifications = MutableStateFlow<List<InboxItem>>(emptyList())
    val notifications: StateFlow<List<InboxItem>> = _notifications.asStateFlow()

    init {
        // Daftarkan HP ini supaya menerima notifikasi push (setiap aplikasi dibuka; aman diulang).
        viewModelScope.launch { runCatching { push.registerDevice() } }
        // Ada push masuk saat aplikasi terbuka → langsung muat data terbaru.
        viewModelScope.launch { push.received.collect { refresh() } }
    }

    /** Pesan untuk snackbar, ditampilkan oleh TenantShell. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private var syncJob: Job? = null
    private var refreshAgain = false
    /** Pesan gagal hanya ditampilkan sekali sampai sinkron berhasil lagi. */
    private var lastSyncOk = true

    /** Ambil data terbaru dari server. Dipanggil TenantShell saat aplikasi tampil lalu tiap 30 detik. */
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
                        repo.syncTenantSelf(tenantId)
                        _notifications.value = repo.fetchNotifications()
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

    fun submitReport(
        category: String, description: String, photoPath: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(runCatching { repo.submitReport(category, description, photoPath) }.onSuccess { refresh() })
        }
    }

    suspend fun loadReceipt(paymentId: Long): ReceiptData? = repo.buildReceipt(paymentId)

    // ---- Pembayaran ----

    fun submitTransfer(paymentId: Long, proofPath: String, note: String, onResult: (Result<PaymentEntity>) -> Unit) {
        viewModelScope.launch { onResult(repo.submitTransfer(paymentId, proofPath, note).onSuccess { refresh() }) }
    }

    fun loadContact() {
        if (_contact.value != null) return
        viewModelScope.launch { _contact.value = repo.fetchContact() }
    }

    fun proofUrl(payment: PaymentEntity): String = repo.proofUrl(payment)
    fun authHeader(): String? = repo.authHeader()

    // ---- Data diri ----

    /** Ambil data diri lengkap (termasuk NIK) dari server. */
    fun loadProfile() {
        viewModelScope.launch { repo.fetchMyProfile() }
    }

    fun updateProfile(name: String, phone: String, onResult: (Result<TenantEntity>) -> Unit) {
        viewModelScope.launch { onResult(repo.updateMyProfile(name, phone)) }
    }

    /** Dipanggil saat meninggalkan layar Pengumuman & Notifikasi. */
    fun markNotificationsRead() {
        if (_notifications.value.none { !it.read }) return
        viewModelScope.launch {
            runCatching { repo.markAllNotificationsRead() }
                .onSuccess { _notifications.value = _notifications.value.map { it.copy(read = true) } }
        }
    }

    companion object {
        fun factory(tenantId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KostKuApp
                TenantViewModel(app.container.kostRepository, app.container.pushRepository, tenantId)
            }
        }
    }
}
