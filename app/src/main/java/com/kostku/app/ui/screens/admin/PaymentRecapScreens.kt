package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.remote.PaymentSettings
import com.kostku.app.data.repository.Recap
import com.kostku.app.data.repository.RecapRow
import com.kostku.app.data.repository.RoomPayState
import com.kostku.app.data.repository.isTransfer
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.ErrorBanner
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.NoticeBox
import com.kostku.app.ui.components.NoticeTone
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PayStateChip
import com.kostku.app.ui.components.PaymentStateChip
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.ProofImage
import com.kostku.app.ui.components.ProofViewerDialog
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.DarkGreen
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents
import com.kostku.app.util.RecapPdf
import com.kostku.app.util.ReceiptPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.util.Locale

private val floorOptions = listOf("Semua lantai", "Lantai 1", "Lantai 2")
private fun floorOf(index: Int): Int? = if (index == 0) null else index
private fun percent(v: Double) = String.format(Locale("id", "ID"), "%.1f%%", v)

// ═════════════════════════════════════════════════════════════════════════════
//  1. Nota Pembayaran (hub): Rekap per bulan / per kamar, verifikasi, metode
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PaymentHubScreen(vm: AdminViewModel, nav: NavHostController) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val tenants by vm.allTenants.collectAsStateWithLifecycle()
    val payments by vm.allPayments.collectAsStateWithLifecycle()
    val pending by vm.pendingTransfers.collectAsStateWithLifecycle()
    val settings by vm.paymentSettings.collectAsStateWithLifecycle()
    val recap = remember(rooms, tenants, payments) { Recap.monthly(vm.period, null, rooms, tenants, payments) }
    val tenantName = remember(tenants) { tenants.associate { it.id to it.name } }
    val roomNumber = remember(rooms) { rooms.associate { it.id to it.number } }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Nota Pembayaran", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PageHeader("Rekap pembayaran", "Pilih cara melihat pembayaran kost.", Modifier.entrance(0)) }

            // Rekap per bulan (kartu utama)
            item {
                Surface(
                    onClick = { nav.navigate(AdminRoutes.RECAP_MONTH) },
                    shape = RoundedCornerShape(22.dp),
                    color = DarkGreen,
                    modifier = Modifier.fillMaxWidth().entrance(1)
                ) {
                    Box(Modifier.background(Brush.linearGradient(listOf(DarkGreen, Green700)))) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HubIcon(Icons.Outlined.CalendarMonth, onDark = true)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Rekap per bulan", style = MaterialTheme.typography.titleMedium, color = White)
                                    Text("Total diterima, status lunas, dan unduh PDF", style = MaterialTheme.typography.bodySmall, color = White.copy(alpha = 0.8f))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "${Fmt.month(vm.period.year, vm.period.monthValue)} • diterima",
                                style = MaterialTheme.typography.labelMedium, color = White.copy(alpha = 0.8f)
                            )
                            Text(
                                Fmt.rupiah(recap.paidAmount),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = White
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (recap.paidPercent / 100.0).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = White, trackColor = White.copy(alpha = 0.25f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text("${recap.paidCount} lunas / ${recap.billCount} tagihan", style = MaterialTheme.typography.bodySmall, color = White, modifier = Modifier.weight(1f))
                                Text("Lihat rekap →", style = MaterialTheme.typography.labelMedium, color = White)
                            }
                        }
                    }
                }
            }

            // Rekap per kamar
            item {
                AppCard(Modifier.fillMaxWidth().entrance(2), onClick = { nav.navigate(AdminRoutes.RECAP_ROOM) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HubIcon(Icons.Outlined.MeetingRoom)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Rekap per kamar", style = MaterialTheme.typography.titleMedium, color = Ink900)
                            Text("Status bayar tiap kamar dan riwayat pembayarannya", style = MaterialTheme.typography.bodySmall, color = Ink600)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
                    }
                }
            }

            // Menunggu verifikasi transfer
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Menunggu verifikasi transfer", Modifier.weight(1f))
                    if (pending.isNotEmpty()) {
                        Box(Modifier.background(Warning, CircleShape).padding(horizontal = 9.dp, vertical = 2.dp)) {
                            Text(pending.size.toString(), style = MaterialTheme.typography.labelMedium, color = White)
                        }
                    }
                }
            }
            if (pending.isEmpty()) {
                item {
                    Text(
                        "Tidak ada bukti transfer yang menunggu. Bukti baru dari penghuni akan muncul di sini.",
                        style = MaterialTheme.typography.bodySmall, color = Ink600
                    )
                }
            }
            items(pending, key = { "pending${it.id}" }) { p ->
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(AdminRoutes.transfer(p.id)) }, containerColor = Gold100) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HourglassTop, null, tint = Warning)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kamar ${roomNumber[p.roomId] ?: "-"} • ${tenantName[p.tenantId] ?: "-"}", style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(
                                "${Fmt.month(p.periodYear, p.periodMonth)} • ${Fmt.rupiah(p.amount)}" +
                                    (p.transferSubmittedAt?.let { " • dikirim ${Fmt.shortDate(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Text("Periksa", style = MaterialTheme.typography.labelMedium, color = Green700)
                        Icon(Icons.Outlined.ChevronRight, null, tint = Green700)
                    }
                }
            }

            // Metode pembayaran
            item {
                SectionLabel("Metode pembayaran", Modifier.padding(top = 8.dp))
            }
            item {
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(AdminRoutes.PAYMENT_SETTINGS) }) {
                    MethodStatusRow(
                        Icons.Outlined.AccountBalance, "Transfer bank", settings.transferEnabled,
                        if (settings.hasAccount) "${settings.bankName} • ${settings.accountNumber}" else "Rekening belum diatur"
                    )
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 10.dp))
                    MethodStatusRow(Icons.Outlined.Payments, "Tunai ke pengelola", settings.cashEnabled, "Dikonfirmasi admin di aplikasi")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Settings, null, tint = Green700, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ubah rekening & metode", style = MaterialTheme.typography.labelMedium, color = Green700)
                    }
                }
            }
            item {
                Text(
                    "Rekap hanya untuk admin. Total diterima dihitung dari pembayaran yang sudah dikonfirmasi atau diverifikasi.",
                    style = MaterialTheme.typography.bodySmall, color = Ink400, modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HubIcon(icon: ImageVector, onDark: Boolean = false) {
    Box(
        Modifier.size(44.dp).background(if (onDark) White.copy(alpha = 0.16f) else Green50, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (onDark) White else Green700) }
}

@Composable
private fun MethodStatusRow(icon: ImageVector, title: String, enabled: Boolean, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (enabled) Green700 else Ink400)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
        if (enabled) com.kostku.app.ui.components.StatusChip("AKTIF", Green100, Green700)
        else com.kostku.app.ui.components.StatusChip("NONAKTIF", Line, Ink600)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  2. Rekap bulanan
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MonthlyRecapScreen(vm: AdminViewModel, nav: NavHostController) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val tenants by vm.allTenants.collectAsStateWithLifecycle()
    val payments by vm.allPayments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notify = LocalNotifier.current
    val scope = rememberCoroutineScope()

    val months = remember(payments) { Recap.availableMonths(payments, vm.period) }
    var period by rememberSaveable { mutableStateOf(vm.period.toString()) }
    val ym = YearMonth.parse(period)
    var floorIndex by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 semua, 1 lunas, 2 belum
    var showAll by rememberSaveable(period, floorIndex, tab) { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val recap = remember(ym, floorIndex, rooms, tenants, payments) {
        Recap.monthly(ym, floorOf(floorIndex), rooms, tenants, payments)
    }
    val filtered = when (tab) {
        1 -> recap.paidRows
        2 -> recap.unpaidRows
        else -> recap.rows
    }
    val visible = if (showAll) filtered else filtered.take(10)

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Rekap pembayaran", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { PageHeader("Rekap bulanan", "Pantau pembayaran kamar tiap bulan.") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactDropdown(
                        value = Fmt.month(ym.year, ym.monthValue),
                        options = months.map { Fmt.month(it.year, it.monthValue) },
                        onSelect = { period = months[it].toString() },
                        modifier = Modifier.weight(1f)
                    )
                    CompactDropdown(floorOptions[floorIndex], floorOptions, { floorIndex = it }, Modifier.weight(1f))
                }
            }

            // Ringkasan
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = DarkGreen, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.linearGradient(listOf(DarkGreen, Green700)))) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Total diterima • ${Fmt.month(ym.year, ym.monthValue).substringBefore(' ')}", style = MaterialTheme.typography.labelMedium, color = White.copy(alpha = 0.8f))
                            Text(Fmt.rupiah(recap.paidAmount), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold), color = White)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (recap.paidPercent / 100.0).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = White, trackColor = White.copy(alpha = 0.25f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text("${recap.paidCount} lunas / ${recap.billCount} tagihan", style = MaterialTheme.typography.bodySmall, color = White, modifier = Modifier.weight(1f))
                                Text(percent(recap.paidPercent), style = MaterialTheme.typography.titleSmall, color = White)
                            }
                            HorizontalDivider(color = White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                            SummaryLine("Target tagihan", Fmt.rupiah(recap.targetAmount))
                            SummaryLine("Belum dibayar • ${recap.unpaidCount} kamar", Fmt.rupiah(recap.unpaidAmount), highlight = recap.unpaidAmount > 0)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${recap.roomCount} kamar • ${recap.maintenanceCount} perbaikan" +
                                    (if (recap.emptyCount > 0) " • ${recap.emptyCount} kosong" else "") + ", tidak ditagih",
                                style = MaterialTheme.typography.bodySmall, color = White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("Tunai", Fmt.rupiah(recap.cashAmount), Icons.Outlined.Payments, Modifier.weight(1f))
                    MiniStat("Transfer", Fmt.rupiah(recap.transferAmount), Icons.Outlined.AccountBalance, Modifier.weight(1f))
                }
            }
            if (recap.pendingCount > 0) {
                item {
                    NoticeBox(
                        "${recap.pendingCount} bukti transfer menunggu verifikasi",
                        "Ketuk kamar berstatus VERIFIKASI untuk memeriksa bukti dan menandai lunas."
                    )
                }
            }

            item { SectionLabel("Daftar pembayaran kamar", Modifier.padding(top = 6.dp)) }
            item {
                FloorTabs(
                    listOf("Semua (${recap.billCount})", "Lunas (${recap.paidCount})", "Belum (${recap.unpaidCount})"),
                    tab, { tab = it }
                )
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Outlined.ReceiptLong,
                        if (recap.billCount == 0) "Belum ada tagihan" else "Tidak ada data",
                        if (recap.billCount == 0) "Tagihan periode ini belum dibuat." else "Tidak ada kamar pada filter ini."
                    )
                }
            }
            items(visible, key = { it.payment.id }) { row -> RecapRowCard(row, nav) }
            if (!showAll && filtered.size > visible.size) {
                item {
                    SecondaryButton("Lihat ${filtered.size - visible.size} kamar lainnya", { showAll = true })
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    "Unduh rekap bulanan",
                    {
                        exporting = true
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                runCatching { RecapPdf.create(context, recap, floorOptions[floorIndex], vm.adminName) }.getOrNull()
                            }
                            exporting = false
                            when {
                                file == null -> notify("Rekap gagal dibuat")
                                withContext(Dispatchers.IO) { ReceiptPdf.saveToDownloads(context, file) } -> {
                                    notify("Rekap tersimpan di folder Unduhan")
                                    Intents.sharePdf(context, file)
                                }
                                !Intents.sharePdf(context, file) -> notify("Rekap tidak dapat dibagikan")
                            }
                        }
                    },
                    loading = exporting,
                    enabled = recap.billCount > 0
                )
            }
            item {
                Text(
                    "Rekap hanya untuk admin. Total diterima dihitung dari pembayaran yang sudah diverifikasi.",
                    style = MaterialTheme.typography.bodySmall, color = Ink400, modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = if (highlight) Gold100 else White)
    }
}

@Composable
private fun MiniStat(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green700, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink900)
    }
}

@Composable
private fun RecapRowCard(row: RecapRow, nav: NavHostController) {
    val p = row.payment
    val action: (() -> Unit)? = when (row.state) {
        RoomPayState.PAID -> ({ nav.navigate(AdminRoutes.receipt(p.id)) })
        RoomPayState.PENDING -> ({ nav.navigate(AdminRoutes.transfer(p.id)) })
        else -> row.room?.let { r -> { nav.navigate(AdminRoutes.room(r.id)) } }
    }
    AppCard(Modifier.fillMaxWidth(), onClick = action) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kamar ${row.roomNumber} • ${row.tenantName}", style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = Modifier.weight(1f))
            PaymentStateChip(p, short = true)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.rupiah(p.amount), style = MaterialTheme.typography.bodyMedium, color = Ink900, modifier = Modifier.weight(1f))
            Text(
                when (row.state) {
                    RoomPayState.PAID -> "Rincian nota →"
                    RoomPayState.PENDING -> "Periksa bukti →"
                    else -> "Belum ada nota"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (row.state == RoomPayState.PAID || row.state == RoomPayState.PENDING) Green700 else Ink400
            )
        }
        Text(
            when {
                p.isPaid -> "${if (p.isTransfer()) "Transfer" else "Tunai"} • ${p.paidAt?.let { Fmt.date(it) } ?: "-"}"
                row.state == RoomPayState.PENDING -> "Bukti dikirim ${p.transferSubmittedAt?.let { Fmt.date(it) } ?: "-"}"
                row.state == RoomPayState.REJECTED -> "Bukti ditolak • jatuh tempo ${Fmt.date(p.dueDate)}"
                else -> "Jatuh tempo ${Fmt.date(p.dueDate)}"
            },
            style = MaterialTheme.typography.bodySmall, color = Ink600
        )
    }
}

@Composable
private fun CompactDropdown(value: String, options: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true }, shape = RoundedCornerShape(12.dp), color = White,
            border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.labelLarge, color = Ink900, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Outlined.ExpandMore, null, tint = Green700, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(White)) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); open = false }) }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  3. Rekap per kamar + 4. Riwayat pembayaran kamar
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun RoomRecapScreen(vm: AdminViewModel, nav: NavHostController) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val tenants by vm.allTenants.collectAsStateWithLifecycle()
    val payments by vm.allPayments.collectAsStateWithLifecycle()
    val months = remember(payments) { Recap.availableMonths(payments, vm.period) }
    var period by rememberSaveable { mutableStateOf(vm.period.toString()) }
    val ym = YearMonth.parse(period)
    var floorIndex by rememberSaveable { mutableIntStateOf(1) }
    val rows = remember(ym, floorIndex, rooms, tenants, payments) { Recap.byRoom(ym, floorOf(floorIndex), rooms, tenants, payments) }
    val billed = rows.count { it.payment != null }
    val paid = rows.count { it.state == RoomPayState.PAID }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Rekap per kamar", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { PageHeader("Pembayaran per kamar", "Ringkasan status pembayaran setiap kamar. Ketuk kamar untuk melihat riwayatnya.") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactDropdown(
                        Fmt.month(ym.year, ym.monthValue), months.map { Fmt.month(it.year, it.monthValue) },
                        { period = months[it].toString() }, Modifier.weight(1f)
                    )
                    CompactDropdown(floorOptions[floorIndex], floorOptions, { floorIndex = it }, Modifier.weight(1f))
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), containerColor = Green50) {
                    Text("${Fmt.month(ym.year, ym.monthValue)} • ${floorOptions[floorIndex]}", style = MaterialTheme.typography.titleSmall, color = Green700)
                    Text(
                        "$paid lunas dari $billed kamar terisi • ${rows.size} kamar",
                        style = MaterialTheme.typography.bodySmall, color = Ink600
                    )
                }
            }
            item { SectionLabel("Daftar kamar", Modifier.padding(top = 4.dp)) }
            items(rows, key = { it.room.id }) { r ->
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(AdminRoutes.roomHistory(r.room.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Kamar ${r.room.number}" + (r.tenantName?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.titleSmall, color = Ink900
                            )
                            Spacer(Modifier.height(4.dp))
                            PayStateChip(r.state)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    r.payment?.isPaid == true -> "${if (r.payment.isTransfer()) "Transfer" else "Tunai"} • ${r.payment.paidAt?.let { Fmt.date(it) } ?: "-"}"
                                    r.payment != null -> "${Fmt.rupiah(r.payment.amount)} • jatuh tempo ${Fmt.date(r.payment.dueDate)}"
                                    r.state == RoomPayState.MAINTENANCE -> "Dalam perbaikan, tidak ditagih"
                                    else -> "Tidak ditempati"
                                },
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun RoomPaymentHistoryScreen(roomId: Long, vm: AdminViewModel, nav: NavHostController) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val tenants by vm.allTenants.collectAsStateWithLifecycle()
    val payments by vm.allPayments.collectAsStateWithLifecycle()
    val room = rooms.firstOrNull { it.id == roomId }
    val history = remember(roomId, tenants, payments) { Recap.roomHistory(roomId, tenants, payments) }
    val current = history.firstOrNull { it.payment.periodYear == vm.period.year && it.payment.periodMonth == vm.period.monthValue }
    val activeTenant = tenants.firstOrNull { it.roomId == roomId && it.active }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Riwayat pembayaran kamar", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PageHeader(
                    "Kamar ${room?.number ?: "-"}",
                    listOfNotNull(activeTenant?.name ?: "Tidak ditempati", room?.let { "Lantai ${it.floor}" }).joinToString(" • ")
                )
            }
            item {
                val p = current?.payment
                Surface(shape = RoundedCornerShape(22.dp), color = Green50, border = BorderStroke(1.dp, Green100), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Periode: ${Fmt.month(vm.period.year, vm.period.monthValue)}", style = MaterialTheme.typography.labelMedium, color = Green700, modifier = Modifier.weight(1f))
                            if (p != null) PaymentStateChip(p, short = true)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            Fmt.rupiah(p?.amount ?: room?.monthlyRate ?: 0L),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = Ink900
                        )
                        Text(
                            when {
                                p == null -> "Tidak ada tagihan bulan ini"
                                p.isPaid -> "Lunas • ${if (p.isTransfer()) "Transfer" else "Tunai"} • ${p.paidAt?.let { Fmt.date(it) } ?: "-"}"
                                p.isAwaitingVerification -> "Bukti transfer menunggu verifikasi"
                                else -> "Belum dibayar • jatuh tempo ${Fmt.date(p.dueDate)}"
                            },
                            style = MaterialTheme.typography.bodySmall, color = Ink600
                        )
                        if (p != null) {
                            Spacer(Modifier.height(12.dp))
                            when {
                                p.isPaid -> PrimaryButton("Lihat rincian nota", { nav.navigate(AdminRoutes.receipt(p.id)) })
                                p.isAwaitingVerification -> PrimaryButton("Periksa bukti transfer", { nav.navigate(AdminRoutes.transfer(p.id)) })
                                room != null -> SecondaryButton("Buka detail kamar", { nav.navigate(AdminRoutes.room(room.id)) })
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Riwayat pembayaran", Modifier.padding(top = 6.dp)) }
            if (history.isEmpty()) {
                item { EmptyState(Icons.Outlined.ReceiptLong, "Belum ada riwayat", "Tagihan kamar ini akan tampil di sini.") }
            }
            items(history, key = { it.payment.id }) { h ->
                val p = h.payment
                AppCard(
                    Modifier.fillMaxWidth(),
                    onClick = when {
                        p.isPaid -> ({ nav.navigate(AdminRoutes.receipt(p.id)) })
                        p.isAwaitingVerification -> ({ nav.navigate(AdminRoutes.transfer(p.id)) })
                        else -> null
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Fmt.month(p.periodYear, p.periodMonth), style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(
                                h.tenantName + " • " + if (p.isPaid) "${if (p.isTransfer()) "Transfer" else "Tunai"} ${p.paidAt?.let { Fmt.shortDate(it) } ?: ""}"
                                else "jatuh tempo ${Fmt.shortDate(p.dueDate)}",
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PaymentStateChip(p, short = true)
                            Spacer(Modifier.height(4.dp))
                            Text(Fmt.rupiah(p.amount), style = MaterialTheme.typography.labelMedium, color = Ink900)
                        }
                    }
                }
            }
            item {
                Text(
                    "Hanya pembayaran yang sudah diverifikasi pengelola yang memiliki nota.",
                    style = MaterialTheme.typography.bodySmall, color = Ink400, modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  5. Verifikasi bukti transfer
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TransferVerifyScreen(paymentId: Long, vm: AdminViewModel, nav: NavHostController) {
    val payments by vm.allPayments.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val tenants by vm.allTenants.collectAsStateWithLifecycle()
    val settings by vm.paymentSettings.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    val p = payments.firstOrNull { it.id == paymentId }
    var zoom by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var showReject by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Verifikasi transfer", { nav.popBackStack() })
        if (p == null) {
            EmptyState(Icons.Outlined.ReceiptLong, "Memuat data…", "Tagihan tidak ditemukan atau sedang dimuat.")
            return@Column
        }
        val room = rooms.firstOrNull { it.id == p.roomId }
        val tenant = tenants.firstOrNull { it.id == p.tenantId }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            PageHeader("Bukti transfer", "Kamar ${room?.number ?: "-"} • ${tenant?.name ?: "-"} • ${Fmt.month(p.periodYear, p.periodMonth)}")
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total tagihan", style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.weight(1f))
                    PaymentStateChip(p)
                }
                Text(Fmt.rupiahSpaced(p.amount), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900)
                HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 10.dp))
                InfoLine("Dikirim", p.transferSubmittedAt?.let { Fmt.date(it) } ?: "-")
                InfoLine("Rekening tujuan", if (settings.hasAccount) "${settings.bankName} • ${settings.accountNumber}" else "-")
                if (!p.transferNote.isNullOrBlank()) InfoLine("Catatan penghuni", p.transferNote)
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("Foto bukti transfer")
            Spacer(Modifier.height(8.dp))
            if (p.hasProof) {
                ProofImage(vm.proofUrl(p), vm.authHeader(), Modifier.fillMaxWidth().height(340.dp), onClick = { zoom = true })
            } else {
                Text("Tidak ada foto bukti.", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            Spacer(Modifier.height(14.dp))
            when {
                p.isPaid -> NoticeBox("Sudah lunas", "Pembayaran ini sudah diverifikasi.", NoticeTone.INFO)
                p.isTransferRejected -> NoticeBox("Bukti ditolak", "Alasan: ${p.rejectReason ?: "-"}. Menunggu penghuni mengirim ulang.", NoticeTone.DANGER)
                else -> NoticeBox(
                    "Cocokkan dengan mutasi rekening",
                    "Pastikan nominal ${Fmt.rupiah(p.amount)} sudah masuk ke rekening sebelum menandai lunas. Nota dibuat otomatis setelah diverifikasi."
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        if (p.isAwaitingVerification) {
            Surface(color = White, shadowElevation = 10.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    PrimaryButton("Verifikasi & tandai lunas", {
                        working = true
                        vm.verifyTransfer(p.id) { r ->
                            working = false
                            r.onSuccess {
                                notify("Transfer Kamar ${room?.number ?: ""} diverifikasi, nota dibuat")
                                nav.popBackStack()
                                nav.navigate(AdminRoutes.receipt(it.id))
                            }.onFailure { notify(it.message ?: "Gagal memverifikasi") }
                        }
                    }, loading = working)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showReject = true }, enabled = !working, modifier = Modifier.fillMaxWidth()) {
                        Text("Tolak bukti transfer", color = Danger)
                    }
                }
            }
        }
    }

    if (zoom && p != null) ProofViewerDialog(vm.proofUrl(p), vm.authHeader()) { zoom = false }

    if (showReject && p != null) {
        var reason by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var sending by remember { mutableStateOf(false) }
        val quick = listOf("Nominal tidak sesuai", "Foto bukti tidak jelas", "Dana belum masuk ke rekening")
        AlertDialog(
            onDismissRequest = { if (!sending) showReject = false },
            containerColor = White,
            title = { Text("Tolak bukti transfer?") },
            text = {
                Column {
                    Text("Penghuni akan menerima notifikasi beserta alasan, lalu bisa mengirim ulang bukti.", style = MaterialTheme.typography.bodySmall, color = Ink600)
                    Spacer(Modifier.height(10.dp))
                    quick.forEach { q ->
                        TextButton(onClick = { reason = q; error = null }) { Text(q, color = Green700, style = MaterialTheme.typography.labelMedium) }
                    }
                    KostKuTextField("Alasan penolakan", reason, { reason = it; error = null }, "Tulis alasan singkat", error = error, enabled = !sending, singleLine = false, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(enabled = !sending, onClick = {
                    if (reason.trim().length < 5) { error = "Alasan minimal 5 huruf"; return@TextButton }
                    sending = true
                    vm.rejectTransfer(p.id, reason) { r ->
                        sending = false
                        r.onSuccess { showReject = false; notify("Bukti transfer ditolak"); nav.popBackStack() }
                            .onFailure { error = it.message }
                    }
                }) { Text("Tolak", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { showReject = false }, enabled = !sending) { Text("Batal", color = Ink600) } }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink600, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink900)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  6. Pengaturan metode pembayaran
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PaymentSettingsScreen(vm: AdminViewModel, nav: NavHostController) {
    val current by vm.paymentSettings.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    var transfer by rememberSaveable { mutableStateOf(current.transferEnabled) }
    var cash by rememberSaveable { mutableStateOf(current.cashEnabled) }
    var bank by rememberSaveable { mutableStateOf(current.bankName) }
    var number by rememberSaveable { mutableStateOf(current.accountNumber) }
    var holder by rememberSaveable { mutableStateOf(current.accountHolder) }
    var note by rememberSaveable { mutableStateOf(current.transferNote) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var failure by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    // Isi ulang formulir jika data dari server baru masuk (mis. layar dibuka sebelum sinkron selesai).
    var loadedFrom by remember { mutableStateOf(current) }
    LaunchedEffect(current) {
        if (current != loadedFrom && !saving) {
            transfer = current.transferEnabled; cash = current.cashEnabled; bank = current.bankName
            number = current.accountNumber; holder = current.accountHolder; note = current.transferNote
            loadedFrom = current
        }
    }

    fun save() {
        val e = buildMap {
            if (transfer && bank.isBlank()) put("bank_name", "Nama bank wajib diisi")
            if (transfer && number.filter(Char::isDigit).length < 5) put("account_number", "Nomor rekening minimal 5 angka")
            if (transfer && holder.trim().length < 3) put("account_holder", "Nama pemilik rekening wajib diisi")
            if (!transfer && !cash) put("method", "Minimal satu metode pembayaran harus aktif")
        }
        errors = e
        if (e.isNotEmpty()) return
        saving = true
        failure = null
        vm.savePaymentSettings(PaymentSettings(transfer, cash, bank.trim(), number.trim(), holder.trim(), note.trim())) { r ->
            saving = false
            r.onSuccess { notify("Pengaturan pembayaran disimpan"); nav.popBackStack() }
                .onFailure { failure = it.message }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Metode pembayaran", { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            PageHeader("Atur metode pembayaran", "Penghuni hanya melihat metode yang aktif.")
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                ToggleRow("Transfer bank", "Penghuni mengunggah bukti, Anda memverifikasi.", transfer) { transfer = it; errors = errors - "method" }
                HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 10.dp))
                ToggleRow("Tunai", "Penghuni membayar langsung, Anda mencatat di aplikasi.", cash) { cash = it; errors = errors - "method" }
            }
            errors["method"]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.padding(top = 6.dp)) }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Rekening tujuan transfer")
            Spacer(Modifier.height(10.dp))
            KostKuTextField("Nama bank", bank, { bank = it.take(60); errors = errors - "bank_name" }, "Contoh: BCA, BRI, Mandiri", error = errors["bank_name"], enabled = !saving)
            Spacer(Modifier.height(14.dp))
            KostKuTextField(
                "Nomor rekening", number, { number = it.filter { c -> c.isDigit() || c == ' ' || c == '-' || c == '.' }.take(40); errors = errors - "account_number" },
                "Contoh: 123 456 7890", keyboardType = KeyboardType.Number, error = errors["account_number"], enabled = !saving
            )
            Spacer(Modifier.height(14.dp))
            KostKuTextField("Atas nama", holder, { holder = it.take(100); errors = errors - "account_holder" }, "Nama pemilik rekening", error = errors["account_holder"], enabled = !saving)
            Spacer(Modifier.height(14.dp))
            KostKuTextField(
                "Catatan untuk penghuni (opsional)", note, { note = it.take(255) },
                "Contoh: Tulis nomor kamar di berita transfer", enabled = !saving, singleLine = false, minLines = 2
            )
            Spacer(Modifier.height(16.dp))
            NoticeBox(
                "Data rekening tampil di aplikasi penghuni",
                "Periksa kembali nomor rekening sebelum menyimpan. Mengubah rekening tidak memengaruhi bukti yang sudah dikirim.",
                NoticeTone.INFO
            )
            Spacer(Modifier.height(12.dp))
            ErrorBanner(failure)
            Spacer(Modifier.height(16.dp))
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton("Simpan pengaturan", { save() }, loading = saving)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Green700)
        )
    }
}

/** Dipakai RoomDetailScreen: ringkasan tagihan yang buktinya menunggu verifikasi. */
@Composable
fun PendingTransferBanner(payment: PaymentEntity, onReview: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onReview, containerColor = Gold100) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.HourglassTop, null, tint = Warning)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Bukti transfer menunggu verifikasi", style = MaterialTheme.typography.titleSmall, color = Ink900)
                Text(
                    "Dikirim ${payment.transferSubmittedAt?.let { Fmt.date(it) } ?: "-"} • ketuk untuk memeriksa",
                    style = MaterialTheme.typography.bodySmall, color = Ink600
                )
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
        }
    }
}

