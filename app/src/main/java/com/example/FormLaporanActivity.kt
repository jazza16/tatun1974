package com.example

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

        // Memaksa aplikasi menggunakan mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_form_laporan)

        // Inisialisasi View
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnSebelumnya = findViewById(R.id.btnSebelumnya)
        btnSelanjutnya = findViewById(R.id.btnSelanjutnya)

        // Sesi Petugas Pengisi Form
        val sessionManager = com.example.util.SessionManager(this)
        val namaPetugas = sessionManager.getNamaPetugas().ifBlank { "HITATUN ARAZAK" }
        toolbar.subtitle = "Diisi oleh: $namaPetugas"

        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            petugasJaga = namaPetugas,
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

        // Kita simpan data laporan dan log yang sedang diinput ke dalam variabel lokal dulu
        // agar bisa dilempar ke fungsi PDF nanti
        val dataLaporan = sharedViewModel.laporanSementara
        val dataLog = sharedViewModel.listLogSementara.toList()
        val dataLogistik = sharedViewModel.listLogistikSementara.toList()

        sharedViewModel.simpanKeDatabase(
            dao = db.laporanHarianDao(),
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
}
