package com.example.ui.form

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

        val verifiedUser = com.example.util.SessionManager(this).authenticateToken()
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            petugasJaga = verifiedUser.name,
            stationId = verifiedUser.stationId
        )

        setContentView(R.layout.activity_form_laporan_harian)

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

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnSebelumnya = findViewById(R.id.btnSebelumnya)
        btnSelanjutnya = findViewById(R.id.btnSelanjutnya)

        toolbar.subtitle = "Diisi oleh: ${verifiedUser.name} (NIP. ${verifiedUser.nip})"
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
            val currentLaporan = sharedViewModel.laporanSementara
            val isKondusif = currentLaporan.jenisBencana.contains("Kondusif", ignoreCase = true) ||
                currentLaporan.jenisBencana.contains("Tidak Ada", ignoreCase = true)
            val coordCheck = com.example.util.CoordinateValidator.validateKoordinatString(
                koordinat = currentLaporan.koordinat,
                allowKondusifDash = isKondusif
            )
            if (coordCheck.isFailure) {
                val errMsg = coordCheck.exceptionOrNull()?.message
                    ?: com.example.util.CoordinateValidator.ERROR_INVALID_RANGE
                Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show()
                viewPager.currentItem = 0
                return@setOnClickListener
            }

            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                // JIKA DI TAB TERAKHIR (TOMBOL SIMPAN DITEKAN)
                simpanLaporanKeDatabase()
            }
        }
    }

    private fun simpanLaporanKeDatabase() {
        val currentLaporan = sharedViewModel.laporanSementara
        val isKondusif = currentLaporan.jenisBencana.contains("Kondusif", ignoreCase = true) ||
            currentLaporan.jenisBencana.contains("Tidak Ada", ignoreCase = true)
        val coordCheck = com.example.util.CoordinateValidator.validateKoordinatString(
            koordinat = currentLaporan.koordinat,
            allowKondusifDash = isKondusif
        )
        if (coordCheck.isFailure) {
            val errMsg = coordCheck.exceptionOrNull()?.message
                ?: com.example.util.CoordinateValidator.ERROR_INVALID_RANGE
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show()
            viewPager.currentItem = 0
            return
        }

        btnSelanjutnya.isEnabled = false
        btnSelanjutnya.text = "MENYIMPAN..."

        // BENAR: Ambil dari data user yang terverifikasi di token (bukan dari body input form)
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

    override fun onResume() {
        super.onResume()
        if (!com.example.data.local.OfficerAuthManager(this).isOfficerLoggedIn()) {
            finish()
        }
    }
}
