package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.data.local.LaporanHarian
import com.example.data.local.LaporanHarianWithLogKomunikasi
import com.example.data.local.LogKomunikasi
import com.example.data.local.OfficerRoles
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.Personnel
import com.example.data.local.PersonnelAuditLogEntity
import com.example.data.local.SyncConflictLogEntity
import com.example.data.repository.SmartOfficerAuthRepository
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RoomTable(val label: String, val tableName: String) {
    PERSONNEL("Personil ASN", "personnel"),
    LAPORAN_HARIAN("Laporan Harian", "laporan_harian"),
    CITIZEN_REPORTS("Laporan Bencana", "citizen_disaster_reports"),
    AUDIT_LOGS("Audit Log", "personnel_audit_logs"),
    CONFLICT_LOGS("Log Konflik Sync", "sync_conflict_logs")
}

@Composable
fun DatabaseExplorerScreen(
    database: AppDatabase,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOfficerSessionActive = com.example.data.local.OfficerAuthManager(context).isOfficerLoggedIn()
    if (!isOfficerSessionActive) {
        InternalModuleLockedScreen(
            moduleTitle = "Database Explorer",
            testTag = "database_explorer_locked",
            onClose = onClose,
            modifier = modifier
        )
        return
    }
    val smartOfficerAuthRepo = remember(context) { SmartOfficerAuthRepository(context) }
    var selectedTable by remember { mutableStateOf(RoomTable.PERSONNEL) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var personnelList by remember { mutableStateOf<List<Personnel>>(emptyList()) }
    var laporanHarianList by remember { mutableStateOf<List<LaporanHarianWithLogKomunikasi>>(emptyList()) }
    var reportsList by remember { mutableStateOf<List<CitizenDisasterReportEntity>>(emptyList()) }
    var auditLogsList by remember { mutableStateOf<List<PersonnelAuditLogEntity>>(emptyList()) }
    var conflictLogsList by remember { mutableStateOf<List<SyncConflictLogEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Maintenance CRUD states untuk tabel Personil ASN
    var showAddOrEditPersonnelDialog by remember { mutableStateOf(false) }
    var editingDbPersonnel by remember { mutableStateOf<Personnel?>(null) }
    var deletingDbPersonnel by remember { mutableStateOf<Personnel?>(null) }

    fun refreshData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val p = database.personnelDao().getAllPersonnelList()
            val l = try {
                database.laporanHarianDao().getAllLaporanHarianSync().map { lap ->
                    val logs = database.laporanHarianDao().getLogsForLaporanSync(lap.id)
                    LaporanHarianWithLogKomunikasi(lap, logs)
                }
            } catch (e: Exception) {
                emptyList()
            }
            val r = database.weatherDao().getAllCitizenDisasterReportsSync()
            val a = database.auditLogDao().getAllAuditLogsList()
            val c = database.syncConflictLogDao().getAllConflictLogsList()
            withContext(Dispatchers.Main) {
                personnelList = p
                laporanHarianList = l
                reportsList = r
                auditLogsList = a
                conflictLogsList = c
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    var showFormLaporanHarian by remember { mutableStateOf(false) }

    if (showFormLaporanHarian) {
        FormLaporanHarianScreen(
            database = database,
            onClose = { showFormLaporanHarian = false },
            onSavedSuccess = {
                showFormLaporanHarian = false
                refreshData()
            }
        )
        return
    }

    val adaptiveInsets = rememberAdaptiveSystemBarInsets()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("database_explorer_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(top = adaptiveInsets.topBarInset)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_db_explorer")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Database Explorer (Room SQLite)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Inspeksi tabel lokal, status WAL & resolusi data",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTable == RoomTable.PERSONNEL) {
                        Button(
                            onClick = {
                                editingDbPersonnel = null
                                showAddOrEditPersonnelDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_db_add_personnel")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Personil",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (selectedTable == RoomTable.LAPORAN_HARIAN) {
                        Button(
                            onClick = { showFormLaporanHarian = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("btn_open_form_laporan_harian")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Input Form",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshData() },
                        modifier = Modifier.testTag("btn_refresh_db_explorer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = OrangePrimary
                        )
                    }
                }
            }

            // Table Selection Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RoomTable.values()) { table ->
                    val isSelected = selectedTable == table
                    val count = when (table) {
                        RoomTable.PERSONNEL -> personnelList.size
                        RoomTable.LAPORAN_HARIAN -> laporanHarianList.size
                        RoomTable.CITIZEN_REPORTS -> reportsList.size
                        RoomTable.AUDIT_LOGS -> auditLogsList.size
                        RoomTable.CONFLICT_LOGS -> conflictLogsList.size
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTable = table },
                        label = {
                            Text(
                                text = "${table.label} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        leadingIcon = {
                            val icon = when (table) {
                                RoomTable.PERSONNEL -> Icons.Default.Badge
                                RoomTable.LAPORAN_HARIAN -> Icons.Default.Storage
                                RoomTable.CITIZEN_REPORTS -> Icons.Default.Report
                                RoomTable.AUDIT_LOGS -> Icons.Default.History
                                RoomTable.CONFLICT_LOGS -> Icons.Default.SyncProblem
                            }
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = OrangeOnPrimary,
                            selectedLeadingIconColor = OrangeOnPrimary,
                            containerColor = Color(0xFF334155),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("chip_table_${table.tableName}")
                    )
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari baris data pada tabel...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("input_db_search")
                )
            }

            // Table Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTable) {
                    RoomTable.PERSONNEL -> {
                        val filtered = personnelList.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.nip.contains(searchQuery)
                        }
                        if (filtered.isEmpty()) {
                            EmptyTablePlaceholder("Tidak ada data personil.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + adaptiveInsets.effectiveBottomInset)
                            ) {
                                items(filtered, key = { it.id }) { p ->
                                    val (badgeBg, badgeText) = OfficialAsnBpbdPersonnel.getRoleBadgeColor(p.role)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("row_personnel_${p.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = p.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = badgeBg,
                                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, badgeText.copy(alpha = 0.35f)),
                                                    modifier = Modifier.wrapContentWidth()
                                                ) {
                                                    Text(
                                                        text = p.role.uppercase(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = badgeText,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "NIP: ${p.nip} • Shift: ${p.shift}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Unit: ${p.unit} • Telp: ${if (p.phone.isNotBlank()) p.phone else "-"}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        editingDbPersonnel = p
                                                        showAddOrEditPersonnelDialog = true
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 34.dp)
                                                        .testTag("btn_db_edit_personnel_${p.id}")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Personil", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Edit", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                                                }
                                                OutlinedButton(
                                                    onClick = { deletingDbPersonnel = p },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 34.dp)
                                                        .testTag("btn_db_delete_personnel_${p.id}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Personil", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Hapus", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    RoomTable.LAPORAN_HARIAN -> {
                        val filtered = laporanHarianList.filter {
                            val lap = it.laporan
                            searchQuery.isBlank() ||
                                lap.jenisBencana.contains(searchQuery, ignoreCase = true) ||
                                lap.lokasi.contains(searchQuery, ignoreCase = true) ||
                                lap.operatorName.contains(searchQuery, ignoreCase = true) ||
                                lap.tanggalLaporan.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyTablePlaceholder("Belum ada data laporan harian Pusdalops.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + adaptiveInsets.effectiveBottomInset)
                            ) {
                                items(filtered, key = { it.laporan.id }) { item ->
                                    val lap = item.laporan
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("row_laporan_harian_${lap.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "#${lap.id}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFDC2626),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = lap.jenisBencana,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFF0284C7).copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = lap.tanggalLaporan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF0369A1),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "📍 ${lap.lokasi} (${lap.koordinat})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (lap.kedalaman.isNotBlank() && lap.kedalaman != "-") {
                                                Text(
                                                    text = "Kedalaman: ${lap.kedalaman} • Cuaca: ${lap.kondisiCuaca}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                                )
                                            } else {
                                                Text(
                                                    text = "Cuaca: ${lap.kondisiCuaca} • Waktu: ${lap.tanggalKejadian} ${lap.waktuKejadian}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "Dampak & Korban: ${lap.cakupanDampak}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Korban: ${lap.korbanMeninggal} Meninggal, ${lap.korbanLuka} Luka • Kerusakan: ${lap.kerusakan}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (lap.korbanMeninggal > 0 || lap.korbanLuka > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                                    )
                                                    Text(
                                                        text = "Upaya: ${lap.upayaDilakukan}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                                    )
                                                    Text(
                                                        text = "Kondisi Mutakhir: ${lap.kondisiMutakhir}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }

                                            // Log Komunikasi (Relasi One-to-Many)
                                            if (item.logs.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(end = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "📡 Log Komunikasi (${item.logs.size} Riwayat):",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    item.logs.forEach { log ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 4.dp, start = 4.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = "• [${log.waktuKomunikasi} | ${log.saluran}]",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFB45309),
                                                                lineHeight = 14.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(0.4f)
                                                            )
                                                            Text(
                                                                text = "${log.pihakDihubungi}: \"${log.pesan}\" (Arahan: ${log.arahanTindakan})",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                                lineHeight = 14.sp,
                                                                maxLines = 4,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Operator: ${lap.operatorName}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                                )
                                                Text(
                                                    text = "Stasiun: ${lap.stationId}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    RoomTable.CITIZEN_REPORTS -> {
                        val filtered = reportsList.filter {
                            searchQuery.isBlank() || it.id.contains(searchQuery, ignoreCase = true) || it.disasterType.contains(searchQuery, ignoreCase = true) || it.district.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyTablePlaceholder("Tidak ada laporan bencana.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + adaptiveInsets.effectiveBottomInset)
                            ) {
                                items(filtered, key = { it.id }) { r ->
                                    val (repBg, repColor) = when {
                                        r.status.contains("Valid", ignoreCase = true) -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                                        r.status.contains("Selesai", ignoreCase = true) -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB))
                                        else -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("row_report_${r.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "[${r.id}] ${r.disasterType}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = OrangePrimary,
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = repBg,
                                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, repColor.copy(alpha = 0.35f)),
                                                    modifier = Modifier.wrapContentWidth()
                                                ) {
                                                    Text(
                                                        text = r.status.uppercase(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = repColor,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Lokasi: ${r.incidentLocation}, Kec. ${r.district} (${r.village})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "GPS: Lat ${r.latitude}, Lon ${r.longitude} • Akurasi: ${r.gpsAccuracyMeters ?: "-"}m",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (r.additionalNotes.isNotBlank()) {
                                                Text(
                                                    text = "Catatan: ${r.additionalNotes}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    RoomTable.AUDIT_LOGS -> {
                        val filtered = auditLogsList.filter {
                            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.action.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyTablePlaceholder("Tidak ada log audit petugas.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + adaptiveInsets.effectiveBottomInset)
                            ) {
                                items(filtered, key = { it.id }) { a ->
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(a.timestamp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("row_audit_${a.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = a.action,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = Color(0xFF0369A1),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 8.dp)
                                                )
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${a.name} (${a.nip}) • ${a.role}",
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Perangkat: ${a.deviceInfo}",
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    RoomTable.CONFLICT_LOGS -> {
                        val filtered = conflictLogsList.filter {
                            searchQuery.isBlank() || it.reportId.contains(searchQuery, ignoreCase = true) || it.conflictType.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyTablePlaceholder("Belum ada event konflik sinkronisasi tercatat. Seluruh data tersinkron mulus.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + adaptiveInsets.effectiveBottomInset)
                            ) {
                                items(filtered, key = { it.id }) { c ->
                                    val dateStr = SimpleDateFormat("HH:mm:ss • dd MMM yyyy", Locale.getDefault()).format(Date(c.timestamp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("row_conflict_${c.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${c.reportId} • ${c.conflictType.replace('_', ' ')}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFDC2626),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "Resolusi: ${c.resolution}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
                                            Text(text = "Status: Lokal(${c.localStatus}) vs Server(${c.remoteStatus})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "Detail: ${c.details}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Input / Edit Personil langsung dari Database Explorer (Maintenance Admin)
    if (showAddOrEditPersonnelDialog) {
        val target = editingDbPersonnel
        val isEdit = target != null
        var fName by remember(target) { mutableStateOf(target?.name ?: "") }
        var fNip by remember(target) { mutableStateOf(target?.nip ?: "") }
        var fRole by remember(target) { mutableStateOf(target?.role ?: OfficerRoles.PETUGAS_JAGA) }
        var fUnit by remember(target) { mutableStateOf(target?.unit ?: "Pusdalops BPBD Kab. Mukomuko") }
        var fShift by remember(target) { mutableStateOf(target?.shift ?: "Shift Siaga 24 Jam") }
        var fPhone by remember(target) { mutableStateOf(target?.phone ?: "") }
        var fPin by remember(target) { mutableStateOf("") }
        var fError by remember(target) { mutableStateOf<String?>(null) }
        var isSaving by remember(target) { mutableStateOf(false) }

        BalancedModalDialog(
            onDismissRequest = { if (!isSaving) showAddOrEditPersonnelDialog = false },
            horizontalMargin = 16.dp,
            extraVerticalMargin = 16.dp
        ) {
            val kbActions = rememberFormKeyboardActions()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = if (isEdit) "Edit Personil (Maintenance DB)" else "Input Personil Baru (Maintenance DB)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = fName,
                            onValueChange = { fName = it; fError = null },
                            label = { Text("Nama Lengkap & Gelar *", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = kbActions,
                            modifier = Modifier.fillMaxWidth().formScrollPadding()
                        )
                        OutlinedTextField(
                            value = fNip,
                            onValueChange = { fNip = it; fError = null },
                            label = { Text("NIP ASN / No. Reg TRC *", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = kbActions,
                            modifier = Modifier.fillMaxWidth().formScrollPadding()
                        )
                        OutlinedTextField(
                            value = fRole,
                            onValueChange = { fRole = it },
                            label = { Text("Jabatan / Peran", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = kbActions,
                            modifier = Modifier.fillMaxWidth().formScrollPadding()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = fShift,
                                onValueChange = { fShift = it },
                                label = { Text("Regu / Shift", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = kbActions,
                                modifier = Modifier.weight(1f).formScrollPadding()
                            )
                            OutlinedTextField(
                                value = fUnit,
                                onValueChange = { fUnit = it },
                                label = { Text("Unit / Satuan", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = kbActions,
                                modifier = Modifier.weight(1f).formScrollPadding()
                            )
                        }
                        OutlinedTextField(
                            value = fPhone,
                            onValueChange = { fPhone = it },
                            label = { Text("No. HP / WhatsApp (Opsional)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = kbActions,
                            modifier = Modifier.fillMaxWidth().formScrollPadding()
                        )
                        OutlinedTextField(
                            value = fPin,
                            onValueChange = { fPin = it; fError = null },
                            label = {
                                Text(
                                    if (isEdit) "PIN Khusus Baru (Opsional)" else "PIN Login Awal Khusus (Opsional)",
                                    fontSize = 11.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    if (isEdit) "Kosongkan agar PIN petugas tetap aman (tidak berubah)" else "Kosongkan untuk aktivasi NIP awal",
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = formDoneKeyboardOptions(),
                            keyboardActions = kbActions,
                            modifier = Modifier.fillMaxWidth().formScrollPadding()
                        )
                        if (fError != null) {
                            Text(text = fError!!, color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddOrEditPersonnelDialog = false },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (!isSaving) {
                                    val trimmedPin = fPin.trim()
                                    val cleanNip = fNip.trim().replace(Regex("[\\s\\-]+"), "")
                                    val defaultNipPin = com.example.data.local.OfficerCredentialEntity.generateInitialPinForNip(cleanNip)
                                    if (isEdit && trimmedPin.isNotBlank()) {
                                        if (trimmedPin.length < 4) {
                                            fError = "PIN khusus baru minimal 4 digit/karakter."
                                            return@Button
                                        }
                                        if (SmartOfficerAuthRepository.FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
                                            fError = "PIN khusus dilarang menggunakan PIN default umum (117 / TRC2026 / dsb)."
                                            return@Button
                                        }
                                        if (trimmedPin.equals(defaultNipPin, ignoreCase = true) ||
                                            trimmedPin.equals(cleanNip.takeLast(6), ignoreCase = true)
                                        ) {
                                            fError = "PIN petugas dilarang dikembalikan ke 6 digit terakhir NIP (PIN default)."
                                            return@Button
                                        }
                                    }
                                    isSaving = true
                                    scope.launch {
                                        val res = smartOfficerAuthRepo.registerOrUpdateManualOfficer(
                                            fullName = fName.trim(),
                                            nip = fNip.trim(),
                                            role = fRole.trim(),
                                            unit = fUnit.trim(),
                                            shift = fShift.trim(),
                                            phone = fPhone.trim(),
                                            customInitialPin = trimmedPin.ifBlank { null },
                                            existingId = target?.let { "officer_${it.nip.trim()}" },
                                            previousNip = target?.nip,
                                            existingRoomId = target?.id
                                        )
                                        isSaving = false
                                        res.fold(
                                            onSuccess = { saved ->
                                                showAddOrEditPersonnelDialog = false
                                                refreshData()
                                                Toast.makeText(
                                                    context,
                                                    "Data personil '${saved.profile.fullName}' berhasil disimpan.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onFailure = { err ->
                                                fError = err.message ?: "Gagal menyimpan data personil."
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEdit) OrangePrimary else Color(0xFF0F766E)),
                            modifier = Modifier.weight(1.2f).heightIn(min = 44.dp)
                        ) {
                            Text(if (isSaving) "Menyimpan..." else if (isEdit) "Simpan Perubahan" else "Simpan Personil", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Konfirmasi Hapus Personil Permanen dari Database Explorer
    if (deletingDbPersonnel != null) {
        val p = deletingDbPersonnel!!
        var isDeleting by remember(p) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isDeleting) deletingDbPersonnel = null },
            title = { Text("Hapus Personil Permanen?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    text = "Personil '${p.name}' (NIP: ${p.nip}) akan dihapus permanen dari database lokal & Cloud (tidak akan muncul kembali).",
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            scope.launch {
                                smartOfficerAuthRepo.deleteOfficerPermanently(
                                    nip = p.nip,
                                    officerId = "officer_${p.nip.trim()}",
                                    roomId = p.id,
                                    fullName = p.name,
                                    role = p.role,
                                    shift = p.shift,
                                    reason = "Maintenance Database Explorer"
                                )
                                isDeleting = false
                                deletingDbPersonnel = null
                                refreshData()
                                Toast.makeText(context, "Personil '${p.name}' berhasil dihapus permanen.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isDeleting) "Menghapus..." else "Hapus Permanen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingDbPersonnel = null }, enabled = !isDeleting) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun EmptyTablePlaceholder(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
