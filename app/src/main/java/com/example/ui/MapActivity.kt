package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MukomukoDistricts
import com.example.ui.components.EmergencyDialer
import com.example.ui.components.FirmsHotspotOsmMapSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.GoogleMapsHelper
import kotlin.math.roundToInt

/**
 * MapActivity: Dedicated Interactive Map Module using Google Maps SDK integration
 * to display all 15 sub-districts (kecamatan) in Mukomuko with custom markers for
 * Weather Monitoring Stations (AWS BMKG), BPBD Emergency Response Posts (Posko Siaga TRC),
 * and severe weather incident spots with full zoom, pan, and real-time navigation.
 */
class MapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MukomukoInteractiveMapScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, MapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}

enum class MapMarkerType(val label: String, val color: Color, val iconColor: Color) {
    ALL("Semua Titik", Color(0xFF0F172A), Color.White),
    HOTSPOT_FIRMS("Hotspot FIRMS (OSM)", Color(0xFFEA580C), Color.White),
    POSKO_TRC("Posko Siaga TRC", Color(0xFFDC2626), Color.White),
    STASIUN_CUACA("Stasiun Cuaca AWS", Color(0xFF0284C7), Color.White),
    RAWAN_BENCANA("Titik Cuaca Ekstrem", Color(0xFFD97706), Color.White)
}

data class MukomukoMapMarker(
    val id: String,
    val name: String,
    val subDistrict: String,
    val type: MapMarkerType,
    val latitude: Double,
    val longitude: Double,
    val normX: Float, // 0.0f (West coastline) to 1.0f (East Bukit Barisan)
    val normY: Float, // 0.0f (North Lubuk Pinang) to 1.0f (South Air Rami)
    val description: String,
    val readinessStatus: String,
    val phoneContact: String = "117"
)

@Composable
fun MukomukoInteractiveMapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(MapMarkerType.ALL) }
    var selectedMarker by remember { mutableStateOf<MukomukoMapMarker?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // 15 Sub-Districts (Kecamatan) of Mukomuko with Monitoring Stations and Posko Siaga TRC
    val allMarkers = remember {
        listOf(
            MukomukoMapMarker(
                id = "MK_01",
                name = "Posko Induk TRC Pusdalops-PB Mukomuko",
                subDistrict = "Kota Mukomuko",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.5833,
                longitude = 101.1167,
                normX = 0.28f,
                normY = 0.22f,
                description = "Markas Komando Utama Pusdalops-PB BPBD & Call Center 117 Bebas Pulsa. Kesiapsiagaan 24 Jam Tim Reaksi Cepat.",
                readinessStatus = "SIAGA 24 JAM",
                phoneContact = "117"
            ),
            MukomukoMapMarker(
                id = "MK_02",
                name = "Stasiun Meteorologi AWS BMKG Mukomuko",
                subDistrict = "Kota Mukomuko",
                type = MapMarkerType.STASIUN_CUACA,
                latitude = -2.5620,
                longitude = 101.1210,
                normX = 0.31f,
                normY = 0.20f,
                description = "Stasiun Pengamatan Cuaca Otomatis & Radar Cuaca BMKG. Pemantauan curah hujan, arah angin, dan tekanan atmosfer.",
                readinessStatus = "ONLINE TELEMETRI",
                phoneContact = "(0737) 71117"
            ),
            MukomukoMapMarker(
                id = "MK_03",
                name = "Posko Siaga TRC Sektor Utara Lubuk Pinang",
                subDistrict = "Lubuk Pinang",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.4833,
                longitude = 101.1833,
                normX = 0.45f,
                normY = 0.08f,
                description = "Pos Komando Sektor Perbatasan Sumbar-Bengkulu & Pengendalian Daerah Aliran Sungai (DAS) Manjunto.",
                readinessStatus = "SIAGA 24 JAM",
                phoneContact = "08117301117"
            ),
            MukomukoMapMarker(
                id = "MK_04",
                name = "Titik Pantau Rawan Banjir DAS Manjunto",
                subDistrict = "Lubuk Pinang",
                type = MapMarkerType.RAWAN_BENCANA,
                latitude = -2.4910,
                longitude = 101.1750,
                normX = 0.42f,
                normY = 0.10f,
                description = "Daerah rawan genangan luapan air sungai saat hujan lebat di hulu Bukit Barisan.",
                readinessStatus = "WASPADA BANJIR"
            ),
            MukomukoMapMarker(
                id = "MK_05",
                name = "Posko Siaga TRC XIV Koto",
                subDistrict = "XIV Koto",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.5167,
                longitude = 101.1333,
                normX = 0.35f,
                normY = 0.14f,
                description = "Posko TRC & Jalur Evakuasi Dataran Tinggi Air Bakung.",
                readinessStatus = "SIAGA PENYELAMATAN"
            ),
            MukomukoMapMarker(
                id = "MK_06",
                name = "Stasiun Pemantau AWS Pesisir Air Dikit",
                subDistrict = "Air Dikit",
                type = MapMarkerType.STASIUN_CUACA,
                latitude = -2.6500,
                longitude = 101.2000,
                normX = 0.32f,
                normY = 0.34f,
                description = "Sensor pengukur pasang surut maritim Samudra Hindia dan peringatan abrasi pantai.",
                readinessStatus = "ONLINE TELEMETRI"
            ),
            MukomukoMapMarker(
                id = "MK_07",
                name = "Posko Penanggulangan Bencana Penarik",
                subDistrict = "Penarik",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.7333,
                longitude = 101.3000,
                normX = 0.50f,
                normY = 0.45f,
                description = "Pusat logistik darurat pangan dan armada perahu karet penyelamat wilayah tengah.",
                readinessStatus = "SIAGA LOGISTIK"
            ),
            MukomukoMapMarker(
                id = "MK_08",
                name = "Pos Pantau Pohon Tumbang & Cuaca Teras Terunjam",
                subDistrict = "Teras Terunjam",
                type = MapMarkerType.RAWAN_BENCANA,
                latitude = -2.6167,
                longitude = 101.2500,
                normX = 0.46f,
                normY = 0.28f,
                description = "Zona rawan angin kencang puting beliung dan pohon tumbang lintas perkebunan.",
                readinessStatus = "WASPADA BADAI"
            ),
            MukomukoMapMarker(
                id = "MK_09",
                name = "Posko Siaga TRC Sektor Pondok Suguh",
                subDistrict = "Pondok Suguh",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.8500,
                longitude = 101.4667,
                normX = 0.52f,
                normY = 0.58f,
                description = "Posko Kesiapsiagaan Bencana wilayah pesisir muara sungai dan jalur perbukitan.",
                readinessStatus = "SIAGA 24 JAM"
            ),
            MukomukoMapMarker(
                id = "MK_10",
                name = "Posko Terpadu Kedaruratan TRC Ipuh",
                subDistrict = "Ipuh",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.9833,
                longitude = 101.6000,
                normX = 0.42f,
                normY = 0.72f,
                description = "Pusat Evakuasi Tsunami (TES) Sektor Selatan, terhubung langsung Puskesmas Perawatan Ipuh & Polsek Mukomuko Selatan.",
                readinessStatus = "SIAGA 24 JAM",
                phoneContact = "08127400117"
            ),
            MukomukoMapMarker(
                id = "MK_11",
                name = "Stasiun Cuaca Maritim & Dermaga Nelayan Ipuh",
                subDistrict = "Ipuh",
                type = MapMarkerType.STASIUN_CUACA,
                latitude = -2.9920,
                longitude = 101.5910,
                normX = 0.38f,
                normY = 0.74f,
                description = "Pemantauan tinggi gelombang laut Samudra Hindia dan keselamatan melaut nelayan tradisional.",
                readinessStatus = "ONLINE BMKG"
            ),
            MukomukoMapMarker(
                id = "MK_12",
                name = "Titik Rawan Longsor Tebing Malin Deman",
                subDistrict = "Malin Deman",
                type = MapMarkerType.RAWAN_BENCANA,
                latitude = -3.0500,
                longitude = 101.6833,
                normX = 0.64f,
                normY = 0.80f,
                description = "Tebing curam rawan longsor saat curah hujan tinggi ekstrem di lintasan perbukitan.",
                readinessStatus = "WASPADA LONGSOR"
            ),
            MukomukoMapMarker(
                id = "MK_13",
                name = "Posko Pantau Kesiapsiagaan Air Rami",
                subDistrict = "Air Rami",
                type = MapMarkerType.POSKO_TRC,
                latitude = -3.1500,
                longitude = 101.7667,
                normX = 0.48f,
                normY = 0.92f,
                description = "Posko Siaga ujung selatan perbatasan Bengkulu Utara. Mitigasi abrasi pantai dan gelombang pasang.",
                readinessStatus = "SIAGA PERBATASAN"
            ),
            MukomukoMapMarker(
                id = "MK_14",
                name = "Posko Siaga TRC Teramang Jaya",
                subDistrict = "Teramang Jaya",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.8000,
                longitude = 101.3833,
                normX = 0.38f,
                normY = 0.52f,
                description = "Pos Siaga muara Teramang Jaya & pemantauan banjir pasang rob air laut.",
                readinessStatus = "SIAGA 24 JAM"
            ),
            MukomukoMapMarker(
                id = "MK_15",
                name = "Titik Pantau DAS Selagan Raya",
                subDistrict = "Selagan Raya",
                type = MapMarkerType.RAWAN_BENCANA,
                latitude = -2.6833,
                longitude = 101.2667,
                normX = 0.55f,
                normY = 0.38f,
                description = "Hulu sungai Selagan, titik telemetri ketinggian permukaan air sungai dan jembatan gantung.",
                readinessStatus = "WASPADA DEBIT AIR"
            ),
            MukomukoMapMarker(
                id = "MK_16",
                name = "Posko TRC V Koto",
                subDistrict = "V Koto",
                type = MapMarkerType.POSKO_TRC,
                latitude = -2.5500,
                longitude = 101.2167,
                normX = 0.52f,
                normY = 0.18f,
                description = "Posko Siaga TRC V Koto untuk wilayah pemukiman lereng dan perkebunan rakyat.",
                readinessStatus = "SIAGA 24 JAM"
            ),
            MukomukoMapMarker(
                id = "MK_17",
                name = "Pos Pantau Abrasi Pantai Sungai Rumbai",
                subDistrict = "Sungai Rumbai",
                type = MapMarkerType.RAWAN_BENCANA,
                latitude = -3.0833,
                longitude = 101.7167,
                normX = 0.56f,
                normY = 0.85f,
                description = "Garis pantai kritis abrasi dan pemantauan perahu nelayan saat cuaca buruk badai.",
                readinessStatus = "WASPADA ABRASI"
            ),
            MukomukoMapMarker(
                id = "MK_18",
                name = "Stasiun AWS Agroklimat Air Manjunto",
                subDistrict = "Air Manjunto",
                type = MapMarkerType.STASIUN_CUACA,
                latitude = -2.5667,
                longitude = 101.1667,
                normX = 0.40f,
                normY = 0.21f,
                description = "Stasiun pengamatan agroklimatologi, kelembapan tanah, dan kecepatan angin pertanian.",
                readinessStatus = "ONLINE BMKG"
            )
        )
    }

    val filteredMarkers = remember(selectedFilter, allMarkers) {
        if (selectedFilter == MapMarkerType.ALL) allMarkers
        else allMarkers.filter { it.type == selectedFilter }
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(0.85f, 4.0f)
        panOffsetX = (panOffsetX + offsetChange.x).coerceIn(-1200f, 1200f)
        panOffsetY = (panOffsetY + offsetChange.y).coerceIn(-1400f, 1400f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .testTag("map_activity_screen")
        ) {
            // 1. TOP APP BAR HEADER
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = "Peta Interaktif 15 Kecamatan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "PUSDALOPS BPBD • GOOGLE MAPS GIS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Action to open full Google Maps Navigation
                Button(
                    onClick = { GoogleMapsHelper.openMukomukoDisasterMap(context) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Google Maps",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Buka Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }

        // 2. FILTER CHIPS ROW (All, Posko TRC, Stasiun Cuaca, Cuaca Ekstrem)
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.75f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapMarkerType.values().forEach { type ->
                    val isSelected = selectedFilter == type
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) type.color else Color(0xFF334155),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent
                        ),
                        modifier = Modifier.clickable { selectedFilter = type }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else type.color)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = type.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. MAIN INTERACTIVE MAP CANVAS OR OPENSTREETMAP FIRMS HOTSPOT OVERLAY
        if (selectedFilter == MapMarkerType.HOTSPOT_FIRMS) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                FirmsHotspotOsmMapSection(
                    currentDistrict = MukomukoDistricts.defaultDistrict,
                    onCall117 = { EmergencyDialer.dial(context, "117") },
                    isDark = true
                )
            }
        } else {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF090D16))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-1200f, 1200f)
                        panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-1400f, 1400f)
                    }
                }
                .transformable(state = transformState)
                .testTag("interactive_map_canvas_container")
        ) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()

            // Map Geospatial Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val oceanWidth = canvasW * 0.26f * zoomScale + panOffsetX
                // Ocean gradient (West)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7)),
                        startX = 0f,
                        endX = oceanWidth.coerceAtLeast(10f)
                    )
                )

                // Coastline border line
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(oceanWidth, 0f),
                    end = Offset(oceanWidth, size.height),
                    strokeWidth = 3f * zoomScale
                )

                // Grid lines (GIS aesthetic)
                val gridSpacing = 60f * zoomScale
                var curX = (panOffsetX % gridSpacing)
                while (curX < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(curX, 0f),
                        end = Offset(curX, size.height),
                        strokeWidth = 1f
                    )
                    curX += gridSpacing
                }

                var curY = (panOffsetY % gridSpacing)
                while (curY < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, curY),
                        end = Offset(size.width, curY),
                        strokeWidth = 1f
                    )
                    curY += gridSpacing
                }
            }

            // Render Custom Markers on the Canvas
            filteredMarkers.forEach { marker ->
                val markerX = (marker.normX * canvasW * zoomScale + panOffsetX).roundToInt()
                val markerY = (marker.normY * canvasH * zoomScale + panOffsetY).roundToInt()

                val isSelected = selectedMarker?.id == marker.id

                Box(
                    modifier = Modifier
                        .offset { IntOffset(markerX - 18, markerY - 36) }
                        .clickable { selectedMarker = marker }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = marker.type.color,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.5.dp else 1.2.dp,
                                if (isSelected) Color(0xFFFBBF24) else Color.White
                            ),
                            shadowElevation = if (isSelected) 8.dp else 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 34.dp else 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (marker.type) {
                                        MapMarkerType.POSKO_TRC -> Icons.Default.Shield
                                        MapMarkerType.STASIUN_CUACA -> Icons.Default.WbSunny
                                        MapMarkerType.RAWAN_BENCANA -> Icons.Default.Warning
                                        else -> Icons.Default.LocationOn
                                    },
                                    contentDescription = marker.name,
                                    tint = marker.type.iconColor,
                                    modifier = Modifier.size(if (isSelected) 20.dp else 16.dp)
                                )
                            }
                        }

                        // Label badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, marker.type.color)
                        ) {
                            Text(
                                text = marker.subDistrict,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Floating Map Controls: Zoom In, Zoom Out, Center/Reset
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = if (selectedMarker != null) 180.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { zoomScale = (zoomScale * 1.3f).coerceAtMost(4.0f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Perbesar", tint = Color.White)
                    }
                }

                // Zoom Out
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.85f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Perkecil", tint = Color.White)
                    }
                }

                // Reset Pan & Zoom
                Surface(
                    shape = CircleShape,
                    color = OrangePrimary,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Peta", tint = OrangeOnPrimary)
                    }
                }
            }

            // Legend Overlay (Top Left)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.70f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF475569)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("15 Kecamatan Mukomuko", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                    Text("Zoom: ${(zoomScale * 100).toInt()}% • Titik: ${filteredMarkers.size}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                }
            }
        }
        }

        // 4. BOTTOM DETAIL CARD WHEN MARKER IS SELECTED
        AnimatedVisibility(visible = selectedMarker != null) {
            val marker = selectedMarker
            if (marker != null) {
                Card(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, marker.type.color),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = marker.type.color.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (marker.type) {
                                                MapMarkerType.POSKO_TRC -> Icons.Default.Shield
                                                MapMarkerType.STASIUN_CUACA -> Icons.Default.WbSunny
                                                else -> Icons.Default.Warning
                                            },
                                            contentDescription = null,
                                            tint = marker.type.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = marker.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Kecamatan ${marker.subDistrict} • ${marker.readinessStatus}",
                                        fontSize = 11.sp,
                                        color = marker.type.color,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedMarker = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = marker.description,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Koordinat GPS: %.4f, %.4f".format(marker.latitude, marker.longitude),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Rute Google Maps & Hubungi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    GoogleMapsHelper.openLocation(
                                        context = context,
                                        latitude = marker.latitude,
                                        longitude = marker.longitude,
                                        label = marker.name
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rute Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${marker.phoneContact}"))
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Kontak: ${marker.phoneContact}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.weight(0.9f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kontak Posko", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
