package com.kostku.app.ui.screens.tenant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.Session
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.ContextualTooltipCard
import com.kostku.app.ui.components.InteractiveTourDialog
import com.kostku.app.ui.components.KostKuLogo
import com.kostku.app.ui.components.LocalTourController
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.SpotlightTarget
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.components.tenantTourSteps
import com.kostku.app.ui.components.tourTarget
import com.kostku.app.ui.navigation.AppViewModel
import com.kostku.app.ui.theme.DarkGreen
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt

@Composable
fun TenantHomeScreen(
    vm: TenantViewModel,
    session: Session,
    nav: NavHostController,
    appViewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AppViewModel.Factory)
) {
    val payments by vm.payments.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val isTenantTourCompleted by appViewModel.isTenantTourCompleted.collectAsStateWithLifecycle()
    val tour = LocalTourController.current
    val scrollState = rememberScrollState()
    var showTour by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(true) }

    LaunchedEffect(isTenantTourCompleted) {
        if (isTenantTourCompleted == false) {
            showTour = true
        }
    }

    // "Ulangi Panduan Aplikasi" di halaman Profil & Info → panduan diputar ulang di Beranda.
    LaunchedEffect(tour?.replayRequested) {
        if (tour?.consumeReplayRequest() == true) showTour = true
    }

    val current = payments.firstOrNull {
        it.periodYear == vm.period.year && it.periodMonth == vm.period.monthValue
    }
    val paid = current?.status == PaymentStatus.PAID

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth().entrance(0),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandHeader(Modifier.weight(1f))
                IconButton(onClick = { showTour = true }) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = "Panduan Aplikasi", tint = Green700)
                }
                KostKuLogo(size = 36.dp)
            }
            Spacer(Modifier.height(18.dp))

            if (showTooltip && isTenantTourCompleted == true) {
                ContextualTooltipCard(
                    title = "Petunjuk Penghuni Kost",
                    message = "Periksa tagihan bulanan Anda di sini, kirim laporan perbaikan fasilitas jika ada kendala, atau ketuk ikon (?) untuk melihat panduan.",
                    onDismiss = { showTooltip = false },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Banner Header Hijau Gradasi
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(1)
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Selamat Datang,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = White.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.weight(1f))
                            if (room != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Kamar ${room?.number}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = session.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "New Afra Kost • Hunian Nyaman & Terpercaya",
                            style = MaterialTheme.typography.bodySmall,
                            color = White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Card Tagihan Utama
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2)
                    .tourTarget(SpotlightTarget.TAGIHAN_PENGHUNI)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tagihan ${Fmt.month(vm.period.year, vm.period.monthValue)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink900,
                        modifier = Modifier.weight(1f)
                    )
                    if (current != null) PaymentStatusChip(paid)
                }
                Spacer(Modifier.height(12.dp))
                Text("Total Tagihan All-In", style = MaterialTheme.typography.bodySmall, color = Ink600)
                Text(
                    Fmt.rupiahSpaced(current?.amount ?: room?.monthlyRate ?: 0L),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Green700
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Line)
                Spacer(Modifier.height(14.dp))
                if (current != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = Ink600, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Jatuh Tempo: ${Fmt.date(current.dueDate)}", style = MaterialTheme.typography.bodyMedium, color = Ink900)
                    }
                    if (paid) {
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton("Lihat Nota Pembayaran", { nav.navigate(TenantRoutes.receipt(current.id)) })
                    }
                } else {
                    Text("Belum ada tagihan untuk bulan ini.", style = MaterialTheme.typography.bodyMedium, color = Ink600)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Tombol Perbaikan Fasilitas
            PrimaryButton(
                text = "Ajukan Perbaikan Fasilitas",
                onClick = { nav.navigateTab(TenantRoutes.REPAIR, TenantRoutes.HOME) },
                modifier = Modifier
                    .entrance(3)
                    .tourTarget(SpotlightTarget.TOMBOL_PERBAIKAN)
            )
            Spacer(Modifier.height(16.dp))

            // Menu Cards
            MenuCard(
                icon = Icons.Outlined.NotificationsNone,
                title = "Pengumuman & Notifikasi",
                subtitle = "Tenggat pembayaran dan informasi kost terbaru.",
                modifier = Modifier
                    .entrance(4)
                    .tourTarget(SpotlightTarget.PENGUMUMAN)
            ) { nav.navigate(TenantRoutes.NOTICE) }
            Spacer(Modifier.height(12.dp))
            MenuCard(
                icon = Icons.Outlined.Payments,
                title = "Status & Riwayat Pembayaran",
                subtitle = "Lihat status tagihan dan histori pembayaran.",
                modifier = Modifier
                    .entrance(5)
                    .tourTarget(SpotlightTarget.NOTA_PEMBAYARAN)
            ) { nav.navigate(TenantRoutes.PAYMENTS) }
            Spacer(Modifier.height(16.dp))
        }

        if (showTour) {
            InteractiveTourDialog(
                steps = tenantTourSteps,
                onFinish = {
                    showTour = false
                    appViewModel.completeTenantTour()
                },
                onBringIntoView = { target ->
                    if (target == SpotlightTarget.FINAL_OVERVIEW) {
                        // Langkah penutup: kembalikan halaman ke posisi paling atas.
                        scrollState.animateScrollTo(0)
                    } else {
                        // Gulung agar target utuh dan tidak tertutup kartu penjelasan.
                        tour?.scrollIntoView(target, scrollState)
                    }
                }
            )
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Green50, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Green700, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
        }
    }
}
