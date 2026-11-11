package com.kostku.app.ui.screens.tenant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kostku.app.data.local.Session
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.DropdownField
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.ReportStatusChip
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.dashedBorder
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.util.Fmt
import com.kostku.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

private val categories = listOf("AC", "Keran/Air", "Listrik", "Bangunan")

@Composable
fun TenantRepairScreen(vm: TenantViewModel, session: Session) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notify = LocalNotifier.current

    var category by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            importing = true
            scope.launch {
                val path = ImageStorage.importImage(context, uri, "report")
                importing = false
                if (path != null) photoPath = path else notify("Foto tidak dapat dibaca. Coba foto lain.")
            }
        }
    }

    fun submit() {
        val e = buildMap {
            if (category == null) put("category", "Pilih kategori kerusakan")
            if (description.trim().length < 10) put("description", "Jelaskan kerusakan minimal 10 karakter")
        }
        errors = e
        val cat = category
        if (e.isNotEmpty() || cat == null) return
        sending = true
        vm.submitReport(cat, description, photoPath) {
            sending = false
            category = null; description = ""; photoPath = null
            notify("Laporan terkirim ke pengelola")
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        PageHeader(
            "Lapor Kerusakan Fasilitas",
            "${session.name} • Kamar ${room?.number ?: "-"}",
            Modifier.entrance(0)
        )
        Spacer(Modifier.height(20.dp))
        DropdownField(
            label = "Kategori Kerusakan",
            value = category ?: "",
            placeholder = "Pilih kategori",
            options = categories,
            onSelect = { category = categories[it]; errors = errors - "category" },
            error = errors["category"],
            modifier = Modifier.entrance(1)
        )
        Text(
            "Pilihan: AC, Keran/Air, Listrik, Bangunan",
            style = MaterialTheme.typography.bodySmall, color = Ink400, modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel("Foto Fasilitas (opsional)")
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(150.dp).dashedBorder(Green700.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !importing) {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            val path = photoPath
            when {
                importing -> CircularProgressIndicator(color = Green700)
                path != null -> AsyncImage(
                    model = File(path), contentDescription = "Foto kerusakan",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = Green700)
                    Spacer(Modifier.height(6.dp))
                    Text("Ketuk untuk unggah foto", style = MaterialTheme.typography.labelLarge, color = Green700)
                    Text("JPG atau PNG • dikompres otomatis", style = MaterialTheme.typography.bodySmall, color = Ink400)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        KostKuTextField(
            "Deskripsi Kerusakan", description, { description = it; errors = errors - "description" },
            "Jelaskan kerusakan yang dialami…", error = errors["description"], enabled = !sending,
            singleLine = false, minLines = 4
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Kirim Laporan", { submit() }, loading = sending)

        Spacer(Modifier.height(28.dp))
        SectionLabel("Laporan saya")
        Spacer(Modifier.height(10.dp))
        if (reports.isEmpty()) {
            Text("Belum ada laporan yang Anda kirim.", style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
        reports.forEach { r ->
            AppCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.category, style = MaterialTheme.typography.titleSmall, color = Ink900)
                        Text(Fmt.date(r.createdAt), style = MaterialTheme.typography.bodySmall, color = Ink400)
                    }
                    ReportStatusChip(r.status)
                }
                Spacer(Modifier.height(6.dp))
                Text(r.description, style = MaterialTheme.typography.bodyMedium, color = Ink600)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
