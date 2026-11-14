package com.example.ui.form

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.example.R
import com.example.data.local.AppDatabase
import com.example.util.PdfGenerator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity yang menghubungkan layout XML activity_form_laporan_harian.xml
 * dengan FormLaporanAdapter dan ViewPager2 + TabLayoutMediator.
 */
class FormLaporanHarianActivity : AppCompatActivity() {

    private val sharedViewModel: LaporanViewModel by viewModels()
    private val db by lazy { AppDatabase.getDatabase(this) }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSebelumnya: MaterialButton
    private lateinit var btnSelanjutnya: MaterialButton

    private val tabTitles = arrayOf("Kejadian", "Dampak", "Log BPBD", "Simpan")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memaksa aplikasi menggunakan mode terang
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_form_laporan_harian)

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnSebelumnya = findViewById(R.id.btnSebelumnya)
        btnSelanjutnya = findViewById(R.id.btnSelanjutnya)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val adapter = FormLaporanAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles.getOrElse(position) { "Tab ${position + 1}" }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                btnSebelumnya.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
                btnSelanjutnya.text = if (position == adapter.itemCount - 1) "SIMPAN LAPORAN" else "Selanjutnya"
            }
        })

        btnSebelumnya.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        btnSelanjutnya.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                // JIKA DI TAB TERAKHIR (TOMBOL SIMPAN DITEKAN)
                simpanLaporanKeDatabase()
            }
        }
    }

    private fun simpanLaporanKeDatabase() {
        btnSelanjutnya.isEnabled = false
        btnSelanjutnya.text = "MENYIMPAN..."

        val dataLaporan = sharedViewModel.laporanSementara
        val dataLog = sharedViewModel.listLogSementara.toList()
        val dataLogistik = sharedViewModel.listLogistikSementara.toList()

        sharedViewModel.simpanKeDatabase(
            dao = db.laporanHarianDao(),
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Laporan Berhasil Disimpan! Membuka menu cetak...", Toast.LENGTH_LONG).show()

                    PdfGenerator.cetakLaporanKePdf(
                        context = this@FormLaporanHarianActivity,
                        laporan = dataLaporan,
                        listLog = dataLog,
                        listLogistik = dataLogistik
                    )

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
