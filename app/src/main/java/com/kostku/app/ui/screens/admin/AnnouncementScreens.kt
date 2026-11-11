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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.AnnouncementEntity
import com.kostku.app.data.local.AnnouncementTarget
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.DateField
import com.kostku.app.ui.components.DropdownField
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.InfoNote
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.StatusChip
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import java.time.LocalDate
import java.time.LocalTime

fun targetLabel(target: AnnouncementTarget) = when (target) {
    AnnouncementTarget.ALL -> "Semua penghuni"
    AnnouncementTarget.FLOOR_1 -> "Penghuni Lantai 1"
    AnnouncementTarget.FLOOR_2 -> "Penghuni Lantai 2"
}

@Composable
fun AnnouncementListScreen(vm: AdminViewModel, nav: NavHostController) {
    val list by vm.announcements.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    var toDelete by remember { mutableStateOf<AnnouncementEntity?>(null) }
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Pengumuman", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PageHeader("Pengumuman kost", "Informasi yang sudah dan akan tampil di halaman penghuni.", Modifier.entrance(0)) }
            if (list.isEmpty()) {
                item { EmptyState(Icons.Outlined.Campaign, "Belum ada pengumuman", "Buat pengumuman pertama untuk penghuni.") }
            }
            items(list, key = { it.id }) { a ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${Fmt.date(a.publishedAt)} • ${targetLabel(a.target)}",
                                style = MaterialTheme.typography.bodySmall, color = Ink400
                            )
                        }
                        if (a.publishedAt > now) StatusChip("TERJADWAL", Green50, Green700)
                        IconButton(onClick = { toDelete = a }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.DeleteOutline, "Hapus pengumuman", tint = Ink400)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(a.body, style = MaterialTheme.typography.bodyMedium, color = Ink600)
                }
            }
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton("+ Buat Pengumuman", { nav.navigate(AdminRoutes.ANNOUNCE_NEW) })
            }
        }
    }

    toDelete?.let { a ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = White,
            title = { Text("Hapus pengumuman?") },
            text = { Text("\"${a.title}\" tidak akan tampil lagi di halaman penghuni.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAnnouncement(a.id); toDelete = null; notify("Pengumuman dihapus")
                }) { Text("Hapus", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal", color = Ink600) } }
        )
    }
}

@Composable
fun AnnouncementCreateScreen(vm: AdminViewModel, nav: NavHostController) {
    val notify = LocalNotifier.current
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var targetIndex by rememberSaveable { mutableIntStateOf(0) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var saving by remember { mutableStateOf(false) }
    val targets = AnnouncementTarget.entries

    fun submit() {
        val e = buildMap {
            if (title.trim().length < 3) put("title", "Judul minimal 3 karakter")
            if (body.trim().length < 10) put("body", "Isi pengumuman minimal 10 karakter")
        }
        errors = e
        if (e.isNotEmpty()) return
        saving = true
        // Jika tanggal terbit hari ini, terbitkan sekarang; selain itu pukul 00.00 hari tersebut.
        val publishedAt = if (date == LocalDate.now()) System.currentTimeMillis()
        else Fmt.startOfDayMillis(date.atTime(LocalTime.MIN).toLocalDate())
        vm.publishAnnouncement(title, body, targets[targetIndex], publishedAt) {
            saving = false
            notify("Pengumuman diterbitkan")
            nav.popBackStack()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Buat Pengumuman", { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            PageHeader("Informasi untuk penghuni", "Sampaikan kabar terbaru di New Afra Kost.")
            Spacer(Modifier.height(20.dp))
            KostKuTextField(
                "Judul pengumuman", title, { title = it; errors = errors - "title" },
                "Pembersihan area bersama", error = errors["title"], enabled = !saving
            )
            Spacer(Modifier.height(16.dp))
            KostKuTextField(
                "Isi pengumuman", body, { body = it; errors = errors - "body" },
                "Tulis isi pengumuman…", error = errors["body"], enabled = !saving,
                singleLine = false, minLines = 5
            )
            Spacer(Modifier.height(16.dp))
            DropdownField(
                label = "Tujuan penerima",
                value = targetLabel(targets[targetIndex]),
                placeholder = "Pilih penerima",
                options = targets.map { targetLabel(it) },
                onSelect = { targetIndex = it }
            )
            Spacer(Modifier.height(16.dp))
            DateField("Tanggal terbit", date, { date = it })
            Spacer(Modifier.height(16.dp))
            InfoNote("Pengumuman akan tampil di halaman penghuni setelah diterbitkan oleh ${vm.adminName}.")
            Spacer(Modifier.height(20.dp))
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton("Terbitkan Pengumuman", { submit() }, loading = saving)
            }
        }
    }
}
