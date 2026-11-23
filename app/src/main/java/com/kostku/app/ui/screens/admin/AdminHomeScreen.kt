package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.repository.RoomCardData
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.ContextualTooltipCard
import com.kostku.app.ui.components.EmptyChip
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.InteractiveTourDialog
import com.kostku.app.ui.components.KostKuLogo
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.LocalTourController
import com.kostku.app.ui.components.MaintenanceChip
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.PaymentStateChip
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.SpotlightTarget
import com.kostku.app.ui.components.adminTourSteps
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.components.tourTarget
import com.kostku.app.ui.navigation.AppViewModel
import com.kostku.app.ui.theme.DarkGreen
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents

private data class FeatureTile(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val target: SpotlightTarget
)

@Composable
fun AdminHomeScreen(
    vm: AdminViewModel,
    nav: NavHostController,
    appViewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModel.Factory)
) {
    val cards by vm.roomCards.collectAsStateWithLifecycle()
    val pendingTransfers by vm.pendingTransfers.collectAsStateWithLifecycle()
    val unreadNotifications by vm.unreadCount.collectAsStateWithLifecycle()
    val isAdminTourCompleted by appViewModel.isAdminTourCompleted.collectAsStateWithLifecycle()
    val tour = LocalTourController.current
    val gridState = rememberLazyGridState()
    var showTour by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(true) }
    var floor by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current
    val notify = LocalNotifier.current

    LaunchedEffect(isAdminTourCompleted) {
        if (isAdminTourCompleted == false) {
            showTour = true
        }
    }

    // "Ulangi Panduan Aplikasi" di halaman Profil & Info → panduan diputar ulang di Beranda.
    LaunchedEffect(tour?.replayRequested) {
        if (tour?.consumeReplayRequest() == true) showTour = true
    }

    val occupied = cards.count { it.room.status == RoomStatus.OCCUPIED }
    val inactive = cards.count { it.room.status == RoomStatus.MAINTENANCE }
    val floor1 = cards.count { it.room.floor == 1 }
    val floor2 = cards.count { it.room.floor == 2 }
    val shown = cards.filter { it.room.floor == floor }

    val tiles = remember {
        listOf(
            FeatureTile("Denah kamar", Icons.Outlined.Map, AdminRoutes.ROOMS, SpotlightTarget.DENAH_KAMAR),
            FeatureTile("Pengumuman", Icons.Outlined.Campaign, AdminRoutes.ANNOUNCE, SpotlightTarget.PENGUMUMAN),
            FeatureTile("Data penghuni", Icons.Outlined.Groups, AdminRoutes.TENANTS, SpotlightTarget.DATA_PENGHUNI),
            FeatureTile("Katalog kamar", Icons.Outlined.Apartment, AdminRoutes.CATALOG, SpotlightTarget.KATALOG_KAMAR),
            FeatureTile("Nota pembayaran", Icons.Outlined.ReceiptLong, AdminRoutes.RECEIPTS, SpotlightTarget.NOTA_PEMBAYARAN)
        )
    }

    // Posisi item di dalam grid: 0 = banner, 1 = kartu petunjuk (hanya bila tampil),
    // lalu label "Akses fitur", lalu tile fitur. Dipakai panduan untuk menggulung ke target.
    val tooltipVisible = showTooltip && isAdminTourCompleted == true
    val firstTileIndex = if (tooltipVisible) 3 else 2

    fun gridIndexOf(target: SpotlightTarget): Int? = when (target) {
        SpotlightTarget.HEADER -> 0
        else -> tiles.indexOfFirst { it.target == target }
            .takeIf { it >= 0 }
            ?.let { firstTileIndex + it }
    }

    fun remind(card: RoomCardData) {
        val tenant = card.tenant ?: return
        val payment = card.payment ?: return
        val text = Intents.reminderText(
            tenant.name, card.room.number, Fmt.month(payment.periodYear, payment.periodMonth),
            payment.amount, Fmt.date(payment.dueDate)
        )
        if (Intents.openWhatsApp(context, tenant.phone, text)) vm.markReminderSent(payment.id)
        else notify("WhatsApp atau browser tidak ditemukan.")
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner Header Admin
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(0)
                        .tourTarget(SpotlightTarget.HEADER),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(DarkGreen, Green700)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Halo, Pengelola Kost",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = White.copy(alpha = 0.85f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    vm.adminName,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = White
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "New Afra Kost • Jl. Perintis Kemerdekaan 4",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = White.copy(alpha = 0.8f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NotificationBell(unreadNotifications, White) { nav.navigate(AdminRoutes.NOTIFICATIONS) }
                                IconButton(onClick = { showTour = true }) {
                                    Icon(Icons.Outlined.HelpOutline, contentDescription = "Panduan Aplikasi", tint = White)
                                }
                                KostKuLogo(size = 38.dp)
                            }
                        }
                    }
                }
            }

            if (tooltipVisible) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContextualTooltipCard(
                        title = "Petunjuk Pengelola Kost",
                        message = "Gunakan Denah Kamar untuk cek status hunian, Nota Pembayaran untuk riwayat transaksi, atau ikon bantuan (?), untuk mengulang Panduan.",
                        onDismiss = { showTooltip = false }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel("Akses fitur", Modifier.padding(top = 4.dp).entrance(1))
            }
            items(tiles) { tile ->
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(2)
                        .tourTarget(tile.target),
                    onClick = {
                        if (tile.route == AdminRoutes.ROOMS) nav.navigateTab(tile.route, AdminRoutes.HOME)
                        else nav.navigate(tile.route)
                    }
                ) {
                    Box(
                        Modifier.size(42.dp).background(Green50, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(tile.icon, null, tint = Green700, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text(tile.label, style = MaterialTheme.typography.titleSmall, color = Ink900)
                    // Badge jumlah bukti transfer yang menunggu verifikasi
                    if (tile.route == AdminRoutes.RECEIPTS && pendingTransfers.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${pendingTransfers.size} bukti transfer menunggu",
                            style = MaterialTheme.typography.labelSmall, color = Warning
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .entrance(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(occupied.toString(), "Kamar Terisi", Modifier.weight(1f))
                    StatCard(inactive.toString(), "Kamar Non-Aktif / Perbaikan", Modifier.weight(1f))
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FloorTabs(
                    options = listOf("Lantai 1 ($floor1 Kamar)", "Lantai 2 ($floor2 Kamar)"),
                    selected = floor - 1,
                    onSelect = { floor = it + 1 },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(shown, key = { it.room.id }) { card ->
                RoomSummaryCard(
                    card = card,
                    onClick = { nav.navigate(AdminRoutes.room(card.room.id)) },
                    onRemind = { remind(card) }
                )
            }
        }

        if (showTour) {
            InteractiveTourDialog(
                steps = adminTourSteps,
                onFinish = {
                    showTour = false
                    appViewModel.completeAdminTour()
                },
                onBringIntoView = { target ->
                    if (target == SpotlightTarget.FINAL_OVERVIEW) {
                        // Langkah penutup: kembalikan halaman ke posisi paling atas.
                        gridState.animateScrollToItem(0)
                    } else {
                        // Target di bottom bar (tab Perbaikan) tidak punya index → tidak perlu digulung.
                        val index = gridIndexOf(target)
                        if (index != null) {
                            // Item yang belum tampil di layar digulung dulu sampai muncul…
                            if (tour?.isOnScreen(target) != true) gridState.animateScrollToItem(index)
                            // …lalu dirapikan agar utuh dan tidak tertutup kartu penjelasan.
                            tour?.scrollIntoView(target, gridState)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = Ink900)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink600)
    }
}

@Composable
private fun RoomSummaryCard(card: RoomCardData, onClick: () -> Unit, onRemind: () -> Unit) {
    val room = card.room
    AppCard(modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp), onClick = onClick) {
        Text("Kamar ${room.number}", style = MaterialTheme.typography.titleMedium, color = Ink900)
        Text(
            card.tenant?.name ?: "Tidak ditempati",
            style = MaterialTheme.typography.bodySmall, color = Ink600, maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        when (room.status) {
            RoomStatus.OCCUPIED -> {
                val payment = card.payment
                val paid = payment?.status == PaymentStatus.PAID
                if (payment != null) PaymentStateChip(payment, short = true) else PaymentStatusChip(paid)
                Spacer(Modifier.height(8.dp))
                Text(Fmt.rupiah(room.monthlyRate), style = MaterialTheme.typography.titleSmall, color = Ink900)
                Spacer(Modifier.height(6.dp))
                if (paid && payment?.paidAt != null) {
                    Text(
                        "${payment.method ?: "Tunai"} • ${Fmt.shortDate(payment.paidAt)}",
                        style = MaterialTheme.typography.bodySmall, color = Ink400
                    )
                } else if (payment?.isAwaitingVerification == true) {
                    // Bukti transfer sudah masuk: ketuk kartu untuk memeriksa, tidak perlu diingatkan.
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Periksa bukti", style = MaterialTheme.typography.labelSmall, color = Green700)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRemind,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(15.dp), tint = Green700)
                        Spacer(Modifier.width(6.dp))
                        Text("Ingatkan via WA", style = MaterialTheme.typography.labelSmall, color = Green700)
                    }
                }
            }
            RoomStatus.MAINTENANCE -> {
                MaintenanceChip()
                Spacer(Modifier.height(8.dp))
                Text(
                    room.maintenanceNote ?: "Perbaikan fasilitas",
                    style = MaterialTheme.typography.titleSmall, color = Ink900
                )
                Spacer(Modifier.height(6.dp))
                Text("Kamar non-aktif", style = MaterialTheme.typography.bodySmall, color = Ink400)
            }
            RoomStatus.EMPTY -> {
                EmptyChip("KOSONG")
                Spacer(Modifier.height(8.dp))
                Text(Fmt.rupiah(room.monthlyRate), style = MaterialTheme.typography.titleSmall, color = Ink900)
                Spacer(Modifier.height(6.dp))
                Text("Siap huni", style = MaterialTheme.typography.bodySmall, color = Ink400)
            }
        }
    }
}
