package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.PoskoLogisticCommodity
import com.example.data.repository.PoskoLogisticsRepository
import com.example.data.repository.PoskoRefugeeUnit
import com.example.util.PoskoStockOpnameExporter

private val CommandNavy = Color(0xFF0F172A)
private val CommandCardBg = Color(0xFF1E293B)
private val CriticalRed = Color(0xFFDC2626)
private val CriticalRedBg = Color(0xFF450A0A)
private val SafeGreen = Color(0xFF16A34A)
private val SafeGreenBg = Color(0xFF052E16)
private val WarningAmber = Color(0xFFD97706)
private val BpbdOrange = Color(0xFFEA580C)

/**
 * Layar Utama Modul Manajemen Posko, Pengungsi, & Logistik Darurat (Master PRD SIPAGAR KITA).
 * Mengimplementasikan:
 * 1. Pemantauan Stok Logistik Posko & Gudang dengan Indikator Kritis (< 10) & Kedaluwarsa FEFO.
 * 2. Form Input Pengungsi Terpilah dengan Validasi Matematika Ketat:
 *    (Lansia + Ibu Hamil + Balita + Disabilitas) <= Total Jiwa.
 * 3. Distribusi Bantuan Berbasis NIK 16 Digit (Primary Key Anti-Data Ganda) & Snapshot ke DaLA.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoskoLogisticsManagementScreen(
    officerName: String,
    officerNip: String,
    officerRole: String,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val repository = remember { PoskoLogisticsRepository.getInstance(context) }

    val poskoUnits by repository.poskoUnits.collectAsStateWithLifecycle()
    val logisticStocks by repository.logisticStocks.collectAsStateWithLifecycle()
    val distributionLedger by repository.distributionLedger.collectAsStateWithLifecycle()
    val dalaLockedSnapshot by repository.dalaLockedSnapshot.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingCommodity by remember { mutableStateOf<PoskoLogisticCommodity?>(null) }
    var showAddCommodityDialog by remember { mutableStateOf(false) }
    var showStockOpnameExportDialog by remember { mutableStateOf(false) }

    val totalAllRefugees = remember(poskoUnits) { poskoUnits.sumOf { it.totalJiwa } }
    val totalAllVulnerable = remember(poskoUnits) { poskoUnits.sumOf { it.totalKelompokRentan } }
    val criticalStocksCount = remember(logisticStocks) { logisticStocks.count { it.isCritical } }
    val fefoWarningCount = remember(logisticStocks) {
        logisticStocks.count { it.isExpired || it.isExpiringSoonFefo }
    }

    val adaptiveInsets = rememberAdaptiveSystemBarInsets()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CommandNavy,
        bottomBar = {
            // Menjamin ruang aman di bagian bawah layar baik untuk Navigasi 3 Tombol maupun Navigasi Penuh/Usap (Gesture)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (adaptiveInsets.isThreeButtonNavigation) 16.dp else 8.dp)
            )
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090E17),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_close_posko_logistics")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Konsol Pusdalops",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "MANAJEMEN POSKO & LOGISTIK",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "RBAC: $officerRole • ${officerName.ifBlank { "Petugas Posko" }}",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showStockOpnameExportDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_top_export_stock_opname")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Unduh Laporan Stock Opname BMN/BMD",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    Button(
                        onClick = {
                            val locked = repository.captureAndLockDalaSnapshot(officerName)
                            Toast.makeText(
                                context,
                                "Snapshot Posko (${locked.totalJiwa} Jiwa) berhasil dikunci untuk Modul DaLA!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = 38.dp)
                            .testTag("btn_lock_posko_snapshot_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kunci Snapshot Posko untuk DaLA",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Snapshot DaLA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // SUMMARY CARDS (TOTAL PENGUNGSI, LOGISTIK KRITIS < 10, & FEFO KEDALUWARSA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Pengungsi & Kelompok Rentan
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CommandCardBg),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_summary_total_refugees")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOTAL PENGUNGSI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalAllRefugees Jiwa",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Rentan: $totalAllVulnerable Jiwa (${poskoUnits.size} Posko)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                // Card 2: Stok Logistik Kritis (< 10) & FEFO
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (criticalStocksCount > 0) CriticalRedBg else SafeGreenBg
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (criticalStocksCount > 0) CriticalRed else SafeGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_summary_critical_logistics")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (criticalStocksCount > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOGISTIK KRITIS (<10)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$criticalStocksCount Komoditas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (criticalStocksCount > 0) Color(0xFFFEE2E2) else Color.White
                        )
                        Text(
                            text = if (fefoWarningCount > 0) {
                                "FEFO Waspada Exp: $fefoWarningCount Item"
                            } else {
                                "Semua Masa Exp Aman"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE047)
                        )
                    }
                }
            }

            // SUB-TAB NAVIGATION
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF090E17),
                contentColor = Color(0xFFFBBF24),
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_posko_logistics_stock"),
                    text = {
                        Text(
                            text = "1. Stok Logistik & FEFO ($criticalStocksCount Kritis)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_posko_refugee_validation"),
                    text = {
                        Text(
                            text = "2. Pengungsi Posko (Validasi)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("tab_posko_nik_distribution"),
                    text = {
                        Text(
                            text = "3. Distribusi Bantuan (NIK 16 Digit)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> PoskoLogisticsStockTab(
                    logisticStocks = logisticStocks,
                    onEditStock = { editingCommodity = it },
                    onAddCommodityClick = { showAddCommodityDialog = true },
                    onDownloadStockOpnameClick = { showStockOpnameExportDialog = true }
                )
                1 -> PoskoRefugeeValidationTab(
                    poskoUnits = poskoUnits,
                    dalaLockedSnapshot = dalaLockedSnapshot,
                    officerName = officerName,
                    onValidateAndSave = { poskoId, total, lansia, bumil, balita, disabilitas ->
                        repository.validateAndUpdatePoskoRefugees(
                            poskoId = poskoId,
                            totalJiwa = total,
                            lansia = lansia,
                            ibuHamil = bumil,
                            balita = balita,
                            disabilitas = disabilitas,
                            officerName = officerName
                        )
                    },
                    onCaptureSnapshotForDala = {
                        repository.captureAndLockDalaSnapshot(officerName)
                    }
                )
                2 -> PoskoNikAidDistributionTab(
                    logisticStocks = logisticStocks,
                    poskoUnits = poskoUnits,
                    distributionLedger = distributionLedger,
                    officerName = officerName,
                    onDistribute = { nik, kk, name, vuln, commodityCode, qty, poskoCode ->
                        repository.distributeLogisticToNik(
                            recipientNik = nik,
                            recipientNoKk = kk,
                            recipientName = name,
                            vulnerabilityGroup = vuln,
                            commodityCode = commodityCode,
                            quantity = qty,
                            poskoCode = poskoCode,
                            officerName = officerName
                        )
                    }
                )
            }
        }
    }

    // DIALOG RESMI UNDUH BERITA ACARA STOCK OPNAME LOGISTIK & PERTANGGUNGJAWABAN BMN/BMD (PDF & CSV/EXCEL)
    if (showStockOpnameExportDialog) {
        val purposeOptions = PoskoStockOpnameExporter.OFFICIAL_REPORT_PURPOSES
        var selectedPurpose by remember { mutableStateOf(purposeOptions.first()) }

        AlertDialog(
            onDismissRequest = { showStockOpnameExportDialog = false },
            containerColor = CommandCardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Column {
                    Text(
                        text = "Unduh Laporan Stock Opname & BMN/BMD",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tertib Administrasi Pemerintah & Tata Kelola Aset Kebencanaan BPBD",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih tujuan pelaporan administrasi pemerintah untuk dicantumkan pada Berita Acara Stock Opname (${logisticStocks.size} Komoditas Logistik, ${poskoUnits.size} Posko Aktif, & ${distributionLedger.size} Mutasi Distribusi NIK 16 Digit):",
                        fontSize = 11.5.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    purposeOptions.forEach { purpose ->
                        val isSelected = purpose == selectedPurpose
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF064E3B) else Color(0xFF0F172A),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPurpose = purpose }
                        ) {
                            Text(
                                text = purpose,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showStockOpnameExportDialog = false
                                PoskoStockOpnameExporter.exportStockOpnameToPdf(
                                    context = context,
                                    officerName = officerName,
                                    officerNip = officerNip,
                                    officerRole = officerRole,
                                    documentPurpose = selectedPurpose
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("btn_confirm_export_stock_opname_pdf")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Unduh PDF Resmi",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                showStockOpnameExportDialog = false
                                PoskoStockOpnameExporter.exportStockOpnameToCsv(
                                    context = context,
                                    officerName = officerName,
                                    officerNip = officerNip,
                                    officerRole = officerRole,
                                    documentPurpose = selectedPurpose
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("btn_confirm_export_stock_opname_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Unduh CSV/Excel",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStockOpnameExportDialog = false }) {
                    Text("Tutup", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // DIALOG UPDATE STOK & TANGGAL KEDALUWARSA FEFO
    editingCommodity?.let { commodity ->
        var stockInput by remember(commodity) { mutableStateOf(commodity.currentStock.toString()) }
        var expiryInput by remember(commodity) { mutableStateOf(commodity.expiryDateIso.orEmpty()) }

        AlertDialog(
            onDismissRequest = { editingCommodity = null },
            containerColor = CommandCardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = "Update Stok & FEFO: ${commodity.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Kode: ${commodity.code} • Posko: ${commodity.poskoCode}\nCatatan PRD: Jika stok < 10, sistem otomatis menandai STATUS KRITIS (Merah).",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { raw -> stockInput = raw.filter { it.isDigit() } },
                        label = { Text("Jumlah Stok Tersedia (${commodity.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_logistic_stock")
                    )
                    OutlinedTextField(
                        value = expiryInput,
                        onValueChange = { expiryInput = it },
                        label = { Text("Tanggal Kedaluwarsa FEFO (YYYY-MM-DD)") },
                        placeholder = { Text("Contoh: 2027-06-30") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFFBBF24),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_logistic_expiry")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedStock = stockInput.toIntOrNull() ?: 0
                        repository.updateCommodityStock(
                            code = commodity.code,
                            newStock = parsedStock,
                            newExpiryDateIso = expiryInput.trim().ifEmpty { null }
                        )
                        editingCommodity = null
                        Toast.makeText(
                            context,
                            "Stok ${commodity.name} diperbarui menjadi $parsedStock ${commodity.unit}.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    modifier = Modifier.testTag("btn_confirm_update_stock")
                ) {
                    Text("Simpan Stok", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCommodity = null }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // DIALOG TAMBAH KOMODITAS LOGISTIK BARU
    if (showAddCommodityDialog) {
        var nameInput by remember { mutableStateOf("") }
        var unitInput by remember { mutableStateOf("Paket") }
        var stockInput by remember { mutableStateOf("25") }
        var expiryInput by remember { mutableStateOf("2027-06-30") }

        AlertDialog(
            onDismissRequest = { showAddCommodityDialog = false },
            containerColor = CommandCardBg,
            titleContentColor = Color.White,
            title = {
                Text(
                    text = "+ Tambah Komoditas Logistik Posko",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Barang Logistik") },
                        placeholder = { Text("Misal: Hygiene Kit Keluarga / Susu Balita") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitInput,
                            onValueChange = { unitInput = it },
                            label = { Text("Satuan") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = expiryInput,
                        onValueChange = { expiryInput = it },
                        label = { Text("Tgl Kedaluwarsa FEFO (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            repository.addCustomCommodity(
                                name = nameInput,
                                category = "LOGISTIK_POSKO",
                                unit = unitInput,
                                initialStock = stockInput.toIntOrNull() ?: 0,
                                expiryDateIso = expiryInput,
                                poskoCode = "POSKO-MM-001"
                            )
                            showAddCommodityDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange)
                ) {
                    Text("Tambah Komoditas", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCommodityDialog = false }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// =================================================================================================
// SUB-TAB 1: STOK LOGISTIK POSKO & GUDANG (INDIKATOR STATUS KRITIS < 10 & FEFO EXPIRY)
// =================================================================================================
@Composable
private fun PoskoLogisticsStockTab(
    logisticStocks: List<PoskoLogisticCommodity>,
    onEditStock: (PoskoLogisticCommodity) -> Unit,
    onAddCommodityClick: () -> Unit,
    onDownloadStockOpnameClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("list_posko_logistics_stock"),
        contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KARTU TERTIB ADMINISTRASI PEMERINTAH: UNDUH BERITA ACARA STOCK OPNAME & TATA KELOLA BMN/BMD
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.2.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_stock_opname_bmn_export")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PELAPORAN STOCK OPNAME & TATA KELOLA BMN/BMD",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "TERTIB ASET",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Unduh Berita Acara Pemeriksaan Fisik (Stock Opname) persediaan logistik, status kelayakan FEFO, serta buku besar mutasi bantuan warga (NIK 16 Digit) sebagai bukti pertanggungjawaban Petugas dalam Tata Kelola Barang Milik Negara/Daerah (PDF & Excel/CSV).",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = onDownloadStockOpnameClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_open_stock_opname_export_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UNDUH LAPORAN STOCK OPNAME BMN/BMD (PDF / CSV)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONITOR STOK LOGISTIK DARURAT (PRD BAB 1)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "Otomatis memberi label merah STATUS KRITIS jika stok < 10 unit & peringatan FEFO <= 30 hari.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddCommodityClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("btn_add_custom_logistic")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tambah",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        items(logisticStocks, key = { it.code }) { item ->
            val isCrit = item.isCritical
            val isExp = item.isExpired
            val isFefoSoon = item.isExpiringSoonFefo

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCrit) CriticalRedBg else CommandCardBg
                ),
                border = BorderStroke(
                    width = if (isCrit) 2.dp else 1.dp,
                    color = if (isCrit) CriticalRed else Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logistic_card_${item.code}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCrit) CriticalRed else SafeGreen,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCrit) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "${item.code} • ${item.poskoCode}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // BADGE STATUS KRITIS (< 10) ATAU STOK AMAN (TIDAK TERPOTONG/MENUMPUK)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCrit) CriticalRed else SafeGreen
                        ) {
                            Text(
                                text = if (isCrit) "STATUS KRITIS (< 10)" else "STOK AMAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Sisa Stok: ${item.currentStock} ${item.unit}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isCrit) Color(0xFFFDE047) else Color(0xFF38BDF8)
                            )
                            if (!item.expiryDateIso.isNullOrBlank()) {
                                val fefoText = when {
                                    isExp -> "KEDALUWARSA (${item.expiryDateIso}) - TARIK DARI POSKO!"
                                    isFefoSoon -> "FEFO PRIORITAS KELUAR: Exp ${item.expiryDateIso} (<= 30 Hari)"
                                    else -> "Masa Kedaluwarsa FEFO: ${item.expiryDateIso}"
                                }
                                Text(
                                    text = fefoText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp,
                                    color = when {
                                        isExp -> Color(0xFFF87171)
                                        isFefoSoon -> Color(0xFFFBBF24)
                                        else -> Color(0xFF94A3B8)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = { onEditStock(item) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("btn_edit_stock_${item.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ubah Stok",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update Stok",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// =================================================================================================
// SUB-TAB 2: FORM PENGUNGSI POSKO (VALIDASI KETAT KELOMPOK RENTAN <= TOTAL JIWA)
// =================================================================================================
@Composable
private fun PoskoRefugeeValidationTab(
    poskoUnits: List<PoskoRefugeeUnit>,
    dalaLockedSnapshot: com.example.data.repository.PoskoDalaLockedSnapshot?,
    officerName: String,
    onValidateAndSave: (String, Int, Int, Int, Int, Int) -> Result<PoskoRefugeeUnit>,
    onCaptureSnapshotForDala: () -> com.example.data.repository.PoskoDalaLockedSnapshot
) {
    val context = LocalContext.current
    var selectedPoskoId by rememberSaveable {
        mutableStateOf(poskoUnits.firstOrNull()?.poskoId ?: "posko_mm_01")
    }
    val selectedPosko = poskoUnits.firstOrNull { it.poskoId == selectedPoskoId }
        ?: poskoUnits.firstOrNull()
        ?: return

    var totalJiwaText by remember(selectedPosko) { mutableStateOf(selectedPosko.totalJiwa.toString()) }
    var lansiaText by remember(selectedPosko) { mutableStateOf(selectedPosko.lansia.toString()) }
    var ibuHamilText by remember(selectedPosko) { mutableStateOf(selectedPosko.ibuHamil.toString()) }
    var balitaText by remember(selectedPosko) { mutableStateOf(selectedPosko.balita.toString()) }
    var disabilitasText by remember(selectedPosko) { mutableStateOf(selectedPosko.disabilitas.toString()) }

    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(totalJiwaText, lansiaText, ibuHamilText, balitaText, disabilitasText) {
        val total = totalJiwaText.toIntOrNull() ?: 0
        val sumRentan = (lansiaText.toIntOrNull() ?: 0) +
            (ibuHamilText.toIntOrNull() ?: 0) +
            (balitaText.toIntOrNull() ?: 0) +
            (disabilitasText.toIntOrNull() ?: 0)
        validationErrorMsg = if (total > 0 && sumRentan > total) {
            "VALIDASI DITOLAK: Total Kelompok Rentan ($sumRentan jiwa) TIDAK BOLEH melebihi Total Jiwa ($total jiwa)!"
        } else {
            null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("list_posko_refugee_validation"),
        contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daftar Posko Aktif di bawah incident_id
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CommandCardBg),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DAFTAR POSKO AKTIF (INCIDENT: ${selectedPosko.incidentId})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    poskoUnits.forEach { posko ->
                        val isSelected = posko.poskoId == selectedPosko.poskoId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF064E3B) else Color(0xFF0F172A),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedPoskoId = posko.poskoId }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${posko.poskoCode} • ${posko.poskoName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Total: ${posko.totalJiwa}/${posko.maxCapacity} Jiwa • Lansia: ${posko.lansia} • Bumil: ${posko.ibuHamil} • Balita: ${posko.balita} • Disabilitas: ${posko.disabilitas}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Form Input Pengungsi dengan Validasi Ketat
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CommandCardBg),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "FORM PENGUNGSI TERPILAH: ${selectedPosko.poskoName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Aturan Validasi Ketat: (Lansia + Ibu Hamil + Balita + Disabilitas) <= Total Jiwa",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    PoskoDigitInputField(
                        label = "TOTAL JIWA PENGUNGSI (SELURUHNYA)",
                        value = totalJiwaText,
                        onValueChange = { totalJiwaText = it },
                        testTag = "input_posko_total_jiwa"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoskoDigitInputField(
                            label = "Lansia (>=60 Th)",
                            value = lansiaText,
                            onValueChange = { lansiaText = it },
                            testTag = "input_posko_lansia",
                            modifier = Modifier.weight(1f)
                        )
                        PoskoDigitInputField(
                            label = "Ibu Hamil",
                            value = ibuHamilText,
                            onValueChange = { ibuHamilText = it },
                            testTag = "input_posko_ibuhamil",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoskoDigitInputField(
                            label = "Balita (0-5 Th)",
                            value = balitaText,
                            onValueChange = { balitaText = it },
                            testTag = "input_posko_balita",
                            modifier = Modifier.weight(1f)
                        )
                        PoskoDigitInputField(
                            label = "Disabilitas",
                            value = disabilitasText,
                            onValueChange = { disabilitasText = it },
                            testTag = "input_posko_disabilitas",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (validationErrorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CriticalRedBg,
                            border = BorderStroke(1.5.dp, CriticalRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banner_posko_refugee_validation_error")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = validationErrorMsg.orEmpty(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val result = onValidateAndSave(
                                selectedPosko.poskoId,
                                totalJiwaText.toIntOrNull() ?: 0,
                                lansiaText.toIntOrNull() ?: 0,
                                ibuHamilText.toIntOrNull() ?: 0,
                                balitaText.toIntOrNull() ?: 0,
                                disabilitasText.toIntOrNull() ?: 0
                            )
                            result.onSuccess {
                                validationErrorMsg = null
                                Toast.makeText(
                                    context,
                                    "Data Pengungsi ${it.poskoCode} (${it.totalJiwa} Jiwa) berhasil divalidasi & disimpan!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { err ->
                                validationErrorMsg = err.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_save_posko_refugees")
                    ) {
                        Text(
                            text = "VALIDASI & SIMPAN DATA PENGUNGSI POSKO",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // Card Integrasi Snapshot Data Posko ke Modul DaLA (agar form bagian bawah tampil utuh & jelas)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, BpbdOrange.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTEGRASI AUTO-POPULATE KE MODUL DALA",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (dalaLockedSnapshot?.isLocked == true) SafeGreen else BpbdOrange
                        ) {
                            Text(
                                text = if (dalaLockedSnapshot?.isLocked == true) "SNAPSHOT TERKUNCI" else "LIVE POSKO",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (dalaLockedSnapshot != null) {
                            "Terkunci: ${dalaLockedSnapshot.totalJiwa} Jiwa (Rentan: ${dalaLockedSnapshot.totalKelompokRentan} Jiwa) • ${dalaLockedSnapshot.lockedAtIso}"
                        } else {
                            "Data pengungsi dari seluruh Posko Aktif otomatis terhubung ke panel Read-Only pada Modul Pasca-Bencana (DaLA)."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    OutlinedButton(
                        onClick = {
                            val snap = onCaptureSnapshotForDala()
                            Toast.makeText(
                                context,
                                "Snapshot Data Posko (${snap.totalJiwa} Jiwa) berhasil dikunci untuk Laporan DaLA!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BpbdOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_lock_posko_snapshot_tab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KUNCI SNAPSHOT DATA POSKO UNTUK DALA",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// =================================================================================================
// SUB-TAB 3: DISTRIBUSI BANTUAN WARGA (ANTI-DATA GANDA NIK 16 DIGIT)
// =================================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoskoNikAidDistributionTab(
    logisticStocks: List<PoskoLogisticCommodity>,
    poskoUnits: List<PoskoRefugeeUnit>,
    distributionLedger: List<com.example.data.repository.NikAidDistributionRecord>,
    officerName: String,
    onDistribute: (String, String, String, String, String, Int, String) -> Result<com.example.data.repository.NikAidDistributionRecord>
) {
    val context = LocalContext.current
    var nikInput by rememberSaveable { mutableStateOf("") }
    var kkInput by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var selectedVuln by rememberSaveable { mutableStateOf("Lansia") }
    var selectedCommodityCode by rememberSaveable {
        mutableStateOf(logisticStocks.firstOrNull()?.code ?: "LOG-SEMBAKO")
    }
    var quantityInput by rememberSaveable { mutableStateOf("1") }
    var selectedPoskoCode by rememberSaveable {
        mutableStateOf(poskoUnits.firstOrNull()?.poskoCode ?: "POSKO-MM-001")
    }
    var errorBannerText by remember { mutableStateOf<String?>(null) }
    var commodityDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCommodity = logisticStocks.firstOrNull { it.code == selectedCommodityCode }
        ?: logisticStocks.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("list_posko_nik_distribution"),
        contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CommandCardBg),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISTRIBUSI BANTUAN ANTI-DATA GANDA (NIK 16 DIGIT PK)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Text(
                        text = "Sistem menolak otomatis apabila NIK 16 Digit yang sama sudah pernah menerima komoditas bantuan yang sama.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    PoskoDigitInputField(
                        label = "NIK Penerima Bantuan (Wajib 16 Digit Angka)",
                        value = nikInput,
                        onValueChange = { if (it.length <= 16) nikInput = it },
                        testTag = "input_dist_recipient_nik"
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap Kepala Keluarga / Pengungsi") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_dist_recipient_name")
                    )

                    // Pemilih Komoditas Logistik
                    ExposedDropdownMenuBox(
                        expanded = commodityDropdownExpanded,
                        onExpandedChange = { commodityDropdownExpanded = !commodityDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCommodity?.let {
                                "${it.name} (Sisa: ${it.currentStock} ${it.unit})"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Komoditas Bantuan") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = commodityDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = commodityDropdownExpanded,
                            onDismissRequest = { commodityDropdownExpanded = false }
                        ) {
                            logisticStocks.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${item.name} • Stok: ${item.currentStock} ${item.unit}")
                                    },
                                    onClick = {
                                        selectedCommodityCode = item.code
                                        commodityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PoskoDigitInputField(
                            label = "Jumlah (${selectedCommodity?.unit ?: "Unit"})",
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            testTag = "input_dist_quantity",
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = selectedVuln,
                            onValueChange = { selectedVuln = it },
                            label = { Text("Kategori Rentan") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (errorBannerText != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CriticalRedBg,
                            border = BorderStroke(1.5.dp, CriticalRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banner_nik_duplicate_error")
                        ) {
                            Text(
                                text = errorBannerText.orEmpty(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val res = onDistribute(
                                nikInput,
                                kkInput,
                                nameInput,
                                selectedVuln,
                                selectedCommodityCode,
                                quantityInput.toIntOrNull() ?: 1,
                                selectedPoskoCode
                            )
                            res.onSuccess { rec ->
                                errorBannerText = null
                                nikInput = ""
                                nameInput = ""
                                Toast.makeText(
                                    context,
                                    "Distribusi ${rec.commodityName} ke NIK ${rec.recipientNik} berhasil dicatat!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { err ->
                                errorBannerText = err.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_submit_nik_distribution")
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VERIFIKASI NIK 16 DIGIT & SERAHKAN BANTUAN",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Riwayat Ledger Distribusi Bantuan Berbasis NIK
        item {
            Text(
                text = "LOG AUDIT DISTRIBUSI BANTUAN (${distributionLedger.size} TRANSAKSI TERVERIFIKASI NIK)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF38BDF8)
            )
        }

        items(distributionLedger, key = { it.distributionId }) { log ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CommandCardBg),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NIK PK: ${log.recipientNik}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = log.timestampIso,
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${log.recipientName} (${log.vulnerabilityGroup})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Menerima: ${log.quantity} ${log.unit} ${log.commodityName} • ${log.poskoCode}",
                        fontSize = 11.5.sp,
                        color = Color(0xFF86EFAC),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun PoskoDigitInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }) },
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF38BDF8),
            unfocusedLabelColor = Color(0xFF94A3B8)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
