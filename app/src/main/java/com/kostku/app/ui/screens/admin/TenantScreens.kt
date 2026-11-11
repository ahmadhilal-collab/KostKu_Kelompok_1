package com.kostku.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentStatus
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.DateField
import com.kostku.app.ui.components.DropdownField
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.ErrorBanner
import com.kostku.app.ui.components.InfoNote
import com.kostku.app.ui.components.InitialsAvatar
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PaymentStatusChip
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.components.entrance
import com.kostku.app.ui.theme.Danger
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents
import com.kostku.app.util.Validators
import java.time.LocalDate

private fun maskPhone(phone: String) = phone.take(2) + "•• •••• ••••"

@Composable
fun TenantListScreen(vm: AdminViewModel, nav: NavHostController) {
    val tenants by vm.tenants.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var floor by rememberSaveable { mutableIntStateOf(0) } // 0 = semua
    var menuOpen by remember { mutableStateOf(false) }

    val filtered = tenants.filter { item ->
        val q = query.trim().lowercase()
        (floor == 0 || item.room?.floor == floor) &&
            (q.isEmpty() || item.tenant.name.lowercase().contains(q) ||
                (item.room?.number?.toString() ?: "").contains(q))
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Data Penghuni", { nav.popBackStack() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    PageHeader("Penghuni New Afra Kost", "Kelola identitas dan kamar penghuni.", Modifier.entrance(0))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Cari nama atau nomor kamar", color = Ink400) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Ink400) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green700, unfocusedBorderColor = Line,
                            focusedContainerColor = White, unfocusedContainerColor = White
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${filtered.size} penghuni aktif", style = MaterialTheme.typography.titleSmall,
                            color = Ink900, modifier = Modifier.weight(1f)
                        )
                        Box {
                            TextButton(onClick = { menuOpen = true }) {
                                Text(
                                    when (floor) { 1 -> "Lantai 1"; 2 -> "Lantai 2"; else -> "Semua lantai" },
                                    color = Green700, style = MaterialTheme.typography.labelMedium
                                )
                                Icon(Icons.Outlined.ExpandMore, null, tint = Green700, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(White)) {
                                listOf("Semua lantai", "Lantai 1", "Lantai 2").forEachIndexed { i, label ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { floor = i; menuOpen = false })
                                }
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(Icons.Outlined.Groups, "Belum ada penghuni", "Tambahkan penghuni baru atau ubah kata kunci pencarian.")
                }
            }
            items(filtered, key = { it.tenant.id }) { item ->
                AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(AdminRoutes.tenant(item.tenant.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(item.tenant.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.tenant.name, style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(
                                "Kamar ${item.room?.number ?: "-"} • ${maskPhone(item.tenant.phone)}",
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Ink400)
                    }
                }
            }
            item {
                Text(
                    "Pilih penghuni untuk melihat detail data.",
                    style = MaterialTheme.typography.bodySmall, color = Ink400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton("+ Tambah Penghuni", { nav.navigate(AdminRoutes.tenantAdd()) })
            }
        }
    }
}

@Composable
fun TenantAddScreen(vm: AdminViewModel, nav: NavHostController, presetRoomId: Long) {
    val rooms by vm.availableRooms.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    var name by rememberSaveable { mutableStateOf("") }
    var nik by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("800000") }
    var roomId by rememberSaveable { mutableLongStateOf(presetRoomId) }
    var entry by remember { mutableStateOf(LocalDate.now()) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var saving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val selectedRoom = rooms.firstOrNull { it.id == roomId }

    fun submit() {
        val e = buildMap {
            if (name.trim().length < 3) put("name", "Nama lengkap minimal 3 huruf")
            if (!Validators.isValidNik(nik)) put("nik", "NIK harus 16 digit angka")
            if (!Validators.isValidPhone(phone)) put("phone", "Nomor HP tidak valid (contoh: 0812xxxxxxxx)")
            if (selectedRoom == null) put("room", "Pilih kamar yang tersedia")
            if ((rate.toLongOrNull() ?: 0L) < 100_000L) put("rate", "Tarif minimal Rp100.000")
        }
        errors = e
        if (e.isNotEmpty() || selectedRoom == null) return
        saving = true
        failure = null
        vm.addTenant(name, nik, phone, selectedRoom.id, Fmt.startOfDayMillis(entry), rate.toLong()) { result ->
            saving = false
            result.onSuccess {
                notify("Penghuni $name ditambahkan ke Kamar ${selectedRoom.number}")
                nav.popBackStack()
            }.onFailure { failure = it.message }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Tambah Penghuni", { nav.popBackStack() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            PageHeader("Data penghuni baru", "Lengkapi data sesuai identitas penghuni.")
            Spacer(Modifier.height(20.dp))
            KostKuTextField(
                "Nama lengkap", name, { name = it; errors = errors - "name" }, "Masukkan nama lengkap",
                error = errors["name"], enabled = !saving
            )
            Spacer(Modifier.height(16.dp))
            KostKuTextField(
                "NIK", nik, { nik = it.filter(Char::isDigit).take(16); errors = errors - "nik" },
                "Masukkan 16 digit NIK", keyboardType = KeyboardType.Number,
                error = errors["nik"], enabled = !saving
            )
            Spacer(Modifier.height(16.dp))
            KostKuTextField(
                "Nomor HP", phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15); errors = errors - "phone" },
                "08xx xxxx xxxx", keyboardType = KeyboardType.Phone,
                error = errors["phone"], enabled = !saving
            )
            Spacer(Modifier.height(16.dp))
            DropdownField(
                label = "Kamar",
                value = selectedRoom?.let { "Kamar ${it.number} • Lantai ${it.floor}" } ?: "",
                placeholder = if (rooms.isEmpty()) "Tidak ada kamar kosong" else "Pilih kamar yang tersedia",
                options = rooms.map { "Kamar ${it.number} • Lantai ${it.floor}" },
                onSelect = { roomId = rooms[it].id; errors = errors - "room" },
                error = errors["room"]
            )
            Spacer(Modifier.height(16.dp))
            DateField("Tanggal masuk", entry, { entry = it })
            Spacer(Modifier.height(16.dp))
            KostKuTextField(
                "Tarif per bulan (All-In)", rate, { rate = it.filter(Char::isDigit).take(9); errors = errors - "rate" },
                "800000", keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
                error = errors["rate"], enabled = !saving
            )
            Text(
                "${Fmt.rupiah(rate.toLongOrNull() ?: 0L)} • Termasuk sewa kamar, Wi-Fi, listrik, dan air.",
                style = MaterialTheme.typography.bodySmall, color = Ink600, modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            InfoNote("NIK dan nomor HP digunakan untuk verifikasi akses penghuni. Pastikan data sudah benar.")
            Spacer(Modifier.height(16.dp))
            ErrorBanner(failure)
            Spacer(Modifier.height(16.dp))
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton("Simpan Data Penghuni", { submit() }, loading = saving)
            }
        }
    }
}

@Composable
fun TenantDetailScreen(tenantId: Long, vm: AdminViewModel, nav: NavHostController) {
    val tenant by remember(tenantId) { vm.observeTenant(tenantId) }.collectAsStateWithLifecycle(initialValue = null)
    val payments by remember(tenantId) { vm.observePayments(tenantId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rooms by vm.roomCards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notify = LocalNotifier.current
    var confirmEnd by remember { mutableStateOf(false) }

    val t = tenant
    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Detail Penghuni", { nav.popBackStack() })
        if (t == null) {
            EmptyState(Icons.Outlined.Groups, "Memuat data…", "Mohon tunggu sebentar.")
            return@Column
        }
        val room = rooms.firstOrNull { it.room.id == t.roomId }?.room
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(t.name, size = 60.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(t.name, style = MaterialTheme.typography.headlineSmall, color = Ink900)
                    Text(
                        "Kamar ${room?.number ?: "-"} • Lantai ${room?.floor ?: "-"}" + if (!t.active) " • Tidak aktif" else "",
                        style = MaterialTheme.typography.bodyMedium, color = Ink600
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            AppCard(Modifier.fillMaxWidth()) {
                InfoRow("Nomor HP", t.phone)
                InfoRow("NIK", t.nik)
                InfoRow("Masuk sejak", Fmt.date(t.entryDate))
                InfoRow("Tarif / bulan", Fmt.rupiah(t.monthlyRate))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { if (!Intents.openWhatsApp(context, t.phone, "Halo ${t.name}, ")) notify("WhatsApp tidak ditemukan.") },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Green700, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("WhatsApp", color = Green700)
                }
                OutlinedButton(
                    onClick = { if (!Intents.dial(context, t.phone)) notify("Aplikasi telepon tidak ditemukan.") },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Phone, null, tint = Green700, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Telepon", color = Green700)
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Riwayat pembayaran")
            Spacer(Modifier.height(10.dp))
            if (payments.isEmpty()) {
                Text("Belum ada tagihan.", style = MaterialTheme.typography.bodySmall, color = Ink600)
            }
            payments.forEach { p ->
                val paid = p.status == PaymentStatus.PAID
                AppCard(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    onClick = if (paid) ({ nav.navigate(AdminRoutes.receipt(p.id)) }) else null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Fmt.month(p.periodYear, p.periodMonth), style = MaterialTheme.typography.titleSmall, color = Ink900)
                            Text(
                                if (paid) "Tunai • ${Fmt.date(p.paidAt ?: p.dueDate)}" else "Jatuh tempo ${Fmt.date(p.dueDate)}",
                                style = MaterialTheme.typography.bodySmall, color = Ink600
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PaymentStatusChip(paid)
                            Spacer(Modifier.height(4.dp))
                            Text(Fmt.rupiah(p.amount), style = MaterialTheme.typography.labelMedium, color = Ink900)
                        }
                    }
                }
            }
            if (t.active) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { confirmEnd = true }, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Akhiri Sewa Penghuni", color = Danger) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmEnd && t != null) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = White,
            title = { Text("Akhiri sewa ${t.name}?") },
            text = {
                Text(
                    "Kamar akan kembali berstatus kosong dan tagihan yang belum dibayar dihapus. Riwayat pembayaran lunas tetap tersimpan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    vm.endTenancy(t.id) { r ->
                        r.onSuccess { notify("Sewa ${t.name} diakhiri"); nav.popBackStack() }
                            .onFailure { notify(it.message ?: "Gagal mengakhiri sewa") }
                    }
                }) { Text("Akhiri Sewa", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Batal", color = Ink600) } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink600)
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink900)
    }
}
