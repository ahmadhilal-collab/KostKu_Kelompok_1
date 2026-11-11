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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    init {
        viewModelScope.launch { repo.ensureBills(period) }
    }

    fun observeTenant(id: Long): Flow<TenantEntity?> = repo.observeTenant(id)
    fun observePayments(tenantId: Long): Flow<List<PaymentEntity>> = repo.observePaymentsByTenant(tenantId)

    fun addPhoto(roomId: Long, path: String, caption: String) {
        viewModelScope.launch { repo.addPhoto(roomId, path, caption) }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch { repo.deletePhoto(id) }
    }

    fun setMaintenance(roomId: Long, maintenance: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(repo.setRoomMaintenance(roomId, maintenance)) }
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
        viewModelScope.launch { onResult(repo.confirmCashPayment(paymentId, adminName)) }
    }

    fun markReminderSent(paymentId: Long) {
        viewModelScope.launch { repo.markReminderSent(paymentId) }
    }

    fun publishAnnouncement(
        title: String, body: String, target: AnnouncementTarget, publishedAt: Long, onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repo.publishAnnouncement(title, body, target, publishedAt, adminName)
            onDone()
        }
    }

    fun deleteAnnouncement(id: Long) {
        viewModelScope.launch { repo.deleteAnnouncement(id) }
    }

    fun updateReportStatus(report: RepairReportEntity, status: ReportStatus) {
        viewModelScope.launch { repo.updateReportStatus(report, status) }
    }

    suspend fun loadReceipt(paymentId: Long): ReceiptData? = repo.buildReceipt(paymentId)

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
