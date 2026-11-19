package com.example.ui.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.data.local.DistribusiLogistikEntity
import com.example.data.local.LogKomunikasi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment untuk Tab 4: Administrasi Stasiun, Distribusi Logistik & Simpan Laporan.
 */
class FormTab4Fragment : Fragment() {
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
                    FormTab4Content(sharedViewModel = sharedViewModel)
                }
            }
        }
    }
}

@Composable
fun FormTab4Content(sharedViewModel: LaporanViewModel? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val officerAuthManager = remember { com.example.data.local.OfficerAuthManager(context) }
    val session = remember { com.example.util.SessionManager(context) }
    val verifiedUser = remember { session.authenticateToken() }
    val resolvedOfficerName = verifiedUser.name
    val resolvedOfficerNip = verifiedUser.nip
    val defaultDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var tanggalSurat by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.tanggalSurat?.ifBlank { defaultDate } ?: defaultDate
        )
    }
    var tanggalTerima by remember {
        mutableStateOf(
            sharedViewModel?.laporanSementara?.tanggalTerima?.ifBlank { defaultDate } ?: defaultDate
        )
    }
    var operatorName by remember {
        mutableStateOf(verifiedUser.name)
    }
    var stationId by remember {
        mutableStateOf(verifiedUser.stationId)
    }

    LaunchedEffect(Unit) {
        sharedViewModel?.let { vm ->
            vm.laporanSementara = vm.laporanSementara.copy(
                petugasJaga = operatorName,
                stationId = stationId
            )
        }
        session.namaPetugasFlow.collect { sessionName ->
            val activeName = officerAuthManager.getOfficerName().ifBlank { sessionName }
            if (activeName.isNotBlank()) {
                operatorName = activeName
                sharedViewModel?.let { vm ->
                    vm.laporanSementara = vm.laporanSementara.copy(petugasJaga = activeName)
                }
            }
        }
    }

    val logsList = remember {
        mutableStateListOf<LogKomunikasi>().apply {
            val vmLogs = sharedViewModel?.listLogSementara
            if (!vmLogs.isNullOrEmpty()) {
                addAll(vmLogs)
            } else {
                add(
                    LogKomunikasi(
                        laporanId = 0,
                        waktu = SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault()).format(Date()),
                        stationIdFrom = "PUSDALOPS-MM-01",
                        stationIdTo = "Kalaksa BPBD Mukomuko",
                        subjek = "Laporan awal kejadian disampaikan. Tidak terdeteksi potensi tsunami.",
                        operatorNama = resolvedOfficerName,
                        operatorNip = resolvedOfficerNip
                    )
                )
            }
        }
    }

    // State daftar Distribusi Logistik / Aset Bantuan
    val logistikList = remember {
        mutableStateListOf<DistribusiLogistikEntity>().apply {
            val vmLogistik = sharedViewModel?.listLogistikSementara
            if (!vmLogistik.isNullOrEmpty()) {
                addAll(vmLogistik)
            }
        }
    }
    var namaBarangInput by remember { mutableStateOf("") }
    var jumlahInput by remember { mutableStateOf("") }
    var satuanInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var pendingPdfBytesToSave by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val bytes = pendingPdfBytesToSave
        if (result.resultCode == Activity.RESULT_OK && uri != null && bytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                } ?: throw IllegalStateException("OutputStream null")
                Toast.makeText(context, "Laporan berhasil disimpan", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
        }
        pendingPdfBytesToSave = null
    }

    fun savePdfToDevice() {
        val currentTokenUser = session.authenticateToken()
        val baseLaporan = sharedViewModel?.laporanSementara ?: com.example.data.local.LaporanHarianEntity(
            tanggalSurat = tanggalSurat,
            tanggalTerima = tanggalTerima,
            operatorName = currentTokenUser.name,
            stationId = currentTokenUser.stationId
        )
        val laporan = baseLaporan.copy(
            petugasJaga = currentTokenUser.name,
            stationId = currentTokenUser.stationId
        )
        val listLog = if (!sharedViewModel?.listLogSementara.isNullOrEmpty()) {
            sharedViewModel?.listLogSementara ?: emptyList()
        } else {
            logsList.toList()
        }
        val listLogistik = if (!sharedViewModel?.listLogistikSementara.isNullOrEmpty()) {
            sharedViewModel?.listLogistikSementara ?: emptyList()
        } else {
            logistikList.toList()
        }
        val cleanDate = laporan.tanggalKejadian
            .replace("/", "-")
            .replace(" ", "_")
            .ifBlank { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
        val defaultFileName = "Laporan_BPBD_$cleanDate.pdf"

        coroutineScope.launch {
            try {
                val pdfBytes = com.example.util.PdfGenerator.generateLaporanPdfBytes(
                    context = context,
                    laporan = laporan,
                    listLog = listLog,
                    listLogistik = listLogistik
                )
                pendingPdfBytesToSave = pdfBytes
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, defaultFileName)
                }
                savePdfLauncher.launch(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tab 4: Administrasi Stasiun & Operator",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tanggalSurat,
                        onValueChange = {
                            tanggalSurat = it
                            sharedViewModel?.let { vm ->
                                vm.laporanSementara = vm.laporanSementara.copy(tanggalSurat = it)
                            }
                        },
                        label = { Text("Tanggal Surat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tanggalTerima,
                        onValueChange = {
                            tanggalTerima = it
                            sharedViewModel?.let { vm ->
                                vm.laporanSementara = vm.laporanSementara.copy(tanggalTerima = it)
                            }
                        },
                        label = { Text("Tanggal Terima") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                val syncedOperatorProfile = remember(operatorName) {
                    officerAuthManager.resolveSyncedOfficer(rawName = operatorName, rawNip = resolvedOfficerNip)
                }
                OutlinedTextField(
                    value = "${syncedOperatorProfile.fullName} (NIP. ${syncedOperatorProfile.nip})",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Nama & NIP Operator Pusdalops") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci otomatis dari sesi login",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledTextColor = Color(0xFF374151),
                        disabledLabelColor = Color(0xFF6B7280),
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledTrailingIconColor = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_operator_pusdalops"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stationId,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("ID Stasiun / Pos Komando") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci otomatis dari sesi stasiun",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledTextColor = Color(0xFF374151),
                        disabledLabelColor = Color(0xFF6B7280),
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledTrailingIconColor = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_station_id"),
                    singleLine = true
                )
            }
        }

        // Card Distribusi Logistik / Aset Bantuan (Form Dinamis)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Distribusi Logistik / Aset Bantuan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )
                Text(
                    text = "Input daftar bantuan/aset darurat untuk dicetak pada Halaman 2 PDF (${logistikList.size} item)",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )

                OutlinedTextField(
                    value = namaBarangInput,
                    onValueChange = { namaBarangInput = it },
                    label = { Text("Nama Barang (cth: Tenda Gulung, Beras, Air Mineral)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jumlahInput,
                        onValueChange = { jumlahInput = it },
                        label = { Text("Jumlah") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = satuanInput,
                        onValueChange = { satuanInput = it },
                        label = { Text("Satuan (Unit/Karung/Dus)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = keteranganInput,
                    onValueChange = { keteranganInput = it },
                    label = { Text("Keterangan (cth: Diserahkan ke Kecamatan X)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (namaBarangInput.isNotBlank()) {
                            val newItem = DistribusiLogistikEntity(
                                namaBarang = namaBarangInput.trim(),
                                jumlah = jumlahInput.trim().ifBlank { "1" },
                                satuan = satuanInput.trim().ifBlank { "Unit" },
                                keterangan = keteranganInput.trim().ifBlank { "-" }
                            )
                            logistikList.add(newItem)
                            sharedViewModel?.listLogistikSementara?.add(newItem)
                            namaBarangInput = ""
                            jumlahInput = ""
                            satuanInput = ""
                            keteranganInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TAMBAH BARANG LOGISTIK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (logistikList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    logistikList.forEachIndexed { idx, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${item.namaBarang} (${item.jumlah} ${item.satuan})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Keterangan: ${item.keterangan}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        logistikList.remove(item)
                                        sharedViewModel?.listLogistikSementara?.remove(item)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus Barang",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card Log Komunikasi
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Log Komunikasi Kedaruratan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        Text(text = "Relasi One-to-Many (${logsList.size} Riwayat)", fontSize = 11.sp, color = Color(0xFF6B7280))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                logsList.forEach { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${log.waktuKomunikasi} • ${log.saluran}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                                IconButton(
                                    onClick = { logsList.remove(log) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(text = "Tujuan: ${log.pihakDihubungi}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            Text(text = "Pesan: \"${log.pesan}\"", fontSize = 11.sp, color = Color(0xFF334155))
                            Text(text = "Petugas: ${log.operatorNama} (NIP: ${log.operatorNip})", fontSize = 11.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val currentTokenUser = session.authenticateToken()
                            val baseLaporan = sharedViewModel?.laporanSementara ?: com.example.data.local.LaporanHarianEntity(
                                tanggalSurat = tanggalSurat,
                                tanggalTerima = tanggalTerima,
                                operatorName = currentTokenUser.name,
                                stationId = currentTokenUser.stationId
                            )
                            val laporan = baseLaporan.copy(
                                petugasJaga = currentTokenUser.name,
                                stationId = currentTokenUser.stationId
                            )
                            val listLog = if (!sharedViewModel?.listLogSementara.isNullOrEmpty()) {
                                sharedViewModel?.listLogSementara ?: emptyList()
                            } else {
                                logsList.toList()
                            }
                            val listLogistik = if (!sharedViewModel?.listLogistikSementara.isNullOrEmpty()) {
                                sharedViewModel?.listLogistikSementara ?: emptyList()
                            } else {
                                logistikList.toList()
                            }
                            com.example.util.PdfGenerator.cetakLaporanKePdf(
                                context = context,
                                laporan = laporan,
                                listLog = listLog,
                                listLogistik = listLogistik
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cetak_pdf_tab4"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cetak PDF",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = {
                            savePdfToDevice()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_simpan_pdf_tab4"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Simpan PDF",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan PDF",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

