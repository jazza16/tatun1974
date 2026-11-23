# Catatan Perubahan & Riwayat Rilis (CHANGELOG) — SIPAGAR KITA

**Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko**

Dokumen ini mencatat seluruh **penambahan fitur**, **fungsi baru**, **peningkatan performa & tampilan**, serta **perbaikan bug & keamanan** pada setiap versi rilis aplikasi **SIPAGAR KITA** dalam bahasa Indonesia, dan disinkronkan langsung dengan catatan rilis di dalam aplikasi maupun GitHub Releases.

---

## [1.0.9] - 23 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Integrasi Info Lanskap & Wallpaper HD ke Info Aplikasi**: Memindahkan akses dialog *"Info Lanskap & Wallpaper HD Kabupaten Mukomuko"* ke dalam menu **Info Aplikasi** agar tampilan utama lebih ringkas namun tetap mudah diakses kapan saja.
- **Perlindungan Sesi Reset PIN Cerdas**: Menambahkan mekanisme peredam pencabutan sesi palsu (*PIN Reset suppression*) saat petugas melakukan Reset PIN ke 6 digit terakhir NIP sehingga perangkat asli tetap diizinkan login kembali tanpa kendala.

### ⚡ Peningkatan & Optimalisasi
- **Optimalisasi Performa & Hemat Baterai Mode Administrator (Ruang Kerja)**:
  - Memutus siklus penulisan berulang antara database lokal dan Cloud Firestore pada daftar personil sehingga layar Ruang Kerja bebas *lag*/*freeze* di perangkat fisik.
  - Mengganti pelacakan aktivitas sentuhan/gulir agar tidak memicu penggambaran ulang (*recomposition*) seluruh layar setiap sentuhan.
  - Memindahkan pemrosesan data sesi keamanan, aktivitas login, dan rekonsiliasi detak jantung perangkat (*Device Heartbeat*) ke *background thread*.
  - Melonggarkan jadwal sinkronisasi cadangan menjadi setiap 60 detik (karena pembaruan laporan sudah mengalir secara *real-time*), serta membatasi waktu tunggu ping/heartbeat maksimal 5 detik untuk menghemat baterai.
  - Membatasi pemuatan awal kartu sesi keamanan dan menggunakan cache perhitungan statistik laporan agar transisi tab Administrator berjalan mulus.
- **Presisi Tata Letak Tombol Alarm Sirene**: Menyelaraskan ukuran huruf dan ruang dalam tombol **"SEGERA CEK & MATIKAN ALARM"** dan **"MATIKAN SIRENE"** agar proporsional, rapi, dan tidak terpotong di berbagai ukuran layar.
- **Batas Waktu Terukur pada Proses Login & Reset PIN**: Menambahkan *timeout* terukur serta jaminan penutupan indikator *loading* pada proses login PIN Cloud, login Biometrik, Reset PIN, dan verifikasi sesi Ruang Kerja agar logo *spin* tidak pernah menggantung.

### 🐛 Perbaikan Bug & Keamanan
- **Perbaikan Notifikasi "Sesi Dicabut" Palsu saat Reset PIN**: Menghapus penulisan status `REVOKED` / `is_active = false` ketika petugas menjalankan Reset PIN, lalu mengarahkan petugas secara bersih ke halaman awal untuk langsung login kembali dengan 6 digit terakhir NIP.
- **Perbaikan Skema Login, Logout, & Biometrik**:
  - Memulihkan alur login petugas pasca-logout resmi menggunakan PIN pribadi yang tersimpan di Cloud Firestore.
  - Mengembalikan dukungan **Biometrik Native Android (`BiometricPrompt`)** pada halaman awal (*Landing Page*) dan dialog login petugas.
  - Menutup celah keamanan **Mode Warga** agar pemilihan masuk sebagai warga selalu mengisolasi dan menghapus sesi petugas yang tersisa sehingga tidak pernah langsung membuka Ruang Kerja tanpa input PIN.
- **Perbaikan Audio Sirene (`ToneGenerator`)**: Menangani potensi *timeout* saat menghentikan nada alarm agar pemutaran dan pematian sirene darurat berlangsung stabil tanpa error sistem.

---

## [1.0.8] - 21 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Autentikasi PIN Cloud Ketat (`Source.SERVER`)**: Seluruh verifikasi PIN petugas kini diverifikasi langsung ke server Cloud Firestore tanpa menyimpan PIN mentah ataupun hash PIN secara lokal di perangkat.
- **Pendaftaran Personil Manual & Sinkronisasi Lintas Perangkat**: Administrator dapat menambahkan petugas baru (Nama & NIP) melalui menu **Kelola Personil** dan petugas dapat langsung login menggunakan Nama atau NIP dari perangkat lain.
- **Kontrol Akses Berbasis Peran (RBAC Dinamis)**: Penerapan pemisahan hak akses dinamis berdasarkan peran di Cloud Firestore (**Manajer / Tier 1 Full Access**, **Pengawas / Koord. Administrasi**, dan **Operator Pusdalops**).

### ⚡ Peningkatan & Optimalisasi
- **Ubah PIN Pribadi Permanen di Cloud**: PIN pribadi yang sudah diubah oleh petugas dipertahankan secara permanen di server meskipun aplikasi diperbarui atau dipasang ulang (*reinstall*).

### 🐛 Perbaikan Bug & Keamanan
- **Penghapusan Permanen PIN Default Umum**: Memblokir secara penuh penggunaan PIN default umum (`117`, `BPBD117`, `TRC2026`, `0000`, `1234`) pada seluruh jalur autentikasi.

---

## [1.0.7] - 20 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Panel Inspeksi Keamanan Perangkat & Sesi (Play Integrity Classic)**: Administrator dapat memantau daftar perangkat aktif petugas, riwayat kronologis aktivitas login, status verifikasi admin, serta melakukan pencabutan akses perangkat secara *real-time*.
- **Detak Jantung Perangkat (*Device Heartbeat*) & Pemantauan Latensi**: Pemantauan konektivitas jaringan dan perangkat petugas yang aktif secara langsung di Ruang Kerja.
- **Banner & Alarm Sirene Darurat Real-Time**: Peringatan darurat otomatis berwarna merah disertai sirene saat laporan bencana baru masuk dari warga.

### ⚡ Peningkatan & Optimalisasi
- **Otorisasi Sandi Master Admin Sisi Server**: Menu sensitif (Kelola Personil, Database Explorer, Cabut Akses) dilindungi verifikasi sandi admin sisi server disertai Token Akses Sementara (TTL 5–15 menit) dan pencatatan Audit Log otomatis.

---

## [1.0.6] - 19 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Kop Laporan PDF Resmi Dua Logo**: Pemasangan logo resmi BPBD Kabupaten Mukomuko (kiri) dan lambang perisai *"Kapuang Sakti Ratau Batuah"* Kabupaten Mukomuko (kanan) pada ekspor PDF rekap cuaca dan laporan kejadian bencana.
- **Catatan Operasional Adaptif & Ringkasan AI**: Tampilan *split dual-pane* pada tablet/layar lipat dan *single-pane* pada ponsel, lengkap dengan fitur ringkasan catatan dan draf SOP rekomendasi.
- **Pemisahan Otoritas Aksi Laporan**: Aksi **Validasi Laporan** dikhususkan bagi Operator Pusdalops, sedangkan aksi **Selesai (Finalisasi)** dikhususkan bagi Manajer Pusdalops / Koord. Administrasi.

### ⚡ Peningkatan & Optimalisasi
- **Penyeragaman Tata Letak 8dp & Badge 24dp**: Menyeragamkan jarak antar kartu Ruang Kerja sebesar 8dp dan ketinggian badge status agar konsisten di ponsel, foldable, dan tablet.
- **Peta Interaktif Responsif**: Pembatasan kontainer adaptif pada peta sebaran laporan warga agar presisi di semua ukuran layar.
- **Desain Badge Bulat Kontak Darurat**: Pembaruan tombol panggil darurat menjadi badge melingkar modern yang serasi dengan direktori kontak penting.

---

## [1.0.5] - 18 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Penguncian Otomatis Identitas Petugas pada Formulir**: Kolom Nama Operator dan ID Stasiun otomatis terkunci (*Read-Only*) sesuai akun petugas yang sedang aktif login.

### ⚡ Peningkatan & Optimalisasi
- **Otomatisasi Waktu Komunikasi Log BPBD**: Waktu komunikasi pada formulir log Pusdalops otomatis terisi sesuai waktu perangkat saat pencatatan.

### 🐛 Perbaikan Bug & Keamanan
- **Aktivasi Tombol GPS Formulir Pelaporan**: Pengambilan koordinat Lintang & Bujur lokasi kejadian bencana kini berfungsi otomatis dan akurat.

---

## [1.0.4] - 17 November 2026

### ⚡ Peningkatan & Optimalisasi
- **Pembaruan Direktori Kontak Darurat**: Penyesuaian kontak bantuan kepolisian dari *"Jajaran Polsek"* menjadi **"Polres Mukomuko"** pada menu Pusat Bantuan.
- **Optimalisasi Sinkronisasi Laporan Lapangan**: Mengurangi jeda (*delay*) pengiriman laporan dari petugas lapangan menuju konsol Pusdalops.

---

## [1.0.3] - 17 November 2026

### ⚡ Peningkatan & Optimalisasi
- **Penyempurnaan UI/UX Formulir Pelaporan**: Perbaikan tata letak formulir laporan warga agar lebih nyaman digunakan pada layar ponsel.
- **Pembaruan Data Peta Geospasial**: Penambahan dan pemutakhiran data titik rawan bencana wilayah Kabupaten Mukomuko.

---

## [1.0.2] - 16 November 2026

### ⚡ Peningkatan & Optimalisasi
- **Peningkatan Stabilitas Aplikasi**: Mengoptimalkan manajemen memori dan *lifecycle* sehingga aplikasi lebih stabil dan tidak mudah tertutup paksa (*force close*).

### 🐛 Perbaikan Bug & Keamanan
- **Perbaikan Unggah Foto Laporan Warga**: Memperbaiki kendala saat melampirkan dan mengunggah foto kejadian bencana dari perangkat warga.

---

## [1.0.1] - 16 November 2026

### ⚡ Peningkatan & Optimalisasi
- **Penyesuaian Teks Modul Cuaca BMKG**: Perbaikan redaksi dan format tampilan informasi prakiraan cuaca serta peringatan dini BMKG.

### 🐛 Perbaikan Bug & Keamanan
- **Perbaikan Login Biometrik Awal**: Menyempurnakan kestabilan verifikasi sidik jari/wajah saat masuk ke aplikasi.

---

## [1.0.0] - 15 November 2026

### ✨ Penambahan Fitur & Fungsi Baru
- **Rilis Perdana SIPAGAR KITA v1.0.0**: Aplikasi resmi pemantauan cuaca, maritim, dan kesiapsiagaan bencana Kabupaten Mukomuko oleh Pusdalops-PB BPBD Kabupaten Mukomuko.
- **Prakiraan Cuaca & Peringatan Dini BMKG**: Pemantauan cuaca 15 kecamatan, kualitas udara (ISPU), indeks UV, gelombang laut, dan informasi gempa bumi InaTEWS *real-time*.
- **Peta Interaktif GIS & SOP Evakuasi**: Peta titik rawan bencana, panduan jalur evakuasi mandiri, serta akses cepat Call Center 117 BPBD.
- **Pelaporan Bencana Warga Real-Time**: Pengiriman laporan kejadian bencana warga dilengkapi verifikasi GPS dan foto langsung ke Ruang Kerja Pusdalops BPBD.
- **Keamanan Berlapis & Login Biometrik**: Perlindungan sesi petugas dengan autentikasi biometrik, enkripsi lokal AES-256-GCM, serta isolasi data cloud terproteksi.
