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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.kostku.app.ui.components.LocalTourController
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.White

/** Info Aplikasi & Profil Pencipta + info akun + tombol keluar. Dipakai Admin dan Penghuni. */
@Composable
fun ProfileInfoScreen(
    session: Session,
    roleLabel: String,
    onLogout: () -> Unit,
    /** Dipanggil saat "Ulangi Panduan Aplikasi" diketuk: shell berpindah ke tab Beranda. */
    onReplayTour: () -> Unit = {},
    /** Jika diisi (penghuni), tampil kartu "Ubah data diri". */
    onEditProfile: (() -> Unit)? = null
) {
    var confirm by remember { mutableStateOf(false) }
    val tour = LocalTourController.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Info Aplikasi & Profil", style = MaterialTheme.typography.headlineMedium, color = Ink900, modifier = Modifier.entrance(0))
        Spacer(Modifier.height(18.dp))

        // Profile Card
        AppCard(Modifier.fillMaxWidth().entrance(1)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(session.name, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(session.name, style = MaterialTheme.typography.titleLarge, color = Ink900)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        roleLabel + (session.label?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium, color = Ink600
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (onEditProfile != null) {
            AppCard(modifier = Modifier.fillMaxWidth().entrance(1), onClick = onEditProfile) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Green700, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ubah Data Diri", style = MaterialTheme.typography.titleSmall, color = Ink900)
                        Text("Perbarui nama dan nomor HP / WhatsApp", style = MaterialTheme.typography.bodySmall, color = Ink600)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Ink400)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Card Replay Tutorial / Tour
        AppCard(
            modifier = Modifier.fillMaxWidth().entrance(2),
            onClick = {
                // Panduan menyorot elemen di Beranda, jadi diputar di sana — bukan di halaman ini.
                tour?.requestReplay()
                onReplayTour()
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Green700, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ulangi Panduan Aplikasi", style = MaterialTheme.typography.titleSmall, color = Ink900)
                    Text("Lihat kembali fitur utama KostKu sesuai peran Anda", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Ink400)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Info Application Card
        AppCard(Modifier.fillMaxWidth().entrance(3)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KostKuLogo(32.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("KostKu Mobile v1.0", style = MaterialTheme.typography.titleMedium, color = Ink900)
                    Text("Aplikasi Manajemen Kost & Pencatatan Pembayaran", style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Info Partner Card
        AppCard(Modifier.fillMaxWidth().entrance(4)) {
            Text("Informasi Mitra", style = MaterialTheme.typography.titleSmall, color = Ink900)
            Spacer(Modifier.height(6.dp))
            Text(
                "New Afra Kost (Jl. Perintis Kemerdekaan 4)\nPemilik: Hj. Muhammad Sahrul Latif\nPengelola: Pak Abbas",
                style = MaterialTheme.typography.bodyMedium, color = Ink600
            )
        }
        Spacer(Modifier.height(12.dp))

        // Info Developer Card
        AppCard(Modifier.fillMaxWidth().entrance(5)) {
            Text("Tim Pengembang", style = MaterialTheme.typography.titleSmall, color = Ink900)
            Spacer(Modifier.height(6.dp))
            Text(
                "Anak Baik Team\nD4 Teknik Komputer dan Jaringan — Politeknik Negeri Ujung Pandang (PNUP)",
                style = MaterialTheme.typography.bodyMedium, color = Ink600
            )
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth().height(54.dp).entrance(6),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = White)
        ) { Text("Keluar dari Akun (Logout)", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(20.dp))
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
