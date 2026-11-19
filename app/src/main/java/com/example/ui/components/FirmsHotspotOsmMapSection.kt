package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.FirmsSensorSource
import com.example.data.api.HotspotConfidenceLevel
import com.example.data.api.NasaFirmsHotspot
import com.example.data.api.NasaFirmsHotspotService
import com.example.data.local.KarhutlaHotspotEntity
import com.example.data.repository.KarhutlaHotspotRepository
import com.example.data.repository.KarhutlaSyncSummary
import com.example.model.District
import com.example.service.KarhutlaSyncScheduler
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.GoogleMapsHelper
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Locale

enum class OsmBaseLayerType(val label: String) {
    MAPNIK("OSM Standar"),
    OPEN_TOPO("OpenTopoMap")
}

enum class HotspotFilterCategory(val label: String) {
    ALL("Semua Titik"),
    FIRMS("Satelit FIRMS"),
    VERIFIED_MATCH("Terkonfirmasi (FIRMS+BMKG)"),
    HIGH("Tinggi (High)"),
    BMKG("Stasiun/Darat BMKG")
}

/**
 * Card "Monitoring Karhutla":
 * - Synchronizes VIIRS (NASA FIRMS) every 1–3 hours and BMKG every 3–6 hours via WorkManager.
 * - Stores time as UTC epoch millis (`acqDateTime: Long`) and uses deterministic `sourceId`
 *   with `INSERT IGNORE` (`"$satellite|$acqDateTime|$lat|$lon"` for FIRMS,
 *   `"$acqDateTime|$lat|$lon|$confidenceLabel"` for BMKG).
 * - Runs cross-source matching and 30–90 day retention pruning inside a single `@Transaction` in repository.
 * - Stores only coordinates and status in Room, rendering OpenStreetMap markers dynamically when opened.
 */
@Composable
fun FirmsHotspotOsmMapSection(
    currentDistrict: District,
    onCall117: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { KarhutlaHotspotRepository(context) }

    // Configure osmdroid user-agent once
    remember {
        val appCtx = context.applicationContext
        Configuration.getInstance().load(appCtx, PreferenceManager.getDefaultSharedPreferences(appCtx))
        Configuration.getInstance().userAgentValue = "${appCtx.packageName}/1.0.5 (BPBD-Mukomuko-SIPAGAR)"
        true
    }

    var selectedSensor by remember { mutableStateOf(FirmsSensorSource.VIIRS_SNPP) }
    var selectedDayRange by remember { mutableIntStateOf(2) } // 1 = 24h, 2 = 48h, 5 = 5 days
    var selectedBaseLayer by remember { mutableStateOf(OsmBaseLayerType.MAPNIK) }
    var showFirmsWmsOverlay by remember { mutableStateOf(true) }
    var showDistrictMarkers by remember { mutableStateOf(true) }
    var isSimulationMode by remember { mutableStateOf(false) }
    var showWorkManagerConfig by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf(HotspotFilterCategory.ALL) }

    var firmsIntervalHours by remember { mutableIntStateOf(repository.getFirmsIntervalHours()) }
    var bmkgIntervalHours by remember { mutableIntStateOf(repository.getBmkgIntervalHours()) }
    var retentionDays by remember { mutableIntStateOf(repository.getRetentionDays()) }

    var isLoading by remember { mutableStateOf(false) }
    var syncSummary by remember { mutableStateOf<KarhutlaSyncSummary?>(null) }
    var selectedHotspotId by remember { mutableStateOf<String?>(null) }
    var osmMapViewRef by remember { mutableStateOf<MapView?>(null) }

    val mukomukoCenter = remember { GeoPoint(-2.7600, 101.3800) }

    val lookbackEpochMillisUtc = remember(selectedDayRange) {
        System.currentTimeMillis() - (selectedDayRange.toLong() * 24L * 3600_000L)
    }

    // Observe primitive coordinates & status reactively from Room (never storing Marker objects)
    val dbEntities by remember(lookbackEpochMillisUtc) {
        repository.observeHotspotsSince(lookbackEpochMillisUtc)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allHotspots = remember(dbEntities) {
        dbEntities.map { NasaFirmsHotspot.fromEntity(it) }
    }

    fun triggerSyncAndMatch(forceBmkg: Boolean = false) {
        coroutineScope.launch {
            isLoading = true
            val summary = repository.syncBothSourcesAndMatchInTransaction(
                sensor = selectedSensor,
                dayRange = selectedDayRange,
                forceBmkgCheck = forceBmkg,
                enableSimulation = isSimulationMode
            )
            syncSummary = summary
            isLoading = false
        }
    }

    LaunchedEffect(selectedSensor, selectedDayRange, isSimulationMode) {
        triggerSyncAndMatch(forceBmkg = false)
    }

    DisposableEffect(Unit) {
        onDispose {
            osmMapViewRef?.onDetach()
        }
    }

    val filteredHotspots = remember(allHotspots, selectedFilterCategory) {
        when (selectedFilterCategory) {
            HotspotFilterCategory.ALL -> allHotspots
            HotspotFilterCategory.FIRMS -> allHotspots.filter { it.sourceType == KarhutlaHotspotEntity.SOURCE_FIRMS }
            HotspotFilterCategory.VERIFIED_MATCH -> allHotspots.filter { it.isMatchedWithCrossSource }
            HotspotFilterCategory.HIGH -> allHotspots.filter { it.confidenceLevel == HotspotConfidenceLevel.HIGH }
            HotspotFilterCategory.BMKG -> allHotspots.filter { it.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG }
        }
    }

    val selectedHotspot = remember(allHotspots, selectedHotspotId) {
        allHotspots.find { it.id == selectedHotspotId }
    }

    val matchedCount = remember(allHotspots) { allHotspots.count { it.isMatchedWithCrossSource } }
    val highCount = remember(allHotspots) { allHotspots.count { it.confidenceLevel == HotspotConfidenceLevel.HIGH } }
    val firmsCount = remember(allHotspots) { allHotspots.count { it.sourceType == KarhutlaHotspotEntity.SOURCE_FIRMS } }
    val bmkgCount = remember(allHotspots) { allHotspots.count { it.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG } }

    val containerBg = if (isDark) Color(0xFF0F172A) else Color.White
    val borderColor = if (matchedCount > 0 || highCount > 0) Color(0xFFDC2626).copy(alpha = 0.6f)
    else if (isDark) OrangePrimary.copy(alpha = 0.4f) else OrangePrimary.copy(alpha = 0.25f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firms_hotspot_osm_map_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. HEADER: Title ("Monitoring Karhutla"), Subtitle, WorkManager Config & Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFDC2626), Color(0xFFEA580C))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Monitoring Karhutla",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monitoring Karhutla",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Sinkronisasi VIIRS (FIRMS) & BMKG • OpenStreetMap Mukomuko",
                            fontSize = 11.sp,
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showWorkManagerConfig = !showWorkManagerConfig },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (showWorkManagerConfig) OrangePrimary else OrangePrimary.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .testTag("btn_toggle_karhutla_workmanager_config")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pengaturan WorkManager & Retensi",
                            tint = if (showWorkManagerConfig) Color.White else OrangePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            KarhutlaSyncScheduler.triggerChainedSyncNow(context)
                            triggerSyncAndMatch(forceBmkg = true)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = OrangePrimary.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .testTag("btn_refresh_firms_osm_hotspots")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = OrangePrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronkan FIRMS & BMKG Sekarang",
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. WORKMANAGER PERIODIC SCHEDULE & DATA RETENTION PANEL
            AnimatedVisibility(visible = showWorkManagerConfig) {
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("karhutla_workmanager_settings_panel")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Konfigurasi WorkManager Periodik & Retensi Database (UTC Epoch)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // FIRMS Interval (1–3 Jam)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interval VIIRS (FIRMS):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3).forEach { hrs ->
                                    val selected = firmsIntervalHours == hrs
                                    Surface(
                                        color = if (selected) Color(0xFFDC2626) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                        modifier = Modifier.clickable {
                                            firmsIntervalHours = hrs
                                            KarhutlaSyncScheduler.schedulePeriodicSync(
                                                context = context,
                                                firmsIntervalHours = hrs,
                                                bmkgIntervalHours = bmkgIntervalHours,
                                                replaceExisting = true
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "$hrs Jam",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // BMKG Interval (3–6 Jam)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interval BMKG (Sopan):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 4, 6).forEach { hrs ->
                                    val selected = bmkgIntervalHours == hrs
                                    Surface(
                                        color = if (selected) Color(0xFF0284C7) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                        modifier = Modifier.clickable {
                                            bmkgIntervalHours = hrs
                                            KarhutlaSyncScheduler.schedulePeriodicSync(
                                                context = context,
                                                firmsIntervalHours = firmsIntervalHours,
                                                bmkgIntervalHours = hrs,
                                                replaceExisting = true
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "$hrs Jam",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Data Retention (30–90 Hari)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Retensi Hapus Otomatis:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(30, 60, 90).forEach { days ->
                                    val selected = retentionDays == days
                                    Surface(
                                        color = if (selected) Color(0xFF15803D) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF15803D)),
                                        modifier = Modifier.clickable {
                                            retentionDays = days
                                            repository.setRetentionDays(days)
                                            triggerSyncAndMatch(forceBmkg = false)
                                        }
                                    ) {
                                        Text(
                                            text = "$days Hari",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SIMULATION MODE WARNING BANNER
            AnimatedVisibility(visible = isSimulationMode) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("firms_osm_simulation_mode_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MODE SIMULASI LATIHAN POSKO: Menguji sinkronisasi @Transaction & cross-matching deterministik antara titik panas VIIRS (FIRMS) dan BMKG.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. SUMMARY METRICS BAR (Total, Terkonfirmasi FIRMS+BMKG, High, Sumber BMKG)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsmHotspotStatChip(
                    title = "Total Titik",
                    count = allHotspots.size,
                    badgeColor = Color(0xFF0F172A),
                    isSelected = selectedFilterCategory == HotspotFilterCategory.ALL,
                    onClick = { selectedFilterCategory = HotspotFilterCategory.ALL },
                    modifier = Modifier.weight(1f)
                )
                OsmHotspotStatChip(
                    title = "FIRMS+BMKG",
                    count = matchedCount,
                    badgeColor = Color(0xFF7C3AED),
                    isSelected = selectedFilterCategory == HotspotFilterCategory.VERIFIED_MATCH,
                    onClick = {
                        selectedFilterCategory =
                            if (selectedFilterCategory == HotspotFilterCategory.VERIFIED_MATCH) HotspotFilterCategory.ALL
                            else HotspotFilterCategory.VERIFIED_MATCH
                    },
                    modifier = Modifier.weight(1.1f)
                )
                OsmHotspotStatChip(
                    title = "Tinggi (High)",
                    count = highCount,
                    badgeColor = Color(0xFFDC2626),
                    isSelected = selectedFilterCategory == HotspotFilterCategory.HIGH,
                    onClick = {
                        selectedFilterCategory =
                            if (selectedFilterCategory == HotspotFilterCategory.HIGH) HotspotFilterCategory.ALL
                            else HotspotFilterCategory.HIGH
                    },
                    modifier = Modifier.weight(1f)
                )
                OsmHotspotStatChip(
                    title = "Sumber BMKG",
                    count = bmkgCount,
                    badgeColor = Color(0xFF0284C7),
                    isSelected = selectedFilterCategory == HotspotFilterCategory.BMKG,
                    onClick = {
                        selectedFilterCategory =
                            if (selectedFilterCategory == HotspotFilterCategory.BMKG) HotspotFilterCategory.ALL
                            else HotspotFilterCategory.BMKG
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. SENSOR & TIME RANGE CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FirmsSensorSource.entries.forEach { sensor ->
                    val selected = selectedSensor == sensor
                    FilterChip(
                        selected = selected,
                        onClick = { selectedSensor = sensor },
                        label = {
                            Text(
                                text = "${sensor.label} (${sensor.resolution})",
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SatelliteAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                listOf(1 to "24 Jam", 2 to "48 Jam", 5 to "5 Hari").forEach { (days, label) ->
                    val selected = selectedDayRange == days
                    FilterChip(
                        selected = selected,
                        onClick = { selectedDayRange = days },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = OrangeOnPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. OPENSTREETMAP BASE LAYER & OVERLAY CONTROLS
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OsmBaseLayerType.entries.forEach { layerType ->
                                val isSelected = selectedBaseLayer == layerType
                                Surface(
                                    color = if (isSelected) Color(0xFF15803D) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF15803D) else Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier.clickable { selectedBaseLayer = layerType }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = layerType.label,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = if (showDistrictMarkers) Color(0xFF0284C7) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            modifier = Modifier.clickable { showDistrictMarkers = !showDistrictMarkers }
                        ) {
                            Text(
                                text = "15 Kec.",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showDistrictMarkers) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = showFirmsWmsOverlay,
                                onCheckedChange = { showFirmsWmsOverlay = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFDC2626)),
                                modifier = Modifier.testTag("switch_osm_firms_wms_layer")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Layer WMS FIRMS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isSimulationMode,
                                onCheckedChange = { isSimulationMode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFD97706)),
                                modifier = Modifier.testTag("switch_osm_firms_simulation_mode")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulasi Posko",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. OPENSTREETMAP VIEWPORT WITH DYNAMICALLY RENDERED MARKERS FROM ROOM COORDINATES
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color(0xFF090D16))
                    .testTag("firms_osm_map_viewport_box")
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("osm_firms_map_view"),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(9.8)
                            controller.setCenter(mukomukoCenter)
                            osmMapViewRef = this
                        }
                    },
                    update = { mapView ->
                        val targetTileSource = when (selectedBaseLayer) {
                            OsmBaseLayerType.MAPNIK -> TileSourceFactory.MAPNIK
                            OsmBaseLayerType.OPEN_TOPO -> TileSourceFactory.OpenTopo
                        }
                        if (mapView.tileProvider.tileSource != targetTileSource) {
                            mapView.setTileSource(targetTileSource)
                        }

                        mapView.overlays.clear()

                        // Layer 1: NASA FIRMS / GIBS Satellite Thermal Anomaly WMS TilesOverlay
                        if (showFirmsWmsOverlay) {
                            val firmsTileSource = createFirmsWmsTileSource(selectedSensor)
                            val tileProvider = MapTileProviderBasic(mapView.context.applicationContext, firmsTileSource)
                            val tilesOverlay = TilesOverlay(tileProvider, mapView.context).apply {
                                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                                loadingLineColor = android.graphics.Color.TRANSPARENT
                            }
                            mapView.overlays.add(tilesOverlay)
                        }

                        // Layer 2: Kabupaten Mukomuko Administrative Perimeter Polygon
                        val mukomukoPolygon = Polygon(mapView).apply {
                            points = listOf(
                                GeoPoint(-2.3800, 101.0800),
                                GeoPoint(-2.3800, 101.3500),
                                GeoPoint(-2.6500, 101.5500),
                                GeoPoint(-2.9800, 101.8200),
                                GeoPoint(-3.2400, 101.8200),
                                GeoPoint(-3.2400, 101.5800),
                                GeoPoint(-2.8500, 101.2800),
                                GeoPoint(-2.5800, 101.0800)
                            )
                            fillPaint.color = android.graphics.Color.argb(18, 234, 88, 12)
                            outlinePaint.color = android.graphics.Color.rgb(234, 88, 12)
                            outlinePaint.strokeWidth = 4f
                            title = "Wilayah Pantau Karhutla Kab. Mukomuko"
                        }
                        mapView.overlays.add(mukomukoPolygon)

                        // Layer 3: 15 Kecamatan Reference Markers
                        if (showDistrictMarkers) {
                            val districtIcon = createCircleMarkerDrawable(
                                context = mapView.context,
                                fillColor = android.graphics.Color.rgb(2, 132, 199),
                                strokeColor = android.graphics.Color.WHITE,
                                sizeDp = 12
                            )
                            NasaFirmsHotspotService.MUKOMUKO_15_DISTRICTS.forEach { dist ->
                                val distMarker = Marker(mapView).apply {
                                    position = GeoPoint(dist.latitude, dist.longitude)
                                    title = "Kecamatan ${dist.name}"
                                    snippet = "Titik Pantau Wilayah BPBD Kab. Mukomuko"
                                    icon = districtIcon
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                }
                                mapView.overlays.add(distMarker)
                            }
                        }

                        // Layer 4: Dynamically rendered Markers & Thermal Circles from stored coordinates/status
                        filteredHotspots.forEach { hotspot ->
                            val geoPos = GeoPoint(hotspot.latitude, hotspot.longitude)
                            val (fillArgb, strokeRgb) = when {
                                hotspot.isMatchedWithCrossSource -> Pair(
                                    android.graphics.Color.argb(105, 124, 58, 237), // Ungu Terkonfirmasi Ganda (FIRMS+BMKG)
                                    android.graphics.Color.rgb(124, 58, 237)
                                )
                                hotspot.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG -> Pair(
                                    android.graphics.Color.argb(90, 2, 132, 199), // Biru BMKG
                                    android.graphics.Color.rgb(2, 132, 199)
                                )
                                hotspot.confidenceLevel == HotspotConfidenceLevel.HIGH -> Pair(
                                    android.graphics.Color.argb(95, 220, 38, 38),
                                    android.graphics.Color.rgb(220, 38, 38)
                                )
                                hotspot.confidenceLevel == HotspotConfidenceLevel.NOMINAL -> Pair(
                                    android.graphics.Color.argb(85, 234, 88, 12),
                                    android.graphics.Color.rgb(234, 88, 12)
                                )
                                else -> Pair(
                                    android.graphics.Color.argb(75, 217, 119, 6),
                                    android.graphics.Color.rgb(217, 119, 6)
                                )
                            }

                            val radiusMeters = (550.0 + (hotspot.frp * 25.0)).coerceIn(500.0, 2400.0)
                            val thermalCircle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(geoPos, radiusMeters)
                                fillPaint.color = fillArgb
                                outlinePaint.color = strokeRgb
                                outlinePaint.strokeWidth = if (hotspot.isMatchedWithCrossSource) 4.5f else 3f
                                setOnClickListener { _, _, _ ->
                                    selectedHotspotId = hotspot.id
                                    true
                                }
                            }
                            mapView.overlays.add(thermalCircle)

                            val isSelected = selectedHotspotId == hotspot.id
                            val hotspotIcon = createHotspotBeaconDrawable(
                                context = mapView.context,
                                coreColor = strokeRgb,
                                sourceType = hotspot.sourceType,
                                isMatched = hotspot.isMatchedWithCrossSource,
                                isSelected = isSelected
                            )

                            val statusPrefix = if (hotspot.isMatchedWithCrossSource) {
                                "[TERKONFIRMASI FIRMS+BMKG]"
                            } else if (hotspot.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG) {
                                "[BMKG - PANTAUAN DARAT]"
                            } else {
                                "[FIRMS - SATELIT]"
                            }

                            val spotMarker = Marker(mapView).apply {
                                position = geoPos
                                title = "$statusPrefix Kec. ${hotspot.nearestSubDistrict} (${String.format(Locale.US, "%.1f°C", hotspot.brightnessCelsius)})"
                                snippet = "ID: ${hotspot.id} • ${hotspot.acqTimeWib}"
                                icon = hotspotIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                setOnMarkerClickListener { marker, _ ->
                                    selectedHotspotId = hotspot.id
                                    marker.showInfoWindow()
                                    true
                                }
                            }
                            mapView.overlays.add(spotMarker)
                        }

                        mapView.invalidate()
                    }
                )

                // Top-Left Satellite vs Ground-Based Quick Map Overlay Legend
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .testTag("karhutla_map_overlay_mini_legend")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "IKON PETA: SATELIT × DARAT",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KarhutlaMarkerIconPreview(
                                sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                                isMatched = false,
                                coreColor = Color(0xFFDC2626),
                                sizeDp = 12
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("FIRMS • Satelit (${firmsIntervalHours}j)", fontSize = 9.5.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KarhutlaMarkerIconPreview(
                                sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
                                isMatched = false,
                                coreColor = Color(0xFF0284C7),
                                sizeDp = 12
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("BMKG • Stasiun/Darat (${bmkgIntervalHours}j)", fontSize = 9.5.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KarhutlaMarkerIconPreview(
                                sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                                isMatched = true,
                                coreColor = Color(0xFF7C3AED),
                                sizeDp = 12
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Terkonfirmasi (FIRMS+BMKG)", fontSize = 9.5.sp, color = Color.White)
                        }
                    }
                }

                // Top-Right Quick Focus Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier.clickable {
                            val distCoord = NasaFirmsHotspotService.MUKOMUKO_15_DISTRICTS.find {
                                it.name.equals(currentDistrict.name, ignoreCase = true)
                            }
                            val target = if (distCoord != null) {
                                GeoPoint(distCoord.latitude, distCoord.longitude)
                            } else {
                                GeoPoint(currentDistrict.latitude, currentDistrict.longitude)
                            }
                            osmMapViewRef?.controller?.animateTo(target, 11.8, 650L)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Fokus Kecamatan",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentDistrict.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom-Right Zoom & Reset Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { osmMapViewRef?.controller?.zoomIn() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Perbesar Peta OSM", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { osmMapViewRef?.controller?.zoomOut() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Perkecil Peta OSM", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(
                        color = OrangePrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable {
                                osmMapViewRef?.controller?.animateTo(mukomukoCenter, 9.8, 600L)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Peta Mukomuko", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7B. DEDICATED MAP LEGEND COMPONENT: FIRMS (SATELLITE) VS BMKG (GROUND-BASED)
            KarhutlaHotspotLegendComponent(
                firmsCount = firmsCount,
                bmkgCount = bmkgCount,
                matchedCount = matchedCount,
                firmsIntervalHours = firmsIntervalHours,
                bmkgIntervalHours = bmkgIntervalHours,
                selectedFilterCategory = selectedFilterCategory,
                onSelectFilterCategory = { category ->
                    selectedFilterCategory = if (selectedFilterCategory == category) {
                        HotspotFilterCategory.ALL
                    } else {
                        category
                    }
                },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 8. WORKMANAGER & CROSS-MATCHING STATUS BAR
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (matchedCount > 0) Icons.Default.Verified else Icons.Default.Public,
                        contentDescription = null,
                        tint = if (matchedCount > 0) Color(0xFF7C3AED) else OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncSummary?.statusMessage
                            ?: "WorkManager terjadwal: FIRMS tiap $firmsIntervalHours jam, BMKG tiap $bmkgIntervalHours jam • Retensi $retentionDays hari.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 9. SELECTED HOTSPOT DETAIL INSPECTOR CARD (Shows deterministic sourceId & UTC epoch millis)
            AnimatedVisibility(visible = selectedHotspot != null) {
                selectedHotspot?.let { spot ->
                    val confColor = when {
                        spot.isMatchedWithCrossSource -> Color(0xFF7C3AED)
                        spot.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG -> Color(0xFF0284C7)
                        spot.confidenceLevel == HotspotConfidenceLevel.HIGH -> Color(0xFFDC2626)
                        spot.confidenceLevel == HotspotConfidenceLevel.NOMINAL -> Color(0xFFEA580C)
                        else -> Color(0xFFD97706)
                    }

                    Surface(
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, confColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("selected_firms_osm_hotspot_detail_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(confColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (spot.isMatchedWithCrossSource) Icons.Default.Verified else Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Titik Panas Kec. ${spot.nearestSubDistrict}" +
                                                if (spot.isSimulation) " [SIMULASI]" else "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val statusLabel = if (spot.isMatchedWithCrossSource) {
                                            "TERKONFIRMASI GANDA (FIRMS + BMKG • Selisih ±${String.format(Locale.US, "%.2f", spot.matchedDistanceKm ?: 0.0)} km)"
                                        } else {
                                            "Sumber: ${spot.sourceType} • Confidence: ${spot.confidenceLevel.label}"
                                        }
                                        Text(
                                            text = statusLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = confColor
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedHotspotId = null },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup detail hotspot",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Deterministic sourceId & Partner ID
                            Surface(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Text(
                                        text = "Deterministic sourceId (INSERT IGNORE):",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = spot.id,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!spot.matchedPartnerSourceId.isNullOrBlank()) {
                                        Text(
                                            text = "Matched Partner ID: ${spot.matchedPartnerSourceId}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OsmHotspotTelemetryItem(
                                    label = "Waktu Akuisisi (UTC Epoch)",
                                    value = "${spot.acqDateTime} ms",
                                    modifier = Modifier.weight(1f)
                                )
                                OsmHotspotTelemetryItem(
                                    label = "Waktu Lokal (WIB)",
                                    value = spot.acqTimeWib,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OsmHotspotTelemetryItem(
                                    label = "Suhu & FRP",
                                    value = String.format(Locale.US, "%.1f°C • %.1f MW", spot.brightnessCelsius, spot.frp),
                                    modifier = Modifier.weight(1f)
                                )
                                OsmHotspotTelemetryItem(
                                    label = "Koordinat GPS",
                                    value = String.format(Locale.US, "%.5f, %.5f", spot.latitude, spot.longitude),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        GoogleMapsHelper.openNavigationRoute(
                                            context = context,
                                            destLat = spot.latitude,
                                            destLon = spot.longitude,
                                            destName = "Hotspot Karhutla ${spot.nearestSubDistrict}"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 38.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Navigasi Rute TRC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onCall117,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Siaga 117", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 10. QUICK HOTSPOT LIST CHIPS
            if (filteredHotspots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Daftar Titik Panas Tersinkronisasi (${filteredHotspots.size} Titik):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredHotspots.forEach { spot ->
                        val isSelected = selectedHotspotId == spot.id
                        val chipColor = when {
                            spot.isMatchedWithCrossSource -> Color(0xFF7C3AED)
                            spot.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG -> Color(0xFF0284C7)
                            spot.confidenceLevel == HotspotConfidenceLevel.HIGH -> Color(0xFFDC2626)
                            spot.confidenceLevel == HotspotConfidenceLevel.NOMINAL -> Color(0xFFEA580C)
                            else -> Color(0xFFD97706)
                        }
                        Surface(
                            color = if (isSelected) chipColor else chipColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, chipColor),
                            modifier = Modifier.clickable {
                                selectedHotspotId = spot.id
                                osmMapViewRef?.controller?.animateTo(
                                    GeoPoint(spot.latitude, spot.longitude),
                                    12.2,
                                    650L
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (spot.isMatchedWithCrossSource) Icons.Default.Verified else Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else chipColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Kec. ${spot.nearestSubDistrict} (${spot.sourceType})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (spot.isMatchedWithCrossSource) {
                                            "Terkonfirmasi FIRMS+BMKG • ${String.format(Locale.US, "%.1f°C", spot.brightnessCelsius)}"
                                        } else {
                                            String.format(Locale.US, "%.1f°C • FRP %.1f MW", spot.brightnessCelsius, spot.frp)
                                        },
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createFirmsWmsTileSource(sensor: FirmsSensorSource): OnlineTileSourceBase {
    return object : OnlineTileSourceBase(
        "NASA_FIRMS_WMS_${sensor.name}",
        5,
        16,
        256,
        ".png",
        arrayOf("https://gibs.earthdata.nasa.gov/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return NasaFirmsHotspotService.buildOsmFirmsWmsTileUrl(
                zoom = zoom,
                tileX = x,
                tileY = y,
                sensor = sensor
            )
        }
    }
}

private fun createCircleMarkerDrawable(
    context: Context,
    fillColor: Int,
    strokeColor: Int,
    sizeDp: Int
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val radius = px / 2f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = 2f * density
    }
    canvas.drawCircle(radius, radius, radius - (1.5f * density), fillPaint)
    canvas.drawCircle(radius, radius, radius - (1.5f * density), strokePaint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun createHotspotBeaconDrawable(
    context: Context,
    coreColor: Int,
    sourceType: String,
    isMatched: Boolean,
    isSelected: Boolean
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizeDp = if (isSelected) 32 else 24
    val px = (sizeDp * density).toInt().coerceAtLeast(24)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val center = px / 2f

    // Outer thermal / sensor glow ring
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = coreColor
        alpha = if (isSelected) 115 else 80
    }
    canvas.drawCircle(center, center, center, haloPaint)

    if (sourceType == KarhutlaHotspotEntity.SOURCE_BMKG && !isMatched) {
        // BMKG Ground-Based Station Icon: Distinct Diamond (Belah Ketupat) + Crosshair
        val r = center * 0.72f
        val diamondPath = AndroidPath().apply {
            moveTo(center, center - r)
            lineTo(center + r, center)
            lineTo(center, center + r)
            lineTo(center - r, center)
            close()
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = coreColor
        }
        canvas.drawPath(diamondPath, corePaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSelected) android.graphics.Color.rgb(251, 191, 36) else android.graphics.Color.WHITE
            strokeWidth = (if (isSelected) 2.4f else 1.8f) * density
        }
        canvas.drawPath(diamondPath, strokePaint)

        // Inner ground-station crosshair & center dot
        val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
            strokeWidth = 1.3f * density
        }
        val cr = center * 0.32f
        canvas.drawLine(center - cr, center, center + cr, center, crossPaint)
        canvas.drawLine(center, center - cr, center, center + cr, crossPaint)
    } else if (isMatched) {
        // Cross-Matched (FIRMS Satellite + BMKG Ground): Dual-Ring Gold & Purple Verified Badge
        val goldRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.rgb(251, 191, 36)
            strokeWidth = 2.2f * density
        }
        canvas.drawCircle(center, center, center * 0.84f, goldRingPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = coreColor
        }
        canvas.drawCircle(center, center, center * 0.62f, corePaint)

        val innerDiamond = AndroidPath().apply {
            val dr = center * 0.34f
            moveTo(center, center - dr)
            lineTo(center + dr, center)
            lineTo(center, center + dr)
            lineTo(center - dr, center)
            close()
        }
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
        }
        canvas.drawPath(innerDiamond, sparkPaint)
    } else {
        // FIRMS Satellite Thermal Beacon: Concentric Circular Thermal Ring + Flame Spark Core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = coreColor
        }
        canvas.drawCircle(center, center, center * 0.65f, corePaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isSelected) android.graphics.Color.rgb(251, 191, 36) else android.graphics.Color.WHITE
            strokeWidth = (if (isSelected) 2.5f else 1.8f) * density
        }
        canvas.drawCircle(center, center, center * 0.65f, strokePaint)

        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
        }
        canvas.drawCircle(center, center, center * 0.23f, sparkPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Dedicated Legend Component for Monitoring Karhutla Tab:
 * Helps users visually differentiate between FIRMS (Satellite) circular thermal icons
 * and BMKG (Ground-based) diamond sensor icons on the OpenStreetMap viewport,
 * as well as cross-matched (FIRMS + BMKG) verified hotspots and confidence levels.
 */
@Composable
fun KarhutlaHotspotLegendComponent(
    firmsCount: Int,
    bmkgCount: Int,
    matchedCount: Int,
    firmsIntervalHours: Int,
    bmkgIntervalHours: Int,
    selectedFilterCategory: HotspotFilterCategory,
    onSelectFilterCategory: (HotspotFilterCategory) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("karhutla_hotspot_legend_component")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Legenda Ikon Titik Panas (Satelit FIRMS vs Darat BMKG)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedFilterCategory != HotspotFilterCategory.ALL) {
                    Surface(
                        color = OrangePrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onSelectFilterCategory(HotspotFilterCategory.ALL) }
                            .testTag("btn_reset_legend_filter")
                    ) {
                        Text(
                            text = "Reset Filter",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ketuk kartu legenda di bawah untuk memfilter tampilan ikon titik panas pada peta:",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Side-by-side comparison: FIRMS (Satellite) vs BMKG (Ground-Based)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. FIRMS (Satellite) Legend Card
                KarhutlaLegendSourceCard(
                    title = "FIRMS (Satelit)",
                    subtitle = "Lingkaran Termal • VIIRS/MODIS",
                    description = "Deteksi anomali suhu satelit polar NASA/NOAA (resolusi 375m). Sinkronisasi tiap $firmsIntervalHours jam.",
                    count = firmsCount,
                    accentColor = Color(0xFFDC2626),
                    badgeIcon = Icons.Default.SatelliteAlt,
                    sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                    isMatched = false,
                    isSelected = selectedFilterCategory == HotspotFilterCategory.FIRMS,
                    onClick = { onSelectFilterCategory(HotspotFilterCategory.FIRMS) },
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("legend_item_firms_satellite")
                )

                // 2. BMKG (Ground-Based) Legend Card
                KarhutlaLegendSourceCard(
                    title = "BMKG (Pantauan Darat)",
                    subtitle = "Belah Ketupat • Stasiun/Darat",
                    description = "Jaringan pengamatan berbasis darat, radar cuaca & verifikasi lapangan BMKG. Sinkronisasi tiap $bmkgIntervalHours jam.",
                    count = bmkgCount,
                    accentColor = Color(0xFF0284C7),
                    badgeIcon = Icons.Default.Radar,
                    sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
                    isMatched = false,
                    isSelected = selectedFilterCategory == HotspotFilterCategory.BMKG,
                    onClick = { onSelectFilterCategory(HotspotFilterCategory.BMKG) },
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("legend_item_bmkg_ground")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Cross-Matched (FIRMS + BMKG) & Confidence Level Row
            val isVerifiedSelected = selectedFilterCategory == HotspotFilterCategory.VERIFIED_MATCH
            Surface(
                color = if (isVerifiedSelected) Color(0xFF7C3AED).copy(alpha = 0.16f)
                else if (isDark) Color(0xFF0F172A) else Color.White,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = if (isVerifiedSelected) 1.5.dp else 1.dp,
                    color = if (isVerifiedSelected) Color(0xFF7C3AED) else Color(0xFF7C3AED).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFilterCategory(HotspotFilterCategory.VERIFIED_MATCH) }
                    .testTag("legend_item_verified_cross_match")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KarhutlaMarkerIconPreview(
                        sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                        isMatched = true,
                        coreColor = Color(0xFF7C3AED),
                        sizeDp = 26
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Terkonfirmasi Ganda (FIRMS Satelit + BMKG Darat)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Cincin Emas-Ungu: Titik panas satelit FIRMS yang cocok secara spasial-temporal (≤ 2.5 km & ≤ 6 jam) dengan pantauan darat BMKG.",
                            fontSize = 10.sp,
                            lineHeight = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$matchedCount Titik",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Color Scale & Reference Markers Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFDC2626), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FIRMS Tinggi (High)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFEA580C), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FIRMS Nominal", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFD97706), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FIRMS Rendah (Low)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF0284C7), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pos Pantau 15 Kecamatan", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun KarhutlaLegendSourceCard(
    title: String,
    subtitle: String,
    description: String,
    count: Int,
    accentColor: Color,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    sourceType: String,
    isMatched: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accentColor.copy(alpha = 0.15f)
        else if (isDark) Color(0xFF0F172A) else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 1.dp,
            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KarhutlaMarkerIconPreview(
                        sourceType = sourceType,
                        isMatched = isMatched,
                        coreColor = accentColor,
                        sizeDp = 26
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * Custom Canvas preview that renders the exact visual marker shape used on the OpenStreetMap:
 * - FIRMS (Satellite): Circular thermal beacon with concentric glow & center spark dot.
 * - BMKG (Ground-Based): Diamond (belah ketupat) ground sensor marker with white border & crosshair.
 * - Cross-Matched (FIRMS + BMKG): Dual-ring gold/purple verified badge with inner diamond spark.
 */
@Composable
fun KarhutlaMarkerIconPreview(
    sourceType: String,
    isMatched: Boolean,
    coreColor: Color,
    sizeDp: Int = 24,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Outer halo glow
        drawCircle(
            color = coreColor.copy(alpha = 0.32f),
            radius = maxRadius,
            center = center
        )

        if (sourceType == KarhutlaHotspotEntity.SOURCE_BMKG && !isMatched) {
            // BMKG Ground-Based: Diamond shape + white border + crosshair
            val r = maxRadius * 0.74f
            val diamondPath = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            drawPath(path = diamondPath, color = coreColor)
            drawPath(
                path = diamondPath,
                color = Color.White,
                style = Stroke(width = maxRadius * 0.14f)
            )
            val cr = maxRadius * 0.32f
            drawLine(
                color = Color.White,
                start = Offset(center.x - cr, center.y),
                end = Offset(center.x + cr, center.y),
                strokeWidth = maxRadius * 0.12f
            )
            drawLine(
                color = Color.White,
                start = Offset(center.x, center.y - cr),
                end = Offset(center.x, center.y + cr),
                strokeWidth = maxRadius * 0.12f
            )
        } else if (isMatched) {
            // Cross-Matched (FIRMS + BMKG): Gold outer ring + purple circle + white inner diamond
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = maxRadius * 0.84f,
                center = center,
                style = Stroke(width = maxRadius * 0.16f)
            )
            drawCircle(
                color = coreColor,
                radius = maxRadius * 0.62f,
                center = center
            )
            val dr = maxRadius * 0.34f
            val innerDiamond = Path().apply {
                moveTo(center.x, center.y - dr)
                lineTo(center.x + dr, center.y)
                lineTo(center.x, center.y + dr)
                lineTo(center.x - dr, center.y)
                close()
            }
            drawPath(path = innerDiamond, color = Color.White)
        } else {
            // FIRMS Satellite: Circular thermal beacon + white border + center spark dot
            drawCircle(
                color = coreColor,
                radius = maxRadius * 0.65f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = maxRadius * 0.65f,
                center = center,
                style = Stroke(width = maxRadius * 0.14f)
            )
            drawCircle(
                color = Color.White,
                radius = maxRadius * 0.23f,
                center = center
            )
        }
    }
}

@Composable
private fun OsmHotspotStatChip(
    title: String,
    count: Int,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) badgeColor else badgeColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) badgeColor else badgeColor.copy(alpha = 0.35f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else badgeColor
            )
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OsmHotspotTelemetryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

