package com.kostku.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.RoomStatus
import com.kostku.app.data.repository.RoomCardData
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyChip
import com.kostku.app.ui.components.InitialsAvatar
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.MaintenanceChip
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.RoomPhotoPager
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.dashedBorder
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.DangerBg
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.ImageStorage
import com.kostku.app.util.photoModel
import com.kostku.app.util.Intents
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RoomDetailScreen(roomId: Long, vm: AdminViewModel, nav: NavHostController) {
    val cards by vm.roomCards.collectAsStateWithLifecycle()
    val photosMap by vm.photosByRoom.collectAsStateWithLifecycle()
    val card = cards.firstOrNull { it.room.id == roomId }
    val photos = photosMap[roomId].orEmpty()
    val pager = rememberPagerState { photos.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notify = LocalNotifier.current
    var showUpload by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green700)
        }
        return
    }

    val room = card.room
    val tenant = card.tenant
    val payment = card.payment
    val paid = payment?.status == PaymentStatus.PAID

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Detail Kamar ${room.number} (Lantai ${room.floor})", { nav.popBackStack() })

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            when (room.status) {
                RoomStatus.OCCUPIED -> PaymentStatusChip(paid)
                RoomStatus.EMPTY -> EmptyChip("Kosong / Siap Huni")
                RoomStatus.MAINTENANCE -> MaintenanceChip()
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Foto kamar")
                Text("${photos.size} foto", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            Spacer(Modifier.height(8.dp))
            RoomPhotoPager(photos, pager, Modifier.fillMaxWidth().height(230.dp))

            if (photos.size > 1) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(photos, key = { _, p -> p.id }) { index, photo ->
                        val selected = pager.currentPage == index
                        AsyncImage(
                            model = photoModel(photo.uri), contentDescription = "Foto ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp, 54.dp).clip(RoundedCornerShape(10.dp))
                                .border(if (selected) 2.dp else 0.dp, Green700, RoundedCornerShape(10.dp))
                                .clickable { scope.launch { pager.animateScrollToPage(index) } }
                        )
                    }
                }
            }
            if (photos.isNotEmpty()) {
                TextButton(onClick = {
                    photos.getOrNull(pager.currentPage)?.let { vm.deletePhoto(it.id) }
                }) { Text("Hapus foto ini", color = Danger, style = MaterialTheme.typography.labelMedium) }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().dashedBorder(Green700.copy(alpha = 0.45f)).clip(RoundedCornerShape(16.dp))
                    .clickable { showUpload = true }.padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = Green700)
                    Spacer(Modifier.height(4.dp))
                    Text("+ Tambah / Ganti Foto Kamar", style = MaterialTheme.typography.labelMedium, color = Green700)
                }
            }

            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Text("Fasilitas All-In", style = MaterialTheme.typography.titleSmall, color = Ink900)
                Spacer(Modifier.height(4.dp))
                Text(room.facilities, style = MaterialTheme.typography.bodyMedium, color = Ink600)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${Fmt.rupiah(room.monthlyRate)} / bulan",
                    style = MaterialTheme.typography.titleSmall, color = Green700
                )
            }

            if (tenant != null) {
                Spacer(Modifier.height(12.dp))
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(AdminRoutes.tenant(tenant.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(tenant.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tenant.name, style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(
                                "Masuk ${Fmt.date(tenant.entryDate)}",
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
                    }
                    if (payment != null && !paid) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val text = Intents.reminderText(
                                    tenant.name, room.number, Fmt.month(payment.periodYear, payment.periodMonth),
                                    payment.amount, Fmt.date(payment.dueDate)
                                )
                                if (Intents.openWhatsApp(context, tenant.phone, text)) vm.markReminderSent(payment.id)
                                else notify("WhatsApp atau browser tidak ditemukan.")
                            },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Green700, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ingatkan via WhatsApp", color = Green700)
                        }
                    }
                }
            }

            // Informasi Pembayaran (Card)
            if (payment != null) {
                Spacer(Modifier.height(12.dp))
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Status Pembayaran (${Fmt.month(payment.periodYear, payment.periodMonth)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink900,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentStatusChip(paid)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (paid) "Lunas pada ${payment.paidAt?.let { Fmt.date(it) } ?: "-"}"
                        else "Jatuh tempo ${Fmt.date(payment.dueDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink600
                    )
                }
            }

            // Spacer bawah agar konten tidak tertutup oleh Bottom Action Bar
            Spacer(Modifier.height(84.dp))
        }

        // Aksi utama di bawah
        Surface(color = White, shadowElevation = 10.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                when (room.status) {
                    RoomStatus.OCCUPIED -> {
                        if (payment != null && !paid) {
                            PrimaryButton("CATAT PEMBAYARAN TUNAI (${Fmt.rupiah(payment.amount)})", { showConfirm = true })
                        } else if (payment != null) {
                            SecondaryButton("Lihat Nota Pembayaran", { nav.navigate(AdminRoutes.receipt(payment.id)) })
                        }
                    }
                    RoomStatus.EMPTY -> {
                        PrimaryButton("Tambah Penghuni di Kamar Ini", { nav.navigate(AdminRoutes.tenantAdd(room.id)) })
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Tandai Dalam Perbaikan", {
                            vm.setMaintenance(room.id, true) { r ->
                                r.onSuccess { notify("Kamar ${room.number} ditandai dalam perbaikan") }
                                    .onFailure { notify(it.message ?: "Gagal mengubah status kamar") }
                            }
                        })
                    }
                    RoomStatus.MAINTENANCE -> PrimaryButton("Aktifkan Kembali Kamar", {
                        vm.setMaintenance(room.id, false) { r ->
                            r.onSuccess { notify("Kamar ${room.number} kembali tersedia") }
                                .onFailure { notify(it.message ?: "Gagal mengubah status kamar") }
                        }
                    })
                }
            }
        }
    }

    if (showUpload) {
        UploadPhotoDialog(
            roomNumber = room.number,
            onDismiss = { showUpload = false },
            onSave = { path, caption ->
                // Pesan "tersimpan" / gagal ditampilkan setelah unggahan ke server selesai.
                vm.addPhoto(room.id, path, caption, room.number)
                showUpload = false
                notify("Mengunggah foto kamar ${room.number}…")
            }
        )
    }

    if (showConfirm && payment != null && tenant != null) {
        ConfirmCashSheet(
            card = card,
            periodLabel = Fmt.month(payment.periodYear, payment.periodMonth),
            loading = confirming,
            onDismiss = { if (!confirming) showConfirm = false },
            onConfirm = {
                confirming = true
                vm.confirmPayment(payment.id) { result ->
                    confirming = false
                    result.onSuccess {
                        showConfirm = false
                        notify("Pembayaran Kamar ${room.number} dicatat lunas")
                        nav.navigate(AdminRoutes.receipt(it.id))
                    }.onFailure {
                        showConfirm = false
                        notify(it.message ?: "Gagal mencatat pembayaran")
                    }
                }
            }
        )
    }
}

@Composable
private fun UploadPhotoDialog(roomNumber: Int, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { picked = uri; error = null }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            picked = tempCameraUri
            error = null
        }
    }

    fun launchCamera() {
        runCatching {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val tempFile = File(dir, "cam_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            error = "Gagal membuka kamera: ${it.message}"
        }
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface(shape = RoundedCornerShape(24.dp), color = White) {
            Column(Modifier.padding(20.dp)) {
                Text("Unggah Foto Kamar $roomNumber", style = MaterialTheme.typography.titleLarge, color = Ink900)
                Spacer(Modifier.height(14.dp))

                val uri = picked
                if (uri != null) {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = uri, contentDescription = "Pratinjau foto",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { picked = null }) {
                            Text("Ganti Foto", color = Green700, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text("Pilih Sumber Foto", style = MaterialTheme.typography.titleSmall, color = Ink900)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { launchCamera() },
                            shape = RoundedCornerShape(14.dp),
                            color = Green50,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, null, tint = Green700, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Ambil Kamera", style = MaterialTheme.typography.labelMedium, color = Green700)
                            }
                        }
                        Surface(
                            onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Green50,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, null, tint = Green700, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Pilih Galeri", style = MaterialTheme.typography.labelMedium, color = Green700)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                KostKuTextField(
                    label = "Keterangan Foto (opsional)",
                    value = caption, onValueChange = { caption = it },
                    placeholder = "Misal: Kondisi kamar mandi atau kasur", enabled = !saving
                )
                if (error != null) {
                    Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, enabled = !saving,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.2.dp, Green700)
                    ) { Text("Batal", color = Green700) }
                    Button(
                        onClick = {
                            val u = picked
                            if (u == null) { error = "Pilih foto dari Kamera atau Galeri terlebih dahulu."; return@Button }
                            saving = true
                            scope.launch {
                                val path = ImageStorage.importImage(context, u, "room$roomNumber")
                                if (path == null) {
                                    error = "Foto tidak dapat dibaca. Coba ambil/pilih foto lain."
                                    saving = false
                                } else onSave(path, caption)
                            }
                        },
                        enabled = !saving, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700, contentColor = White)
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                        else Text("Simpan Foto")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmCashSheet(
    card: RoomCardData,
    periodLabel: String,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val amount = card.payment?.amount ?: card.room.monthlyRate
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.size(44.dp).background(Green50, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Green700)
            }
            Spacer(Modifier.height(14.dp))
            Text("Konfirmasi Pembayaran Tunai", style = MaterialTheme.typography.headlineSmall, color = Ink900)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Kamar ${card.room.number} - ${card.tenant?.name ?: ""}",
                    style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = Modifier.weight(1f)
                )
                PaymentStatusChip(paid = false)
            }
            Spacer(Modifier.height(6.dp))
            Text("Periode: $periodLabel", style = MaterialTheme.typography.bodyMedium, color = Ink600)
            Spacer(Modifier.height(16.dp))
            Text("Total Pembayaran All-In", style = MaterialTheme.typography.bodySmall, color = Ink600)
            Text(
                Fmt.rupiahSpaced(amount),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900
            )
            Text("Terbilang: ${Fmt.terbilang(amount)}.", style = MaterialTheme.typography.bodySmall, color = Ink600)
            Spacer(Modifier.height(14.dp))
            Text(
                "Pastikan uang tunai sudah Anda terima sebelum konfirmasi.",
                style = MaterialTheme.typography.bodyMedium, color = Ink600
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton("YA, SUDAH TERIMA UANG", onConfirm, loading = loading)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Batal", onDismiss)
        }
    }
}
