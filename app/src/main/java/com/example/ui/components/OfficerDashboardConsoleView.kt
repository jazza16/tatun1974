package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.util.GoogleMapsHelper
import com.example.util.ReportExporter

/**
 * Modul Petugas Dashboard Aman (Khusus Staf Pusdalops BPBD & Tim Reaksi Cepat TRC).
 * Fitur:
 * 1. Filter status, kecamatan, dan multi-kategori bencana (Banjir, Cuaca Ekstrem, Keamanan Pesisir, dll.)
 * 2. Auto-Pin lokasi tepat di peta mini interaktif saat meninjau laporan
 * 3. Grafik batang visualisasi statistik tren bulanan kategori bencana
 * 4. Ekspor dokumen resmi ke PDF (arsip digital) dan CSV (analisis Excel)
 * 5. Modul 'Arsip' untuk memisahkan laporan berstatus 'Selesai'
 * 6. Validasi status ('Diproses', 'Divalidasi', 'Selesai') dengan toast notifikasi
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

    // View Mode: 0 = Antrean Aktif, 1 = Modul Arsip (Selesai), 2 = Grafik Statistik
    var dashboardSubTab by remember { mutableStateOf(0) }

    // Filters State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") } // "Semua", "Diproses", "Divalidasi", "Selesai"
    var selectedCategoryFilter by remember { mutableStateOf("Semua Kategori") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDistrictFilter by remember { mutableStateOf("Semua Kecamatan") }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }
    var onlyWithCasualties by remember { mutableStateOf(false) }

    // Auto-Pin Location on Mini Map State
    var pinnedReportForMap by remember {
        mutableStateOf<CitizenDisasterReport?>(reports.firstOrNull { it.status != "Selesai" } ?: reports.firstOrNull())
    }

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

    val categoryOptions = listOf(
        "Semua Kategori",
        "Banjir",
        "Cuaca Ekstrem",
        "Keamanan Pesisir",
        "Gempa Bumi",
        "Tanah Longsor",
        "Pohon Tumbang",
        "Kebakaran Hutan/Lahan",
        "Lainnya"
    )

    val districtOptions = listOf("Semua Kecamatan") + listOf(
        "Kota Mukomuko", "Ipuh", "Teramang Jaya", "Penarik", "Lubuk Pinang",
        "Pondok Suguh", "Air Dikit", "Selagan Raya", "XIV Koto", "Malin Deman",
        "V Koto", "Teras Terunjam", "Sungai Rumbai", "Air Rami", "Air Manjunto"
    )

    // Filtered Reports
    val baseReports = when (dashboardSubTab) {
        1 -> reports.filter { it.status.equals("Selesai", ignoreCase = true) } // Modul Arsip
        else -> reports.filter { !it.status.equals("Selesai", ignoreCase = true) } // Antrean Aktif
    }

    val filteredReports = baseReports.filter { rep ->
        val matchesStatus = when (selectedStatusFilter) {
            "Semua" -> true
            "Diproses" -> rep.status.equals("Diproses", ignoreCase = true) || rep.status.equals("Terverifikasi GPS", ignoreCase = true)
            "Divalidasi" -> rep.status.equals("Divalidasi", ignoreCase = true) || rep.status.contains("TRC", ignoreCase = true)
            "Selesai" -> rep.status.equals("Selesai", ignoreCase = true)
            else -> true
        }

        val matchesCategory = if (selectedCategoryFilter == "Semua Kategori") true else {
            rep.disasterType.contains(selectedCategoryFilter, ignoreCase = true) ||
            (selectedCategoryFilter == "Keamanan Pesisir" && (rep.disasterType.contains("Abrasi", ignoreCase = true) || rep.disasterType.contains("Gelombang", ignoreCase = true))) ||
            (selectedCategoryFilter == "Cuaca Ekstrem" && (rep.disasterType.contains("Angin", ignoreCase = true) || rep.disasterType.contains("Badai", ignoreCase = true)))
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

        matchesStatus && matchesCategory && matchesDistrict && matchesCasualties && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("officer_dashboard_console_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. OFFICER COMMAND BAR WITH EXPORT PDF & CSV BUTTONS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOfficerLoggedIn) Color(0xFF0F172A) else Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("officer_command_bar")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Profile Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOfficerLoggedIn) Color(0xFF16A34A) else Color(0xFFEA580C),
                                modifier = Modifier.size(38.dp)
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
                                        text = "PETUGAS DASHBOARD PUSDALOPS",
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
                                            text = if (isOfficerLoggedIn) "SESI RESMI AKTIF" else "TERKUNCI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isOfficerLoggedIn) officerName else "Autentikasi Petugas Diperlukan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // Logout / Login
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
                                modifier = Modifier.height(34.dp)
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
                        Text(
                            text = "NIP/ID: $officerBadgeId • $officerShift • BPBD Kab. Mukomuko",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // EXPORT DIGITAL ARCHIVE (PDF & CSV) ACTIONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // PDF Export Button
                            Button(
                                onClick = {
                                    val exportList = reports.filter { it.status.equals("Divalidasi", ignoreCase = true) || it.status.equals("Selesai", ignoreCase = true) }
                                    val finalExport = if (exportList.isNotEmpty()) exportList else reports
                                    ReportExporter.exportToPdf(context, finalExport, officerName)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Ekspor PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Ekspor PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // CSV Export Button
                            Button(
                                onClick = {
                                    ReportExporter.exportToCsv(context, reports)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Ekspor CSV",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Ekspor CSV (Excel)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. SUB-TABS: ANTREAN AKTIF vs MODUL ARSIP vs STATISTIK
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to "🚨 Antrean Aktif (${pendingCount + validatedCount})",
                    1 to "🗄️ Modul Arsip ($resolvedCount)",
                    2 to "📊 Tren Bulanan"
                ).forEach { (idx, label) ->
                    val isSel = dashboardSubTab == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) Color.White else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { dashboardSubTab = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color(0xFF0F172A) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 3. STATISTICAL BAR CHART VIEW (IF SELECTED)
        if (dashboardSubTab == 2) {
            item {
                DisasterMonthlyBarChartCard(reports = reports)
            }
        }

        // 4. REAL-TIME KPI CARDS
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metrik Triage & Validasi Laporan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "$totalCount Total Aduan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiMetricCard(
                        title = "Diproses",
                        count = pendingCount,
                        badgeColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        subtitle = "Butuh Validasi",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Divalidasi",
                        count = validatedCount,
                        badgeColor = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        subtitle = "Tim TRC Meluncur",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Selesai",
                        count = resolvedCount,
                        badgeColor = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE),
                        subtitle = "Tuntas / Arsip",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. AUTO-PIN MINI-MAP CARD FOR INSPECTING INCIDENTS
        if (pinnedReportForMap != null) {
            val rep = pinnedReportForMap!!
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Pin Peta Lokasi Tinjauan Laporan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "#${rep.id.takeLast(7)}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Mini Map Canvas with Pin & Radar Pulse
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        ) {
                            MiniMapWithPinCanvas(
                                latitude = rep.latitude,
                                longitude = rep.longitude,
                                disasterType = rep.disasterType
                            )

                            // Coords & District Overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${rep.village}, Kec. ${rep.district} • ${rep.disasterType}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = "GPS: %.4f, %.4f (±%.0fm)".format(rep.latitude, rep.longitude, rep.gpsAccuracyMeters ?: 5f),
                                            fontSize = 9.sp,
                                            color = Color(0xFF86EFAC),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            GoogleMapsHelper.openNavigationRoute(
                                                context = context,
                                                destLat = rep.latitude,
                                                destLon = rep.longitude,
                                                destName = "Bencana ${rep.disasterType} - ${rep.village}"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Rute Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. SEARCH & MULTI-CATEGORY FILTERS
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
                            .height(48.dp)
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Disaster Category Filter Chips (Scrollable)
                    Text(
                        text = "Kategori Bencana:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryOptions) { cat ->
                            val isSel = selectedCategoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) Color(0xFF0F172A) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.clickable { selectedCategoryFilter = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // District Selector Dropdown & Casualties Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { isDistrictDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                                        text = { Text(dist, fontSize = 11.sp) },
                                        onClick = {
                                            selectedDistrictFilter = dist
                                            isDistrictDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

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

        // 7. REPORTS LIST WITH ACTION BUTTONS & STATUS TOASTS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (dashboardSubTab == 1) "Daftar Arsip Laporan Selesai" else "Antrean Penanganan Bencana Aktif",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${filteredReports.size} laporan",
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
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (dashboardSubTab == 1) "Belum Ada Laporan yang Diarsipkan" else "Tidak Ada Laporan Aktif",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Semua laporan dalam kriteria ini telah ditangani atau periksa kata kunci pencarian.",
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
                    onInspectMapPin = {
                        pinnedReportForMap = report
                        Toast.makeText(context, "Pin peta dipusatkan ke #${report.id.takeLast(7)} (${report.village})", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateStatus = { targetStatus ->
                        if (isOfficerLoggedIn) {
                            onUpdateReportStatus(report.id, targetStatus)
                            Toast.makeText(
                                context,
                                "Status laporan #${report.id.takeLast(7)} diperbarui menjadi '$targetStatus' oleh Petugas $officerName",
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

    // 8. OFFICIAL OFFICER FOLLOW-UP NOTE EDITOR DIALOG
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
                        text = "Catatan Tindak Lanjut Personel TRC:",
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
 * Visualisasi Grafik Batang Tren Bulanan Kategori Bencana
 */
@Composable
private fun DisasterMonthlyBarChartCard(
    reports: List<CitizenDisasterReport>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Grafik Tren Kategori Bencana Bulanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Tahun 2026",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Categories tally
            val categories = listOf(
                Triple("Banjir", reports.count { it.disasterType.contains("Banjir", true) }, Color(0xFF2563EB)),
                Triple("Badai", reports.count { it.disasterType.contains("Angin", true) || it.disasterType.contains("Badai", true) }, Color(0xFFD97706)),
                Triple("Abrasi", reports.count { it.disasterType.contains("Gelombang", true) || it.disasterType.contains("Abrasi", true) }, Color(0xFF0284C7)),
                Triple("Longsor", reports.count { it.disasterType.contains("Longsor", true) }, Color(0xFFB45309)),
                Triple("Gempa", reports.count { it.disasterType.contains("Gempa", true) }, Color(0xFFDC2626)),
                Triple("Lainnya", reports.count { !it.disasterType.contains("Banjir", true) && !it.disasterType.contains("Angin", true) && !it.disasterType.contains("Gelombang", true) && !it.disasterType.contains("Longsor", true) && !it.disasterType.contains("Gempa", true) }, Color(0xFF64748B))
            )

            val maxCount = (categories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

            // Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val barWidth = 24.dp.toPx()
                val spacing = (size.width - (categories.size * barWidth)) / (categories.size + 1)
                val chartHeight = size.height - 20f

                categories.forEachIndexed { i, (_, count, color) ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = (count.toFloat() / maxCount) * chartHeight
                    val y = size.height - barHeight

                    // Draw bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                categories.forEach { (name, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = name,
                            fontSize = 9.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini Map Canvas with Pin and Pulsing Radar Circle
 */
@Composable
private fun MiniMapWithPinCanvas(
    latitude: Double,
    longitude: Double,
    disasterType: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pin_radar_anim")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Grid lines (GIS Map aesthetic)
        val gridStep = 30f
        var gx = 0f
        while (gx <= size.width) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(gx, 0f),
                end = Offset(gx, size.height),
                strokeWidth = 1f
            )
            gx += gridStep
        }
        var gy = 0f
        while (gy <= size.height) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1f
            )
            gy += gridStep
        }

        // Pulsing radar circles
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = (1f - (pulseRadius / 35f)).coerceIn(0f, 0.7f)),
            radius = pulseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Center Pin
        drawCircle(
            color = Color(0xFFDC2626),
            radius = 7f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(centerX, centerY)
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
 * Kartu Insiden Petugas Responsif dengan Aksi Validasi & Auto-Pin
 */
@Composable
private fun OfficerIncidentReportCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    onInspectMapPin: () -> Unit,
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
            // Header
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

            // Location & Date
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

            // Casualties banner if any
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
                            text = "Korban: ${report.casualtiesDead} Meninggal • ${report.casualtiesInjured} Luka • ${report.casualtiesMissing} Hilang",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Photo thumbnail & Inspect map pin button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!report.photoUri.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onViewPhoto(report.photoUri) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(48.dp)
                        ) {
                            AsyncImage(
                                model = report.photoUri,
                                contentDescription = "Foto Kejadian",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Foto Bukti Warga", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Sentuh untuk zoom", fontSize = 9.sp, color = Color(0xFF15803D))
                        }
                    }
                } else {
                    Text(
                        text = "Laporan tanpa lampiran foto",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Tap to pin on map
                OutlinedButton(
                    onClick = onInspectMapPin,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Tinjau Peta", fontSize = 10.sp, color = Color(0xFF0284C7))
                }
            }

            // Official notes if any
            if (report.additionalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catatan Tindak Lanjut TRC:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
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

            // Action Buttons: Validasi, Selesai, Tindak Lanjut, Rute Maps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status buttons
                if (!report.status.equals("Divalidasi", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Divalidasi") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Validasi TRC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!report.status.equals("Selesai", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Selesai") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onUpdateStatus("Diproses") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text(text = "Buka Kembali", fontSize = 10.sp)
                    }
                }

                // Note editor
                OutlinedButton(
                    onClick = onOpenNoteEditor,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Catatan", fontSize = 10.sp)
                }

                // Google Maps route
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
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
