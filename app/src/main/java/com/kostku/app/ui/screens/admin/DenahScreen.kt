package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.repository.RoomCardData
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.DangerBg
import com.kostku.app.ui.theme.Gold500
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green500
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.WarningBg
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt

private val EmptyBg = Color(0xFFF3F5F4)
private val UnpaidBorder = Color(0xFFE5A3A3)

@Composable
fun DenahScreen(vm: AdminViewModel, nav: NavHostController, showBack: Boolean) {
    val cards by vm.roomCards.collectAsStateWithLifecycle()
    var floor by rememberSaveable { mutableIntStateOf(1) }
    val rooms = cards.filter { it.room.floor == floor }
    val occupied = rooms.count { it.room.status == RoomStatus.OCCUPIED }
    val maintenance = rooms.count { it.room.status == RoomStatus.MAINTENANCE }
    val paid = rooms.count { it.payment?.status == PaymentStatus.PAID }
    val unpaid = rooms.count { it.room.status == RoomStatus.OCCUPIED && it.payment?.status != PaymentStatus.PAID }

    Column(Modifier.fillMaxSize()) {
        if (showBack) TopBarWithBack("Denah Kamar", { nav.popBackStack() })
        else Text(
            "Denah Kamar",
            style = MaterialTheme.typography.titleMedium, color = Ink900,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Denah New Afra Kost", style = MaterialTheme.typography.headlineMedium, color = Ink900,
                modifier = Modifier.entrance(0)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pantau hunian dan pembayaran setiap kamar.",
                style = MaterialTheme.typography.bodyMedium, color = Ink600, modifier = Modifier.entrance(1)
            )
            Spacer(Modifier.height(16.dp))
            FloorTabs(
                options = listOf("Lantai 1 (16 Kamar)", "Lantai 2 (20 Kamar)"),
                selected = floor - 1, onSelect = { floor = it + 1 }
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val extra = if (maintenance > 0) " • $maintenance perbaikan" else ""
                Text("$occupied terisi$extra", style = MaterialTheme.typography.labelMedium, color = Ink900)
                Text(
                    Fmt.month(vm.period.year, vm.period.monthValue),
                    style = MaterialTheme.typography.labelMedium, color = Ink600
                )
            }
            Spacer(Modifier.height(10.dp))

            // Denah: dua kolom kamar dengan koridor di tengah
            Surface(
                shape = RoundedCornerShape(18.dp), color = White,
                border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rooms.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RoomTile(pair[0], Modifier.weight(1f)) { nav.navigate(AdminRoutes.room(pair[0].room.id)) }
                                Spacer(Modifier.width(32.dp))
                                if (pair.size > 1) {
                                    RoomTile(pair[1], Modifier.weight(1f)) { nav.navigate(AdminRoutes.room(pair[1].room.id)) }
                                } else Spacer(Modifier.weight(1f))
                            }
                        }
                        Text(
                            "Pintu masuk lantai $floor", style = MaterialTheme.typography.labelSmall,
                            color = Ink400, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }
                    Text(
                        "K O R I D O R", softWrap = false,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Ink400,
                        modifier = Modifier.align(Alignment.Center).rotate(90f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Keterangan status", style = MaterialTheme.typography.titleSmall, color = Ink900)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Legend(Green50, Green500, "Terisi • Lunas", Modifier.weight(1f))
                Legend(DangerBg, UnpaidBorder, "Terisi • Belum bayar", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Legend(WarningBg, Gold500, "Perbaikan • Nonaktif", Modifier.weight(1f))
                Legend(EmptyBg, Line, "Kosong", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "$paid lunas • $unpaid belum bayar. Warna menunjukkan status pembayaran dan hunian.",
                style = MaterialTheme.typography.bodySmall, color = Ink600
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoomTile(card: RoomCardData, modifier: Modifier, onClick: () -> Unit) {
    val paid = card.payment?.status == PaymentStatus.PAID
    val (bg, border, fg) = when {
        card.room.status == RoomStatus.MAINTENANCE -> Triple(WarningBg, Gold500, Warning)
        card.room.status == RoomStatus.EMPTY -> Triple(EmptyBg, Line, Ink400)
        paid -> Triple(Green50, Green500, Green700)
        else -> Triple(DangerBg, UnpaidBorder, Danger)
    }
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp),
        color = bg, border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(card.room.number.toString(), style = MaterialTheme.typography.titleSmall, color = fg)
            Text(
                card.tenant?.name ?: if (card.room.status == RoomStatus.MAINTENANCE) "Perbaikan" else "Kosong",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Ink600, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Legend(bg: Color, border: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).background(bg, RoundedCornerShape(4.dp)).padding(1.dp)) {
            Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(4.dp), color = bg, border = BorderStroke(1.dp, border)) {}
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink600)
    }
}
