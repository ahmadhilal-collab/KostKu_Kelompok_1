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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.data.local.Session
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.components.navigateTab
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.util.Fmt

@Composable
fun TenantHomeScreen(vm: TenantViewModel, session: Session, nav: NavHostController) {
    val payments by vm.payments.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val current = payments.firstOrNull {
        it.periodYear == vm.period.year && it.periodMonth == vm.period.monthValue
    }
    val paid = current?.status == PaymentStatus.PAID

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
        BrandHeader(Modifier.entrance(0))
        Spacer(Modifier.height(20.dp))
        Text(
            "Selamat Datang, ${session.name}" + (room?.let { " (Kamar ${it.number})" } ?: ""),
            style = MaterialTheme.typography.headlineMedium, color = Ink900, modifier = Modifier.entrance(1)
        )
        Text("New Afra Kost", style = MaterialTheme.typography.bodyMedium, color = Ink600, modifier = Modifier.entrance(1))
        Spacer(Modifier.height(20.dp))

        AppCard(Modifier.fillMaxWidth().entrance(2)) {
            Text(
                "Tagihan ${Fmt.month(vm.period.year, vm.period.monthValue)}",
                style = MaterialTheme.typography.titleSmall, color = Ink900
            )
            Spacer(Modifier.height(10.dp))
            Text("Total Tagihan All-In", style = MaterialTheme.typography.bodySmall, color = Ink600)
            Text(
                Fmt.rupiahSpaced(current?.amount ?: room?.monthlyRate ?: 0L),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(12.dp))
            if (current != null) {
                Text("Jatuh Tempo: ${Fmt.date(current.dueDate)}", style = MaterialTheme.typography.bodyMedium, color = Ink900)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.bodyMedium, color = Ink600)
                    PaymentStatusChip(paid)
                }
                if (paid) {
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Lihat Nota", { nav.navigate(TenantRoutes.receipt(current.id)) })
                }
            } else {
                Text("Belum ada tagihan untuk bulan ini.", style = MaterialTheme.typography.bodyMedium, color = Ink600)
            }
        }
        Spacer(Modifier.height(14.dp))
        SecondaryButton(
            "Ajukan Perbaikan Fasilitas",
            { nav.navigateTab(TenantRoutes.REPAIR, TenantRoutes.HOME) },
            Modifier.entrance(3)
        )
        Spacer(Modifier.height(14.dp))
        MenuCard(
            Icons.Outlined.NotificationsNone, "Pengumuman & Notifikasi",
            "Tenggat pembayaran dan informasi kost terbaru.", Modifier.entrance(4)
        ) { nav.navigate(TenantRoutes.NOTICE) }
        Spacer(Modifier.height(12.dp))
        MenuCard(
            Icons.Outlined.Payments, "Status & Riwayat Pembayaran",
            "Lihat status tagihan dan history pembayaran.", Modifier.entrance(5)
        ) { nav.navigate(TenantRoutes.PAYMENTS) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Green50, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Green700)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
        }
    }
}
