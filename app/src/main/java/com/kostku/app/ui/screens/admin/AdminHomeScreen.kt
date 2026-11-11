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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.repository.RoomCardData
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyChip
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.KostKuLogo
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.MaintenanceChip
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents

private data class FeatureTile(val label: String, val icon: ImageVector, val route: String)

@Composable
fun AdminHomeScreen(vm: AdminViewModel, nav: NavHostController) {
    val cards by vm.roomCards.collectAsStateWithLifecycle()
    var floor by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current
    val notify = LocalNotifier.current

    val occupied = cards.count { it.room.status == RoomStatus.OCCUPIED }
    val inactive = cards.count { it.room.status == RoomStatus.MAINTENANCE }
    val floor1 = cards.count { it.room.floor == 1 }
    val floor2 = cards.count { it.room.floor == 2 }
    val shown = cards.filter { it.room.floor == floor }

    val tiles = remember {
        listOf(
            FeatureTile("Denah kamar", Icons.Outlined.Map, AdminRoutes.ROOMS),
            FeatureTile("Pengumuman", Icons.Outlined.Campaign, AdminRoutes.ANNOUNCE),
            FeatureTile("Data penghuni", Icons.Outlined.Groups, AdminRoutes.TENANTS),
            FeatureTile("Katalog kamar", Icons.Outlined.Apartment, AdminRoutes.CATALOG),
            FeatureTile("Nota pembayaran", Icons.Outlined.ReceiptLong, AdminRoutes.RECEIPTS)
        )
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().entrance(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Halo, ${vm.adminName}", style = MaterialTheme.typography.headlineMedium, color = Ink900)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "New Afra Kost - Jl. Perintis Kemerdekaan 4",
                        style = MaterialTheme.typography.bodySmall, color = Ink600
                    )
                }
                KostKuLogo(44.dp)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionLabel("Akses fitur", Modifier.padding(top = 8.dp).entrance(1))
        }
        items(tiles) { tile ->
            AppCard(
                modifier = Modifier.fillMaxWidth().entrance(2),
                onClick = {
                    if (tile.route == AdminRoutes.ROOMS) nav.navigateTab(tile.route, AdminRoutes.HOME)
                    else nav.navigate(tile.route)
                }
            ) {
                Box(
                    Modifier.size(38.dp).background(Green50, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(tile.icon, null, tint = Green700, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(12.dp))
                Text(tile.label, style = MaterialTheme.typography.titleSmall, color = Ink900)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).entrance(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(occupied.toString(), "Kamar Terisi", Modifier.weight(1f))
                StatCard(inactive.toString(), "Kamar Non-Aktif/ Perbaikan", Modifier.weight(1f))
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
                PaymentStatusChip(paid)
                Spacer(Modifier.height(8.dp))
                Text(Fmt.rupiah(room.monthlyRate), style = MaterialTheme.typography.titleSmall, color = Ink900)
                Spacer(Modifier.height(6.dp))
                if (paid && payment?.paidAt != null) {
                    Text(
                        "Tunai • ${Fmt.shortDate(payment.paidAt)}",
                        style = MaterialTheme.typography.bodySmall, color = Ink400
                    )
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
