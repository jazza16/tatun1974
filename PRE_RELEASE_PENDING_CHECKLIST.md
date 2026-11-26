# Daftar Tugas Pra-Rilis & Catatan Tunda (Pre-Release Checklist) — SIPAGAR KITA

Dokumen ini mencatat seluruh item tindak lanjut pra-rilis resmi **SIPAGAR KITA** (BPBD Kabupaten Mukomuko) untuk eksekusi penyelesaian akhir sebelum publikasi ke **Google Play Store**.

> **Tanggal Pencatatan:** 26 November 2026  
> **Versi Kode Saat Ini:** `1.1.0` (`versionCode = 12`)  
> **Status Dokumen:** Acuan Penyelesaian Akhir Pra-Rilis Resmi Pemkab / BPBD Kabupaten Mukomuko

---

## Ringkasan Status Eksekusi Saat Ini

### A. Sudah Dieksekusi (Selesai di Sisi Kode Aplikasi)
- [x] **Penguncian Modul Internal di Balik Sesi Petugas (`isOfficerLoggedIn`)**: `DatabaseExplorerScreen`, `DalaAssessmentScreen`, `PoskoLogisticsManagementScreen`, `FormLaporanHarianScreen`, `FormLaporanActivity`, `FormLaporanHarianActivity`, `OfficerDashboardConsoleView`, dan Panel Keamanan sudah terkunci di balik sesi petugas aktif dan tidak dapat dibuka langsung dari tab/pintasan Warga.
- [x] **Isolasi Sinkronisasi & Cache Lokal Mode Warga**: Perangkat warga umum tidak menarik maupun menyimpan data sensitif internal petugas (`officers`, `officer_sessions`, `device_security_sessions`, `device_login_activities`, `device_heartbeats`, dan PII `bpbd_volunteers`) ke penyimpanan lokal.
- [x] **Pengetatan Awal `firestore.rules` & Disklaimer/Kebijakan Privasi In-App (UU PDP)**: Aturan keamanan Firestore tahap awal sudah diperketat dan dialog persetujuan serta teks lengkap Disklaimer & Kebijakan Privasi (UU No. 27/2022) sudah tersedia di dalam aplikasi (`SipagarDisclaimerAndPrivacyModal.kt`).

---

## B. Daftar Tugas "TUNDA" (Belum Dieksekusi — Wajib Sebelum Rilis Play Store)

### 1. Atribusi Resmi Instansi pada Teks & Profil Aplikasi
- **Status:** `[BELUM DIEKSEKUSI]` *(Masih menggunakan teks pra-rilis untuk presentasi)*
- **Kondisi Saat Ini di Kode:**
  - `app/src/main/res/values/strings.xml` (baris 7): masih tertulis `Developed by TnT@74 • PUSDALOPS-PB BPBD MUKOMUKO`.
  - `app/src/main/java/com/example/model/AppUpdateInfo.kt` (baris 159–163) & `app/src/main/java/com/example/ui/components/DeveloperInfoCard.kt` (baris 79–83): masih mencantumkan identitas personal (`tathoenz_arazak`, `jazzakallah@gmail.com`, `@takajuikgue`) dan tautan rilis GitHub pribadi (`https://github.com/jazza16/tatun1974/releases/latest`).
  - `app/src/main/java/com/example/ui/components/AppUpdateBottomSheet.kt` (baris 447): bagian informasi pengembang masih merujuk pada atribusi pra-rilis.
- **Tindakan Penyelesaian Akhir:**
  - [ ] Ubah `developt_by_pusdalops` di `strings.xml` menjadi atribusi formal instansi, misalnya: `Dikelola oleh BPBD Kabupaten Mukomuko • Pusdalops-PB`.
  - [ ] Perbarui `AppUpdateInfo.kt` dan `DeveloperInfoCard.kt` ke identitas, email resmi, dan kanal informasi resmi Pemkab/BPBD Kabupaten Mukomuko tanpa nama alias pribadi.

---

### 2. Migrasi Mekanisme Update Aplikasi (`AppUpdater` → Google Play In-App Updates API)
- **Status:** `[BELUM DIEKSEKUSI]` *(Masih memakai konfigurasi uji coba pra-rilis)*
- **Kondisi Saat Ini di Kode:**
  - `app/build.gradle.kts` (baris 140–142): pustaka pihak ketiga `com.github.jenly1314:AppUpdater:1.2.0` masih aktif, sedangkan `libs.play.app.update` dan `libs.play.app.update.ktx` masih di-comment.
  - `app/src/main/java/com/example/ui/viewmodel/WeatherViewModel.kt` (`downloadAndApplyUpdate`, baris 910–934): masih membuka tautan GitHub eksternal melalui `Intent.ACTION_VIEW` dan menyertakan alur simulasi QA/OTA.
  - `app/src/main/java/com/example/ui/components/DeveloperInfoCard.kt`: masih memuat panduan *sideloading* / Play Protect untuk instalasi APK luar Play Store.
- **Risiko Kebijakan Google Play:** Mengunduh atau memicu pemasangan APK di luar Play Store melanggar kebijakan *Device and Network Abuse* dan berisiko penolakan aplikasi.
- **Tindakan Penyelesaian Akhir:**
  - [ ] Hapus/comment `com.github.jenly1314:AppUpdater:1.2.0` dari `app/build.gradle.kts`.
  - [ ] Aktifkan `implementation(libs.play.app.update)` dan `implementation(libs.play.app.update.ktx)` (`AppUpdateManagerFactory`).
  - [ ] Ganti logika `checkForUpdatesOnline` & `downloadAndApplyUpdate` pada `WeatherViewModel.kt` agar menggunakan alur resmi Google Play In-App Updates (*Flexible / Immediate Update*), serta nonaktifkan tombol simulasi rilis QA dan panduan *sideloading* pada build produksi.

---

### 3. Penggantian `applicationId` ke Identitas Resmi Instansi
- **Status:** `[BELUM DIEKSEKUSI]` *(Menunggu eksekusi Tim Teknis BPBD)*
- **Kondisi Saat Ini di Kode:**
  - `app/build.gradle.kts` (baris 17): masih tercatat `applicationId = "com.aistudio.cuacamukomuko.wmzq"`.
  - `app/src/test/java/com/example/base/FirestoreEmulatorTestBase.kt` (baris 43): masih merujuk `com.aistudio.cuacamukomuko.wmzq`.
- **Catatan Kritis:** `applicationId` **tidak dapat diubah** setelah aplikasi pertama kali dipublikasikan di Google Play Store.
- **Tindakan Penyelesaian Akhir:**
  - [ ] Tetapkan `applicationId` resmi instansi (mis. `id.go.mukomukokab.bpbd.sipagarkita`) di `app/build.gradle.kts` (dengan tetap menjaga `namespace = "com.example"` atau melakukan refaktor paket terencana bila diperlukan).
  - [ ] Daftarkan Android App baru di Firebase Console sesuai `applicationId` resmi dan SHA-1/SHA-256 *Play App Signing*, lalu perbarui file `app/google-services.json`.
  - [ ] Selaraskan referensi `applicationId` di berkas pengujian (`FirestoreEmulatorTestBase.kt`).

---

### 4. Kelengkapan Administrasi Google Play Console (Akun Aplikasi Pemerintah)
- **Status:** `[BELUM DIEKSEKUSI — DALAM PROSES PENGURUSAN BPBD]`
- **Detail Item:**
  1. **Surat Penugasan / Verifikasi Aplikasi Pemerintah:**
     - *Status dari Pengembang:* Izin sudah tersedia.
     - *Tindak Lanjut:* [ ] Unggah Surat Penugasan resmi Pemkab/BPBD Kabupaten Mukomuko pada verifikasi akun/aplikasi pemerintah di Google Play Console.
  2. **URL Kebijakan Privasi Publik (HTTPS) & Formulir Data Safety:**
     - *Status Saat Ini:* Baru tersedia tampilan *in-app* (`SipagarDisclaimerAndPrivacyModal.kt`), belum ada URL web publik yang terpasang di Play Console.
     - *Status dari Pengembang:* Dalam proses pengurusan oleh pihak BPBD.
     - *Tindak Lanjut:*
       - [ ] Publikasikan halaman Kebijakan Privasi resmi (disarankan domain pemerintah `.go.id`) yang menjelaskan pemrosesan **GPS, Kamera, Foto, NIK, dan Nomor HP** sesuai **UU No. 27 Tahun 2022 (UU PDP)**.
       - [ ] Tautkan URL Kebijakan Privasi tersebut pada Play Console (Store Listing & App Content) serta tambahkan tautan rujukan bila diperlukan di modal privasi in-app.
       - [ ] Isi formulir **Data Safety** di Google Play Console secara akurat sesuai data yang dikumpulkan aplikasi.

---

### 5. Penyediaan Akun Uji (*App Access*) untuk Tim Peninjau Google Play
- **Status:** `[BELUM DIEKSEKUSI]`
- **Latar Belakang:** Karena portal login Petugas berada di dalam satu APK publik yang sama dengan portal Warga, kebijakan Google Play mewajibkan penyediaan kredensial akses uji pada bagian **App Access** di Play Console agar tim peninjau dapat memverifikasi fitur di balik layar login.
- **Tindakan Penyelesaian Akhir:**
  - [ ] Siapkan minimal 1 akun demo peninjauan khusus (NIP & PIN demo yang aktif di Firestore, bukan akun operasional pejabat sesungguhnya, dan batasi/bersihkan hak destruktif jika perlu).
  - [ ] Tuliskan panduan langkah masuk (*step-by-step*) di bagian **App Access** Google Play Console sebelum mengirim build rilis untuk review.
  - [ ] Nonaktifkan/hapus atau evaluasi ulang kredensial demo pasca-review sesuai kebijakan operasional BPBD.

---

### 6. Evaluasi Pemisahan Aplikasi Two-Tier (Warga vs Petugas) & Pengetatan Lanjutan Backend
- **Status:** `[DITUNDA — MENUNGGU KONFIRMASI RESMI BPBD]`
- **Detail Item:**
  - Pengamanan *Single-APK* saat ini sudah berjalan (modul internal terkunci di balik sesi petugas dan data sensitif terisolasi di mode Warga).
  - [ ] Konfirmasi keputusan resmi BPBD Kabupaten Mukomuko apakah untuk rilis jangka panjang akan tetap memakai 1 APK atau dipisah menjadi 2 aplikasi terpisah (`SIPAGAR Warga` & `SIPAGAR Petugas`).
  - [ ] Lakukan pengetatan `firestore.rules` tahap final setelah seluruh data/skema konfirmasi dari BPBD lengkap (terutama peninjauan koleksi yang masih mengizinkan akses baca/tulis tanpa Firebase Auth penuh).
