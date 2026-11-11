package com.kostku.app.ui.screens.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kostku.app.KostKuApp
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.data.local.RoomEntity
import com.kostku.app.data.repository.KostRepository
import com.kostku.app.data.repository.ReceiptData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TenantViewModel(
    private val repo: KostRepository,
    private val tenantId: Long
) : ViewModel() {

    val period: YearMonth = YearMonth.now()

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val room: StateFlow<RoomEntity?> = repo.observeTenant(tenantId)
        .flatMapLatest { t -> if (t == null) flowOf<RoomEntity?>(null) else repo.observeRoom(t.roomId) }
        .state(null)

    val payments = repo.observePaymentsByTenant(tenantId).state(emptyList())

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

    init {
        viewModelScope.launch { repo.ensureBills(period) }
    }

    fun submitReport(category: String, description: String, photoPath: String?, onDone: () -> Unit) {
        val roomId = room.value?.id ?: return
        viewModelScope.launch {
            repo.submitReport(tenantId, roomId, category, description, photoPath)
            onDone()
        }
    }

    suspend fun loadReceipt(paymentId: Long): ReceiptData? = repo.buildReceipt(paymentId)

    companion object {
        fun factory(tenantId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KostKuApp
                TenantViewModel(app.container.kostRepository, tenantId)
            }
        }
    }
}
