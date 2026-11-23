package com.kostku.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.repository.RoomPayState
import com.kostku.app.data.repository.payState
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.DangerBg
import com.kostku.app.ui.theme.Gold100
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.Warning
import com.kostku.app.ui.theme.White
import com.kostku.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

/** Chip status pembayaran: LUNAS / MENUNGGU VERIFIKASI / DITOLAK / BELUM BAYAR / KOSONG / PERBAIKAN. */
@Composable
fun PayStateChip(state: RoomPayState, modifier: Modifier = Modifier, short: Boolean = false) {
    when (state) {
        RoomPayState.PAID -> StatusChip("LUNAS", Green100, Green700, modifier)
        RoomPayState.PENDING -> StatusChip(if (short) "VERIFIKASI" else "MENUNGGU VERIFIKASI", Gold100, Warning, modifier)
        RoomPayState.REJECTED -> StatusChip(if (short) "DITOLAK" else "BUKTI DITOLAK", DangerBg, Danger, modifier)
        RoomPayState.UNPAID -> StatusChip(if (short) "BELUM" else "BELUM BAYAR", DangerBg, Danger, modifier)
        RoomPayState.EMPTY -> StatusChip("KOSONG", Green50, Ink600, modifier)
        RoomPayState.MAINTENANCE -> StatusChip("PERBAIKAN", Gold100, Warning, modifier)
    }
}

@Composable
fun PaymentStateChip(payment: PaymentEntity, modifier: Modifier = Modifier, short: Boolean = false) =
    PayStateChip(payment.payState(), modifier, short)

/** Kotak berwarna untuk pesan penting (kuning = menunggu, merah = ditolak, hijau = info). */
@Composable
fun NoticeBox(title: String, message: String, tone: NoticeTone = NoticeTone.WARNING, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        NoticeTone.WARNING -> Gold100 to Warning
        NoticeTone.DANGER -> DangerBg to Danger
        NoticeTone.INFO -> Green50 to Green700
    }
    Surface(shape = RoundedCornerShape(14.dp), color = bg, border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
    }
}

enum class NoticeTone { WARNING, DANGER, INFO }

/**
 * Foto bukti transfer dari server. Bukti transfer hanya bisa dibuka dengan login,
 * jadi permintaan gambar menyertakan header Authorization.
 */
@Composable
fun ProofImage(
    url: String,
    authHeader: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val request = remember(url, authHeader) {
        ImageRequest.Builder(context)
            .data(url)
            .apply { if (authHeader != null) addHeader("Authorization", authHeader) }
            .crossfade(true)
            .build()
    }
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Green50)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = "Bukti transfer",
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green700, strokeWidth = 2.dp, modifier = Modifier.size(28.dp)) } },
            error = {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.BrokenImage, null, tint = Ink400)
                    Text("Bukti tidak dapat dimuat", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
            }
        )
        if (onClick != null) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ZoomIn, null, tint = White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Perbesar", style = MaterialTheme.typography.labelSmall, color = White)
                }
            }
        }
    }
}

/** Tampilan layar penuh untuk bukti transfer (cubit untuk memperbesar). */
@Composable
fun ProofViewerDialog(url: String, authHeader: String?, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ProofImage(
                url, authHeader,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) { offsetX += pan.x; offsetY += pan.y } else { offsetX = 0f; offsetY = 0f }
                        }
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY },
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onDismiss, modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Close, "Tutup", tint = White)
            }
        }
    }
}

/**
 * Pilih foto bukti transfer dari Galeri atau Kamera. Foto langsung diperkecil & disalin ke HP
 * (ImageStorage), lalu path-nya dikirim lewat [onPicked]. [pickedPath] = foto yang sedang dipilih.
 */
@Composable
fun ProofPicker(
    pickedPath: String?,
    onPicked: (String?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun import(uri: Uri) {
        importing = true
        error = null
        scope.launch {
            val path = ImageStorage.importImage(context, uri, "bukti")
            importing = false
            if (path == null) error = "Foto tidak dapat dibaca. Pilih foto lain." else onPicked(path)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) import(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> cameraUri?.takeIf { ok }?.let(::import) }

    Column(modifier.fillMaxWidth()) {
        if (pickedPath != null) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))) {
                coil.compose.AsyncImage(
                    model = File(pickedPath), contentDescription = "Bukti transfer dipilih",
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onPicked(null) }, enabled = enabled) {
                    Text("Ganti foto", color = Green700, style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerButton("Pilih dari Galeri", Icons.Outlined.PhotoLibrary, enabled && !importing, Modifier.weight(1f)) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                PickerButton("Ambil Foto", Icons.Outlined.PhotoCamera, enabled && !importing, Modifier.weight(1f)) {
                    runCatching {
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "bukti_${System.currentTimeMillis()}.jpg"))
                        cameraUri = uri
                        camera.launch(uri)
                    }.onFailure { error = "Kamera tidak dapat dibuka." }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (importing) "Menyiapkan foto…" else "Foto atau screenshot bukti transfer (JPG/PNG, maks. 5 MB).",
                style = MaterialTheme.typography.bodySmall, color = Ink600
            )
        }
        if (error != null) {
            Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PickerButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier,
        shape = RoundedCornerShape(14.dp), color = Green50, border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Green700)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Green700)
        }
    }
}
