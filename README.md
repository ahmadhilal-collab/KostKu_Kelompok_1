# KostKu - Aplikasi Manajemen New Afra Kost

Kotlin - Jetpack Compose + Material 3 - MVVM + Repository - Retrofit + OkHttp (REST API) -
Room (salinan data di HP) - SharedPreferences (sesi login) - Firebase Cloud Messaging - Coroutines/Flow - Coil.

## Cara menjalankan
1. Android Studio (Ladybug 2024.2.1+) -> File > Open -> folder `KostKu_K1`.
2. Tunggu Gradle Sync (butuh internet). JDK 17 (bawaan Android Studio).
3. Cek `app/build.gradle.kts`:
   - `API_BASE_URL` = alamat KostKu REST API, diakhiri `/api/v1/`.
   - `OWNER_WHATSAPP` = nomor cadangan untuk "Hubungi pengelola" (dipakai hanya jika server tidak bisa dihubungi).
4. Pilih HP/emulator (API 26+) yang terhubung internet, lalu Run.

## Akun
- Admin: daftar lewat aplikasi (Daftar Admin) memakai sandi rahasia `ADMIN_SECRET_CODE` di `config/.env` server,
  atau masuk dengan akun admin yang sudah dibuat.
- Penghuni: ditambahkan admin lewat menu Data Penghuni, lalu login dengan nama + NIK + nomor HP yang sama persis.
- Data demo (opsional, di server): `sudo mariadb < /var/www/kostku/database/demo_data.sql`
  -> penghuni contoh `Andi`, NIK `7371010101000102`, HP `081234560102` (Kamar 102).

## Cara data bekerja
- Server (PHP + MySQL) adalah sumber data utama. Room di HP hanya salinan, supaya layar cepat dan tetap tampil saat offline.
- Data diambil saat aplikasi tampil, diperbarui otomatis tiap 30 detik selama aplikasi terbuka, dan segera setelah setiap perubahan.
- Setiap tambah/ubah/hapus dikirim ke server dulu; layar berubah setelah server menjawab berhasil.
- Token login disimpan di SharedPreferences. Jika token kedaluwarsa (401), pengguna otomatis kembali ke halaman login.
- Logout menghapus semua salinan data di HP.

## Notifikasi push (Firebase Cloud Messaging)
- Wajib ada `app/google-services.json` dari Firebase Console (paket `com.kostku.app`), tanpa file ini build gagal.
- Penghuni: HP didaftarkan otomatis ke server setiap aplikasi dibuka (`POST /devices`) dan dilepas saat logout.
- Android 13+: izin notifikasi diminta setelah panduan interaktif selesai.
- Notifikasi juga tersimpan di layar Pengumuman & Notifikasi (bagian "Notifikasi terbaru").

## Fitur Admin
Beranda (statistik, akses fitur, kartu kamar per lantai) - Panduan interaktif - Denah kamar berwarna - Detail kamar
(galeri foto, unggah/hapus foto, tandai perbaikan) - Catat pembayaran tunai + Nota PDF - Pengingat via WhatsApp -
Data penghuni (cari, filter, tambah, detail, akhiri sewa) - Pengumuman (buat, jadwalkan, hapus) - Katalog kamar -
Daftar nota - Perbaikan (proses laporan penghuni) - Profil & Info + Logout.

## Fitur Penghuni
Beranda (tagihan bulan ini) - Panduan interaktif - Pengumuman & Notifikasi - Status & Riwayat Pembayaran (lihat nota) -
Lapor kerusakan fasilitas + foto + riwayat laporan - Profil & Info + Logout.

## Struktur
data/remote (Retrofit API, model JSON, mapper) - data/local (Room, SharedPreferences) -
data/repository (AuthRepository, KostRepository) - ui/theme - ui/components - ui/navigation -
ui/screens/{auth,admin,tenant,common} - util (format, PDF, foto, intent).

## Debug
Saat build debug, semua permintaan ke API tercatat di Logcat (filter: `okhttp`), dengan token disensor.

## Tes unit
`./gradlew test` (UtilTest).
