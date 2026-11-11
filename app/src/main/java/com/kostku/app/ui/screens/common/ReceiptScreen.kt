package com.kostku.app.ui.screens.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kostku.app.data.repository.ReceiptData
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.StatusChip
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents
import com.kostku.app.util.ReceiptPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptScreen(paymentId: Long, load: suspend (Long) -> ReceiptData?, onBack: () -> Unit) {
    var data by remember { mutableStateOf<ReceiptData?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notify = LocalNotifier.current
    val scope = rememberCoroutineScope()
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(paymentId) {
        data = load(paymentId)
        loaded = true
    }
    LaunchedEffect(data) {
        if (data != null) checkScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Nota Pembayaran", onBack)
        val d = data
        when {
            !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
            d == null -> EmptyState(Icons.Outlined.ReceiptLong, "Nota belum tersedia", "Nota terbit setelah pembayaran dikonfirmasi lunas oleh pengelola.")
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.size(64.dp).graphicsLayer { scaleX = checkScale.value; scaleY = checkScale.value }
                        .background(Green100, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Check, null, tint = Green700, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.height(12.dp))
                Text("Pembayaran Lunas", style = MaterialTheme.typography.headlineMedium, color = Ink900, modifier = Modifier.entrance(0))
                Text(
                    "Terima kasih, pembayaran telah diterima.",
                    style = MaterialTheme.typography.bodyMedium, color = Ink600, modifier = Modifier.entrance(1)
                )
                Spacer(Modifier.height(20.dp))
                AppCard(Modifier.fillMaxWidth().entrance(2)) {
                    BrandHeader()
                    Spacer(Modifier.height(12.dp))
                    Text("New Afra Kost", style = MaterialTheme.typography.titleSmall, color = Ink900)
                    Text("Jl. Perintis Kemerdekaan 4", style = MaterialTheme.typography.bodySmall, color = Ink600)
                    Text("Nota: ${d.receiptNo}", style = MaterialTheme.typography.bodySmall, color = Ink400)
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(10.dp))
                    ReceiptLine("Nama penghuni", d.tenantName)
                    ReceiptLine("Kamar", "${d.roomNumber} • Lantai ${d.floor}")
                    ReceiptLine("Periode sewa", d.periodLabel)
                    ReceiptLine("Tanggal bayar", d.paidDate)
                    ReceiptLine("Metode", d.method)
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total dibayar (All-In)", style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.weight(1f))
                        StatusChip("LUNAS", Green50, Green700)
                    }
                    Text(
                        Fmt.rupiah(d.amount),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900
                    )
                    Text(
                        Fmt.terbilang(d.amount),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = Ink600
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(10.dp))
                    ReceiptLine("Diterima oleh", "${d.receivedBy} • Pengelola")
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Unduh Nota Digital", {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { ReceiptPdf.saveToDownloads(context, ReceiptPdf.create(context, d)) }
                                .getOrDefault(false)
                        }
                        notify(if (ok) "Nota tersimpan di folder Unduhan" else "Gagal menyimpan nota")
                    }
                })
                TextButton(
                    onClick = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) { runCatching { ReceiptPdf.create(context, d) }.getOrNull() }
                            if (file == null || !Intents.sharePdf(context, file)) notify("Nota tidak dapat dibagikan")
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Outlined.Share, null, tint = Green700, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bagikan Nota", color = Green700)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink600)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Ink900, textAlign = TextAlign.End)
    }
}
