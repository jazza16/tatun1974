package com.example.ui.form

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity yang menghubungkan layout XML activity_form_laporan_harian.xml
 * dengan FormLaporanAdapter dan ViewPager2 + TabLayoutMediator.
 */
class FormLaporanHarianActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnSebelumnya: MaterialButton
    private lateinit var btnSelanjutnya: MaterialButton

    private val tabTitles = arrayOf("Kejadian", "Dampak & Korban", "Upaya & Kondisi", "Administrasi & Log")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                btnSelanjutnya.text = if (position == adapter.itemCount - 1) "Simpan Laporan" else "Selanjutnya"
            }
        })

        btnSebelumnya.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        btnSelanjutnya.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem += 1
            } else {
                Toast.makeText(this, "Laporan Harian berhasil disimpan ke database Pusdalops", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
