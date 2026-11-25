# Catatan Rilis Resmi SIPAGAR KITA (v1.1.0)

**Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko**  
*Versi Rilis: **v1.1.0** (Build 12) | Tanggal Rilis: **24 November 2026***

---

## Ringkasan Rilis
Rilis **SIPAGAR KITA v1.1.0** menghadirkan kepatuhan penuh terhadap **UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi (UU PDP)** melalui fitur **Disklaimer & Kebijakan Privasi Resmi**, penjelasan transparan mekanisme **Verifikasi Lapangan (*Cross-Check* / Pembuktian Terbalik)** oleh Petugas Piket Resmi BPBD kepada Kades/Lurah/Camat, penyatuan layanan darurat ke **Call Center 117** dan **Nomor Tunggal Panggilan Darurat 112 (Dinas Kominfo Kabupaten Mukomuko)**, serta penyempurnaan sinkronisasi CRUD Kelola Personil di Cloud Firestore.

---

## ✨ Penambahan Fitur & Fungsi Baru
- **Pop-up & Menu Disklaimer Resmi serta Kebijakan Privasi (UU PDP)**:
  - Pop-up **Disklaimer Singkat** otomatis tampil pada saat aplikasi pertama kali dibuka dengan kotak centang persetujuan eksplisit sebelum tombol **Lanjutkan** aktif.
  - Menu **Disklaimer & Kebijakan Privasi** tersedia permanen di dalam **Info Aplikasi** serta tautan cepat pada formulir **Lapor Bencana**.
- **Penjelasan Resmi Metode Pembuktian Kejadian (*Cross-Check* / Pembuktian Terbalik)**:
  - Menjelaskan bahwa Petugas Piket Resmi Pusdalops-PB BPBD melakukan konfirmasi kebenaran laporan warga kepada **Kepala Desa, Lurah, dan/atau Camat** selaku penanggung jawab wilayah secara administratif di Kabupaten Mukomuko melalui telepon dan saluran komunikasi resmi, mengingat cakupan luasan wilayah Kabupaten Mukomuko, jarak tempuh, ketersediaan SDM, dan efisiensi anggaran.
- **Penambahan Nomor Tunggal Panggilan Darurat 112 (Diskominfo Kab. Mukomuko)**:
  - Menambahkan **Nomor Tunggal Panggilan Darurat 112** yang dikelola oleh **Dinas Komunikasi dan Informatika (Diskominfo) Kabupaten Mukomuko** ke dalam **Kontak Darurat Publik 24 Jam** dan akses panggilan darurat cepat.

---

## ⚡ Peningkatan & Optimalisasi
- **Unifikasi Call Center 117 & 112**: Menghapus nomor telepon Posko lokal lama `(0737) 71112` di seluruh komponen aplikasi dan dokumen Disklaimer karena telah terintegrasi dalam **Call Center 117 (BPBD)** dan **Nomor Tunggal Panggilan Darurat 112 (Diskominfo)**.
- **Tata Letak Simetris & Pelindungan Privasi Lacak Laporan**:
  - Menyusun tombol panggilan darurat (`117` & `112`) dan tombol dokumen hukum pada pop-up Disklaimer dalam tata letak 2 kolom yang simetris dan presisi.
  - Melindungi privasi foto laporan warga pada fitur publik **Lacak ID** (foto bukti hanya dapat dibuka oleh Petugas BPBD yang login resmi), dilengkapi petunjuk privasi kamera, deskripsi, dan nama pelapor sesuai UU PDP.

---

## 🐛 Perbaikan Bug & Keamanan
- **Sinkronisasi CRUD Kelola Personil Cloud Firestore**: Memperbarui `firestore.rules` dan logika *tombstone* penghapusan personil agar penambahan, pengubahan, dan penghapusan data personil tersinkronisasi permanen di Cloud Firestore.

---

> 📄 **Riwayat lengkap seluruh versi (v1.0.0 – v1.1.0)** tersedia di berkas [`CHANGELOG.md`](./CHANGELOG.md) serta pada menu **Info Aplikasi → Riwayat Catatan Rilis Versi** di dalam aplikasi.

*Pusdalops-PB BPBD Kabupaten Mukomuko — Siaga, Tanggap, Tangguh Menghadapi Bencana.*
