package com.kostku.app.ui.screens.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.Session
import com.kostku.app.data.remote.InboxItem
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.PaymentStateChip
import com.kostku.app.ui.components.NoticeBox
import com.kostku.app.ui.components.NoticeTone
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.WarningBg
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val NEW_WINDOW_MS = 3L * 24 * 60 * 60 * 1000

@Composable
fun NoticeScreen(vm: TenantViewModel, session: Session, nav: NavHostController) {
    val payments by vm.payments.collectAsStateWithLifecycle()
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val inbox by vm.notifications.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 semua, 1 pengumuman, 2 pembayaran
    // Titik "belum dibaca" tetap terlihat selama layar ini terbuka; ditandai dibaca saat ditinggalkan.
    DisposableEffect(Unit) { onDispose { vm.markNotificationsRead() } }
    val shownInbox = inbox.filter { !it.isAnnouncement && (filter == 0 || (filter == 2 && it.isPayment)) }.take(20)

    val current = payments.firstOrNull {
        it.periodYear == vm.period.year && it.periodMonth == vm.period.monthValue
    }
    val unpaid = current != null && current.status != PaymentStatus.PAID
    val now = System.currentTimeMillis()
    val newCount = announcements.count { now - it.publishedAt < NEW_WINDOW_MS }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Pengumuman & Notifikasi", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    PageHeader(
                        "Kabar untuk Anda",
                        "${session.name} • Kamar ${room?.number ?: "-"} • New Afra Kost",
                        Modifier.entrance(0)
                    )
                    Spacer(Modifier.height(16.dp))
                    FloorTabs(listOf("Semua", "Pengumuman", "Pembayaran"), filter, { filter = it })
                }
            }
            if (filter != 1 && unpaid && current != null) {
                item { ReminderCard(current, onOpen = { nav.navigate(TenantRoutes.PAYMENTS) }) }
            }
            if (filter == 2 && !unpaid && shownInbox.isEmpty()) {
                item { EmptyState(Icons.Outlined.Payments, "Tidak ada tagihan tertunggak", "Pembayaran bulan ini sudah lunas. Terima kasih!") }
            }
            if (filter != 1 && shownInbox.isNotEmpty()) {
                item {
                    val unread = shownInbox.count { !it.read }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Notifikasi terbaru", Modifier.weight(1f))
                        if (unread > 0) Text("$unread belum dibaca", style = MaterialTheme.typography.labelMedium, color = Green700)
                    }
                }
                items(shownInbox, key = { "n${it.id}" }) { n -> InboxCard(n) }
            }
            if (filter != 2) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Pengumuman kost", Modifier.weight(1f))
                        if (newCount > 0) Text("$newCount baru", style = MaterialTheme.typography.labelMedium, color = Green700)
                    }
                }
                if (announcements.isEmpty()) {
                    item { EmptyState(Icons.Outlined.Campaign, "Belum ada pengumuman", "Pengumuman dari pengelola akan tampil di sini.") }
                }
                items(announcements, key = { it.id }) { a ->
                    val isNew = now - a.publishedAt < NEW_WINDOW_MS
                    AppCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Campaign, null, tint = Green700, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.title, style = MaterialTheme.typography.titleSmall, color = Ink900)
                                Text(
                                    "${Fmt.date(a.publishedAt)} • ${a.author}",
                                    style = MaterialTheme.typography.bodySmall, color = Ink400
                                )
                            }
                            if (isNew) Box(Modifier.padding(top = 6.dp).size(8.dp).background(Green700, CircleShape))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(a.body, style = MaterialTheme.typography.bodyMedium, color = Ink600)
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxCard(item: InboxItem) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                when {
                    item.isPayment -> Icons.Outlined.Payments
                    item.isRepair -> Icons.Outlined.Build
                    else -> Icons.Outlined.Notifications
                },
                null,
                tint = if (item.type.startsWith("due:") || item.type == "payment_reminder") Warning else Green700,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = Ink900)
                Text(Fmt.date(item.sentAt), style = MaterialTheme.typography.bodySmall, color = Ink400)
            }
            if (!item.read) Box(Modifier.padding(top = 6.dp).size(8.dp).background(Green700, CircleShape))
        }
        Spacer(Modifier.height(8.dp))
        Text(item.body, style = MaterialTheme.typography.bodyMedium, color = Ink600)
    }
}

@Composable
private fun ReminderCard(payment: PaymentEntity, onOpen: () -> Unit) {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, Fmt.localDate(payment.dueDate))
    val title = when {
        days > 0 -> "Jatuh tempo $days hari lagi"
        days == 0L -> "Jatuh tempo hari ini"
        else -> "Terlambat ${-days} hari"
    }
    val period = Fmt.month(payment.periodYear, payment.periodMonth)
    Column(
        Modifier.fillMaxWidth().background(if (days >= 0) WarningBg else Gold100, RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = Warning, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tagihan $period sebesar ${Fmt.rupiah(payment.amount)} belum dibayar. Jatuh tempo ${Fmt.date(payment.dueDate)}.",
            style = MaterialTheme.typography.bodyMedium, color = Ink900
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaymentStatusChip(paid = false)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpen) {
                Text("Lihat tagihan", color = Green700, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Outlined.ChevronRight, null, tint = Green700, modifier = Modifier.size(16.dp))
            }
        }
        val reminded = payment.reminderSentAt
        Text(
            if (reminded != null) "${Fmt.date(reminded)} • Pengingat dari pengelola"
            else "${Fmt.date(today)} • Pengingat otomatis",
            style = MaterialTheme.typography.bodySmall, color = Warning
        )
    }
}

@Composable
fun PaymentsScreen(vm: TenantViewModel, session: Session, nav: NavHostController) {
    val payments by vm.payments.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val current = payments.firstOrNull {
        it.periodYear == vm.period.year && it.periodMonth == vm.period.monthValue
    }
    val paidNow = current?.status == PaymentStatus.PAID
    val years = (payments.map { it.periodYear } + vm.period.year).distinct().sortedDescending()
    var year by rememberSaveable { mutableIntStateOf(vm.period.year) }
    var menuOpen by remember { mutableStateOf(false) }
    val history = payments.filter { it.status == PaymentStatus.PAID && it.periodYear == year }
    // Tunggakan bulan sebelumnya yang belum lunas (tetap bisa dibayar dari sini)
    val arrears = payments.filter { !it.isPaid && it.id != current?.id }.sortedBy { it.periodYear * 100 + it.periodMonth }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Pembayaran", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    "Status & Histori Pembayaran",
                    "${session.name} • Kamar ${room?.number ?: "-"} • New Afra Kost",
                    Modifier.entrance(0)
                )
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Fmt.month(vm.period.year, vm.period.monthValue),
                            style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = Modifier.weight(1f)
                        )
                        if (current != null) PaymentStateChip(current)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Total Tagihan All-In", style = MaterialTheme.typography.bodySmall, color = Ink600)
                    Text(
                        Fmt.rupiahSpaced(current?.amount ?: 0L),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(10.dp))
                    if (current != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = Ink600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Jatuh tempo: ${Fmt.date(current.dueDate)}", style = MaterialTheme.typography.bodyMedium, color = Ink900)
                        }
                        Spacer(Modifier.height(6.dp))
                        if (paidNow) {
                            TextButton(onClick = { nav.navigate(TenantRoutes.receipt(current.id)) }) {
                                Text("Lihat nota", color = Green700)
                            }
                        } else {
                            when {
                                current.isAwaitingVerification -> NoticeBox(
                                    "Bukti transfer sedang diverifikasi",
                                    "Nota tersedia setelah pengelola memastikan dana masuk.",
                                    NoticeTone.INFO
                                )
                                current.isTransferRejected -> NoticeBox(
                                    "Bukti transfer ditolak",
                                    "Alasan: ${current.rejectReason ?: "-"}",
                                    NoticeTone.DANGER
                                )
                                else -> Text(
                                    "Nota ${Fmt.month(current.periodYear, current.periodMonth).substringBefore(' ')} tersedia setelah pembayaran dikonfirmasi lunas oleh pengelola.",
                                    style = MaterialTheme.typography.bodySmall, color = Ink600
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton(
                                when {
                                    current.isAwaitingVerification -> "Lihat Status Bukti"
                                    current.isTransferRejected -> "Kirim Ulang Bukti"
                                    else -> "Bayar Sekarang"
                                },
                                { nav.navigate(TenantRoutes.pay(current.id)) }
                            )
                        }
                    } else {
                        Text("Belum ada tagihan bulan ini.", style = MaterialTheme.typography.bodyMedium, color = Ink600)
                    }
                }
            }
            if (arrears.isNotEmpty()) {
                item { SectionLabel("Tagihan belum lunas", Modifier.padding(top = 6.dp)) }
                items(arrears, key = { "a" + it.id }) { p ->
                    AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(TenantRoutes.pay(p.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(Fmt.month(p.periodYear, p.periodMonth), style = MaterialTheme.typography.titleSmall, color = Ink900)
                                Text(
                                    "${Fmt.rupiah(p.amount)} • jatuh tempo ${Fmt.date(p.dueDate)}",
                                    style = MaterialTheme.typography.bodySmall, color = Ink600
                                )
                            }
                            PaymentStateChip(p, short = true)
                            Icon(Icons.Outlined.ChevronRight, null, tint = Ink600, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Histori pembayaran", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { menuOpen = true }) {
                            Text(year.toString(), color = Green700, style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Outlined.ExpandMore, null, tint = Green700, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(White)) {
                            years.forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = { year = y; menuOpen = false }) }
                        }
                    }
                }
            }
            if (history.isEmpty()) {
                item { EmptyState(Icons.Outlined.Payments, "Belum ada riwayat", "Riwayat pembayaran lunas tahun $year akan tampil di sini.") }
            }
            items(history, key = { it.id }) { p ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Fmt.month(p.periodYear, p.periodMonth),
                            style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = Modifier.weight(1f)
                        )
                        PaymentStateChip(p, short = true)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(Fmt.rupiah(p.amount), style = MaterialTheme.typography.titleSmall, color = Ink900)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${p.method ?: "Tunai"} • ${Fmt.date(p.paidAt ?: p.dueDate)}",
                            style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { nav.navigate(TenantRoutes.receipt(p.id)) }) {
                            Text("Lihat nota", color = Green700, style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Outlined.ChevronRight, null, tint = Green700, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
