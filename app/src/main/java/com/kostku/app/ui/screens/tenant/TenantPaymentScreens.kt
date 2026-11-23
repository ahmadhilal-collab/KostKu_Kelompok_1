package com.kostku.app.ui.screens.tenant

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.kostku.app.data.local.PaymentEntity
import com.kostku.app.data.local.Session
import com.kostku.app.data.remote.ApiException
import com.kostku.app.ui.components.AppCard
import com.kostku.app.ui.components.EmptyState
import com.kostku.app.ui.components.ErrorBanner
import com.kostku.app.ui.components.FloorTabs
import com.kostku.app.ui.components.InfoNote
import com.kostku.app.ui.components.KostKuTextField
import com.kostku.app.ui.components.LocalNotifier
import com.kostku.app.ui.components.NoticeBox
import com.kostku.app.ui.components.NoticeTone
import com.kostku.app.ui.components.PageHeader
import com.kostku.app.ui.components.PaymentStateChip
import com.kostku.app.ui.components.PrimaryButton
import com.kostku.app.ui.components.ProofImage
import com.kostku.app.ui.components.ProofPicker
import com.kostku.app.ui.components.ProofViewerDialog
import com.kostku.app.ui.components.SecondaryButton
import com.kostku.app.ui.components.SectionLabel
import com.kostku.app.ui.components.TopBarWithBack
import com.kostku.app.ui.theme.Green100
import com.kostku.app.ui.theme.Green50
import com.kostku.app.ui.theme.Green700
import com.kostku.app.ui.theme.Ink400
import com.kostku.app.ui.theme.Ink600
import com.kostku.app.ui.theme.Ink900
import com.kostku.app.ui.theme.Line
import com.kostku.app.ui.theme.White
import com.kostku.app.util.Fmt
import com.kostku.app.util.Intents
import com.kostku.app.util.Validators

private enum class PayMethod(val label: String) { TRANSFER("Transfer"), CASH("Tunai") }

/**
 * Layar bayar tagihan: tab Transfer (rekening + unggah bukti) dan Tunai (langkah bayar ke pengelola).
 * Hanya metode yang diaktifkan pengelola yang tampil.
 */
@Composable
fun PayScreen(paymentId: Long, startWithCash: Boolean, vm: TenantViewModel, session: Session, nav: NavHostController) {
    val payments by vm.payments.collectAsStateWithLifecycle()
    val settings by vm.paymentSettings.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val contact by vm.contact.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadContact() }

    val payment = payments.firstOrNull { it.id == paymentId }
    val methods = buildList {
        if (settings.transferEnabled && settings.hasAccount) add(PayMethod.TRANSFER)
        if (settings.cashEnabled) add(PayMethod.CASH)
    }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val method = methods.firstOrNull { it.name == chosen }
        ?: (if (startWithCash && PayMethod.CASH in methods) PayMethod.CASH else methods.firstOrNull())

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack(
            when (method) {
                PayMethod.TRANSFER -> "Pembayaran transfer"
                PayMethod.CASH -> "Pembayaran tunai"
                null -> "Pembayaran"
            },
            { nav.popBackStack() }
        )
        if (payment == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green700) }
        } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            PageHeader(
                when (method) {
                    PayMethod.TRANSFER -> "Bayar via transfer"
                    PayMethod.CASH -> "Bayar secara tunai"
                    null -> "Bayar tagihan"
                },
                "${session.name} • Kamar ${room?.number ?: "-"} • New Afra Kost"
            )
            Spacer(Modifier.height(16.dp))
            if (methods.size > 1 && !payment.isPaid) {
                FloorTabs(methods.map { it.label }, methods.indexOf(method).coerceAtLeast(0), { chosen = methods[it].name })
                Spacer(Modifier.height(16.dp))
            }
            BillCard(payment)
            Spacer(Modifier.height(18.dp))
            when {
                payment.isPaid -> {
                    NoticeBox("Tagihan sudah lunas", "Terima kasih. Nota pembayaran sudah tersedia.", NoticeTone.INFO)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Lihat nota pembayaran", { nav.navigate(TenantRoutes.receipt(payment.id)) })
                }
                method == null -> NoticeBox(
                    "Metode pembayaran belum tersedia",
                    "Pengelola belum mengaktifkan metode pembayaran. Silakan hubungi pengelola.",
                    NoticeTone.WARNING
                )
                method == PayMethod.TRANSFER -> TransferSection(payment, vm, nav)
                else -> CashSection(payment, room?.number, contact?.name ?: "Pengelola", contact?.phone, session, nav)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BillCard(payment: PaymentEntity) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.month(payment.periodYear, payment.periodMonth), style = MaterialTheme.typography.titleSmall, color = Ink900, modifier = Modifier.weight(1f))
            PaymentStateChip(payment)
        }
        Spacer(Modifier.height(8.dp))
        Text("Total tagihan all-in", style = MaterialTheme.typography.bodySmall, color = Ink600)
        Text(Fmt.rupiahSpaced(payment.amount), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink900)
        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = Ink600, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Jatuh tempo: ${Fmt.date(payment.dueDate)}", style = MaterialTheme.typography.bodyMedium, color = Ink900)
        }
    }
}

@Composable
private fun TransferSection(payment: PaymentEntity, vm: TenantViewModel, nav: NavHostController) {
    val settings by vm.paymentSettings.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val notify = LocalNotifier.current
    var proofPath by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var replacing by rememberSaveable { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(false) }
    val pending = payment.isAwaitingVerification

    // Status bukti yang sudah dikirim
    if (pending) {
        NoticeBox(
            "Bukti terkirim • menunggu verifikasi",
            "Dikirim ${payment.transferSubmittedAt?.let { Fmt.date(it) } ?: "-"}. Tagihan menjadi lunas dan nota muncul setelah pengelola memverifikasi.",
            NoticeTone.WARNING
        )
        Spacer(Modifier.height(12.dp))
        if (payment.hasProof) {
            ProofImage(vm.proofUrl(payment), vm.authHeader(), Modifier.fillMaxWidth().height(220.dp), onClick = { zoom = true })
            Spacer(Modifier.height(8.dp))
        }
        if (!replacing) {
            TextButton(onClick = { replacing = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Salah kirim? Ganti bukti transfer", color = Green700)
            }
        }
    } else if (payment.isTransferRejected) {
        NoticeBox(
            "Bukti transfer ditolak",
            "Alasan: ${payment.rejectReason ?: "-"}. Silakan kirim ulang bukti yang benar.",
            NoticeTone.DANGER
        )
        Spacer(Modifier.height(16.dp))
    }

    if (!pending || replacing) {
        SectionLabel("Rekening tujuan")
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = Green50, border = BorderStroke(1.dp, Green100), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Bank ${settings.bankName}", style = MaterialTheme.typography.bodyMedium, color = Ink600)
                Spacer(Modifier.height(4.dp))
                Text(settings.accountNumber, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = Green700)
                Text("a.n. ${settings.accountHolder}", style = MaterialTheme.typography.bodyMedium, color = Ink900)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(settings.accountNumber.filter(Char::isDigit)))
                        notify("Nomor rekening disalin")
                    },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = White)
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = Green700, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salin nomor rekening", color = Green700)
                }
                if (settings.transferNote.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(settings.transferNote, style = MaterialTheme.typography.bodySmall, color = Ink600)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Transfer tepat ${Fmt.rupiah(payment.amount)} agar mudah diverifikasi.",
                    style = MaterialTheme.typography.bodySmall, color = Ink600
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel("Bukti transfer")
        Spacer(Modifier.height(8.dp))
        ProofPicker(proofPath, { proofPath = it; failure = null }, enabled = !sending)
        Spacer(Modifier.height(14.dp))
        KostKuTextField(
            "Catatan pembayaran (opsional)", note, { note = it.take(255) },
            "Contoh: transfer dari rekening pribadi", enabled = !sending, imeAction = ImeAction.Done
        )
        Spacer(Modifier.height(14.dp))
        NoticeBox(
            "Menunggu verifikasi setelah dikirim",
            "Unggah bukti setelah transfer. Tagihan baru lunas setelah dana diverifikasi pengelola."
        )
        Spacer(Modifier.height(12.dp))
        ErrorBanner(failure)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            if (pending) "Kirim bukti pengganti" else "Kirim bukti pembayaran",
            {
                val path = proofPath
                if (path == null) { failure = "Pilih foto bukti transfer terlebih dahulu."; return@PrimaryButton }
                sending = true
                failure = null
                vm.submitTransfer(payment.id, path, note) { r ->
                    sending = false
                    r.onSuccess {
                        proofPath = null; note = ""; replacing = false
                        notify("Bukti terkirim. Menunggu verifikasi pengelola.")
                    }.onFailure { failure = it.message }
                }
            },
            loading = sending
        )
        if (replacing) {
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Batal mengganti", { replacing = false; proofPath = null })
        }
    }
    Spacer(Modifier.height(10.dp))
    TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
        Text("Kembali ke status pembayaran", color = Ink600)
    }

    if (zoom) ProofViewerDialog(vm.proofUrl(payment), vm.authHeader()) { zoom = false }
}

@Composable
private fun CashSection(
    payment: PaymentEntity,
    roomNumber: Int?,
    ownerName: String,
    ownerPhone: String?,
    session: Session,
    nav: NavHostController
) {
    val context = LocalContext.current
    val notify = LocalNotifier.current
    val period = Fmt.month(payment.periodYear, payment.periodMonth)

    SectionLabel("Cara pembayaran tunai")
    Spacer(Modifier.height(8.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Step(1, "Temui pengelola", "Serahkan ${Fmt.rupiah(payment.amount)} kepada $ownerName untuk tagihan $period.")
        Spacer(Modifier.height(12.dp))
        Step(2, "Minta konfirmasi penerimaan", "Pengelola memeriksa uang dan mencatat pembayaran di akun admin.")
        Spacer(Modifier.height(12.dp))
        Step(3, "Cek status & nota", "Status berubah menjadi lunas dan nota muncul setelah uang dikonfirmasi diterima.")
    }
    Spacer(Modifier.height(14.dp))
    Surface(shape = RoundedCornerShape(18.dp), color = Green50, border = BorderStroke(1.dp, Green100), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Penerima pembayaran", style = MaterialTheme.typography.bodySmall, color = Ink600)
            Text("$ownerName • Pengelola", style = MaterialTheme.typography.titleMedium, color = Green700)
            Text("New Afra Kost • Kamar ${roomNumber ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
    }
    Spacer(Modifier.height(14.dp))
    NoticeBox(
        "Hanya pengelola yang menandai lunas",
        "Memilih tunai atau menghubungi pengelola belum berarti pembayaran telah diterima."
    )
    Spacer(Modifier.height(18.dp))
    PrimaryButton("Hubungi pengelola", {
        val phone = ownerPhone
        val text = "Halo $ownerName, saya ${session.name} (Kamar ${roomNumber ?: "-"}). Saya ingin membayar tunai tagihan kost $period sebesar ${Fmt.rupiah(payment.amount)}."
        if (phone.isNullOrBlank() || !Intents.openWhatsApp(context, phone, text)) notify("WhatsApp tidak dapat dibuka")
    })
    Spacer(Modifier.height(10.dp))
    SecondaryButton("Kembali ke status pembayaran", { nav.popBackStack() })
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(28.dp).background(Green50, CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = Green700)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink900)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Ink600)
        }
    }
}

/** Penghuni mengubah nama dan nomor HP. NIK, kamar, dan tarif hanya bisa diubah pengelola. */
@Composable
fun ProfileEditScreen(vm: TenantViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val room by vm.room.collectAsStateWithLifecycle()
    val notify = LocalNotifier.current
    LaunchedEffect(Unit) { vm.loadProfile() }
    val p = profile

    Column(Modifier.fillMaxSize()) {
        TopBarWithBack("Ubah Data Diri", { nav.popBackStack() })
        if (p == null) {
            EmptyState(Icons.Outlined.Groups, "Memuat data…", "Mohon tunggu sebentar.")
        } else {
        var name by rememberSaveable(p.id) { mutableStateOf(p.name) }
        var phone by rememberSaveable(p.id) { mutableStateOf(p.phone) }
        var errors by remember { mutableStateOf(emptyMap<String, String>()) }
        var saving by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }
        val changed = name.trim() != p.name || phone != p.phone

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            PageHeader("Data diri", "Pastikan nama dan nomor HP sesuai, karena dipakai untuk login dan dihubungi pengelola.")
            Spacer(Modifier.height(20.dp))
            KostKuTextField("Nama lengkap", name, { name = it; errors = errors - "name" }, "Nama sesuai KTP", error = errors["name"], enabled = !saving)
            Spacer(Modifier.height(16.dp))
            KostKuTextField(
                "Nomor HP / WhatsApp", phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15); errors = errors - "phone" },
                "08xx xxxx xxxx", keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done, error = errors["phone"], enabled = !saving
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("Hanya bisa diubah pengelola")
            Spacer(Modifier.height(8.dp))
            AppCard(Modifier.fillMaxWidth()) {
                LockedRow("NIK", p.nik.ifBlank { "-" })
                LockedRow("Kamar", "Kamar ${room?.number ?: "-"} • Lantai ${room?.floor ?: "-"}")
                LockedRow("Masuk sejak", if (p.entryDate > 0) Fmt.date(p.entryDate) else "-")
                LockedRow("Tarif / bulan", Fmt.rupiah(p.monthlyRate))
            }
            Spacer(Modifier.height(16.dp))
            if (changed) {
                InfoNote("Setelah disimpan, gunakan nama dan nomor HP baru ini saat login berikutnya.")
                Spacer(Modifier.height(12.dp))
            }
            ErrorBanner(failure)
            Spacer(Modifier.height(16.dp))
        }
        Surface(color = White, shadowElevation = 10.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryButton(
                    "Simpan Perubahan",
                    {
                        val e = buildMap {
                            if (name.trim().length < 3) put("name", "Nama lengkap minimal 3 huruf")
                            if (!Validators.isValidPhone(phone)) put("phone", "Nomor HP tidak valid (contoh: 0812xxxxxxxx)")
                        }
                        errors = e
                        if (e.isNotEmpty()) return@PrimaryButton
                        saving = true
                        failure = null
                        vm.updateProfile(name, phone) { r ->
                            saving = false
                            r.onSuccess { notify("Data diri diperbarui"); nav.popBackStack() }
                                .onFailure {
                                    failure = it.message
                                    (it as? ApiException)?.fieldErrors?.let { fe -> errors = fe }
                                }
                        }
                    },
                    loading = saving,
                    enabled = changed
                )
            }
        }
        }
    }
}

@Composable
private fun LockedRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink600, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Ink900)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.Lock, null, tint = Ink400, modifier = Modifier.size(14.dp))
    }
}
