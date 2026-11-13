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
import com.example.util.NAMA_PETUGAS
import com.example.util.NIP_PETUGAS
import com.example.util.SessionManager
import com.example.util.dataStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
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

class FormTab1Fragment : Fragment() {

    // Panggil ViewModel yang sama dengan Activity
    private val sharedViewModel: LaporanViewModel by activityViewModels()

    // Variabel global di Fragment untuk nanti disimpan ke Room
    var petugasAktifNama: String = "Petugas Pusdalops"
    var petugasAktifNip: String = "-"

    // Deklarasi komponen UI dan DAO
    private lateinit var database: AppDatabase
    private lateinit var laporanWargaDao: LaporanWargaDao
    private lateinit var btnTarikLaporanWarga: MaterialButton
    private lateinit var btnImporLaporanWarga: MaterialButton
    private lateinit var dropdownJenisBencana: AutoCompleteTextView
    private lateinit var dropdownCuaca: AutoCompleteTextView
    private lateinit var etTanggalKejadian: TextInputEditText
    private lateinit var etWaktuKejadian: TextInputEditText
    private lateinit var etLokasiBencana: TextInputEditText
    private lateinit var etKoordinat: TextInputEditText
    private lateinit var etKedalaman: TextInputEditText
    private lateinit var btnAmbilGps: View

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
        dropdownJenisBencana = view.findViewById(R.id.dropdownJenisBencana)
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
            // Ambil data dari DataStore Anda
            context.dataStore.data.map { preferences ->
                val nama = preferences[NAMA_PETUGAS]
                    ?: sessionManager.getNamaPetugas().ifBlank { "HITATUN ARAZAK" }
                val nip = preferences[NIP_PETUGAS]
                    ?: sessionManager.getNipPetugas().ifBlank { "-" }

                // Simpan ke variabel global di Fragment untuk nanti disimpan ke Room
                petugasAktifNama = nama
                petugasAktifNip = nip

                // Tampilkan di UI form
                tvNamaPetugas?.text = nama
                tvInfoPetugas?.text = "Diisi oleh: $nama"
            }.collect()
        }

        setupDropdowns()
        setupDateTimePickers()

        // Gunakan SwitchMaterial, BUKAN MaterialSwitch
        val switchKondusif = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchKondusif)
        // Ganti 'layoutFormUtama' dengan ID LinearLayout yang membungkus form (Dropdown Bencana, dll)
        // val layoutFormUtama = view.findViewById<LinearLayout>(R.id.layoutFormUtama)

        switchKondusif.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Otomatis isi "Tidak Ada Bencana" dan matikan form
                dropdownJenisBencana.setText("Tidak Ada Bencana (Kondusif)", false)
                dropdownCuaca.setText("Cerah", false) // Default cuaca

                // Sembunyikan atau nonaktifkan form yang tidak perlu
                etTanggalKejadian.setText("-")
                etWaktuKejadian.setText("-")
                etLokasiBencana.setText("Kab. Mukomuko")
                etKoordinat.setText("-")
                etKedalaman.setText("-")

                Toast.makeText(requireContext(), "Mode Kondusif Aktif", Toast.LENGTH_SHORT).show()
            } else {
                // Bersihkan form jika saklar dimatikan
                dropdownJenisBencana.setText("", false)
                dropdownCuaca.setText("", false)
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
                // Sembunyikan / nonaktifkan form kejadian
                switchKondusif?.isChecked = true
                etTanggalKejadian.isEnabled = false
                etKedalaman.isEnabled = false
                Toast.makeText(requireContext(), "Mode Kondusif diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                // Munculkan kembali form kejadian
                if (switchKondusif?.isChecked == true) {
                    switchKondusif.isChecked = false
                }
                etTanggalKejadian.isEnabled = true
                etKedalaman.isEnabled = true
            }
        }

        btnTarik.setOnClickListener {
            // 1. Dapatkan tanggal hari ini (format: "09-11-2026")
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val tanggalHariIni = sdf.format(Date())

            // 2. Panggil Room Database (gunakan Coroutine)
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
                        // Ambil laporan paling baru (index 0 karena diurutkan DESC)
                        val laporanTerbaru = laporanWargaList[0]

                        // 3. Isi otomatis ke form
                        etLokasiBencana.setText(laporanTerbaru.lokasi)
                        etKoordinat.setText(laporanTerbaru.koordinat)

                        // Isi field tambahan pada Tab 1 jika tersedia
                        dropdownJenisBencana.setText(laporanTerbaru.jenisBencana, false)
                        etTanggalKejadian.setText(laporanTerbaru.tanggal)
                        if (laporanTerbaru.waktu.isNotBlank()) {
                            etWaktuKejadian.setText(laporanTerbaru.waktu)
                        }

                        Toast.makeText(requireContext(), "Data ditarik dari Laporan Warga terbaru!", Toast.LENGTH_SHORT).show()
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
                        // Isi form dengan data kemarin
                        etLokasiBencana.setText(laporanKemarin.lokasiBencana)
                        etKoordinat.setText(laporanKemarin.koordinat)
                        etKedalaman.setText(laporanKemarin.kedalaman)
                        dropdownJenisBencana.setText(laporanKemarin.jenisBencana, false)
                        dropdownCuaca.setText(laporanKemarin.kondisiCuaca, false)

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
            // Fungsi GPS akan kita buat di langkah selanjutnya
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

        // List Pilihan Cuaca
        val listCuaca = arrayOf(
            "Cerah",
            "Berawan",
            "Hujan Ringan",
            "Hujan Sedang",
            "Hujan Lebat",
            "Berkabut"
        )
        val adapterCuaca = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listCuaca)
        dropdownCuaca.setAdapter(adapterCuaca)
    }

    private fun setupDateTimePickers() {
        // DatePicker untuk Tanggal Kejadian
        etTanggalKejadian.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // Format: DD-MM-YYYY
                val formattedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                etTanggalKejadian.setText(formattedDate)
            }, year, month, day)
            datePickerDialog.show()
        }

        // TimePicker untuk Waktu Kejadian
        etWaktuKejadian.setOnClickListener {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                // Format: HH:mm WIB
                val formattedTime = String.format("%02d:%02d WIB", selectedHour, selectedMinute)
                etWaktuKejadian.setText(formattedTime)
            }, hour, minute, true)
            timePickerDialog.show()
        }
    }

    // Public getters untuk data binding ke LaporanHarian jika dibutuhkan
    fun getJenisBencana(): String = dropdownJenisBencana.text.toString()
    fun getCuaca(): String = dropdownCuaca.text.toString()
    fun getTanggalKejadian(): String = etTanggalKejadian.text.toString()
    fun getWaktuKejadian(): String = etWaktuKejadian.text.toString()
    fun getLokasi(): String = etLokasiBencana.text.toString()
    fun getKoordinat(): String = etKoordinat.text.toString()

    private fun imporLaporanWargaHariIni() {
        val db = com.example.data.local.AppDatabase.getInstance(requireContext())
        val tanggalHariIni = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val tanggalAlt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var list = db.laporanWargaDao().getLaporanHariIni(tanggalHariIni)
            if (list.isEmpty()) {
                list = db.laporanWargaDao().getLaporanHariIni(tanggalAlt)
            }
            if (list.isEmpty()) {
                list = db.laporanWargaDao().getAllLaporanWargaSync()
            }

            withContext(Dispatchers.Main) {
                if (list.isNotEmpty()) {
                    if (list.size == 1) {
                        isiFormDariLaporanWarga(list[0])
                    } else {
                        val items = list.map { "${it.jenisBencana} • ${it.lokasi} (${it.namaPelapor})" }.toTypedArray()
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Pilih Laporan Warga Hari Ini")
                            .setItems(items) { _, which ->
                                isiFormDariLaporanWarga(list[which])
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Belum ada data laporan warga untuk tanggal $tanggalHariIni",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

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
        if (laporan.latitude != 0.0 && laporan.longitude != 0.0) {
            etKoordinat.setText("${laporan.latitude}, ${laporan.longitude}")
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
        if (::dropdownCuaca.isInitialized) {
            dropdownCuaca.dismissDropDown()
        }
        super.onPause()
        // Simpan data sementara ke ViewModel saat petugas pindah tab
        if (::dropdownJenisBencana.isInitialized) {
            simpanDataKeViewModel()
        }
    }

    private fun simpanDataKeViewModel() {
        sharedViewModel.laporanSementara = sharedViewModel.laporanSementara.copy(
            jenisBencana = dropdownJenisBencana.text.toString(),
            tanggalKejadian = etTanggalKejadian.text.toString(),
            waktuKejadian = etWaktuKejadian.text.toString(),
            lokasiBencana = etLokasiBencana.text.toString(),
            koordinat = etKoordinat.text.toString(),
            kedalaman = etKedalaman.text.toString(),
            kondisiCuaca = dropdownCuaca.text.toString()
        )
    }

    override fun onDestroyView() {
        if (::dropdownJenisBencana.isInitialized) {
            dropdownJenisBencana.dismissDropDown()
        }
        if (::dropdownCuaca.isInitialized) {
            dropdownCuaca.dismissDropDown()
        }
        super.onDestroyView()
    }
}
