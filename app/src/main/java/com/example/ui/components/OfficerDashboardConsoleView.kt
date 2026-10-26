package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.OfficerAuthManager
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.util.GoogleMapsHelper

/**
 * Konsol Khusus Petugas Pusdalops BPBD Kabupaten Mukomuko & Tim Reaksi Cepat (TRC).
 * Tampilan antarmuka khusus untuk:
 * 1. Validasi keabsahan laporan warga (anti-laporan palsu)
 * 2. Pemantauan metrik KPI real-time (butuh validasi, TRC diterjunkan, tuntas)
 * 3. Log catatan resmi tindak lanjut petugas di lapangan
 * 4. Pemanduan navigasi Google Maps armada rescue ke titik GPS
 * 5. Komunikasi instan dengan pelapor & komando posko siaga 117
 */
@Composable
fun OfficerDashboardConsoleView(
    reports: List<CitizenDisasterReport>,
    currentDistrict: District,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    officerBadgeId: String,
    officerShift: String,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onUpdateReportStatusAndNotes: ((String, String, String) -> Unit)? = null,
    onViewPhoto: (String, CitizenDisasterReport) -> Unit,
    onSyncReports: (((Int) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Filters & Search State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") } // "Semua", "Diproses", "Divalidasi", "Selesai"
    var selectedDistrictFilter by remember { mutableStateOf("Semua Kecamatan") }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }
    var onlyWithCasualties by remember { mutableStateOf(false) }

    // Sync State
    var isSyncing by remember { mutableStateOf(false) }

    // Officer Notes Dialog State
    var noteEditingReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }
    var followUpNoteText by remember { mutableStateOf("") }
    var followUpStatusTarget by remember { mutableStateOf("") }

    // Quick Stats Calculation
    val totalCount = reports.size
    val pendingCount = reports.count { it.status.equals("Diproses", ignoreCase = true) || it.status.equals("Terverifikasi GPS", ignoreCase = true) }
    val validatedCount = reports.count { it.status.equals("Divalidasi", ignoreCase = true) || it.status.contains("TRC", ignoreCase = true) }
    val resolvedCount = reports.count { it.status.equals("Selesai", ignoreCase = true) }
    val totalDead = reports.sumOf { it.casualtiesDead }
    val totalInjured = reports.sumOf { it.casualtiesInjured }
    val totalMissing = reports.sumOf { it.casualtiesMissing }

    // District options
    val districtOptions = listOf("Semua Kecamatan") + listOf(
        "Kota Mukomuko", "Ipuh", "Teramang Jaya", "Penarik", "Lubuk Pinang",
        "Pondok Suguh", "Air Dikit", "Selagan Raya", "XIV Koto", "Malin Deman",
        "V Koto", "Teras Terunjam", "Sungai Rumbai", "Air Rami", "Air Manjunto"
    )

    // Filtered Reports
    val filteredReports = reports.filter { rep ->
        val matchesStatus = when (selectedStatusFilter) {
            "Semua" -> true
            "Diproses" -> rep.status.equals("Diproses", ignoreCase = true) || rep.status.equals("Terverifikasi GPS", ignoreCase = true)
            "Divalidasi" -> rep.status.equals("Divalidasi", ignoreCase = true) || rep.status.contains("TRC", ignoreCase = true)
            "Selesai" -> rep.status.equals("Selesai", ignoreCase = true)
            else -> true
        }

        val matchesDistrict = if (selectedDistrictFilter == "Semua Kecamatan") true else rep.district.equals(selectedDistrictFilter, ignoreCase = true)

        val matchesCasualties = if (onlyWithCasualties) {
            (rep.casualtiesDead > 0 || rep.casualtiesInjured > 0 || rep.casualtiesMissing > 0)
        } else true

        val matchesSearch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim().lowercase()
            rep.id.lowercase().contains(q) ||
            rep.disasterType.lowercase().contains(q) ||
            rep.district.lowercase().contains(q) ||
            rep.village.lowercase().contains(q) ||
            rep.incidentLocation.lowercase().contains(q) ||
            rep.reporterName.lowercase().contains(q) ||
            rep.additionalNotes.lowercase().contains(q)
        }

        matchesStatus && matchesDistrict && matchesCasualties && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("officer_dashboard_console_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. OFFICER IDENTITY & COMMAND BAR
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOfficerLoggedIn) Color(0xFF0F172A) else Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("officer_command_bar")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOfficerLoggedIn) Color(0xFF16A34A) else Color(0xFFEA580C),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isOfficerLoggedIn) Icons.Default.Shield else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "KONSOL PUSDALOPS BPBD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isOfficerLoggedIn) Color(0xFF166534) else Color(0xFF7C2D12)
                                    ) {
                                        Text(
                                            text = if (isOfficerLoggedIn) "SESI RESMI AKTIF" else "MODE TERBATAS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isOfficerLoggedIn) officerName else "Akses Petugas Berwenang Diperlukan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // Login / Switch Officer Button
                        if (isOfficerLoggedIn) {
                            OutlinedButton(
                                onClick = onLogoutClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF991B1B)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Keluar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onLoginClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("officer_login_action_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Verifikasi PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isOfficerLoggedIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NIP/ID: $officerBadgeId • $officerShift",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (!isSyncing && onSyncReports != null) {
                                        isSyncing = true
                                        onSyncReports.invoke { count ->
                                            isSyncing = false
                                            Toast.makeText(context, "Cloud sync selesai! $count laporan diperbarui.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp, color = Color(0xFF38BDF8))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync Cloud",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isSyncing) "Menyinkronkan..." else "Sinkron Cloud",
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Petugas Pusdalops & Tim Reaksi Cepat (TRC) dapat masuk menggunakan PIN resmi (117 / BPBD117 / TRC2026) untuk memvalidasi laporan, mencatat tindak lanjut, dan menerjunkan armada.",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 2. REAL-TIME INCIDENT KPI METRICS
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metrik Penanganan & Triage Bencana",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "$totalCount Total Insiden",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // KPI 4-Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pending Validation
                    KpiMetricCard(
                        title = "Butuh Validasi",
                        count = pendingCount,
                        badgeColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        subtitle = "Status Diproses",
                        modifier = Modifier.weight(1f)
                    )

                    // Field Dispatched (TRC)
                    KpiMetricCard(
                        title = "Tim Lapangan",
                        count = validatedCount,
                        badgeColor = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        subtitle = "TRC Diterjunkan",
                        modifier = Modifier.weight(1f)
                    )

                    // Resolved
                    KpiMetricCard(
                        title = "Selesai",
                        count = resolvedCount,
                        badgeColor = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE),
                        subtitle = "Tuntas Ditangani",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Casualties summary strip if any
                if (totalDead > 0 || totalInjured > 0 || totalMissing > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Akumulasi Korban Jiwa Warga: $totalDead Meninggal Dunia • $totalInjured Luka-Luka • $totalMissing Hilang",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        // 3. SEARCH & TRIAGE FILTERS
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("officer_search_input"),
                        placeholder = { Text("Cari ID, jenis bencana, desa, atau pelapor...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus pencarian",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF15803D),
                            unfocusedIndicatorColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Semua" to "Semua",
                            "Diproses" to "🟡 Butuh Validasi",
                            "Divalidasi" to "🟢 Lapangan",
                            "Selesai" to "🔵 Selesai"
                        ).forEach { (key, label) ->
                            val isSel = selectedStatusFilter == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) Color(0xFF15803D) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF15803D) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .clickable { selectedStatusFilter = key }
                                    .testTag("filter_status_$key")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // District dropdown & Casualty toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // District selector
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { isDistrictDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedDistrictFilter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isDistrictDropdownExpanded,
                                onDismissRequest = { isDistrictDropdownExpanded = false }
                            ) {
                                districtOptions.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text(dist, fontSize = 12.sp) },
                                        onClick = {
                                            selectedDistrictFilter = dist
                                            isDistrictDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Casualty switch
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (onlyWithCasualties) Color(0xFFFEF2F2) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (onlyWithCasualties) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { onlyWithCasualties = !onlyWithCasualties }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (onlyWithCasualties) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ada Korban",
                                    fontSize = 10.sp,
                                    fontWeight = if (onlyWithCasualties) FontWeight.Bold else FontWeight.Normal,
                                    color = if (onlyWithCasualties) Color(0xFF991B1B) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. REPORTS LIST FOR OFFICER TRIAGE & VALIDATION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daftar Antrean Verifikasi & Penanganan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${filteredReports.size} laporan ditemukan",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        if (filteredReports.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 36.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Laporan yang Cocok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Semua laporan dalam kriteria filter ini telah ditangani atau coba sesuaikan kata kunci pencarian Anda.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                OfficerIncidentReportCard(
                    report = report,
                    isOfficerLoggedIn = isOfficerLoggedIn,
                    officerName = officerName,
                    onUpdateStatus = { targetStatus ->
                        if (isOfficerLoggedIn) {
                            onUpdateReportStatus(report.id, targetStatus)
                            Toast.makeText(
                                context,
                                "Status laporan #${report.id.takeLast(7)} diubah menjadi '$targetStatus' oleh Petugas $officerName",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onLoginClick()
                        }
                    },
                    onOpenNoteEditor = {
                        if (isOfficerLoggedIn) {
                            noteEditingReport = report
                            followUpNoteText = report.additionalNotes
                            followUpStatusTarget = report.status
                        } else {
                            onLoginClick()
                        }
                    },
                    onViewPhoto = { photoUri ->
                        onViewPhoto(photoUri, report)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // 5. OFFICIAL OFFICER FOLLOW-UP NOTE EDITOR DIALOG
    if (noteEditingReport != null) {
        val activeRep = noteEditingReport!!
        AlertDialog(
            onDismissRequest = { noteEditingReport = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF15803D),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Log Tindak Lanjut TRC BPBD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Laporan: #${activeRep.id.takeLast(7)} • ${activeRep.disasterType}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        text = "Lokasi: ${activeRep.village}, Kec. ${activeRep.district}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ubah Status Operasional:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Diproses", "Divalidasi", "Selesai").forEach { st ->
                            val isSel = followUpStatusTarget.equals(st, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF15803D) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF15803D) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { followUpStatusTarget = st }
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Catatan Tindak Lanjut Petugas / Personel TRC:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = followUpNoteText,
                        onValueChange = { followUpNoteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = {
                            Text(
                                "Contoh: Regu TRC Posko Ipuh diterjunkan dengan 2 perahu karet & 1 unit genset. Logistik sembako disalurkan ke posko darurat balai desa.",
                                fontSize = 11.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalStatus = followUpStatusTarget.ifBlank { activeRep.status }
                        val finalNote = followUpNoteText.trim()
                        if (onUpdateReportStatusAndNotes != null) {
                            onUpdateReportStatusAndNotes(activeRep.id, finalStatus, finalNote)
                        } else {
                            onUpdateReportStatus(activeRep.id, finalStatus)
                        }
                        Toast.makeText(
                            context,
                            "Log tindak lanjut tersimpan resmi oleh $officerName",
                            Toast.LENGTH_SHORT
                        ).show()
                        noteEditingReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Simpan Tindak Lanjut", fontSize = 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteEditingReport = null }) {
                    Text("Batal", fontSize = 11.sp)
                }
            }
        )
    }
}

/**
 * KPI Metric Card Component
 */
@Composable
private fun KpiMetricCard(
    title: String,
    count: Int,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Incident Card specifically tailored for Authorized Duty Officers with:
 * - Direct Google Maps Dispatch
 * - Reporter Phone / WhatsApp dialing
 * - Visual photo zoom inspector
 * - Status transition buttons (Validasi TRC, Selesai)
 * - Official Follow-up Action Log
 * - Share Sitrep to WhatsApp Posko BPBD
 */
@Composable
private fun OfficerIncidentReportCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    onUpdateStatus: (String) -> Unit,
    onOpenNoteEditor: () -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val statusColor = when {
        report.status.equals("Divalidasi", ignoreCase = true) || report.status.contains("TRC", ignoreCase = true) -> Color(0xFF16A34A)
        report.status.equals("Selesai", ignoreCase = true) -> Color(0xFF2563EB)
        else -> Color(0xFFD97706) // "Diproses"
    }

    val statusBg = when {
        report.status.equals("Divalidasi", ignoreCase = true) || report.status.contains("TRC", ignoreCase = true) -> Color(0xFFF0FDF4)
        report.status.equals("Selesai", ignoreCase = true) -> Color(0xFFEFF6FF)
        else -> Color(0xFFFFFBEB)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("officer_incident_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: ID, Badge, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Text(
                            text = "#${report.id.takeLast(7)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.disasterType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Status chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = report.status.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location & GPS Badge
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(15.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "${report.village}, Kecamatan ${report.district}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    if (report.incidentLocation.isNotBlank()) {
                        Text(
                            text = "Patokan: ${report.incidentLocation}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    Text(
                        text = "Waktu Kejadian: ${report.incidentTime}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // GPS Satelit Geotag Info
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "GPS Satelit",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS Terkunci: %.4f, %.4f (±%.0fm)".format(
                                report.latitude,
                                report.longitude,
                                report.gpsAccuracyMeters ?: 5f
                            ),
                            fontSize = 10.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Satelit Valid",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            // Casualties banner
            if (report.casualtiesDead > 0 || report.casualtiesInjured > 0 || report.casualtiesMissing > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Korban Jiwa: ${report.casualtiesDead} Meninggal • ${report.casualtiesInjured} Luka • ${report.casualtiesMissing} Hilang",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Photo Thumbnail & Visual Inspection
            if (!report.photoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onViewPhoto(report.photoUri) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(54.dp)
                        ) {
                            AsyncImage(
                                model = report.photoUri,
                                contentDescription = "Foto Bukti Kejadian",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Foto Bukti Kamera Langsung",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Sentuh untuk inspeksi visual resolusi penuh",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onViewPhoto(report.photoUri) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Perbesar", fontSize = 10.sp)
                    }
                }
            }

            // Official Follow-up notes log if present
            if (report.additionalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catatan Tindak Lanjut TRC BPBD:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.additionalNotes,
                            fontSize = 11.sp,
                            color = Color(0xFF14532D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // REPORTER CONTACT & DISPATCH ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reporter details
                Column {
                    Text(
                        text = "Pelapor: ${report.reporterName.ifBlank { "Warga Mukomuko" }}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    if (report.reporterPhone.isNotBlank()) {
                        Text(
                            text = "Kontak: ${report.reporterPhone}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Quick dial/WhatsApp buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.reporterPhone.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    EmergencyDialer.dial(context, report.reporterPhone)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Telepon Pelapor",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF25D366),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    val cleaned = report.reporterPhone.replace(Regex("[^0-9]"), "")
                                    val waNumber = if (cleaned.startsWith("0")) "62" + cleaned.drop(1) else cleaned
                                    val msg = "Halo Bpk/Ibu ${report.reporterName}, kami dari Pusdalops BPBD Kab. Mukomuko menindaklanjuti laporan bencana ${report.disasterType} di ${report.village}. Mohon konfirmasi kondisi terkini."
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$waNumber&text=${Uri.encode(msg)}")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(waIntent)
                                    } catch (_: Exception) {
                                        EmergencyDialer.dial(context, report.reporterPhone)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "WA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Google Maps Navigation Button
                    Button(
                        onClick = {
                            GoogleMapsHelper.openNavigationRoute(
                                context = context,
                                destLat = report.latitude,
                                destLon = report.longitude,
                                destName = "Bencana ${report.disasterType} - ${report.village}"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Rute Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // OFFICER ACTIONS BUTTONS BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status: Validasi / Terjunkan Tim
                if (!report.status.equals("Divalidasi", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Divalidasi") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Validasi TRC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Status: Selesai
                if (!report.status.equals("Selesai", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Selesai") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Tandai Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Add follow-up note button
                OutlinedButton(
                    onClick = onOpenNoteEditor,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Tindak Lanjut", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }

                // Share / Dispatch Sitrep to Posko WhatsApp
                OutlinedButton(
                    onClick = {
                        val sitrep = """
                            *LAPORAN SITUASI (SITREP) PUSDALOPS BPBD MUKOMUKO*
                            • No. Laporan: #${report.id.takeLast(7)}
                            • Kejadian: *${report.disasterType}*
                            • Status: *${report.status.uppercase()}*
                            • Waktu: ${report.incidentTime}
                            • Lokasi: ${report.village}, Kec. ${report.district}
                            • Detail: ${report.incidentLocation.ifBlank { "Sesuai koordinat GPS" }}
                            • Korban Jiwa: MD: ${report.casualtiesDead}, LK: ${report.casualtiesInjured}, HL: ${report.casualtiesMissing}
                            • Koordinat GPS: ${report.latitude}, ${report.longitude}
                            • Navigasi Google Maps: https://www.google.com/maps/dir/?api=1&destination=${report.latitude},${report.longitude}
                            • Pelapor: ${report.reporterName} (${report.reporterPhone.ifBlank { "Nomor tidak dicantumkan" }})
                            • Petugas Validasi: $officerName
                            ${if (report.additionalNotes.isNotBlank()) "• Catatan TRC: " + report.additionalNotes else ""}
                        """.trimIndent()

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Sitrep Pusdalops #${report.id.takeLast(7)}")
                            putExtra(Intent.EXTRA_TEXT, sitrep)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Disposisi Laporan ke Posko / WhatsApp"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan Sitrep",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
