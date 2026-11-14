package com.example.ui.components

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.util.GoogleMapsHelper
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlin.math.roundToInt

enum class DisasterCategory(val label: String, val color: Color) {
    ALL("Semua", OrangePrimary),
    TSUNAMI("Zona Rawan Tsunami", Color(0xFFDC2626)),
    EVACUATION_ROUTE("Rute Evakuasi", Color(0xFF16A34A)),
    SHELTER("Titik Kumpul (TES)", Color(0xFF2563EB)),
    FLOOD_LANDSLIDE("Banjir & Longsor", Color(0xFFD97706))
}

data class DisasterMapPoint(
    val id: String,
    val name: String,
    val category: DisasterCategory,
    val normalizedX: Float, // 0.0f (West/Ocean) to 1.0f (East/Bukit Barisan)
    val normalizedY: Float, // 0.0f (North/Lubuk Pinang) to 1.0f (South/Air Rami)
    val riskLevel: String, // "SANGAT TINGGI", "TINGGI", "ZONA AMAN"
    val description: String,
    val sopInstruction: String,
    val altitudeMeters: Int,
    val nearestDistrict: String
)

data class EvacuationRouteLine(
    val id: String,
    val name: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val distanceKm: Double,
    val destinationName: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDisasterMapSection(
    currentDistrict: District,
    onCall117: () -> Unit,
    onOpenSopEvacuation: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(DisasterCategory.ALL) }
    var selectedPoint by remember { mutableStateOf<DisasterMapPoint?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Mukomuko Disaster Prone Points & Assembly Hubs
    val mapPoints = remember {
        listOf(
            DisasterMapPoint(
                id = "p1",
                name = "Pantai Abrasi & Pesisir Mukomuko",
                category = DisasterCategory.TSUNAMI,
                normalizedX = 0.28f,
                normalizedY = 0.32f,
                riskLevel = "SANGAT TINGGI",
                description = "Kawasan pesisir rawan hempasan gelombang tsunami < 20 menit pasca gempa megathrust Mentawai.",
                sopInstruction = "Evakuasi mandiri segera ke Gedung Daerah / Jalur SP3 berjarak minimal 2 km dari bibir pantai.",
                altitudeMeters = 4,
                nearestDistrict = "Kota Mukomuko"
            ),
            DisasterMapPoint(
                id = "p2",
                name = "TES Gedung Daerah Mukomuko",
                category = DisasterCategory.SHELTER,
                normalizedX = 0.45f,
                normalizedY = 0.30f,
                riskLevel = "ZONA AMAN (TES)",
                description = "Tempat Evakuasi Sementara konstruksi beton bertingkat dengan kapasitas 1.500 jiwa dan pasokan logistik darurat.",
                sopInstruction = "Ikuti rambu jalur evakuasi hijau dan prioritaskan lansia, anak-anak, serta ibu hamil.",
                altitudeMeters = 22,
                nearestDistrict = "Kota Mukomuko"
            ),
            DisasterMapPoint(
                id = "p3",
                name = "TEA Dataran Tinggi SP3 Penarik",
                category = DisasterCategory.SHELTER,
                normalizedX = 0.65f,
                normalizedY = 0.44f,
                riskLevel = "ZONA AMAN AKHIR (TEA)",
                description = "Tempat Evakuasi Akhir zona perbukitan dataran tinggi, bebas jangkauan gelombang tsunami dan banjir rob.",
                sopInstruction = "Area tenda darurat BPBD, posko kesehatan darurat RSUD Mukomuko, dan dapur umum.",
                altitudeMeters = 58,
                nearestDistrict = "Penarik"
            ),
            DisasterMapPoint(
                id = "p4",
                name = "DAS Sungai Air Manjuto",
                category = DisasterCategory.FLOOD_LANDSLIDE,
                normalizedX = 0.42f,
                normalizedY = 0.18f,
                riskLevel = "TINGGI (BANJIR LUAPAN)",
                description = "Potensi luapan air bah debit tinggi saat hujan lebat berturut-turut di hulu Taman Nasional Kerinci Seblat.",
                sopInstruction = "Amankan dokumen penting ke tempat tinggi, putus aliran listrik rumah, dan laporkan kenaikan TMA ke 117.",
                altitudeMeters = 16,
                nearestDistrict = "Air Manjuto"
            ),
            DisasterMapPoint(
                id = "p5",
                name = "Pesisir Nelayan Ipuh & Air Rami",
                category = DisasterCategory.TSUNAMI,
                normalizedX = 0.38f,
                normalizedY = 0.78f,
                riskLevel = "SANGAT TINGGI",
                description = "Wilayah pesisir selatan Mukomuko berhadapan langsung dengan Samudera Hindia dan palung laut dalam.",
                sopInstruction = "Bila gempa dirasakan > 20 detik atau air laut surut tiba-tiba, lari ke arah perbukitan timur.",
                altitudeMeters = 5,
                nearestDistrict = "Ipuh"
            ),
            DisasterMapPoint(
                id = "p6",
                name = "Jalur Tebing Rawan Longsor Lubuk Pinang",
                category = DisasterCategory.FLOOD_LANDSLIDE,
                normalizedX = 0.62f,
                normalizedY = 0.12f,
                riskLevel = "TINGGI (LONGSOR)",
                description = "Ruas jalan lintas perbatasan Bengkulu - Sumatera Barat berlereng terjal rentan longsor saat intensitas curah hujan tinggi.",
                sopInstruction = "Hindari melintas lereng saat malam hari saat badai petir, perhatikan rambu peringatan longsor BPBD.",
                altitudeMeters = 85,
                nearestDistrict = "Lubuk Pinang"
            ),
            DisasterMapPoint(
                id = "p7",
                name = "Posko Induk Pusdalops BPBD & 117",
                category = DisasterCategory.SHELTER,
                normalizedX = 0.50f,
                normalizedY = 0.34f,
                riskLevel = "PUSAT KOMANDO DARURAT",
                description = "Pusat Pengendalian Operasi Penanggulangan Bencana BPBD Kab. Mukomuko siaga 24 jam radio komunikasi VHF & satelit.",
                sopInstruction = "Pusat komando logistik, armada perahu karet penyelamat, dan koordinasi Basarnas & TNI-Polri.",
                altitudeMeters = 20,
                nearestDistrict = "Kota Mukomuko"
            ),
            DisasterMapPoint(
                id = "p8",
                name = "Kawasan Muara Teramang Jaya",
                category = DisasterCategory.TSUNAMI,
                normalizedX = 0.34f,
                normalizedY = 0.55f,
                riskLevel = "TINGGI (ROB & TSUNAMI)",
                description = "Zona pertemuan pasang muara laut dan sungai, rentan genangan rob pesisir dan gelombang pasang tinggi.",
                sopInstruction = "Nelayan tambatkan kapal di muara terlindung, evakuasi warga bantaran ke bukit belakang desa.",
                altitudeMeters = 3,
                nearestDistrict = "Teramang Jaya"
            )
        )
    }

    // Evacuation routes from Coast to High Plains
    val evacuationRoutes = remember {
        listOf(
            EvacuationRouteLine("r1", "Rute Utama Pantai Mukomuko -> TES Gedung Daerah", 0.28f, 0.32f, 0.45f, 0.30f, 2.8, "Gedung Daerah"),
            EvacuationRouteLine("r2", "Rute Evakuasi Koridor Kota -> TEA Dataran Tinggi SP3 Penarik", 0.45f, 0.30f, 0.65f, 0.44f, 8.5, "Perbukitan SP3"),
            EvacuationRouteLine("r3", "Rute Pesisir Ipuh -> Dataran Tinggi Medan Jaya", 0.38f, 0.78f, 0.58f, 0.72f, 3.4, "TES Medan Jaya"),
            EvacuationRouteLine("r4", "Rute Air Manjuto -> Jalur Evakuasi Bandara Mukomuko", 0.42f, 0.18f, 0.50f, 0.26f, 4.1, "Zona Tinggi Bandara")
        )
    }

    val filteredPoints = remember(selectedCategory, mapPoints) {
        if (selectedCategory == DisasterCategory.ALL) {
            mapPoints
        } else {
            mapPoints.filter { it.category == selectedCategory }
        }
    }

    val containerBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val cardBorder = if (isDark) OrangePrimary.copy(alpha = 0.35f) else OrangePrimary.copy(alpha = 0.2f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_disaster_map_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                            .size(38.dp)
                            .background(OrangePrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Peta",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Peta Interaktif Bencana & SOP Evakuasi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Kabupaten Mukomuko • Berbasis Geospasial BPBD",
                            fontSize = 11.sp,
                            color = OrangePrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DisasterCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = OrangeOnPrimary,
                            containerColor = if (isDark) Color(0xFF262626) else Color(0xFFF3F4F6),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("filter_chip_${cat.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Container with Canvas, Pan & Zoom Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF404040) else Color(0xFFE5E7EB),
                        RoundedCornerShape(16.dp)
                    )
                    .background(if (isDark) Color(0xFF131B24) else Color(0xFFE0F2FE))
                    .testTag("disaster_map_canvas_container")
            ) {
                // Interactive Canvas Map
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panOffsetX += dragAmount.x
                                panOffsetY += dragAmount.y
                            }
                        }
                ) {
                    val mapWidth = constraints.maxWidth.toFloat()
                    val mapHeight = constraints.maxHeight.toFloat()

                    val infinitePulse = rememberInfiniteTransition(label = "pulse")
                    val pulseRadius by infinitePulse.animateFloat(
                        initialValue = 6f,
                        targetValue = 18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "radius"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = (mapWidth / 2f) + panOffsetX
                        val cy = (mapHeight / 2f) + panOffsetY

                        // 1. Draw Stylized Coastline (Samudera Hindia on Left, Mukomuko Mainland on Right)
                        val coastlinePath = Path().apply {
                            moveTo(cx - (mapWidth * 0.45f * zoomScale), cy - (mapHeight * 0.55f * zoomScale))
                            quadraticTo(
                                cx - (mapWidth * 0.35f * zoomScale),
                                cy - (mapHeight * 0.1f * zoomScale),
                                cx - (mapWidth * 0.28f * zoomScale),
                                cy + (mapHeight * 0.15f * zoomScale)
                            )
                            quadraticTo(
                                cx - (mapWidth * 0.22f * zoomScale),
                                cy + (mapHeight * 0.35f * zoomScale),
                                cx - (mapWidth * 0.15f * zoomScale),
                                cy + (mapHeight * 0.55f * zoomScale)
                            )
                            lineTo(cx + (mapWidth * 0.55f * zoomScale), cy + (mapHeight * 0.55f * zoomScale))
                            lineTo(cx + (mapWidth * 0.55f * zoomScale), cy - (mapHeight * 0.55f * zoomScale))
                            close()
                        }

                        // Land mass fill
                        val landColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        drawPath(path = coastlinePath, color = landColor)

                        // Coastline boundary stroke
                        val coastLineColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        drawPath(
                            path = coastlinePath,
                            color = coastLineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 2. Draw Bukit Barisan Mountain ridge on eastern border
                        val mountainPath = Path().apply {
                            moveTo(cx + (mapWidth * 0.30f * zoomScale), cy - (mapHeight * 0.50f * zoomScale))
                            lineTo(cx + (mapWidth * 0.38f * zoomScale), cy - (mapHeight * 0.20f * zoomScale))
                            lineTo(cx + (mapWidth * 0.42f * zoomScale), cy + (mapHeight * 0.15f * zoomScale))
                            lineTo(cx + (mapWidth * 0.46f * zoomScale), cy + (mapHeight * 0.50f * zoomScale))
                        }
                        drawPath(
                            path = mountainPath,
                            color = if (isDark) Color(0xFF15803D) else Color(0xFF86EFAC),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                            )
                        )

                        // 3. Draw River Basin (Air Manjuto)
                        val riverPath = Path().apply {
                            moveTo(cx + (mapWidth * 0.35f * zoomScale), cy - (mapHeight * 0.30f * zoomScale))
                            quadraticTo(
                                cx + (mapWidth * 0.10f * zoomScale),
                                cy - (mapHeight * 0.25f * zoomScale),
                                cx - (mapWidth * 0.32f * zoomScale),
                                cy - (mapHeight * 0.15f * zoomScale)
                            )
                        }
                        drawPath(
                            path = riverPath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 4. Draw Evacuation Routes (Green Arrow vectors)
                        if (selectedCategory == DisasterCategory.ALL || selectedCategory == DisasterCategory.EVACUATION_ROUTE) {
                            evacuationRoutes.forEach { route ->
                                val startPointX = cx + (route.startX - 0.5f) * mapWidth * zoomScale
                                val startPointY = cy + (route.startY - 0.5f) * mapHeight * zoomScale
                                val endPointX = cx + (route.endX - 0.5f) * mapWidth * zoomScale
                                val endPointY = cy + (route.endY - 0.5f) * mapHeight * zoomScale

                                drawLine(
                                    color = Color(0xFF16A34A),
                                    start = Offset(startPointX, startPointY),
                                    end = Offset(endPointX, endPointY),
                                    strokeWidth = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
                                )

                                // Arrow destination circle
                                drawCircle(
                                    color = Color(0xFF16A34A),
                                    radius = 5.dp.toPx(),
                                    center = Offset(endPointX, endPointY)
                                )
                            }
                        }

                        // 5. Draw Water Wave Ripple Effects (Ocean side)
                        drawCircle(
                            color = Color(0xFF0284C7).copy(alpha = 0.12f),
                            radius = 60f * zoomScale,
                            center = Offset(cx - (mapWidth * 0.40f * zoomScale), cy)
                        )
                    }

                    // Render Interactive Marker Pins
                    filteredPoints.forEach { point ->
                        val posX = (mapWidth / 2f) + panOffsetX + (point.normalizedX - 0.5f) * mapWidth * zoomScale
                        val posY = (mapHeight / 2f) + panOffsetY + (point.normalizedY - 0.5f) * mapHeight * zoomScale
                        val isSelected = selectedPoint?.id == point.id

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(posX.roundToInt() - 16, posY.roundToInt() - 32) }
                                .clickable { selectedPoint = point }
                                .testTag("map_marker_${point.id}")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Pulsing aura for tsunami warning or active selection
                                if (point.category == DisasterCategory.TSUNAMI || isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size((if (isSelected) 36.dp else 26.dp))
                                            .background(point.category.color.copy(alpha = 0.35f), CircleShape)
                                    )
                                }

                                Surface(
                                    color = if (isSelected) OrangePrimary else point.category.color,
                                    shape = CircleShape,
                                    shadowElevation = if (isSelected) 6.dp else 3.dp,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(if (isSelected) 30.dp else 22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (point.category) {
                                                DisasterCategory.TSUNAMI -> Icons.Default.Warning
                                                DisasterCategory.SHELTER -> Icons.Default.Shield
                                                DisasterCategory.EVACUATION_ROUTE -> Icons.AutoMirrored.Filled.DirectionsRun
                                                DisasterCategory.FLOOD_LANDSLIDE -> Icons.Default.Layers
                                                else -> Icons.Default.LocationOn
                                            },
                                            contentDescription = point.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isSelected) 18.dp else 13.dp)
                                        )
                                    }
                                }

                                // Pin Label
                                Surface(
                                    color = (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(4.dp),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .widthIn(max = 110.dp)
                                ) {
                                    Text(
                                        text = point.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Map Legend Overlay (Top Left)
                Surface(
                    color = (if (isDark) Color(0xFF0F172A) else Color.White).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFDC2626), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rawan Tsunami", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jalur & Titik Aman", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Zoom & Reset Controls (Top Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = (if (isDark) Color(0xFF1E293B) else Color.White).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 3.dp
                    ) {
                        IconButton(
                            onClick = { if (zoomScale < 2.5f) zoomScale += 0.25f },
                            modifier = Modifier.size(32.dp).testTag("btn_map_zoom_in")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Perbesar", modifier = Modifier.size(16.dp))
                        }
                    }

                    Surface(
                        color = (if (isDark) Color(0xFF1E293B) else Color.White).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 3.dp
                    ) {
                        IconButton(
                            onClick = { if (zoomScale > 0.75f) zoomScale -= 0.25f },
                            modifier = Modifier.size(32.dp).testTag("btn_map_zoom_out")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Perkecil", modifier = Modifier.size(16.dp))
                        }
                    }

                    Surface(
                        color = (if (isDark) Color(0xFF1E293B) else Color.White).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 3.dp
                    ) {
                        IconButton(
                            onClick = {
                                zoomScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                            modifier = Modifier.size(32.dp).testTag("btn_map_reset_view")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Peta", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Locate District Button (Bottom Right)
                Surface(
                    color = OrangePrimary,
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            panOffsetX = 0f
                            panOffsetY = 0f
                            zoomScale = 1.25f
                            // Auto select nearest point
                            selectedPoint = mapPoints.firstOrNull { it.nearestDistrict.contains(currentDistrict.name, ignoreCase = true) }
                                ?: mapPoints.first()
                        },
                        modifier = Modifier.size(36.dp).testTag("btn_map_my_district")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Kecamatan Saya", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Point Details Bottom Sheet Card (When a marker is selected)
            AnimatedVisibility(visible = selectedPoint != null) {
                selectedPoint?.let { point ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_point_detail_card"),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f))
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
                                            .size(28.dp)
                                            .background(point.category.color, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = point.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Status: ${point.riskLevel} • Ketinggian ${point.altitudeMeters} mdpl",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = point.category.color,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedPoint = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = point.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = OrangePrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SOP: ${point.sopInstruction}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412),
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val targetLat = -2.25 - (point.normalizedY * 1.15)
                                        val targetLon = 101.00 + (point.normalizedX * 0.90)
                                        GoogleMapsHelper.openNavigationRoute(
                                            context = context,
                                            destLat = targetLat,
                                            destLon = targetLon,
                                            destName = point.name
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.1f).heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Rute Maps",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }

                                Button(
                                    onClick = onOpenSopEvacuation,
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SOP Evakuasi",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = onCall117,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "117",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
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
