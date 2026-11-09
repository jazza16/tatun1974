# Catatan Rilis Resmi SIPAGAR KITA (v3.6.0)
**Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko**
*Rilis Versi: 3.6.0 (Build 14 - Rilis Kandidat) | Tanggal: 6 November 2026*

---

## 1. Ikhtisar Rilis (Executive Summary)
Versi 3.6.0 merupakan rilis besar yang memfokuskan pada penyempurnaan identitas visual & kearifan lokal Kabupaten Mukomuko, penguatan keamanan berlapis menggunakan Play Integrity API (Mode Classic), penyeragaman tata ruang antarmuka Ruang Kerja dengan *vertical spacing* seragam 8dp, serta otomatisasi persistensi dan sinkronisasi data personil dinas secara *real-time* lintas perangkat berbasis Room Database v9 dan Cloud Firestore.

---

## 2. Pembaruan Visual & Infografis Bencana Mukomuko
- **Integrasi Lambang Resmi & Cap BPBD**:
  - Mengganti logo Kabupaten Mukomuko dengan lambang resmi berperisai *"Kapuang Sakti Ratau Batuah"* lengkap dengan ornamen Sirih Carano, bintang emas, padi dan kapas.
  - Memasang cap/stempel resmi melingkar Badan Penanggulangan Bencana Daerah Kabupaten Mukomuko (BPBD) dengan simbol segitiga biru di atas lingkaran oranye.
- **Visualisasi Kearifan Lokal Budaya Mukomuko**:
  - Infografis wallpaper menyajikan panorama budaya Rumah Adat Mukomuko (arsitektur panggung bertiang tinggi dengan sambungan pasak lentur peredam gempa tektonik) berlatar belakang pesisir Samudra Hindia dan gugusan hijau Bukit Barisan TNKS.
  - Penegasan kearifan tradisi Sirih Carano yang melambangkan penghormatan dan gotong-royong warga saat tanggap darurat bencana.
- **Pembersihan Spanduk & Penyelarasan Istilah Geologis**:
  - Menghapus spanduk oranye kontak darurat di bagian bawah foto, nomor telepon, dan teks *"high resolution (9:16)"*.
  - Menghapus logo Kabupaten Mukomuko dan teks judul besar *"GEOGRAFI & MITIGASI BENCANA KABUPATEN MUKOMUKO, BENGKULU"* dari atas gambar agar menghasilkan wallpaper ponsel yang bersih, elegan, dan estetik.
  - Mengganti istilah "Sunda Megathrust / Gempa Megathrust" menjadi **Zona Subduksi & Megathrust Mentawai - Bengkulu** yang sesuai dengan zona tektonik riil wilayah pesisir Mukomuko.
  - Menghapus penamaan spesifik "Air Rami", "Air Manjuto", "Sungai Manjuto", dan "Sungai Menjuto" pada infografis gambar dan kartu deskripsi.

---

## 3. Tata Letak UI & Ergonomi Ruang Kerja (8dp Uniform Spacing)
- **Arrangement.spacedBy(8.dp)**:
  - Seluruh elemen kartu pada konsol **Ruang Kerja** (`OfficerDashboardConsoleView.kt`) kini tersusun rapi di dalam `LazyColumn` dengan jarak vertikal seragam 8dp.
  - Tata letak ini menjamin konsistensi visual yang padat, profesional, dan nyaman dibaca pada seluruh form factor (Ponsel, Layar Lipat/Foldable, dan Tablet).
- **Penguncian Tinggi Badge 24dp**:
  - Seluruh komponen badge status, indikator cuaca, dan identitas personil dikunci pada ketinggian tetap 24dp (`@dimen/badge_height_fixed`) dengan lebar dinamis mengikuti isi teks (*wrapContentWidth*), mencegah teks bertumpuk atau terpotong.

---

## 4. Keamanan Sistem & Integrasi Play Integrity API (Classic Mode)
- **Validasi Token Play Integrity pada AuthManager**:
  - Modul `AuthManager.kt` kini mengintegrasikan evaluasi `PlayIntegritySecurityManager.verifyCriticalAction(...)` sebelum memfinalisasi sesi pengguna dinas di modul Ruang Kerja.
  - Setiap upaya validasi yang gagal secara otomatis dicatat ke dalam log audit keamanan lokal (`personnel_audit_logs` di Room Database) dengan tindakan `PLAY_INTEGRITY_TOKEN_FAILED` atau `PLAY_INTEGRITY_PERIODIC_CHECK_FAILED`.
  - Mengamankan hardware dari manipulasi sesi, injeksi kredensial, dan lingkungan runtime yang tidak terpercaya.

---

## 5. Otomatisasi Simpan & Sinkronisasi Personil Antar-Perangkat
- **Auto-Save & Real-Time Sync Administrator**:
  - Ketika Administrator menginput Nama dan NIP petugas baru yang belum terdaftar, sistem secara otomatis mengeksekusi `autoSaveAndSyncNewPersonnel(...)`:
    1. Menyimpan data petugas ke tabel `personnel` di Room Database lokal.
    2. Mendaftarkan profil petugas ke `OfficerAuthManager`.
    3. Mengunggah entitas petugas ke koleksi `officers` di Cloud Firestore.
- **Penerimaan Otomatis pada Perangkat Baru**:
  - Perangkat baru yang dipasangi aplikasi SIPAGAR KITA langsung menerima pembaharuan personil melalui *real-time snapshot listener* (`startAutoSyncFromCloud`), sehingga daftar petugas selalu tersinkronisasi tanpa input manual ulang.

---

## 6. Persistensi Data & Catatan Operasional (Room v9 & Gemini AI)
- **Room Database Versi 9**:
  - Entitas `OfficerNoteEntity` dan DAO `NoteDao` terdaftar penuh dengan indeks performa tinggi.
- **Catatan Operasional List-Detail Adaptif**:
  - Menampilkan tampilan split dual-pane pada layar tablet/foldable (≥ 600dp) dan single-pane dengan `BackHandler` pada smartphone (< 600dp).
  - Terintegrasi Gemini AI untuk aksi *"Ringkas Catatan"* dan *"Draf SOP Rekomendasi"*.

---

## 7. Penempatan Logo Resmi Kabupaten Mukomuko pada Kop Laporan PDF & UI
- **Pembaruan Kop Surat Laporan PDF Pimpinan & Rekap Cuaca**:
  - Logo resmi Kabupaten Mukomuko bergambar perisai *"Kapuang Sakti Ratau Batuah"* (`ic_mukomuko_logo.png`) kini terpasang secara resmi di **sisi kanan kop laporan PDF**.
  - Berdampingan secara proporsional dengan logo resmi BPBD Kabupaten Mukomuko di **sisi kiri kop laporan PDF**.
  - Menggantikan logo placeholder sebelumnya pada berkas ekspor laporan rekapitulasi cuaca dan laporan kejadian bencana warga (`ReportExporter.kt`).
- **Penyusunan Logo Bertumpuk (Stacked Logo Badge)**:
  - Pada kartu identitas akronim SIPAGAR KITA (`SipagarKitaAcronymCard.kt`), logo BPBD dan logo Kabupaten Mukomuko ditumpuk rapi menggunakan kontainer `Box` melingkar dengan elevasi 4dp dan safe padding.

---

## 8. Pemisahan Otoritas & Tanggung Jawab Aksi Petugas (Role Segregation)
- **Aksi Validasi Laporan Khusus Operator**:
  - Tombol **Validasi Laporan** ("Divalidasi") kini **hanya dapat dieksekusi oleh Operator Pusdalops** (`OfficerRoles.OPERATOR`).
  - Apabila peran lain mencoba menekan tombol tersebut, sistem menampilkan pesan edukasi akuntabilitas tugas: *"Akses Ditolak: Tombol Validasi Laporan hanya dapat dieksekusi oleh Operator Pusdalops sebagai wujud tanggung jawab petugas."*
- **Aksi Finalisasi Laporan Khusus Pimpinan**:
  - Tombol **Selesai** kini **hanya dapat dieksekusi oleh Manajer Pusdalops** (`OfficerRoles.MANAJER`) dan **Koord. Administrasi** (`OfficerRoles.KOORD_ADMINISTRASI`).
  - Peran lain akan ditolak dengan pesan: *"Akses Ditolak: Tombol Selesai hanya dapat dieksekusi oleh Manajer Pusdalops atau Koord. Administrasi sebagai wujud tanggung jawab petugas."*
- **Label Tombol Informatif**:
  - Kartu laporan pada dashboard dinas memperjelas tombol aksi menjadi *"Validasi (Operator)"* dan *"Selesai (Pimpinan)"*.

---

## 9. Pembaruan Desain Badge Panggil Kontak Darurat (Mode Bulat)
- **Badge Bulat Kontak Darurat**:
  - Tombol panggil darurat pada kartu direktori kontak penting warga (`CitizenEmergencyContactsView.kt`) diperbarui dari bentuk persegi panjang tombol biasa menjadi **Badge Bulat Melingkar** (`CircleShape`, ukuran 40dp).
  - Desain ini serasi dan identik dengan badge bulat pada judul *Direktori Kontak Darurat Mukomuko*.
  - Dilengkapi ikon gagang telepon melingkar (`Icons.Default.PhoneInTalk`) dan warna dinamis sesuai tema instansi darurat (Damkar, BPBD, Kepolisian, PLN, PMI).

---

## 10. Optimasi Responsif Peta Interaktif (Google Maps SDK)
- **Kontainer Adaptif & Konsistensi Padding**:
  - Modul peta sebaran laporan warga (`CitizenReportMapView.kt`) dan layar penuh peta interaktif (`MapActivity.kt`) kini dibungkus dalam kontainer `Box` terpusat dengan batasan lebar maksimum responsif `Modifier.widthIn(max = 840.dp)`.
  - Memastikan tampilan peta presisi, tidak terdistorsi pada perangkat layar lebar (foldable/tablet), dengan padding dan elevasi yang konsisten di semua resolusi.

---

*Pusdalops-PB BPBD Kabupaten Mukomuko — Siaga, Tanggap, Tangguh Menghadapi Bencana.*
