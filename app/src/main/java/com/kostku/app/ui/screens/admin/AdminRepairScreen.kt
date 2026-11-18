package com.kostku.app.ui.screens.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kostku.app.data.local.ReportStatus
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.ReportStatusChip
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.photoModel
import java.io.File

@Composable
fun AdminRepairScreen(vm: AdminViewModel) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val statuses = listOf(null, ReportStatus.SENT, ReportStatus.IN_PROGRESS, ReportStatus.DONE)
    val shown = reports.filter { statuses[filter] == null || it.report.status == statuses[filter] }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                PageHeader("Perbaikan fasilitas", "Laporan kerusakan dari penghuni. Perbarui statusnya agar penghuni tahu progresnya.")
                Spacer(Modifier.height(16.dp))
                FloorTabs(listOf("Semua", "Dikirim", "Diproses", "Selesai"), filter, { filter = it })
            }
        }
        if (shown.isEmpty()) {
            item { EmptyState(Icons.Outlined.Build, "Belum ada laporan", "Laporan kerusakan dari penghuni akan muncul di sini.") }
        }
        items(shown, key = { it.report.id }) { info ->
            val r = info.report
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(r.category, style = MaterialTheme.typography.titleSmall, color = Ink900)
                        Text(
                            "Kamar ${info.roomNumber ?: "-"} • ${info.tenantName ?: "-"} • ${Fmt.shortDate(r.createdAt)}",
                            style = MaterialTheme.typography.bodySmall, color = Ink400
                        )
                    }
                    ReportStatusChip(r.status)
                }
                Spacer(Modifier.height(8.dp))
                Text(r.description, style = MaterialTheme.typography.bodyMedium, color = Ink600)
                if (r.photoUri != null) {
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = photoModel(r.photoUri), contentDescription = "Foto kerusakan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
                when (r.status) {
                    ReportStatus.SENT -> {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.updateReportStatus(r, ReportStatus.IN_PROGRESS) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700, contentColor = White)
                        ) { Text("Proses laporan") }
                    }
                    ReportStatus.IN_PROGRESS -> {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.updateReportStatus(r, ReportStatus.DONE) },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700, contentColor = White)
                        ) { Text("Tandai selesai") }
                    }
                    ReportStatus.DONE -> Unit
                }
            }
        }
    }
}
