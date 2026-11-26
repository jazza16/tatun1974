# Catatan Rilis Resmi SIPAGAR KITA (v1.2.0)

**Pusdalops-PB Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko**  
*Versi Rilis: **v1.2.0** (Build 13) | Tanggal Rilis: **26 November 2026***

---

## Ringkasan Rilis
Rilis **SIPAGAR KITA v1.2.0 (Build 13)** berfokus pada **penguatan keamanan PIN petugas (anti-reset sepihak & anti-downgrade Cloud Firestore)**, **isolasi ketat modul internal di balik sesi petugas**, **notifikasi triase pewarnaan siaga bencana**, serta **peningkatan aksesibilitas aksi darurat warga**.

---

## ✨ Penambahan Fitur & Fungsi Baru
- **Notifikasi Triase Pewarnaan Siaga Bencana (Hijau / Kuning / Oranye / Merah)**:
  - Menambahkan sistem peringatan otomatis berdasarkan tingkat siaga berwarna dengan kanal notifikasi terpisah antara masyarakat umum dan petugas BPBD.
- **Fokus Aksi Darurat Cepat di Beranda Warga**:
  - Menampilkan akses instan ke Panggilan Darurat 117/112, Lapor Bencana Cepat, Peringatan Dini/Gempa Terkini, dan Panduan Evakuasi Offline agar mudah dijangkau saat kondisi darurat.

---

## ⚡ Peningkatan & Optimalisasi
- **Desain Adaptif & Izin Minimalis**: Melepas kunci orientasi layar pada peta interaktif untuk mendukung perangkat tablet/lipat serta menghilangkan izin `SET_WALLPAPER`.
- **Transparansi Statistik Bencana**: Memisahkan data *Baseline Historis* dari data laporan langsung agar informasi publik lebih akurat.

---

## 🐛 Perbaikan Bug & Keamanan
- **Penutupan Celah Reset PIN Sepihak**: Menghapus fitur/alur reset PIN sepihak, memastikan pembaruan profil dan pasca-login hanya memperbarui metadata parsial, serta menegakkan aturan anti-downgrade pada Cloud Firestore.
- **Isolasi Mode Warga & Penyensoran Log Keamanan**: Menutup celah sinkronisasi sesi petugas saat berada di mode warga dan menyensor nilai PIN dari log diagnostik.

---

> 📄 **Riwayat lengkap seluruh versi (v1.0.0 – v1.2.0)** tersedia di berkas [`CHANGELOG.md`](./CHANGELOG.md) serta pada menu **Info Aplikasi → Riwayat Catatan Rilis Versi** di dalam aplikasi.

*Pusdalops-PB BPBD Kabupaten Mukomuko — Siaga, Tanggap, Tangguh Menghadapi Bencana.*
