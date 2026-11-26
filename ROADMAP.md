# Peta Jalan Pengembangan (Product Roadmap & Backlog) — SIPAGAR KITA

Dokumen ini memuat daftar rencana pengembangan fitur (*product backlog*) beserta status realisasi implementasi aplikasi **SIPAGAR KITA (Sistem Informasi & Mitigasi Kebencanaan Terpadu)** Pusdalops-PB BPBD Kabupaten Mukomuko.

> **Status Pembaruan Terakhir:** 26 November 2026 (`v1.2.0` / `Build 13`)  
> **Keterangan Status:** `[x]` = **Selesai Diterapkan & Terverifikasi** | `[ ]` = *Dalam Antrean Rilis Berikutnya*

---

## 1. Fase Tanggap Darurat (*Emergency Response*)

- [x] **Manajemen Pengungsi & Logistik — [SELESAI DITERAPKAN]**
  Mencatat jumlah pengungsi terpilah (lansia, ibu hamil, anak-anak/balita, dan penyandang disabilitas) di setiap titik posko pengungsian secara *real-time* & *offline-first*. Fitur ini juga memantau ketersediaan stok logistik darurat (sembako, tenda, obat-obatan, air bersih) agar distribusi bantuan dari gudang BPBD tepat sasaran.
  - [x] **Monitor Stok Logistik & Indikator Kritis (`Stok < 10`)**: Otomatis memberi label merah menyala **`STATUS KRITIS (< 10)`** pada komoditas yang menipis (Sembako, Tenda, Obat/P3K, Air Bersih, maupun komoditas tambahan).
  - [x] **Pelacakan Masa Kedaluwarsa (*FEFO - First Expired, First Out*)**: Memantau tanggal kedaluwarsa pangan dan obat-obatan dengan peringatan prioritas keluar jika masa berlaku `<= 30 hari` atau sudah kedaluwarsa.
  - [x] **Validasi Matematika Ketat Pengungsi Terpilah**: Menolak input di Posko jika jumlah `(Lansia + Ibu Hamil + Balita + Disabilitas) > Total Jiwa`.
  - [x] **Distribusi Bantuan Anti-Data Ganda (`NIK 16 Digit Primary Key`)**: Memvalidasi NIK 16 digit penerima bantuan, menolak duplikasi penerimaan komoditas yang sama untuk NIK yang sama, dan otomatis mengurangi saldo stok Posko.
  - [x] **Integrasi Auto-Populate & Kunci Snapshot ke Modul DaLA**: Data agregat pengungsi Posko otomatis tampil secara *Read-Only* di bagian atas formulir Pasca-Bencana (DaLA) lengkap dengan tombol **"Ambil Snapshot Data Posko"**.

- [x] **Sirene Digital Otoritas Berlapis (*Four-Eyes Principle*) & Suara Peringatan (*Text-to-Speech / TTS*) — [SELESAI DITERAPKAN]**
  Membunyikan alarm sirene berfrekuensi khusus disertai pembacaan suara otomatis (TTS Bahasa Indonesia) dan kendali otoritas berlapis (`DRAFT -> MENUNGGU PERSETUJUAN -> DIAKTIFKAN` dengan penolakan otomatis *self-approval*). Fitur ini membantu warga dan petugas piket segera menyadari bahaya secara akurat dan aman dari kesalahan aktivasi tunggal.

- [ ] **Tombol Darurat SOS (*One-Tap / 3-Second Hold Emergency SOS*) — [DITUNDA SEMENTARA SESUAI ARAHAN]**
  Memungkinkan warga yang terjebak di lokasi bencana mengirim sinyal darurat beserta titik koordinat GPS presisi, level baterai ponsel, dan kondisi singkat ke konsol Pusdalops BPBD melalui antrean lokal *Offline-First* (`sos_offline_queue`) serta `Idempotency-Key` anti-duplikat.

---

## 2. Fase Pra-Bencana (*Mitigation & Preparedness*)

- [x] **SOP Evakuasi Offline — [SELESAI DITERAPKAN]**
  Menyediakan panduan langkah-langkah penyelamatan diri standar BNPB/BPBD (seperti rumus tsunami 20-20-20, prosedur gempa bumi, banjir, dan tanah longsor) yang tersimpan penuh di memori lokal perangkat (*Visual Messaging* kontras tinggi + pembaca suara TTS). Warga tetap dapat membuka panduan evakuasi dan titik kumpul terdekat meskipun jaringan internet terputus total saat bencana.

- [ ] **Simulasi & Kuis Kesiapsiagaan**
  Menghadirkan modul edukasi interaktif berupa skenario simulasi bencana dan kuis singkat bagi masyarakat, pelajar, serta perangkat desa di 15 kecamatan Kabupaten Mukomuko. Fitur ini bertujuan meningkatkan literasi kebencanaan dan kesiapsiagaan mandiri sebelum bencana terjadi.

- [x] **Checklist Tas Siaga Bencana (*Emergency Go-Bag*) — [SELESAI DITERAPKAN]**
  Menyediakan daftar periksa (*checklist*) interaktif 10 perlengkapan wajib Tas Siaga Bencana (air minum, P3K, dokumen penting, senter, peluit, makanan tahan lama, powerbank, uang tunai, pakaian, dan radio) ditambah fitur `+ Tambah Barang Kustom` serta pelacakan tanggal kedaluwarsa di penyimpanan lokal.

---

## 3. Fase Pasca-Bencana (*Rehabilitation & Reconstruction*)

- [x] **Formulir DALA (*Damage and Loss Assessment* / Data Kerusakan) — [SELESAI DITERAPKAN]**
  Mendukung petugas lapangan dalam mendata tingkat kerusakan pasca-bencana (Rusak Berat, Rusak Sedang, Rusak Ringan) pada rumah warga, fasilitas umum, infrastruktur jalan/jembatan, dan estimasi kerugian lengkap dengan panel **Auto-Populate Read-Only Snapshot Pengungsi dari Modul Posko** dan ekspor dokumen resmi **PDF** standar BNPB.

- [x] **Pendataan & Koordinasi Relawan — [SELESAI DITERAPKAN]**
  Memfasilitasi registrasi dengan proteksi **NIK 16 Digit (Anti-Data Ganda)**, alur verifikasi keahlian (`Pending -> Verified -> Assigned`), penugasan ke Posko aktif, serta pengunduhan arsip PDF daftar relawan untuk pelaporan berjenjang.

---

## 4. Peningkatan Teknis (*Technical Enhancements*)

- [x] **Peta Jalur Evakuasi & Titik Kumpul Lokal (*Google Maps & Offline Cache*) — [SELESAI DITERAPKAN]**
  Mengintegrasikan peta titik evakuasi, zona bahaya, dan navigasi menuju Tempat Evakuasi Sementara (TES) atau Tempat Evakuasi Akhir (TEA) terdekat di Kabupaten Mukomuko.

- [ ] **Mode Offline (*SMS Fallback*)**
  Mengaktifkan mekanisme pengiriman laporan bencana dan sinyal SOS terkompresi melalui jalur SMS standar ke nomor gateway resmi Pusdalops BPBD ketika koneksi data seluler (3G/4G/5G) lumpuh di wilayah terdampak.

- [x] **Sandbox Training Mode (*Mode Latihan Petugas*) — [SELESAI DITERAPKAN]**
  Menyediakan lingkungan simulasi terisolasi bagi operator Pusdalops dan personil TRC untuk berlatih menangani pelaporan darurat, validasi insiden, serta pencetakan laporan harian tanpa mencemari basis data produksi (*production database*) atau memicu notifikasi publik ke masyarakat.

- [x] **Notifikasi Triase Pewarnaan Siaga Bencana (`v1.2.0`) — [SELESAI DITERAPKAN]**
  Menilai tingkat siaga secara otomatis menggunakan triase berwarna (Hijau, Kuning, Oranye, Merah) serta memisahkan kanal notifikasi antara warga umum dan petugas Pusdalops BPBD.

- [x] **Penguatan Anti-Reset PIN, Isolasi Mode Warga, & Desain Adaptif (`v1.2.0`) — [SELESAI DITERAPKAN]**
  Menutup seluruh jalur pengembalian PIN sepihak dengan aturan anti-downgrade Cloud Firestore, mengunci modul internal di balik sesi petugas aktif, menghapus izin `SET_WALLPAPER`, melepas kunci orientasi peta, serta menonjolkan 4 aksi darurat instan di beranda warga.
