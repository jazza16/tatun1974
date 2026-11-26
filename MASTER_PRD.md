# MASTER DOKUMEN PRD (Product Requirements Document) — SIPAGAR KITA

**Instansi:** Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko  
**Versi Rilis Aktif:** `v1.2.0` (`versionCode = 13`)  
**Arsitektur:** Two-Tier Frontend (`sipagar-warga` & `sipagar-petugas`), Offline-First Local Storage, RBAC Ketat (8 Role), & Anti-Data Ganda (`NIK 16 Digit` sebagai Primary Key)  
**Tanggal Pembaruan Status:** 26 November 2026

> **Keterangan Checklist:** `[x]` = **Selesai Diterapkan & Terverifikasi** | `[ ]` = *Ditunda Sementara / Antrean Berikutnya*

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
- [ ] **Modul Tombol Darurat SOS (*Hold 3 Seconds*) — [DITUNDA SEMENTARA SESUAI ARAHAN]**:
  - [ ] Mekanisme tahan 3 detik untuk mencegah kepanikan/salah tekan.
  - [ ] Pengambilan otomatis **Koordinat GPS**, **Level Baterai HP**, dan **Timestamp**.
  - [ ] Penyimpanan ke tabel antrean lokal (`sos_offline_queue`) saat tidak ada sinyal dan pengiriman otomatis oleh *Sync Worker* dengan **`Idempotency-Key`** unik saat sinyal kembali.

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

- [ ] **Peta Komando SOS Tiket Baterai HP (TRC & Operator Pusdalops) — [DITUNDA SEMENTARA, TERKAIT TOMBOL SOS BAB 2]**:
  - [ ] Menampilkan sinyal SOS masuk beserta detail Nama/NIK, Koordinat GPS, Level Baterai HP, dan Waktu Kejadian (menunggu aktivasi Modul Tombol Darurat SOS Warga).
  - [ ] Aksi verifikasi khusus tiket SOS dari `BARU` menjadi `DITUGASKAN` ke regu TRC lapangan *(catatan: alur laporan cepat warga & validasi Konsol Pusdalops sudah berjalan normal)*.
- [x] **Sirene Otoritas Berlapis (*Four-Eyes Principle*)**:
  - [x] Pemisahan pembuatan draf sirene (`operator_pusdalops`) dan persetujuan aktivasi sirene (`kepala_pusdalops` / `kalak`).
  - [x] Penolakan otomatis jika pembuat draf mencoba menyetujui drafnya sendiri, dengan alur state **`DRAFT -> MENUNGGU PERSETUJUAN -> DIAKTIFKAN`** melalui `FourEyesSirenManager` & `FourEyesSirenControlScreen`.

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

---

## BAB 6: PENGUATAN KEAMANAN PIN, ISOLASI MODE WARGA, & NOTIFIKASI TRIASE (RILIS v1.2.0 — SELESAI)

- [x] **Perlindungan Anti-Reset PIN Sepihak & Anti-Downgrade Cloud Firestore**:
  - [x] Menonaktifkan seluruh API/tombol reset PIN sepihak dan mewajibkan ubah PIN pribadi langsung melalui verifikasi Cloud Firestore.
  - [x] Pembaruan profil maupun pasca-login/biometrik hanya menulis metadata parsial tanpa menimpa `pinHash`, `salt`, atau `isCustomPinSet`.
  - [x] Menegakkan aturan anti-downgrade di `firestore.rules` serta menyensor nilai PIN pada log diagnostik keamanan.
- [x] **Penguncian Ketat Modul Internal & Isolasi Mode Warga**:
  - [x] Mengunci seluruh modul internal petugas (*DatabaseExplorer*, DALA, Manajemen Posko/Logistik, Form Laporan Harian, Panel Keamanan) hanya saat sesi petugas terverifikasi aktif (`isOfficerLoggedIn`).
  - [x] Menghentikan penarikan/penyimpanan data sensitif internal petugas ketika berada di Mode Warga.
- [x] **Notifikasi Triase Pewarnaan Siaga Bencana**:
  - [x] Evaluasi tingkat siaga berwarna (**Hijau / Kuning / Oranye / Merah**) dengan kanal notifikasi terpisah antara Warga dan Petugas serta peredam pengulangan (*cooldown*) dan eskalasi prioritas tinggi.
- [x] **Fokus Darurat Beranda Warga, Kepatuhan Izin Minimalis, & Desain Adaptif**:
  - [x] Menonjolkan 4 aksi darurat cepat di beranda warga (117/112, Lapor Cepat, Peringatan Dini, Evakuasi Offline), melepas kunci orientasi layar pada `MapActivity`, memisahkan *Data Baseline Historis* dari *LIVE DATA*, dan menghapus izin `SET_WALLPAPER`.
