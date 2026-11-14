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
    private lateinit var btnAmbilGps: View

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
        btnAmbilGps = view.findViewById(R.id.btnAmbilGps)
        val btnTarik = view.findViewById<MaterialButton>(R.id.btnTarikLaporanWarga)
            ?: view.findViewById(R.id.btnImporLaporanWarga)
        btnTarikLaporanWarga = btnTarik
        btnImporLaporanWarga = btnTarik

        // Pastikan field cuaca bersifat read-only
        dropdownCuaca.keyListener = null
        dropdownCuaca.isFocusable = false
        dropdownCuaca.isClickable = false

        // Inisialisasi Info Sesi Petugas
        val sessionManager = SessionManager(requireContext())
        petugasAktifNama = sessionManager.getNamaPetugas().ifBlank { "HITATUN ARAZAK" }
        petugasAktifNip = sessionManager.getNipPetugas().ifBlank { "-" }
        val tvNamaPetugas = view.findViewById<android.widget.TextView>(R.id.tvNamaPetugas)
        val tvInfoPetugas = view.findViewById<android.widget.TextView>(R.id.tvInfoPetugas)
        tvNamaPetugas?.text = petugasAktifNama
        tvInfoPetugas?.text = "Diisi oleh: $petugasAktifNama"

        // Di dalam onViewCreated FormTab1Fragment:
        lifecycleScope.launch {
            val context = requireContext()
            context.dataStore.data.map { preferences ->
                val nama = preferences[NAMA_PETUGAS]
                    ?: sessionManager.getNamaPetugas().ifBlank { "HITATUN ARAZAK" }
                val nip = preferences[NIP_PETUGAS]
                    ?: sessionManager.getNipPetugas().ifBlank { "-" }

                petugasAktifNama = nama
                petugasAktifNip = nip

                tvNamaPetugas?.text = nama
                tvInfoPetugas?.text = "Diisi oleh: $nama"
            }.collect()
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
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val tanggalHariIni = sdf.format(Date())

            lifecycleScope.launch(Dispatchers.IO) {
                var laporanWargaList = database.laporanWargaDao().getLaporanWargaByDate(tanggalHariIni)
                if (laporanWargaList.isEmpty()) {
                    laporanWargaList = database.laporanWargaDao().getLaporanWargaByDate("09-11-2026")
                }
                if (laporanWargaList.isEmpty()) {
                    laporanWargaList = database.laporanWargaDao().getAllLaporanWargaSync()
                }

                withContext(Dispatchers.Main) {
                    if (laporanWargaList.isNotEmpty()) {
                        val laporanTerbaru = laporanWargaList[0]
                        isiFormDariLaporanWarga(laporanTerbaru)
                    } else {
                        Toast.makeText(requireContext(), "Tidak ada Laporan Warga masuk hari ini.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
            Toast.makeText(requireContext(), "Fitur GPS sedang disiapkan...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDropdowns() {
        // List Pilihan Bencana
        val listBencana = arrayOf(
            "Banjir",
            "Tanah Longsor",
            "Kebakaran Hutan dan Lahan",
            "Angin Puting Beliung",
            "Gempa Bumi",
            "Cuaca Ekstrem",
            "Tidak Ada Bencana (Kondusif)"
        )
        val adapterBencana = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listBencana)
        dropdownJenisBencana.setAdapter(adapterBencana)

        // List 15 Kecamatan Kabupaten Mukomuko
        val listKecamatan = MukomukoDistricts.list.map { it.name }.toTypedArray()
        val adapterKecamatan = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listKecamatan)
        dropdownKecamatan.setAdapter(adapterKecamatan)
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

    private fun isiFormDariLaporanWarga(laporan: com.example.data.local.LaporanWargaEntity) {
        dropdownJenisBencana.setText(laporan.jenisBencana, false)
        etTanggalKejadian.setText(laporan.tanggal)
        etWaktuKejadian.setText(if (laporan.waktu.isNotBlank()) laporan.waktu else "08:30 WIB")
        val detailLokasi = if (laporan.desa.isNotBlank() && laporan.kecamatan.isNotBlank()) {
            "${laporan.lokasi}, Desa ${laporan.desa}, Kec. ${laporan.kecamatan}"
        } else {
            laporan.lokasi
        }
        etLokasiBencana.setText(detailLokasi)
        if (laporan.koordinat.isNotBlank()) {
            etKoordinat.setText(laporan.koordinat)
        } else if (laporan.latitude != 0.0 && laporan.longitude != 0.0) {
            etKoordinat.setText("${laporan.latitude}, ${laporan.longitude}")
        }

        val namaKec = laporan.kecamatan.ifBlank {
            MukomukoDistricts.list.firstOrNull { detailLokasi.contains(it.name, ignoreCase = true) }?.name.orEmpty()
        }
        if (namaKec.isNotBlank()) {
            dropdownKecamatan.setText(namaKec, false)
            muatCuacaKecamatanOtomatis(namaKec, updateLokasiDanKoordinat = false)
        } else {
            setCuacaReadOnly("Data cuaca tidak tersedia")
        }

        Toast.makeText(
            requireContext(),
            "Data laporan warga (${laporan.jenisBencana} - ${laporan.namaPelapor}) berhasil ditarik ke form!",
            Toast.LENGTH_SHORT
        ).show()
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

        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            jenisBencana = dropdownJenisBencana.text.toString(),
            tanggalKejadian = etTanggalKejadian.text.toString(),
            waktuKejadian = etWaktuKejadian.text.toString(),
            lokasiBencana = lokasiGabungan,
            koordinat = etKoordinat.text.toString(),
            kedalaman = etKedalaman.text.toString(),
            kondisiCuaca = cuacaFinal,
            cuacaSaatKejadian = cuacaFinal
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

