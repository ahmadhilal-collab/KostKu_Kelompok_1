# KostKu - Aplikasi Manajemen New Afra Kost (versi lengkap)

Kotlin - Jetpack Compose + Material 3 - MVVM + Repository - Room - DataStore - Coroutines/Flow - Coil.

## Cara menjalankan
1. Android Studio (Ladybug 2024.2.1+) -> File > Open -> folder `KostKu`.
2. Tunggu Gradle Sync (butuh internet). JDK 17 (bawaan Android Studio).
3. Pilih emulator/HP (API 26+) lalu Run.

## Akun contoh (data demo otomatis)
- Admin: `Pak Abbas` / `081234567890`, sandi `admin123`
- Penghuni: nama `Andi`, NIK `7371010101000102`, HP `081234560102` (Kamar 102, belum bayar)
- Penghuni lain: nama sesuai daftar, NIK `737101010100` + nomor kamar 4 digit (mis. Kamar 101 -> `7371010101000101`), HP `08123456` + nomor kamar 4 digit.
- Sandi Rahasia daftar admin baru: `NEWAFRA2026` (ubah di app/build.gradle.kts).
- Matikan data demo: `SEED_DEMO_DATA` = false di app/build.gradle.kts.

## Fitur Admin
Beranda (statistik, akses fitur, kartu kamar per lantai) - Denah kamar berwarna - Detail kamar (galeri foto geser,
unggah/hapus foto, tandai perbaikan) - Catat pembayaran tunai + Nota PDF (unduh/bagikan) - Pengingat via WhatsApp -
Data penghuni (cari, filter, tambah, detail, akhiri sewa) - Pengumuman (buat, jadwalkan, hapus) - Katalog kamar -
Daftar nota - Perbaikan (proses laporan penghuni) - Profil & Info + Logout.

## Fitur Penghuni
Beranda (tagihan bulan ini) - Pengumuman & Notifikasi (pengingat jatuh tempo otomatis) - Status & Riwayat Pembayaran
(filter tahun, lihat nota) - Lapor kerusakan fasilitas + foto + riwayat laporan - Profil & Info + Logout.

## Struktur
data/local (Room, DataStore, seeder) - data/repository (AuthRepository, KostRepository) - ui/theme - ui/components -
ui/navigation - ui/screens/{auth,admin,tenant,common} - util (format, PDF, foto, intent).

## Catatan
- Data tersimpan lokal (Room). Untuk sinkron antar HP perlu backend (Firebase/PHP+MySQL/Laravel).
- Tagihan bulanan dibuat otomatis (jatuh tempo tgl 10) setiap aplikasi dibuka.
- Tes unit: `./gradlew test` (UtilTest).
