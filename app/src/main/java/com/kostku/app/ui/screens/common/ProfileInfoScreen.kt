package com.kostku.app.ui.screens.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kostku.app.data.local.Session
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.InitialsAvatar
import com.kostku.app.ui.components.KostKuLogo
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White

/** Info Aplikasi & Profil Pencipta + info akun + tombol keluar. Dipakai Admin dan Penghuni. */
@Composable
fun ProfileInfoScreen(session: Session, roleLabel: String, onLogout: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Info Aplikasi & Profil Pencipta", style = MaterialTheme.typography.headlineMedium, color = Ink900, modifier = Modifier.entrance(0))
        Spacer(Modifier.height(18.dp))
        AppCard(Modifier.fillMaxWidth().entrance(1)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(session.name, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(session.name, style = MaterialTheme.typography.titleMedium, color = Ink900)
                    Text(
                        roleLabel + (session.label?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = Ink600
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.entrance(2), verticalAlignment = Alignment.CenterVertically) {
            KostKuLogo(34.dp)
            Spacer(Modifier.width(8.dp))
            Text("KostKu", style = MaterialTheme.typography.titleLarge, color = Ink900)
        }
        Spacer(Modifier.height(16.dp))
        InfoBlock("Deskripsi Aplikasi", "KostKu v1.0 - Aplikasi Pengelolaan New Afra Kost", Modifier.entrance(3))
        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 16.dp))
        InfoBlock(
            "Informasi Mitra",
            "New Afra Kost (Jl. Perintis Kemerdekaan 4)\nPemilik: Hj. Muhammad Sahrul Latif\nPengelola: Pak Abbas",
            Modifier.entrance(4)
        )
        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 16.dp))
        InfoBlock(
            "Tim Pengembang",
            "Dikembangkan oleh: Anak Baik Team (D4 Teknik Komputer dan Jaringan - PNUP)",
            Modifier.entrance(5)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth().height(54.dp).entrance(6),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = White)
        ) { Text("Keluar dari Akun (Logout)", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(16.dp))
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = White,
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu masuk kembali untuk mengakses KostKu.") },
            confirmButton = { TextButton(onClick = { confirm = false; onLogout() }) { Text("Keluar", color = Danger) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal", color = Ink600) } }
        )
    }
}

@Composable
private fun InfoBlock(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink600)
    }
}
