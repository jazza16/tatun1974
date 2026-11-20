package com.example.ui.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.data.local.LogKomunikasiEntity
import com.example.ui.components.formDoneKeyboardOptions
import com.example.ui.components.formNextKeyboardOptions
import com.example.ui.components.formScrollPadding
import com.example.ui.components.rememberFormKeyboardActions

/**
 * Fragment untuk Tab 3: Log BPBD & Upaya Penanganan.
 */
class FormTab3Fragment : Fragment() {
    private val sharedViewModel: LaporanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    FormTab3Content(
                        sharedViewModel = sharedViewModel,
                        onTambahLogClick = { waktuInput, fromInput, toInput, subjekInput ->
                            tambahLogBaru(
                                waktuInput = waktuInput,
                                fromInput = fromInput,
                                toInput = toInput,
                                subjekInput = subjekInput
                            )
                        }
                    )
                }
            }
        }
    }

    // Fungsi saat tombol "Tambah Log" di klik:
    private fun tambahLogBaru(
        waktuInput: String = java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).format(java.util.Date()),
        fromInput: String = "Pusdalops-PB BPBD Mukomuko",
        toInput: String = "BPBD Provinsi",
        subjekInput: String = "Penyampaian berita cuaca..."
    ) {
        val currentTimeFormatted = java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).format(java.util.Date())
        val logBaru = LogKomunikasiEntity(
            laporanId = 0, // Nanti akan diisi otomatis oleh Database
            waktu = waktuInput.ifBlank { currentTimeFormatted }, // Ambil dari EditText form log atau waktu perangkat otomatis
            stationIdFrom = fromInput.ifBlank { "Pusdalops-PB BPBD Mukomuko" },
            stationIdTo = toInput.ifBlank { "BPBD Provinsi" },
            subjek = subjekInput.ifBlank { "Penyampaian berita cuaca..." },
            operator = sharedViewModel.laporanSementara.petugasJaga // Otomatis dari ViewModel
        )

        // Tambahkan ke list sementara di ViewModel
        sharedViewModel.listLogSementara.add(logBaru)

        Toast.makeText(requireContext(), "Log komunikasi berhasil ditambahkan", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FormTab3Content(
    sharedViewModel: LaporanViewModel? = null,
    onTambahLogClick: (waktu: String, from: String, to: String, subjek: String) -> Unit = { _, _, _, _ -> }
) {
    val keyboardActions = rememberFormKeyboardActions()
    val currentDeviceTime = remember {
        java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).format(java.util.Date())
    }
    var waktuLog by remember { mutableStateOf(currentDeviceTime) }
    var stationFrom by remember { mutableStateOf("Pusdalops-PB BPBD Mukomuko") }
    var stationTo by remember { mutableStateOf("BPBD Provinsi") }
    var subjekLog by remember { mutableStateOf("Penyampaian berita cuaca dan situasi kondusif Kabupaten Mukomuko") }

    val daftarLogUi = remember {
        mutableStateListOf<LogKomunikasiEntity>().apply {
            sharedViewModel?.listLogSementara?.let { addAll(it) }
        }
    }

    var upayaDilakukan by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.upayaDilakukan?.ifBlank {
                "TRC BPBD Mukomuko melakukan kaji cepat (rapid assessment), monitoring pesisir Muara Manjunto, dan koordinasi dengan pos ronda desa."
            } ?: "TRC BPBD Mukomuko melakukan kaji cepat (rapid assessment), monitoring pesisir Muara Manjunto, dan koordinasi dengan pos ronda desa."
        )
    }
    var kondisiMutakhir by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.kondisiMutakhir?.ifBlank {
                "Situasi kondusif, air pasang telah surut, warga kembali beraktivitas melaut dengan waspada."
            } ?: "Situasi kondusif, air pasang telah surut, warga kembali beraktivitas melaut dengan waspada."
        )
    }
    var sumberInformasi by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.sumberInformasi?.ifBlank {
                "BMKG Stasiun Geofisika Kepahiang & Laporan Camat Kota Mukomuko"
            } ?: "BMKG Stasiun Geofisika Kepahiang & Laporan Camat Kota Mukomuko"
        )
    }

    androidx.compose.runtime.LaunchedEffect(upayaDilakukan, kondisiMutakhir, sumberInformasi) {
        sharedViewModel?.let { vm ->
            vm.laporanSementara = vm.laporanSementara.copy(
                upayaDilakukan = upayaDilakukan,
                kondisiMutakhir = kondisiMutakhir,
                sumberInformasi = sumberInformasi
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // KARTU LOG KOMUNIKASI BPBD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Log Komunikasi Radio / Pusdalops BPBD",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                val activeLaporan = sharedViewModel?.laporanSementara
                val opName = activeLaporan?.operatorCanonicalName ?: "HITATUN ARAZAK"
                val opNip = activeLaporan?.operatorNip ?: "197401282006041006"
                Text(
                    text = "Operator Jaga: $opName (NIP. $opNip)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )

                OutlinedTextField(
                    value = waktuLog,
                    onValueChange = { waktuLog = it },
                    label = { Text("Waktu Komunikasi (Contoh: 08.20 WIB)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stationFrom,
                    onValueChange = { stationFrom = it },
                    label = { Text("Dari Stasiun (Station ID From)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stationTo,
                    onValueChange = { stationTo = it },
                    label = { Text("Ke Stasiun (Station ID To)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subjekLog,
                    onValueChange = { subjekLog = it },
                    label = { Text("Subjek / Isi Berita") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                Button(
                    onClick = {
                        onTambahLogClick(waktuLog, stationFrom, stationTo, subjekLog)
                        daftarLogUi.clear()
                        sharedViewModel?.listLogSementara?.let { daftarLogUi.addAll(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+ TAMBAH LOG KOMUNIKASI",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (daftarLogUi.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Daftar Log Sementara (${daftarLogUi.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    daftarLogUi.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#${index + 1} • ${item.waktu}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "Op: ${item.operatorNama} (NIP. ${item.operatorNip})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF616161)
                                )
                            }
                            Text(
                                text = "${item.stationIdFrom} → ${item.stationIdTo}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF212121)
                            )
                            Text(
                                text = item.subjek,
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            }
        }

        // KARTU UPAYA PENANGANAN & KONDISI MUTAKHIR
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Upaya Penanganan & Kondisi Terkini",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )

                OutlinedTextField(
                    value = upayaDilakukan,
                    onValueChange = {
                        upayaDilakukan = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(upayaDilakukan = it)
                        }
                    },
                    label = { Text("Upaya Penanganan BPBD / TNI / Polri / Warga") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 4
                )

                OutlinedTextField(
                    value = kondisiMutakhir,
                    onValueChange = {
                        kondisiMutakhir = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(kondisiMutakhir = it)
                        }
                    },
                    label = { Text("Kondisi Mutakhir Lapangan") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = sumberInformasi,
                    onValueChange = {
                        sumberInformasi = it
                        sharedViewModel?.let { vm ->
                            vm.laporanSementara = vm.laporanSementara.copy(sumberInformasi = it)
                        }
                    },
                    label = { Text("Sumber Informasi Awal") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )
            }
        }
    }
}
