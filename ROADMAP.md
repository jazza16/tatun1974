# Peta Jalan Pengembangan (Product Roadmap & Backlog) — SIPAGAR KITA

Dokumen ini memuat daftar rencana pengembangan fitur (*product backlog*) aplikasi **SIPAGAR KITA (Sistem Informasi & Mitigasi Kebencanaan Terpadu)** Pusdalops-PB BPBD Kabupaten Mukomuko untuk tahapan rilis berikutnya.

---

## 1. Fase Tanggap Darurat (*Emergency Response*)

- **Manajemen Pengungsi & Logistik**
  Mencatat jumlah pengungsi terpilah (lansia, ibu hamil, anak-anak/balita, dan penyandang disabilitas) di setiap titik posko pengungsian secara *real-time*. Fitur ini juga memantau ketersediaan stok logistik darurat (sembako, tenda, obat-obatan, air bersih) agar distribusi bantuan dari gudang BPBD tepat sasaran.

- **Sirene Digital & Suara Peringatan (*Text-to-Speech / TTS*)**
  Membunyikan alarm sirene berfrekuensi khusus disertai pembacaan suara otomatis (TTS Bahasa Indonesia) saat peringatan dini gempa bumi berpotensi tsunami atau cuaca ekstrem diterima. Fitur ini membantu warga dan petugas piket segera menyadari bahaya tanpa harus melihat layar ponsel terlebih dahulu.

- **Tombol Darurat SOS (*One-Tap Emergency SOS*)**
  Memungkinkan warga yang terjebak di lokasi bencana mengirim sinyal darurat satu ketukan beserta titik koordinat GPS presisi, level baterai ponsel, dan kondisi singkat ke konsol Pusdalops BPBD. Sinyal SOS akan langsung diprioritaskan di peta komando Tim Reaksi Cepat (TRC) untuk operasi penyelamatan (*Search and Rescue*).

---

## 2. Fase Pra-Bencana (*Mitigation & Preparedness*)

- **SOP Evakuasi Offline**
  Menyediakan panduan langkah-langkah penyelamatan diri standar BNPB/BPBD (seperti rumus tsunami 20-20-20, prosedur gempa bumi, banjir, dan tanah longsor) yang tersimpan penuh di memori lokal perangkat. Warga tetap dapat membuka panduan evakuasi dan titik kumpul terdekat meskipun jaringan internet terputus total saat bencana.

- **Simulasi & Kuis Kesiapsiagaan**
  Menghadirkan modul edukasi interaktif berupa skenario simulasi bencana dan kuis singkat bagi masyarakat, pelajar, serta perangkat desa di 15 kecamatan Kabupaten Mukomuko. Fitur ini bertujuan meningkatkan literasi kebencanaan dan kesiapsiagaan mandiri sebelum bencana terjadi.

- **Checklist Tas Siaga Bencana (*Emergency Go-Bag*)**
  Menyediakan daftar periksa (*checklist*) interaktif perlengkapan wajib Tas Siaga Bencana (dokumen penting, P3K, senter, makanan tahan lama, pakaian, dan pengisi daya). Pengguna juga dapat mengatur pengingat berkala untuk memeriksa masa kedaluwarsa logistik dan obat-obatan di dalam tas siaga keluarga.

---

## 3. Fase Pasca-Bencana (*Rehabilitation & Reconstruction*)

- **Formulir DALA (*Damage and Loss Assessment* / Data Kerusakan)**
  Mendukung petugas lapangan dalam mendata tingkat kerusakan pasca-bencana (Rusak Berat, Rusak Sedang, Rusak Ringan) pada rumah warga, fasilitas umum, infrastruktur jalan/jembatan, dan lahan pertanian lengkap dengan foto ber-geotag. Data ini terstruktur sesuai format standar pengajuan rehabilitasi dan rekonstruksi BNPB.

- **Pendataan & Koordinasi Relawan**
  Memfasilitasi registrasi, verifikasi keahlian (medis, dapur umum, SAR, psikososial), dan pembagian wilayah tugas relawan kebencanaan dari berbagai organisasi di Kabupaten Mukomuko. Dengan sistem ini, komando Pusdalops dapat mengoordinasikan penempatan personil bantuan secara merata dan terukur.

---

## 4. Peningkatan Teknis (*Technical Enhancements*)

- **Peta Jalur Evakuasi Dinamis (*Google Maps Directions*)**
  Mengintegrasikan perhitungan rute evakuasi cerdas berbasis kondisi lapangan terkini yang mengarahkan warga dari posisi GPS saat ini menuju Tempat Evakuasi Sementara (TES) atau Tempat Evakuasi Akhir (TEA) terdekat. Jalur otomatis menghindari titik jalan atau jembatan yang dilaporkan terputus akibat banjir maupun tanah longsor.

- **Mode Offline (*SMS Fallback*)**
  Mengaktifkan mekanisme pengiriman laporan bencana dan sinyal SOS terkompresi melalui jalur SMS standar ke nomor gateway resmi Pusdalops BPBD ketika koneksi data seluler (3G/4G/5G) lumpuh di wilayah terdampak. Pesan SMS berformat khusus tersebut akan otomatis diterjemahkan kembali ke dalam dashboard konsol petugas.

- **Sandbox Training Mode (*Mode Latihan Petugas*)**
  Menyediakan lingkungan simulasi terisolasi bagi operator Pusdalops dan personil TRC untuk berlatih menangani pelaporan darurat, validasi insiden, serta pencetakan laporan harian tanpa mencemari basis data produksi (*production database*) atau memicu notifikasi publik ke masyarakat.
