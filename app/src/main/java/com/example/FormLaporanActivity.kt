package com.example

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.ui.form.FormLaporanAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FormLaporanActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSebelumnya: MaterialButton
    private lateinit var btnSelanjutnya: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_laporan)

        // Inisialisasi View
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnSebelumnya = findViewById(R.id.btnSebelumnya)
        btnSelanjutnya = findViewById(R.id.btnSelanjutnya)

        // Sesi Petugas Pengisi Form
        val sessionManager = com.example.util.SessionManager(this)
        val namaPetugas = sessionManager.getNamaPetugas()
        toolbar.subtitle = "Diisi oleh: $namaPetugas"

        // Tombol Close di Toolbar
        toolbar.setNavigationOnClickListener { finish() }

        // Setup Adapter ViewPager
        val adapter = FormLaporanAdapter(this)
        viewPager.adapter = adapter

        // Hubungkan TabLayout dengan ViewPager (Scrollable agar tidak terpotong pada font besar)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        val tabTitles = arrayOf("Kejadian", "Dampak", "Log BNPB", "Simpan")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        btnSelanjutnya.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                saveLaporanAndFinish()
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

    private fun saveLaporanAndFinish() {
        val db = com.example.data.local.AppDatabase.getInstance(this)
        val now = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            val sessionManager = com.example.util.SessionManager(this@FormLaporanActivity)
            val namaPetugas = prefs[NAMA_PETUGAS] ?: sessionManager.getNamaPetugas()
            val nipPetugas = prefs[NIP_PETUGAS] ?: sessionManager.getNipPetugas()
            val laporan = com.example.data.local.LaporanHarian(
                tanggalLaporan = today,
                jenisBencana = "Gempa Bumi Tektonik",
                tanggalKejadian = today,
                waktuKejadian = java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).format(java.util.Date()),
                kedalaman = "24 km",
                lokasi = "Perairan Barat Daya Mukomuko, Bengkulu",
                koordinat = "2.58 LS - 101.12 BT",
                cakupanDampak = "Kecamatan Kota Mukomuko dan Ipuh",
                kondisiCuaca = "Hujan Ringan",
                upayaDilakukan = "Monitoring tim TRC, koordinasi aparatur desa pesisir",
                kondisiMutakhir = "Kondusif, nihil kerusakan fasilitas publik",
                korbanLuka = 0,
                korbanMeninggal = 0,
                kerusakan = "Nihil korban jiwa dan nihil kerusakan berat",
                penyebab = "Aktivitas subduksi Megathrust Mentawai-Sumatera",
                sumberInformasi = "Pusdalops PB & BMKG WRS-NTWC",
                tanggalSurat = today,
                tanggalTerima = today,
                operatorName = namaPetugas,
                stationId = "PUSDALOPS-MM-01"
            )
            val newId = db.laporanHarianDao().insertLaporan(laporan).toInt()
            db.laporanHarianDao().insertLogKomunikasi(
                com.example.data.local.LogKomunikasiEntity(
                    laporanId = newId,
                    waktu = java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).format(java.util.Date()),
                    stationIdFrom = "PUSDALOPS-MM-01",
                    stationIdTo = "Kalaksa BPBD & Camat Kota Mukomuko",
                    subjek = "Laporan harian tercatat resmi dalam sistem database BPBD.",
                    operatorName = namaPetugas.ifBlank { "HITATUN ARAZAK" }
                )
            )
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(this@FormLaporanActivity, "Laporan Harian Berhasil Disimpan ke Database!", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
