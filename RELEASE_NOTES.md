# Catatan Rilis Resmi SIPAGAR KITA (v1.0.9)

**Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko**  
*Versi Rilis: **v1.0.9** (Build 10) | Tanggal Rilis: **23 November 2026***

---

## Ringkasan Rilis
Rilis **SIPAGAR KITA v1.0.9** berfokus pada keandalan penuh alur autentikasi petugas (Login PIN Cloud, Biometrik Native, Logout Resmi, & Reset PIN), penutupan celah keamanan Mode Warga, perbaikan presisi tampilan tombol alarm darurat, serta optimalisasi performa besar-besaran pada **Ruang Kerja Mode Administrator** agar ringan, responsif, dan hemat baterai di perangkat fisik.

---

## ✨ Penambahan Fitur & Fungsi Baru
- **Integrasi Info Lanskap & Wallpaper HD di Menu Info Aplikasi**: Akses dialog *Info Lanskap & Wallpaper HD Kabupaten Mukomuko* dipindahkan secara rapi ke menu **Info Aplikasi** agar layar utama tetap bersih namun fitur tetap tersedia penuh.
- **Mekanisme Perlindungan Sesi Reset PIN**: Sistem kini membedakan secara tegas antara pencabutan akses oleh Administrator dengan proses **Reset PIN** oleh petugas, sehingga perangkat asli tetap diizinkan login kembali tanpa kendala.
- **Otomatisasi Catatan Rilis GitHub (Bahasa Indonesia)**: Seluruh catatan penambahan fitur, fungsi, peningkatan, dan perbaikan kini terdokumentasi terstruktur dalam bahasa Indonesia di `CHANGELOG.md`, `RELEASE_NOTES.md`, halaman **Info Aplikasi**, serta otomatis diterbitkan pada setiap **GitHub Release**.

---

## ⚡ Peningkatan & Optimalisasi
- **Ruang Kerja Mode Administrator Lebih Ringan & Hemat Baterai**:
  - Memutus siklus penulisan berulang antara database lokal (Room) dan Cloud Firestore pada sinkronisasi daftar personil sehingga layar tidak lagi *lag* atau *freeze* di ponsel fisik.
  - Mengganti pelacakan aktivitas sentuhan/gulir agar tidak memicu penggambaran ulang (*recomposition*) seluruh halaman saat disentuh.
  - Memindahkan pemrosesan data sesi keamanan, aktivitas login, dan rekonsiliasi detak jantung perangkat (*Device Heartbeat*) ke *background thread*.
  - Melonggarkan jadwal sinkronisasi cadangan menjadi 60 detik (pembaruan utama tetap berjalan seketika secara *real-time*) dan membatasi waktu tunggu ping/heartbeat maksimal 5 detik.
  - Menambahkan cache statistik laporan serta pembatasan muat awal kartu sesi keamanan agar perpindahan tab Administrator mulus.
- **Presisi Teks Tombol Alarm Sirene**: Ukuran huruf dan jarak padding pada tombol **"SEGERA CEK & MATIKAN ALARM"** dan **"MATIKAN SIRENE"** disesuaikan agar presisi, jelas dibaca, dan tidak terpotong.
- **Jaminan Bebas Spinner Menggantung**: Seluruh proses verifikasi Login PIN Cloud, Login Biometrik, Reset PIN, dan pemeriksaan sesi Ruang Kerja dilengkapi *timeout* terukur dan penutupan status *loading* yang aman.

---

## 🐛 Perbaikan Bug & Keamanan
- **Perbaikan Notifikasi "Sesi Dicabut" Palsu saat Reset PIN**: Reset PIN tidak lagi menulis status `REVOKED` / `is_active = false` ke server. Setelah Reset PIN berhasil, petugas menerima notifikasi jelas bahwa PIN kembali ke **6 digit terakhir NIP** dan dapat langsung login kembali ke Cloud.
- **Perbaikan Alur Login, Logout, & Biometrik**:
  - Relogin petugas setelah logout resmi kini berfungsi normal menggunakan PIN pribadi Cloud (tanpa kembali ke PIN default).
  - Tombol **Login Biometrik (`BiometricPrompt`)** pada halaman awal (*Landing Page*) dan dialog login berfungsi kembali sebagaimana mestinya.
  - **Isolasi Keamanan Mode Warga**: Memilih masuk sebagai warga kini selalu membersihkan sesi petugas yang tersisa sehingga Ruang Kerja tetap terkunci dan wajib autentikasi PIN/Biometrik.
- **Perbaikan Timeout Audio Sirene (`ToneGenerator`)**: Mengatasi pesan error *Stop timed out* saat menghentikan nada alarm sirene darurat.

---

> 📄 **Riwayat lengkap seluruh versi (v1.0.0 – v1.0.9)** tersedia di berkas [`CHANGELOG.md`](./CHANGELOG.md) serta pada menu **Info Aplikasi → Riwayat Catatan Rilis Versi** di dalam aplikasi.

*Pusdalops-PB BPBD Kabupaten Mukomuko — Siaga, Tanggap, Tangguh Menghadapi Bencana.*
