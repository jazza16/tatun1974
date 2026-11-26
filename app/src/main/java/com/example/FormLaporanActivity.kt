package com.example

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.data.local.AppDatabase
import com.example.ui.form.FormLaporanAdapter
import com.example.ui.form.LaporanViewModel
import com.example.util.PdfGenerator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormLaporanActivity : AppCompatActivity() {

    // Panggil ViewModel di level Activity
    private val sharedViewModel: LaporanViewModel by viewModels()

    // Instance Database Room
    private val db by lazy { AppDatabase.getDatabase(this) }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSebelumnya: MaterialButton
    private lateinit var btnSelanjutnya: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict Access Control: Modul Form Laporan Harian hanya boleh diakses oleh Petugas dengan sesi login aktif.
        val officerAuth = com.example.data.local.OfficerAuthManager(this)
        if (!officerAuth.isOfficerLoggedIn()) {
            Toast.makeText(
                this,
                "Akses ditolak: Silakan login sebagai Petugas Pusdalops terlebih dahulu.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Memaksa aplikasi menggunakan mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_form_laporan)

        // Safe Area WindowInsets agar tombol "Selanjutnya" tidak tertutup Navigation Bar / area gestur HP
        val rootView = findViewById<View>(R.id.rootFormLaporan)
        val bottomNavContainer = findViewById<View>(R.id.bottomNavContainer)
        val basePaddingPx = (16 * resources.displayMetrics.density).toInt()
        val compactVerticalPx = (10 * resources.displayMetrics.density).toInt()
        if (rootView != null && bottomNavContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                )
                val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                val verticalPad = if (imeVisible) compactVerticalPx else basePaddingPx
                view.updatePadding(
                    left = bars.left,
                    top = bars.top,
                    right = bars.right
                )
                bottomNavContainer.updatePadding(
                    left = basePaddingPx,
                    top = verticalPad,
                    right = basePaddingPx,
                    bottom = verticalPad + bars.bottom
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        // Inisialisasi View
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnSebelumnya = findViewById(R.id.btnSebelumnya)
        btnSelanjutnya = findViewById(R.id.btnSelanjutnya)

        // Sesi Petugas Pengisi Form (Sinkron Nama & NIP dari Daftar Kontak Personil)
        val sessionManager = com.example.util.SessionManager(this)
        val verifiedUser = sessionManager.authenticateToken()
        val namaPetugas = verifiedUser.name
        val nipPetugas = verifiedUser.nip
        toolbar.subtitle = "Diisi oleh: $namaPetugas (NIP. $nipPetugas)"

        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            petugasJaga = namaPetugas,
            stationId = verifiedUser.stationId,
            tanggalLaporanDibuat = sharedViewModel.laporanSementara.tanggalLaporanDibuat.ifBlank { today }
        )

        // Tombol Close di Toolbar
        toolbar.setNavigationOnClickListener { finish() }

        // Setup Adapter ViewPager
        val adapter = FormLaporanAdapter(this)
        viewPager.adapter = adapter

        // Hubungkan TabLayout dengan ViewPager (Scrollable agar tidak terpotong pada font besar)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        val tabTitles = arrayOf("Kejadian", "Dampak", "Log BPBD", "Simpan")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        btnSelanjutnya.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                // JIKA DI TAB TERAKHIR (TOMBOL SIMPAN DITEKAN)
                simpanLaporanKeDatabase()
            }
        }

        btnSebelumnya.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        }

        // Pantau perpindahan Tab untuk mengatur tombol Kembali/Selanjutnya
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Sembunyikan tombol "Kembali" di Tab pertama
                btnSebelumnya.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE

                // Ubah teks tombol jika di tab terakhir
                btnSelanjutnya.text = if (position == adapter.itemCount - 1) "SIMPAN LAPORAN" else "Selanjutnya"
            }
        })
    }

    private fun simpanLaporanKeDatabase() {
        btnSelanjutnya.isEnabled = false
        btnSelanjutnya.text = "MENYIMPAN..."

        val verifiedUser = com.example.util.SessionManager(this).authenticateToken()
        val dataLaporan = sharedViewModel.laporanSementara.copy(
            petugasJaga = verifiedUser.name,
            stationId = verifiedUser.stationId
        )
        val dataLog = sharedViewModel.listLogSementara.map {
            it.copy(operatorName = verifiedUser.name)
        }
        val dataLogistik = sharedViewModel.listLogistikSementara.toList()

        sharedViewModel.simpanKeDatabase(
            dao = db.laporanHarianDao(),
            verifiedUser = verifiedUser,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Laporan Berhasil Disimpan! Membuka menu cetak...", Toast.LENGTH_LONG).show()

                    // --- TAMBAHKAN KODE INI UNTUK MENCETAK PDF ---
                    PdfGenerator.cetakLaporanKePdf(
                        context = this@FormLaporanActivity,
                        laporan = dataLaporan,
                        listLog = dataLog,
                        listLogistik = dataLogistik
                    )
                    // ---------------------------------------------

                    // (Opsional) Kembali ke halaman Ruang Kerja setelah cetak
                    // finish()

                    btnSelanjutnya.isEnabled = true
                    btnSelanjutnya.text = "SIMPAN LAPORAN"
                }
            },
            onError = { pesanError ->
                runOnUiThread {
                    Toast.makeText(this, "Gagal: $pesanError", Toast.LENGTH_LONG).show()
                    btnSelanjutnya.isEnabled = true
                    btnSelanjutnya.text = "SIMPAN LAPORAN"
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (!com.example.data.local.OfficerAuthManager(this).isOfficerLoggedIn()) {
            finish()
        }
    }
}
