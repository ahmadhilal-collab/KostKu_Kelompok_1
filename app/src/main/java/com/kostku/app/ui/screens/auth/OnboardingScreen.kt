package com.kostku.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kostku.app.ui.components.BrandHeader
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.ScreenContainer
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    ScreenContainer {
        Spacer(Modifier.height(20.dp))
        BrandHeader(Modifier.entrance(0))
        Spacer(Modifier.height(36.dp))
        Text(
            text = "Kelola Kost Lebih Praktis",
            style = MaterialTheme.typography.headlineLarge,
            color = Ink900,
            modifier = Modifier.entrance(1)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pencatatan pembayaran tunai, pengingat jatuh tempo WhatsApp, dan laporan fasilitas dalam satu aplikasi.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ink600,
            modifier = Modifier.entrance(2)
        )
        Spacer(Modifier.height(32.dp))

        FeatureRow(
            icon = Icons.Outlined.Payments,
            title = "Catat pembayaran tunai",
            desc = "Konfirmasi uang masuk, nota lunas terbit otomatis.",
            modifier = Modifier.entrance(3)
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            icon = Icons.Outlined.NotificationsActive,
            title = "Pengingat lewat WhatsApp",
            desc = "Ingatkan penghuni yang mendekati jatuh tempo dengan satu ketukan.",
            modifier = Modifier.entrance(4)
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            icon = Icons.Outlined.Build,
            title = "Laporan fasilitas",
            desc = "Penghuni melapor kerusakan lengkap dengan foto.",
            modifier = Modifier.entrance(5)
        )

        Box(Modifier.weight(1f))
        PrimaryButton(text = "Mulai Sekarang", onClick = onStart, modifier = Modifier.entrance(6))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Green50, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Green700, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
    }
}
