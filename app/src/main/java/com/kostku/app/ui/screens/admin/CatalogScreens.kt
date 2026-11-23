package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.RoomPhotoPager
import com.kostku.app.ui.components.StatusChip
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt

@Composable
fun CatalogScreen(vm: AdminViewModel, nav: NavHostController) {
    val cards by vm.roomCards.collectAsStateWithLifecycle()
    val photosMap by vm.photosByRoom.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val shown = cards.filter { tab == 0 || it.room.floor == tab }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Katalog Kamar", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    PageHeader("Katalog Kamar New Afra Kost", "Pilih kamar sesuai kebutuhanmu", Modifier.entrance(0))
                    Spacer(Modifier.height(16.dp))
                    FloorTabs(listOf("Semua", "Lantai 1", "Lantai 2"), tab, { tab = it })
                }
            }
            if (shown.isEmpty()) {
                item { EmptyState(Icons.Outlined.Bed, "Tidak ada kamar", "Belum ada data kamar untuk ditampilkan.") }
            }
            items(shown, key = { it.room.id }) { card ->
                val room = card.room
                val photos = photosMap[room.id].orEmpty()
                val pager = rememberPagerState { photos.size }
                Surface(
                    onClick = { nav.navigate(AdminRoutes.room(room.id)) },
                    shape = RoundedCornerShape(20.dp), color = White, border = BorderStroke(1.dp, Line)
                ) {
                    Column {
                        RoomPhotoPager(
                            photos, pager, Modifier.fillMaxWidth().height(200.dp),
                            badge = {
                                when (room.status) {
                                    RoomStatus.EMPTY -> StatusChip("TERSEDIA", Green700, White)
                                    RoomStatus.OCCUPIED -> StatusChip("TERISI", Green50, Ink600)
                                    RoomStatus.MAINTENANCE -> StatusChip("PERBAIKAN", Gold100, Warning)
                                }
                            }
                        )
                        Column(Modifier.padding(16.dp)) {
                            Text("Kamar ${room.number} - Lantai ${room.floor}", style = MaterialTheme.typography.titleMedium, color = Ink900)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${Fmt.rupiahSpaced(room.monthlyRate)} / Bulan (All-In)",
                                style = MaterialTheme.typography.titleSmall, color = Green700
                            )
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Line)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Facility(Icons.Outlined.Bed, "Kasur")
                                Facility(Icons.Outlined.Wifi, "Wi-Fi")
                                Facility(Icons.Outlined.Bolt, "Listrik")
                                Facility(Icons.Outlined.WaterDrop, "Air")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Facility(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Green700, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink600)
    }
}
