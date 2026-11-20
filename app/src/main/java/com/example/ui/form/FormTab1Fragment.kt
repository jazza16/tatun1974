package com.example.ui.form

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.LaporanWargaDao
import com.example.data.repository.WeatherRepository
import com.example.model.MukomukoDistricts
import com.example.model.WeatherCodeHelper
import com.example.util.NAMA_PETUGAS
import com.example.util.NIP_PETUGAS
import com.example.util.SessionManager
import com.example.util.dataStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class FormTab1Fragment : Fragment() {

    // Panggil ViewModel yang sama dengan Activity
    private val sharedViewModel: LaporanViewModel by activityViewModels()

    // Variabel global di Fragment untuk nanti disimpan ke Room
    var petugasAktifNama: String = "Petugas Pusdalops"
    var petugasAktifNip: String = "-"

    // Deklarasi komponen UI dan DAO
    private lateinit var database: AppDatabase
    private lateinit var laporanWargaDao: LaporanWargaDao
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var btnTarikLaporanWarga: MaterialButton
    private lateinit var btnImporLaporanWarga: MaterialButton
    private lateinit var dropdownJenisBencana: AutoCompleteTextView
    private lateinit var dropdownKecamatan: AutoCompleteTextView
    private lateinit var dropdownCuaca: AutoCompleteTextView
    private lateinit var etTanggalKejadian: TextInputEditText
    private lateinit var etWaktuKejadian: TextInputEditText
    private lateinit var etLokasiBencana: TextInputEditText
    private lateinit var etKoordinat: TextInputEditText
    private lateinit var etKedalaman: TextInputEditText
    private lateinit var etCakupanDampak: TextInputEditText
    private lateinit var etUpayaDilakukan: TextInputEditText
    private lateinit var etKondisiMutakhir: TextInputEditText
    private lateinit var etKerusakan: TextInputEditText
    private lateinit var etPenyebab: TextInputEditText
    private lateinit var etSumberInformasi: TextInputEditText
    private lateinit var btnAmbilGps: View
    private var tvBadgeDariWarga: TextView? = null
    private var composeAttachedCitizenPreview: ComposeView? = null
    private var cachedIncomingReports: List<IncomingCitizenReportItem> = emptyList()

    private var cuacaSaatKejadianTerkini: String = "Data cuaca tidak tersedia"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Hubungkan dengan layout XML yang sudah kita buat
        return inflater.inflate(R.layout.fragment_form_tab1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi DAO & View
        database = AppDatabase.getInstance(requireContext())
        laporanWargaDao = database.laporanWargaDao()
        weatherRepository = WeatherRepository(database.weatherDao())
        dropdownJenisBencana = view.findViewById(R.id.dropdownJenisBencana)
        dropdownKecamatan = view.findViewById(R.id.dropdownKecamatan)
        dropdownCuaca = view.findViewById(R.id.dropdownCuaca)
        etTanggalKejadian = view.findViewById(R.id.etTanggalKejadian)
        etWaktuKejadian = view.findViewById(R.id.etWaktuKejadian)
        etLokasiBencana = view.findViewById(R.id.etLokasiBencana)
        etKoordinat = view.findViewById(R.id.etKoordinat)
        etKedalaman = view.findViewById(R.id.etKedalaman)
        etCakupanDampak = view.findViewById(R.id.etCakupanDampak)
        etUpayaDilakukan = view.findViewById(R.id.etUpayaDilakukan)
        etKondisiMutakhir = view.findViewById(R.id.etKondisiMutakhir)
        etKerusakan = view.findViewById(R.id.etKerusakan)
        etPenyebab = view.findViewById(R.id.etPenyebab)
        etSumberInformasi = view.findViewById(R.id.etSumberInformasi)
        btnAmbilGps = view.findViewById(R.id.btnAmbilGps)
        val btnTarik = view.findViewById<MaterialButton>(R.id.btnTarikLaporanWarga)
            ?: view.findViewById(R.id.btnImporLaporanWarga)
        btnTarikLaporanWarga = btnTarik
        btnImporLaporanWarga = btnTarik
        tvBadgeDariWarga = view.findViewById(R.id.tvBadgeDariWarga)
        composeAttachedCitizenPreview = view.findViewById(R.id.composeAttachedCitizenPreview)
        setupRedNotificationBadgeStyle()
        refreshIncomingCitizenReportsAndBadge()

        // Pastikan field cuaca bersifat read-only
        dropdownCuaca.keyListener = null
        dropdownCuaca.isFocusable = false
        dropdownCuaca.isClickable = false

        // Konfigurasi scrollPadding (110dp di atas keyboard) dan Keyboard Action (Next / Done)
        val nestedScrollView = view as? androidx.core.widget.NestedScrollView
        val extraScrollPaddingPx = (110 * resources.displayMetrics.density).toInt()
        val orderedFields = listOf(
            etLokasiBencana,
            etKoordinat,
            etKedalaman,
            etCakupanDampak,
            etUpayaDilakukan,
            etKondisiMutakhir,
            etKerusakan,
            etPenyebab,
            etSumberInformasi
        )
        orderedFields.forEachIndexed { index, editText ->
            val isLastField = index == orderedFields.lastIndex
            editText.imeOptions = if (isLastField) {
                android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            } else {
                android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
            }
            editText.setOnFocusChangeListener { focusedView, hasFocus ->
                if (hasFocus && nestedScrollView != null) {
                    focusedView.postDelayed({
                        val rect = android.graphics.Rect(
                            0,
                            0,
                            focusedView.width,
                            focusedView.height + extraScrollPaddingPx
                        )
                        focusedView.requestRectangleOnScreen(rect, false)
                    }, 140)
                }
            }
            editText.setOnEditorActionListener { v, actionId, _ ->
                when (actionId) {
                    android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> {
                        val nextField = orderedFields.getOrNull(index + 1)
                        if (nextField != null) {
                            nextField.requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> {
                        v.clearFocus()
                        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                        true
                    }
                    else -> false
                }
            }
        }

        // Inisialisasi Info Sesi Petugas (Sinkron Nama & NIP dari Daftar Kontak Personil)
        val officerAuthManager = com.example.data.local.OfficerAuthManager(requireContext())
        val sessionManager = SessionManager(requireContext())
        val verifiedUser = sessionManager.authenticateToken()
        petugasAktifNama = verifiedUser.name
        petugasAktifNip = verifiedUser.nip
        val tvNamaPetugas = view.findViewById<android.widget.TextView>(R.id.tvNamaPetugas)
        val tvInfoPetugas = view.findViewById<android.widget.TextView>(R.id.tvInfoPetugas)
        tvNamaPetugas?.text = "$petugasAktifNama (NIP. $petugasAktifNip)"
        tvInfoPetugas?.text = "Diisi oleh: $petugasAktifNama (NIP. $petugasAktifNip)"

        // Di dalam onViewCreated FormTab1Fragment:
        lifecycleScope.launch {
            sessionManager.namaPetugasFlow.collect { decryptedNama ->
                val rawNama = officerAuthManager.getOfficerName().ifBlank {
                    decryptedNama.takeUnless { it.startsWith("enc_v1:") }.orEmpty().ifBlank { "HITATUN ARAZAK" }
                }
                val rawNip = officerAuthManager.getOfficerBadgeId().ifBlank {
                    sessionManager.getNipPetugas().takeUnless { it.startsWith("enc_v1:") }.orEmpty()
                }
                val synced = officerAuthManager.resolveSyncedOfficer(rawName = rawNama, rawNip = rawNip)

                petugasAktifNama = synced.fullName
                petugasAktifNip = synced.nip

                tvNamaPetugas?.text = "${synced.fullName} (NIP. ${synced.nip})"
                tvInfoPetugas?.text = "Diisi oleh: ${synced.fullName} (NIP. ${synced.nip})"
                sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
                    petugasJaga = synced.fullName
                )
            }
        }

        setupDropdowns()
        setupDateTimePickers()

        // Gunakan SwitchMaterial, BUKAN MaterialSwitch
        val switchKondusif = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchKondusif)

        switchKondusif.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dropdownJenisBencana.setText("Tidak Ada Bencana (Kondusif)", false)
                dropdownKecamatan.setText("Kota Mukomuko", false)
                muatCuacaKecamatanOtomatis("Kota Mukomuko", updateLokasiDanKoordinat = false)

                etTanggalKejadian.setText("-")
                etWaktuKejadian.setText("-")
                etLokasiBencana.setText("Kab. Mukomuko (Kec. Kota Mukomuko)")
                etKoordinat.setText("-")
                etKedalaman.setText("-")
                etCakupanDampak.setText("Seluruh 15 Kecamatan Kabupaten Mukomuko dalam keadaan aman dan terkendali")
                etUpayaDilakukan.setText("Pusdalops-PB BPBD Mukomuko melaksanakan pemantauan rutin cuaca, piket siaga 24 jam, dan komunikasi radio VHF seluruh kecamatan")
                etKondisiMutakhir.setText("Situasi wilayah Kabupaten Mukomuko terpantau aman, kondusif, dan terkendali")
                etKerusakan.setText("Nihil kerusakan fasilitas maupun infrastruktur")
                etPenyebab.setText("-")
                etSumberInformasi.setText("Pantauan Pusdalops-PB BPBD Mukomuko & Stasiun BMKG")
                simpanDataKeViewModel()

                Toast.makeText(requireContext(), "Mode Kondusif Aktif", Toast.LENGTH_SHORT).show()
            } else {
                dropdownJenisBencana.setText("", false)
                dropdownKecamatan.setText("", false)
                setCuacaReadOnly("Data cuaca tidak tersedia")
                etTanggalKejadian.setText("")
                etWaktuKejadian.setText("")
                etLokasiBencana.setText("")
                etKoordinat.setText("")
                etKedalaman.setText("")
                etCakupanDampak.setText("")
                etUpayaDilakukan.setText("")
                etKondisiMutakhir.setText("")
                etKerusakan.setText("")
                etPenyebab.setText("")
                etSumberInformasi.setText("")
                simpanDataKeViewModel()
            }
        }

        dropdownJenisBencana.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()

            if (selected.contains("Kondusif", ignoreCase = true) || selected.contains("Tidak Ada", ignoreCase = true)) {
                switchKondusif?.isChecked = true
                etTanggalKejadian.isEnabled = false
                etKedalaman.isEnabled = false
                Toast.makeText(requireContext(), "Mode Kondusif diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                if (switchKondusif?.isChecked == true) {
                    switchKondusif.isChecked = false
                }
                etTanggalKejadian.isEnabled = true
                etKedalaman.isEnabled = true
            }
        }

        // Saat petugas memilih Kecamatan, otomatis muat cuaca terkini dari modul cuaca 15 Kecamatan
        dropdownKecamatan.setOnItemClickListener { parent, _, position, _ ->
            val namaKecamatan = parent.getItemAtPosition(position).toString()
            muatCuacaKecamatanOtomatis(namaKecamatan, updateLokasiDanKoordinat = true)
        }

        btnTarik.setOnClickListener {
            tampilkanModalDaftarLaporanWarga()
        }

        val btnTarikKemarin = view.findViewById<MaterialButton>(R.id.btnTarikLaporanKemarin)

        btnTarikKemarin.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val laporanKemarin = database.laporanHarianDao().getLaporanTerakhir()

                withContext(Dispatchers.Main) {
                    if (laporanKemarin != null) {
                        etLokasiBencana.setText(laporanKemarin.lokasiBencana)
                        etKoordinat.setText(laporanKemarin.koordinat)
                        etKedalaman.setText(laporanKemarin.kedalaman)
                        dropdownJenisBencana.setText(laporanKemarin.jenisBencana, false)
                        etCakupanDampak.setText(laporanKemarin.cakupanDampak)
                        etUpayaDilakukan.setText(laporanKemarin.upayaDilakukan)
                        etKondisiMutakhir.setText(laporanKemarin.kondisiMutakhir)
                        etKerusakan.setText(laporanKemarin.kerusakan)
                        etPenyebab.setText(laporanKemarin.penyebab)
                        etSumberInformasi.setText(laporanKemarin.sumberInformasi)

                        val matchedDistrict = MukomukoDistricts.list.firstOrNull {
                            laporanKemarin.lokasiBencana.contains(it.name, ignoreCase = true)
                        }
                        if (matchedDistrict != null) {
                            dropdownKecamatan.setText(matchedDistrict.name, false)
                            muatCuacaKecamatanOtomatis(matchedDistrict.name, updateLokasiDanKoordinat = false)
                        } else {
                            val savedWeather = laporanKemarin.cuacaSaatKejadian.ifBlank {
                                laporanKemarin.kondisiCuaca.ifBlank { "Data cuaca tidak tersedia" }
                            }
                            setCuacaReadOnly(savedWeather)
                        }
                        simpanDataKeViewModel()

                        Toast.makeText(
                            requireContext(),
                            "Data kejadian sebelumnya berhasil dimuat. Silakan update kondisi terbaru.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(requireContext(), "Belum ada data laporan sebelumnya.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnAmbilGps.setOnClickListener {
            val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val lastLocation = if ((hasFine || hasCoarse) && locationManager != null) {
                try {
                    locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                } catch (_: SecurityException) {
                    null
                }
            } else null

            if (lastLocation != null) {
                val formatted = String.format(Locale.US, "%.5f, %.5f", lastLocation.latitude, lastLocation.longitude)
                etKoordinat.setText(formatted)
                simpanDataKeViewModel()
                Toast.makeText(requireContext(), "Koordinat GPS perangkat berhasil diambil: $formatted", Toast.LENGTH_SHORT).show()
            } else {
                val selectedKec = dropdownKecamatan.text?.toString()?.trim().orEmpty()
                val district = MukomukoDistricts.list.firstOrNull {
                    it.name.equals(selectedKec, ignoreCase = true)
                } ?: MukomukoDistricts.list.first()
                val formatted = String.format(Locale.US, "%.4f, %.4f", district.latitude, district.longitude)
                etKoordinat.setText(formatted)
                simpanDataKeViewModel()
                Toast.makeText(requireContext(), "Koordinat GPS (${district.name}) berhasil diambil: $formatted", Toast.LENGTH_SHORT).show()
            }
        }

        // Pulihkan nilai awal dari ViewModel bila sudah ada dan pasang TextWatcher agar langsung tersinkronisasi
        val existing = sharedViewModel.laporanSementara
        if (existing.cakupanDampak.isNotBlank()) etCakupanDampak.setText(existing.cakupanDampak)
        if (existing.upayaDilakukan.isNotBlank()) etUpayaDilakukan.setText(existing.upayaDilakukan)
        if (existing.kondisiMutakhir.isNotBlank()) etKondisiMutakhir.setText(existing.kondisiMutakhir)
        if (existing.kerusakan.isNotBlank()) etKerusakan.setText(existing.kerusakan)
        if (existing.penyebab.isNotBlank()) etPenyebab.setText(existing.penyebab)
        if (existing.sumberInformasi.isNotBlank()) etSumberInformasi.setText(existing.sumberInformasi)

        val liveSyncWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                simpanDataKeViewModel()
            }
        }
        listOf(
            etTanggalKejadian,
            etWaktuKejadian,
            etLokasiBencana,
            etKoordinat,
            etKedalaman,
            etCakupanDampak,
            etUpayaDilakukan,
            etKondisiMutakhir,
            etKerusakan,
            etPenyebab,
            etSumberInformasi
        ).forEach { it.addTextChangedListener(liveSyncWatcher) }
    }

    private fun setupDropdowns() {
        val listBencana = arrayOf(
            "Banjir",
            "Tanah Longsor",
            "Kebakaran Hutan dan Lahan",
            "Angin Puting Beliung",
            "Gempa Bumi",
            "Cuaca Ekstrem",
            "Tidak Ada Bencana (Kondusif)"
        )
        dropdownJenisBencana.keyListener = null
        dropdownJenisBencana.isFocusable = false
        dropdownJenisBencana.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            val switchKondusif = view?.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchKondusif)
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Pilih Jenis Bencana")
                .setItems(listBencana) { dialog, which ->
                    val selected = listBencana[which]
                    dropdownJenisBencana.setText(selected, false)
                    if (selected.contains("Kondusif", ignoreCase = true) || selected.contains("Tidak Ada", ignoreCase = true)) {
                        switchKondusif?.isChecked = true
                        etTanggalKejadian.isEnabled = false
                        etKedalaman.isEnabled = false
                        Toast.makeText(requireContext(), "Mode Kondusif diaktifkan", Toast.LENGTH_SHORT).show()
                    } else {
                        if (switchKondusif?.isChecked == true) {
                            switchKondusif.isChecked = false
                        }
                        etTanggalKejadian.isEnabled = true
                        etKedalaman.isEnabled = true
                    }
                    dialog.dismiss()
                }
                .show()
        }

        val listKecamatan = MukomukoDistricts.list.map { it.name }.toTypedArray()
        dropdownKecamatan.keyListener = null
        dropdownKecamatan.isFocusable = false
        dropdownKecamatan.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Pilih Kecamatan (15 Kecamatan)")
                .setItems(listKecamatan) { dialog, which ->
                    val namaKecamatan = listKecamatan[which]
                    dropdownKecamatan.setText(namaKecamatan, false)
                    muatCuacaKecamatanOtomatis(namaKecamatan, updateLokasiDanKoordinat = true)
                    dialog.dismiss()
                }
                .show()
        }
    }

    /**
     * Mengambil data cuaca terkini untuk kecamatan yang dipilih dari modul cuaca yang sudah ada
     * (Room cache `weather_cache` / `WeatherRepository`) dan menyimpannya secara read-only.
     */
    private fun muatCuacaKecamatanOtomatis(
        namaKecamatan: String,
        updateLokasiDanKoordinat: Boolean = true
    ) {
        val district = MukomukoDistricts.list.firstOrNull {
            it.name.equals(namaKecamatan.trim(), ignoreCase = true) ||
                namaKecamatan.contains(it.name, ignoreCase = true)
        }

        if (district == null) {
            setCuacaReadOnly("Data cuaca tidak tersedia")
            return
        }

        if (updateLokasiDanKoordinat) {
            val currentLokasi = etLokasiBencana.text?.toString()?.trim().orEmpty()
            if (currentLokasi.isBlank()) {
                etLokasiBencana.setText("Kec. ${district.name}")
            } else if (!currentLokasi.contains(district.name, ignoreCase = true)) {
                etLokasiBencana.setText("$currentLokasi, Kec. ${district.name}")
            }
            if (etKoordinat.text?.toString()?.isBlank() != false) {
                etKoordinat.setText("${district.latitude}, ${district.longitude}")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val formattedWeather = try {
                val cached = database.weatherDao().getCachedWeather(district.id)
                if (cached != null) {
                    val tempInt = cached.temperature.roundToInt()
                    val condition = WeatherCodeHelper.getWeatherInfo(cached.weatherCode).conditionText
                    "${tempInt}°C, $condition"
                } else {
                    val report = weatherRepository.fetchWeatherReport(district)
                    val tempInt = report.current.temperature.roundToInt()
                    val condition = WeatherCodeHelper.getWeatherInfo(report.current.weatherCode).conditionText
                    "${tempInt}°C, $condition"
                }
            } catch (_: Exception) {
                "Data cuaca tidak tersedia"
            }

            withContext(Dispatchers.Main) {
                setCuacaReadOnly(formattedWeather)
            }
        }
    }

    private fun setCuacaReadOnly(weatherText: String) {
        val safeWeather = weatherText.trim().ifBlank { "Data cuaca tidak tersedia" }
        cuacaSaatKejadianTerkini = safeWeather
        if (::dropdownCuaca.isInitialized) {
            dropdownCuaca.setText(safeWeather, false)
        }
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            kondisiCuaca = safeWeather,
            cuacaSaatKejadian = safeWeather
        )
    }

    private fun setupDateTimePickers() {
        etTanggalKejadian.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                etTanggalKejadian.setText(formattedDate)
            }, year, month, day)
            datePickerDialog.show()
        }

        etWaktuKejadian.setOnClickListener {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d WIB", selectedHour, selectedMinute)
                etWaktuKejadian.setText(formattedTime)
            }, hour, minute, true)
            timePickerDialog.show()
        }
    }

    // Public getters untuk data binding ke LaporanHarian jika dibutuhkan
    fun getJenisBencana(): String = dropdownJenisBencana.text.toString()
    fun getCuaca(): String = cuacaSaatKejadianTerkini
    fun getTanggalKejadian(): String = etTanggalKejadian.text.toString()
    fun getWaktuKejadian(): String = etWaktuKejadian.text.toString()
    fun getLokasi(): String = etLokasiBencana.text.toString()
    fun getKoordinat(): String = etKoordinat.text.toString()

    private fun setupRedNotificationBadgeStyle() {
        val badgeView = tvBadgeDariWarga ?: return
        val density = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(AndroidColor.parseColor("#D32F2F"))
            setStroke((1.5f * density).toInt().coerceAtLeast(2), AndroidColor.WHITE)
        }
        badgeView.background = bg
    }

    private fun refreshIncomingCitizenReportsAndBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                context = requireContext(),
                database = database
            )
            cachedIncomingReports = loaded
            val unreadCount = loaded.count { it.isUnreadOrUnprocessed }
            tvBadgeDariWarga?.apply {
                if (unreadCount > 0) {
                    text = if (unreadCount > 99) "99+" else unreadCount.toString()
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun tampilkanModalDaftarLaporanWarga() {
        if (!isAdded) return
        val ctx = requireContext()
        val bottomSheetDialog = BottomSheetDialog(ctx)
        val composeView = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val latestReports = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                context = ctx,
                database = database
            )
            cachedIncomingReports = latestReports
            val unreadCount = latestReports.count { it.isUnreadOrUnprocessed }
            tvBadgeDariWarga?.apply {
                if (unreadCount > 0) {
                    text = if (unreadCount > 99) "99+" else unreadCount.toString()
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            composeView.setContent {
                MaterialTheme {
                    var reportsState by remember { mutableStateOf(latestReports) }
                    var isLoadingState by remember { mutableStateOf(false) }
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ) {
                        CitizenIncomingReportsSheetContent(
                            reports = reportsState,
                            isLoading = isLoadingState,
                            onDismiss = { bottomSheetDialog.dismiss() },
                            onRefresh = {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    isLoadingState = true
                                    reportsState = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                                        context = ctx,
                                        database = database
                                    )
                                    isLoadingState = false
                                }
                            },
                            onUseReportData = { selectedItem: IncomingCitizenReportItem ->
                                bottomSheetDialog.dismiss()
                                gunakanDataLaporanWargaTerpilih(selectedItem)
                            }
                        )
                    }
                }
            }

            bottomSheetDialog.setContentView(composeView)
            bottomSheetDialog.behavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
            bottomSheetDialog.show()
        }
    }

    private fun gunakanDataLaporanWargaTerpilih(selectedItem: IncomingCitizenReportItem) {
        val switchKondusif = view?.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchKondusif)
        if (switchKondusif?.isChecked == true) {
            switchKondusif.isChecked = false
        }
        etTanggalKejadian.isEnabled = true
        etKedalaman.isEnabled = true

        // 1. Autofill Kolom-kolom di "Informasi Dasar Kejadian"
        dropdownJenisBencana.setText(selectedItem.disasterType, false)
        etTanggalKejadian.setText(
            selectedItem.dateText.ifBlank {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            }
        )
        etWaktuKejadian.setText(selectedItem.timeText.ifBlank { "08:30 WIB" })
        val fullLokasi = selectedItem.fullFormattedLocation()
        etLokasiBencana.setText(fullLokasi)
        if (selectedItem.coordinates.isNotBlank()) {
            etKoordinat.setText(selectedItem.coordinates)
        }

        val namaKec = selectedItem.district.ifBlank {
            MukomukoDistricts.list.firstOrNull {
                fullLokasi.contains(it.name, ignoreCase = true)
            }?.name.orEmpty()
        }
        if (namaKec.isNotBlank()) {
            dropdownKecamatan.setText(namaKec, false)
            muatCuacaKecamatanOtomatis(namaKec, updateLokasiDanKoordinat = false)
        } else {
            setCuacaReadOnly("Data cuaca tidak tersedia")
        }

        etCakupanDampak.setText("$fullLokasi — ${selectedItem.description}")
        etKondisiMutakhir.setText(selectedItem.description)
        etSumberInformasi.setText("Laporan Warga (${selectedItem.reporterName} - ${selectedItem.reporterPhone})")
        if (etUpayaDilakukan.text.isNullOrBlank()) {
            etUpayaDilakukan.setText("Tim Reaksi Cepat (TRC) Pusdalops BPBD Mukomuko melakukan verifikasi lapangan dan kaji cepat atas laporan warga.")
        }

        // Sinkronisasi ke ViewModel (termasuk jumlah korban dari warga)
        simpanDataKeViewModel()
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            korbanMeninggal = selectedItem.casualtiesDead,
            korbanLuka = selectedItem.casualtiesInjured
        )

        // 2. Tampilkan preview foto & ringkasan laporan warga yang dipakai di bawah tombol
        tampilkanPreviewLaporanWargaTerlampir(selectedItem)

        // 3. Tandai laporan sudah diproses oleh petugas & perbarui badge notifikasi merah
        viewLifecycleOwner.lifecycleScope.launch {
            CitizenIncomingReportsProvider.markReportAsUsedByOfficer(
                context = requireContext(),
                database = database,
                report = selectedItem
            )
            refreshIncomingCitizenReportsAndBadge()
        }

        Toast.makeText(
            requireContext(),
            "Data laporan warga (${selectedItem.disasterType} - ${selectedItem.reporterName}) berhasil diisi ke formulir!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun tampilkanPreviewLaporanWargaTerlampir(item: IncomingCitizenReportItem) {
        val previewContainer = composeAttachedCitizenPreview ?: return
        previewContainer.visibility = View.VISIBLE
        previewContainer.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        previewContainer.setContent {
            MaterialTheme {
                var showZoomDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F8E9))
                        .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CitizenReportPhotoThumbnail(
                        photoUrl = item.photoUrl,
                        fallbackDrawableRes = item.fallbackDrawableRes,
                        contentDescription = "Foto laporan ${item.reporterName}",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showZoomDialog = true }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Autofill Aktif dari Laporan Warga",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "${item.reporterName} • ${item.disasterType}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.dateText}, ${item.timeText} • Ketuk foto untuk perbesar",
                            fontSize = 11.sp,
                            color = Color(0xFF4E6E52),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showZoomDialog) {
                    CitizenReportFullPhotoDialog(
                        report = item,
                        onDismiss = { showZoomDialog = false }
                    )
                }
            }
        }
    }

    override fun onPause() {
        if (::dropdownJenisBencana.isInitialized) {
            dropdownJenisBencana.dismissDropDown()
        }
        if (::dropdownKecamatan.isInitialized) {
            dropdownKecamatan.dismissDropDown()
        }
        super.onPause()
        // Simpan data sementara ke ViewModel saat petugas pindah tab
        if (::dropdownJenisBencana.isInitialized) {
            simpanDataKeViewModel()
        }
    }

    private fun simpanDataKeViewModel() {
        val kecTerpilih = if (::dropdownKecamatan.isInitialized) dropdownKecamatan.text.toString().trim() else ""
        val lokasiInput = etLokasiBencana.text.toString().trim()
        val lokasiGabungan = when {
            lokasiInput.isNotBlank() && kecTerpilih.isNotBlank() && !lokasiInput.contains(kecTerpilih, ignoreCase = true) ->
                "$lokasiInput, Kec. $kecTerpilih"
            lokasiInput.isBlank() && kecTerpilih.isNotBlank() ->
                "Kec. $kecTerpilih"
            else -> lokasiInput
        }
        val cuacaFinal = cuacaSaatKejadianTerkini.ifBlank { "Data cuaca tidak tersedia" }
        val prev = sharedViewModel.laporanSementara

        val rawKoordinat = etKoordinat.text?.toString()?.trim().orEmpty()
        val jenisBencanaVal = dropdownJenisBencana.text.toString().ifBlank { prev.jenisBencana }
        val isKondusif = jenisBencanaVal.contains("Kondusif", ignoreCase = true) ||
            jenisBencanaVal.contains("Tidak Ada", ignoreCase = true)
        if (rawKoordinat.isNotEmpty()) {
            val coordCheck = com.example.util.CoordinateValidator.validateKoordinatString(
                koordinat = rawKoordinat,
                allowKondusifDash = isKondusif
            )
            etKoordinat.error = coordCheck.exceptionOrNull()?.message
        } else {
            etKoordinat.error = null
        }

        sharedViewModel.laporanSementara = prev.copy(
            jenisBencana = jenisBencanaVal,
            tanggalKejadian = etTanggalKejadian.text.toString().ifBlank { prev.tanggalKejadian },
            waktuKejadian = etWaktuKejadian.text.toString().ifBlank { prev.waktuKejadian },
            lokasiBencana = lokasiGabungan.ifBlank { prev.lokasiBencana },
            koordinat = rawKoordinat.ifBlank { prev.koordinat },
            kedalaman = etKedalaman.text.toString().ifBlank { prev.kedalaman },
            kondisiCuaca = cuacaFinal,
            cuacaSaatKejadian = cuacaFinal,
            cakupanDampak = etCakupanDampak.text?.toString()?.trim()?.ifBlank { prev.cakupanDampak } ?: prev.cakupanDampak,
            upayaDilakukan = etUpayaDilakukan.text?.toString()?.trim()?.ifBlank { prev.upayaDilakukan } ?: prev.upayaDilakukan,
            kondisiMutakhir = etKondisiMutakhir.text?.toString()?.trim()?.ifBlank { prev.kondisiMutakhir } ?: prev.kondisiMutakhir,
            kerusakan = etKerusakan.text?.toString()?.trim()?.ifBlank { prev.kerusakan } ?: prev.kerusakan,
            penyebab = etPenyebab.text?.toString()?.trim()?.ifBlank { prev.penyebab } ?: prev.penyebab,
            sumberInformasi = etSumberInformasi.text?.toString()?.trim()?.ifBlank { prev.sumberInformasi } ?: prev.sumberInformasi
        )
    }

    override fun onDestroyView() {
        if (::dropdownJenisBencana.isInitialized) {
            dropdownJenisBencana.dismissDropDown()
        }
        if (::dropdownKecamatan.isInitialized) {
            dropdownKecamatan.dismissDropDown()
        }
        super.onDestroyView()
    }
}

