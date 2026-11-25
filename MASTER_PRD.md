# MASTER DOKUMEN PRD (Product Requirements Document) — SIPAGAR KITA

**Instansi:** Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko  
**Arsitektur:** Two-Tier Frontend (`sipagar-warga` & `sipagar-petugas`), Offline-First Local Storage, RBAC Ketat (8 Role), & Anti-Data Ganda (`NIK 16 Digit` sebagai Primary Key)  
**Tanggal Pembaruan Status:** 25 November 2026

> **Keterangan Checklist:** `[x]` = **Selesai Diterapkan & Terverifikasi**

---

## BAB 1: FONDASI MASTER DATA, RBAC 8 ROLE, & ANTI-DATA GANDA (TAHAP 1)

- [x] **Skema Master Wilayah (`regions`)**: Hierarki wilayah administratif Kabupaten Mukomuko (15 Kecamatan & Desa/Kelurahan) beserta penanda zona pesisir rawan tsunami (`is_coastal_tsunami_zone`).
- [x] **Master Kependudukan Tunggal (`persons`)**: Menggunakan **`nik CHAR(16)`** sebagai Primary Key dengan validasi regex 16 digit angka murni (`^[0-9]{16}$`) serta kolom `vulnerability_category` (*Lansia, Ibu Hamil, Ibu Menyusui, Balita, Disabilitas*).
- [x] **Otorisasi RBAC 8 Role (`users`)**: Pemisahan hak akses ketat untuk `warga`, `operator_pusdalops`, `kepala_pusdalops`, `kalak`, `trc`, `petugas_posko`, `petugas_dala`, dan `relawan`.
- [x] **Isolasi Token Autentikasi**: Pemisahan kredensial & sesi antara Tier-1 (`JWT_SECRET_WARGA`) dan Tier-2 (`JWT_SECRET_PETUGAS`).
- [x] **Master Fasilitas (`facilities`) & Kejadian Bencana (`incidents`)**: Dilengkapi kolom `sync_version` dan trigger otomatis `fn_update_timestamp_and_sync_version()` untuk sinkronisasi *Offline-First*.

---

## BAB 2: APLIKASI WARGA (`sipagar-warga`) — PRA-BENCANA & DARURAT (TAHAP 2A & 2B)

- [x] **Akses Darurat Tanpa Hambatan (*Zero-Barrier Offline Entry*)**: Tombol besar **"Masuk Sebagai Tamu / Warga"** yang dapat dibuka 100% tanpa koneksi internet, berdampingan dengan Login Petugas & Login Biometrik.
- [x] **Modul SOP Evakuasi Offline (*Visual Messaging & TTS*)**:
  - [x] Kartu panduan visual kontras tinggi (Merah/Kuning/Hijau) untuk **Gempa Bumi, Tsunami Pesisir (20-20-20), Banjir, dan Tanah Longsor**.
  - [x] Tombol **Speaker (Text-to-Speech Bahasa Indonesia)** pada setiap langkah SOP.
  - [x] Fitur **Tes Alarm Sirene Lokal** untuk melatih pendengaran keluarga terhadap bunyi sirene bahaya.
  - [x] Auto-seeding otomatis ke memori SQLite lokal saat aplikasi pertama kali dibuka.
- [x] **Modul Tas Siaga Bencana (*Emergency Go-Bag*)**:
  - [x] Checklist 10 barang wajib (Air Minum, P3K, Dokumen, Senter, Peluit, Makanan Tahan Lama, Powerbank, Uang Tunai, Pakaian, Radio).
  - [x] Fitur **`+ Tambah Barang Kustom`** sesuai kebutuhan spesifik keluarga.
  - [x] Pencatatan & peringatan **Tanggal Kedaluwarsa** untuk makanan/obat di memori lokal.
- [x] **Modul Tombol Darurat SOS (*Hold 3 Seconds*)**:
  - [x] Mekanisme tahan 3 detik untuk mencegah kepanikan/salah tekan.
  - [x] Pengambilan otomatis **Koordinat GPS**, **Level Baterai HP**, dan **Timestamp**.
  - [x] Penyimpanan ke tabel antrean lokal (`sos_offline_queue`) saat tidak ada sinyal dan pengiriman otomatis oleh *Sync Worker* dengan **`Idempotency-Key`** unik saat sinyal kembali.

---

## BAB 3: PENGELOLAAN LOGISTIK & MANAJEMEN POSKO PENGUNGSIAN (TAHAP 3A — SELESAI)

### 3.1. Tujuan & Wewenang RBAC
- [x] **Akses Khusus Petugas Posko & Pusdalops**: Dikelola oleh role `petugas_posko`, `operator_pusdalops`, `kepala_pusdalops`, dan `kalak` melalui modul **Manajemen Posko & Logistik** di Ruang Kerja Konsol Pusdalops.
- [x] **Pemantauan Multi-Posko per Kejadian (`incident_id`)**: Menampilkan daftar seluruh Posko Pengungsian aktif beserta kapasitas tampung dan keterisian aktual.

### 3.2. Pengelolaan Stok Logistik Darurat & Aturan Ambang Kritis
- [x] **4 Komoditas Wajib & Komoditas Tambahan**: Memantau stok **Paket Sembako/Pangan**, **Tenda Pengungsi**, **Paket Obat & P3K**, serta **Air Bersih/Mineral**, dilengkapi fitur **`+ Tambah Komoditas Logistik`**.
- [x] **Indikator Status Kritis Otomatis (`Stok < 10`)**:
  - [x] Jika `currentStock < 10`, sistem otomatis memberi label merah menyala **`STATUS KRITIS (< 10)`** dan mengangkat jumlah komoditas kritis ke kartu ringkasan utama serta badge tombol Konsol Pusdalops.
  - [x] Jika `currentStock >= 10`, sistem menampilkan status hijau **`STOK AMAN`**.
- [x] **Pelacakan Masa Kedaluwarsa (*FEFO - First Expired, First Out*)**:
  - [x] Setiap komoditas pangan/medis memiliki atribut tanggal kedaluwarsa (`expiryDateIso`).
  - [x] Sistem memberikan peringatan kuning **`FEFO PRIORITAS KELUAR (<= 30 Hari)`** dan peringatan merah **`KEDALUWARSA - TARIK DARI POSKO`** apabila melewati tanggal berlaku.

### 3.3. Validasi Matematika Ketat Data Pengungsi Posko
- [x] **Input Terpilah Kelompok Rentan**: Mencatat `Total Jiwa`, `Lansia`, `Ibu Hamil`, `Balita`, dan `Disabilitas` di masing-masing Posko.
- [x] **Guard Validasi Matematika Ketat**:
  - [x] Sistem menolak penyimpanan dan memunculkan pesan error tegas apabila **`(Lansia + Ibu Hamil + Balita + Disabilitas) > Total Jiwa`**.

### 3.4. Distribusi Bantuan Berbasis NIK 16 Digit (Anti-Data Ganda)
- [x] **Validasi Format NIK 16 Digit**: Penerima bantuan wajib memiliki NIK 16 digit angka murni (`^[0-9]{16}$`).
- [x] **Proteksi Anti-Duplikasi Penerima Bantuan**: Sistem memeriksa buku besar distribusi (*Distribution Ledger*) dan **menolak otomatis** apabila NIK yang sama sudah pernah menerima komoditas bantuan yang sama.
- [x] **Pemotongan Stok Otomatis**: Setiap penyerahan bantuan yang lolos verifikasi NIK otomatis mengurangi saldo stok komoditas di Posko terkait dan tercatat di Log Audit Distribusi.

---

## BAB 4: PETA KOMANDO SOS & SIRENE OTORITAS BERLAPIS (TAHAP 3B)

- [x] **Peta Komando SOS (TRC & Operator Pusdalops)**:
  - [x] Menampilkan sinyal SOS masuk beserta detail Nama/NIK, Koordinat GPS, Level Baterai HP, dan Waktu Kejadian.
  - [x] Aksi verifikasi untuk mengubah status tiket dari `BARU` menjadi `DITUGASKAN` ke regu TRC lapangan.
- [x] **Sirene Otoritas Berlapis (*Four-Eyes Principle*)**:
  - [x] Pemisahan pembuatan draf sirene (`operator_pusdalops`) dan persetujuan aktivasi sirene (`kepala_pusdalops` / `kalak`).
  - [x] Penolakan otomatis jika pembuat draf mencoba menyetujui drafnya sendiri, dengan alur state **`DRAFT -> MENUNGGU PERSETUJUAN -> DIAKTIFKAN`**.

---

## BAB 5: MODUL DALA (AUTO-POPULATE SNAPSHOT POSKO) & MANAJEMEN RELAWAN (TAHAP 4)

- [x] **Modul DaLA (*Damage and Loss Assessment*)**:
  - [x] Formulir kerusakan rumah (**Rusak Berat / RB, Rusak Sedang / RS, Rusak Ringan / RR**), fasilitas umum, dan estimasi kerugian ekonomi.
  - [x] **Panel Read-Only Auto-Populate dari Modul Posko**: Menampilkan angka Total Jiwa Pengungsi & rincian Kelompok Rentan yang ditarik langsung dari `PoskoLogisticsRepository` di bagian paling atas Tab 1 DaLA.
  - [x] **Tombol `"Ambil Snapshot Data Posko"`**: Mengunci angka pengungsi beserta timestamp snapshot sehingga laporan DaLA tidak berubah meskipun Posko melakukan pembaruan data sesudahnya.
  - [x] **Ekspor Dokumen Resmi ke PDF**: Mencetak laporan kaji cepat / DaLA ke dokumen PDF resmi standar BNPB.
- [x] **Manajemen & Koordinasi Relawan**:
  - [x] Registrasi dengan proteksi **NIK 16 Digit (Anti-Data Ganda)**.
  - [x] Alur status **`Pending -> Verified -> Assigned`** beserta pemilihan Posko penugasan dan unduh arsip data seluruh relawan.
