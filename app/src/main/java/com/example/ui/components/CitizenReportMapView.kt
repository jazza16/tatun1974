package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CitizenDisasterReport
import kotlin.math.sqrt

// Disaster Categories for Map Filter
enum class DisasterMapFilter(val label: String, val color: Color) {
    ALL("Semua Laporan", Color(0xFF15803D)),
    FLOOD("Banjir / Luapan Air", Color(0xFF2563EB)),
    WIND("Angin Kencang / Badai", Color(0xFFD97706)),
    COASTAL("Gelombang & Abrasi", Color(0xFF0284C7)),
    LANDSLIDE("Tanah Longsor", Color(0xFFB45309)),
    INFRASTRUCTURE("Infrastruktur & Lainnya", Color(0xFFDC2626))
}

// Mukomuko Disaster Prone Hazard Zones (BPBD)
data class HazardRiskZone(
    val name: String,
    val type: String, // Tsunami, Banjir DAS, Longsor, Abrasi
    val riskLevel: String, // Tinggi, Sedang
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Float,
    val color: Color,
    val description: String
)

val MUKOMUKO_HAZARD_ZONES = listOf(
    HazardRiskZone(
        name = "Zona Pesisir Pantai Mukomuko - Abrasi & Gelombang Pasang",
        type = "Gelombang & Abrasi",
        riskLevel = "Tinggi",
        centerLat = -2.5833,
        centerLon = 101.1167,
        radiusKm = 7.5f,
        color = Color(0xFFDC2626),
        description = "Garis pantai rawan abrasi & pasang air laut Samudra Hindia (Koto Jaya - Teramang Jaya)."
    ),
    HazardRiskZone(
        name = "Daerah Aliran Sungai (DAS) Manjunto - Rawan Banjir Luapan",
        type = "Banjir DAS",
        riskLevel = "Tinggi",
        centerLat = -2.4833,
        centerLon = 101.1833,
        radiusKm = 8.0f,
        color = Color(0xFF2563EB),
        description = "Kawasan bantaran sungai dan irigasi primer Lubuk Pinang - XIV Koto."
    ),
    HazardRiskZone(
        name = "DAS Selagan - Dataran Rendah Rawan Genangan",
        type = "Banjir DAS",
        riskLevel = "Sedang",
        centerLat = -2.6500,
        centerLon = 101.2500,
        radiusKm = 6.5f,
        color = Color(0xFF0284C7),
        description = "Kawasan luapan sungai Selagan Raya saat hujan deras di hulu perbukitan."
    ),
    HazardRiskZone(
        name = "Zona Tebing Perbukitan V Koto & Malin Deman - Rawan Longsor",
        type = "Tanah Longsor",
        riskLevel = "Tinggi",
        centerLat = -2.7500,
        centerLon = 101.5500,
        radiusKm = 9.0f,
        color = Color(0xFFB45309),
        description = "Kontur perbukitan curam rawan amblas dan guguran tanah material saat intensitas hujan tinggi."
    ),
    HazardRiskZone(
        name = "Zona Pesisir Ipuh & Air Rami - Potensi Gelombang Ekstrem",
        type = "Gelombang & Abrasi",
        riskLevel = "Tinggi",
        centerLat = -3.0083,
        centerLon = 101.4912,
        radiusKm = 7.0f,
        color = Color(0xFFEA580C),
        description = "Pesisir dermaga dan sentra nelayan Ipuh langsung berhadapan dengan Samudra Hindia terbuka."
    )
)

/**
 * Interactive Spatial Map (Google Maps / GIS Style) for Citizen Disaster Reports
 * Placed inside Tab Laporan Warga.
 */
@Composable
fun CitizenReportMapView(
    reports: List<CitizenDisasterReport>,
    onUpdateReportStatus: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(DisasterMapFilter.ALL) }
    var showHazardOverlay by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }
    var selectedHazardZone by remember { mutableStateOf<HazardRiskZone?>(null) }

    // Map Transformation (Pan and Zoom)
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Coordinate Bounding Box for Mukomuko Regency
    val minLat = -3.40
    val maxLat = -2.25
    val minLon = 101.00
    val maxLon = 101.90

    // Filter reports based on selected category
    val filteredReports = remember(reports, selectedFilter) {
        if (selectedFilter == DisasterMapFilter.ALL) {
            reports
        } else {
            reports.filter { r ->
                when (selectedFilter) {
                    DisasterMapFilter.FLOOD -> r.disasterType.contains("Banjir", ignoreCase = true) || r.disasterType.contains("Genangan", ignoreCase = true)
                    DisasterMapFilter.WIND -> r.disasterType.contains("Angin", ignoreCase = true) || r.disasterType.contains("Badai", ignoreCase = true) || r.disasterType.contains("Pohon Tumbang", ignoreCase = true)
                    DisasterMapFilter.COASTAL -> r.disasterType.contains("Gelombang", ignoreCase = true) || r.disasterType.contains("Abrasi", ignoreCase = true)
                    DisasterMapFilter.LANDSLIDE -> r.disasterType.contains("Longsor", ignoreCase = true)
                    DisasterMapFilter.INFRASTRUCTURE -> !r.disasterType.contains("Banjir", ignoreCase = true) && !r.disasterType.contains("Angin", ignoreCase = true) && !r.disasterType.contains("Gelombang", ignoreCase = true) && !r.disasterType.contains("Longsor", ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .testTag("citizen_report_map_view")
    ) {
        // 1. Category Filter Chips Bar
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peta Spasial Sebaran Titik Bencana",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    // Hazard Overlay Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showHazardOverlay = !showHazardOverlay }
                    ) {
                        Text(
                            text = "Layer Rawan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showHazardOverlay) Color(0xFFDC2626) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = showHazardOverlay,
                            onCheckedChange = { showHazardOverlay = it },
                            modifier = Modifier.size(width = 36.dp, height = 24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFDC2626)
                            )
                        )
                    }
                }

                // Horizontal Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(DisasterMapFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) filter.color else Color(0xFFF3F4F6),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) filter.color else Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Map Canvas Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFE0F2FE)) // Coastal Ocean Blue (Samudra Hindia)
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Ocean Waves Grid Pattern (Samudra Hindia on the left)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFBAE6FD), // Indian Ocean
                            Color(0xFFE0F2FE),
                            Color(0xFFFEF3C7)  // Sandy Coastline
                        ),
                        startX = 0f,
                        endX = canvasWidth * 0.35f
                    )
                )

                // Mainland Mukomuko Landmass Polygon (Greenish Cartographic Styling)
                val landPath = Path().apply {
                    val startX = (canvasWidth * 0.25f + offsetX) * scale
                    val startY = (0f + offsetY) * scale
                    moveTo(startX, startY)
                    // Coastline edge running North-West to South-East
                    lineTo((canvasWidth * 0.28f + offsetX) * scale, (canvasHeight * 0.25f + offsetY) * scale)
                    lineTo((canvasWidth * 0.32f + offsetX) * scale, (canvasHeight * 0.50f + offsetY) * scale)
                    lineTo((canvasWidth * 0.38f + offsetX) * scale, (canvasHeight * 0.75f + offsetY) * scale)
                    lineTo((canvasWidth * 0.45f + offsetX) * scale, (canvasHeight + offsetY) * scale)
                    lineTo((canvasWidth + offsetX) * scale, (canvasHeight + offsetY) * scale)
                    lineTo((canvasWidth + offsetX) * scale, (0f + offsetY) * scale)
                    close()
                }

                // Fill Landmass
                drawPath(
                    path = landPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFDCFCE7), // Coastal lowlands
                            Color(0xFFBBF7D0), // Inland plains
                            Color(0xFF86EFAC)  // Foothills Bukit Barisan
                        )
                    )
                )

                // Coastline Border line
                drawPath(
                    path = landPath,
                    color = Color(0xFF16A34A),
                    style = Stroke(width = 2.5f * scale)
                )

                // Jalur Lintas Barat (Jalinbar) Sumatera Highway (Orange dashed highway line)
                val highwayPath = Path().apply {
                    moveTo((canvasWidth * 0.30f + offsetX) * scale, (0f + offsetY) * scale)
                    lineTo((canvasWidth * 0.34f + offsetX) * scale, (canvasHeight * 0.30f + offsetY) * scale)
                    lineTo((canvasWidth * 0.38f + offsetX) * scale, (canvasHeight * 0.60f + offsetY) * scale)
                    lineTo((canvasWidth * 0.46f + offsetX) * scale, (canvasHeight + offsetY) * scale)
                }
                drawPath(
                    path = highwayPath,
                    color = Color(0xFFEA580C),
                    style = Stroke(
                        width = 3.5f * scale,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )
                )

                // River System (DAS Manjunto & DAS Selagan)
                val riverPath = Path().apply {
                    moveTo((canvasWidth * 0.70f + offsetX) * scale, (canvasHeight * 0.15f + offsetY) * scale)
                    cubicTo(
                        (canvasWidth * 0.50f + offsetX) * scale, (canvasHeight * 0.20f + offsetY) * scale,
                        (canvasWidth * 0.40f + offsetX) * scale, (canvasHeight * 0.25f + offsetY) * scale,
                        (canvasWidth * 0.33f + offsetX) * scale, (canvasHeight * 0.28f + offsetY) * scale
                    )
                    moveTo((canvasWidth * 0.75f + offsetX) * scale, (canvasHeight * 0.45f + offsetY) * scale)
                    cubicTo(
                        (canvasWidth * 0.55f + offsetX) * scale, (canvasHeight * 0.50f + offsetY) * scale,
                        (canvasWidth * 0.45f + offsetX) * scale, (canvasHeight * 0.52f + offsetY) * scale,
                        (canvasWidth * 0.36f + offsetX) * scale, (canvasHeight * 0.55f + offsetY) * scale
                    )
                }
                drawPath(
                    path = riverPath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(width = 2.5f * scale)
                )

                // 3. Draw BPBD Hazard Overlay Zones (if enabled)
                if (showHazardOverlay) {
                    MUKOMUKO_HAZARD_ZONES.forEach { zone ->
                        val normX = ((zone.centerLon - minLon) / (maxLon - minLon)).toFloat().coerceIn(0.1f, 0.9f)
                        val normY = (((maxLat - zone.centerLat) / (maxLat - minLat))).toFloat().coerceIn(0.1f, 0.9f)

                        val zoneScreenX = (normX * canvasWidth + offsetX) * scale
                        val zoneScreenY = (normY * canvasHeight + offsetY) * scale
                        val zoneRadius = (zone.radiusKm * 6.5f) * scale

                        // Hazard risk circle area
                        drawCircle(
                            color = zone.color.copy(alpha = 0.18f),
                            radius = zoneRadius,
                            center = Offset(zoneScreenX, zoneScreenY)
                        )
                        // Hazard border outline
                        drawCircle(
                            color = zone.color.copy(alpha = 0.65f),
                            radius = zoneRadius,
                            center = Offset(zoneScreenX, zoneScreenY),
                            style = Stroke(
                                width = 1.8f * scale,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        )
                    }
                }

                // 4. Draw Citizen Disaster Report Pins
                filteredReports.forEach { report ->
                    val normX = ((report.longitude - minLon) / (maxLon - minLon)).toFloat().coerceIn(0.15f, 0.90f)
                    val normY = (((maxLat - report.latitude) / (maxLat - minLat))).toFloat().coerceIn(0.10f, 0.90f)

                    val pinX = (normX * canvasWidth + offsetX) * scale
                    val pinY = (normY * canvasHeight + offsetY) * scale

                    val isSelected = selectedReport?.id == report.id

                    // Status Indicator Color for Pin Halo:
                    // 'Diproses' = Gold/Orange, 'Divalidasi' = Green, 'Selesai' = Blue
                    val statusColor = when (report.status) {
                        "Divalidasi" -> Color(0xFF16A34A)
                        "Selesai" -> Color(0xFF2563EB)
                        else -> Color(0xFFEA580C)
                    }

                    // Radar pulse / selection ring
                    if (isSelected) {
                        drawCircle(
                            color = statusColor.copy(alpha = 0.35f),
                            radius = 28f * scale,
                            center = Offset(pinX, pinY)
                        )
                        drawCircle(
                            color = statusColor,
                            radius = 32f * scale,
                            center = Offset(pinX, pinY),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    // Pin Outer Circle (Colored by Disaster Category)
                    val pinCategoryColor = when {
                        report.disasterType.contains("Banjir", true) -> Color(0xFF2563EB)
                        report.disasterType.contains("Angin", true) || report.disasterType.contains("Pohon", true) -> Color(0xFFD97706)
                        report.disasterType.contains("Gelombang", true) || report.disasterType.contains("Abrasi", true) -> Color(0xFF0284C7)
                        report.disasterType.contains("Longsor", true) -> Color(0xFFB45309)
                        else -> Color(0xFFDC2626)
                    }

                    // Pin Base Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = 12f * scale,
                        center = Offset(pinX + 2f, pinY + 3f)
                    )

                    // Pin Outer
                    drawCircle(
                        color = pinCategoryColor,
                        radius = 14f * scale,
                        center = Offset(pinX, pinY)
                    )

                    // Pin Inner White Center
                    drawCircle(
                        color = Color.White,
                        radius = 8f * scale,
                        center = Offset(pinX, pinY)
                    )

                    // Pin Core Status Dot
                    drawCircle(
                        color = statusColor,
                        radius = 5f * scale,
                        center = Offset(pinX, pinY)
                    )
                }
            }

            // Click and gesture detection overlay over Canvas for pins and zones
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(filteredReports, scale, offsetX, offsetY) {
                        detectTapGestures { tapOffset ->
                            val hitRadiusSq = (36f * scale) * (36f * scale)
                            val clicked = filteredReports.firstOrNull { report ->
                                val normX = ((report.longitude - minLon) / (maxLon - minLon)).toFloat().coerceIn(0.15f, 0.90f)
                                val normY = (((maxLat - report.latitude) / (maxLat - minLat))).toFloat().coerceIn(0.10f, 0.90f)
                                val px = (normX * canvasWidth + offsetX) * scale
                                val py = (normY * canvasHeight + offsetY) * scale
                                val dx = tapOffset.x - px
                                val dy = tapOffset.y - py
                                (dx * dx + dy * dy) <= hitRadiusSq
                            }
                            if (clicked != null) {
                                selectedReport = clicked
                                selectedHazardZone = null
                            } else {
                                selectedReport = null
                                selectedHazardZone = null
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
            )

            // Map Controls (Zoom In, Zoom Out, Reset, Legend)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            if (scale < 3.0f) scale += 0.25f
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF1F2937))
                    }
                }

                // Zoom Out Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            if (scale > 0.75f) scale -= 0.25f
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF1F2937))
                    }
                }

                // Reset View Button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            scale = 1.0f
                            offsetX = 0f
                            offsetY = 0f
                            Toast.makeText(context, "Posisi peta di-reset", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Posisi", tint = Color(0xFF15803D))
                    }
                }
            }

            // Map Legend Badge Overlay (Bottom Left)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.90f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "LEGENDA STATUS PIN:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFEA580C), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Diproses", fontSize = 9.sp, color = Color(0xFF4B5563))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF16A34A), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Divalidasi", fontSize = 9.sp, color = Color(0xFF4B5563))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF2563EB), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Selesai", fontSize = 9.sp, color = Color(0xFF4B5563))
                    }
                }
            }
        }

        // 3. Selected Report Detail Card (Popup Sheet at bottom of map)
        selectedReport?.let { report ->
            ReportInspectionCard(
                report = report,
                onClose = { selectedReport = null },
                onUpdateStatus = { newStatus ->
                    onUpdateReportStatus(report.id, newStatus)
                    selectedReport = report.copy(status = newStatus)
                    Toast.makeText(context, "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus'", Toast.LENGTH_SHORT).show()
                },
                onOpenExternalMap = {
                    val uri = Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${Uri.encode(report.disasterType + " - " + report.village)})")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Aplikasi peta tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                },
                onCallBpbd = {
                    EmergencyDialer.dial(context, "117")
                }
            )
        }
    }
}

/**
 * Inspection Card for a Pin clicked on the spatial map
 */
@Composable
private fun ReportInspectionCard(
    report: CitizenDisasterReport,
    onClose: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onOpenExternalMap: () -> Unit,
    onCallBpbd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_report_inspection_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Disaster Name, Status Badge, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.disasterType,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C)
                    )
                    Text(
                        text = "ID: ${report.id} • ${report.incidentTime}",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & GPS Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${report.village}, Kec. ${report.district} (📍 %.5f, %.5f)".format(report.latitude, report.longitude),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }

            Text(
                text = "Lokasi detail: ${report.incidentLocation}",
                fontSize = 11.sp,
                color = Color(0xFF4B5563),
                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
            )

            // Casualties Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasualtyTag("Meninggal: ${report.casualtiesDead}", Color(0xFFDC2626), report.casualtiesDead > 0)
                CasualtyTag("Luka: ${report.casualtiesInjured}", Color(0xFFD97706), report.casualtiesInjured > 0)
                CasualtyTag("Hilang: ${report.casualtiesMissing}", Color(0xFF4B5563), report.casualtiesMissing > 0)
            }

            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

            // Status Management Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ubah Status Laporan:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusButton("Diproses", report.status == "Diproses", Color(0xFFEA580C)) {
                        onUpdateStatus("Diproses")
                    }
                    StatusButton("Divalidasi", report.status == "Divalidasi", Color(0xFF16A34A)) {
                        onUpdateStatus("Divalidasi")
                    }
                    StatusButton("Selesai", report.status == "Selesai", Color(0xFF2563EB)) {
                        onUpdateStatus("Selesai")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Open in Google Maps & Call BPBD 117
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenExternalMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buka di Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCallBpbd,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hubungi BPBD 117", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CasualtyTag(text: String, color: Color, isNonZero: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isNonZero) color.copy(alpha = 0.15f) else Color(0xFFF3F4F6)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
            color = if (isNonZero) color else Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatusButton(label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) activeColor else Color(0xFFF3F4F6),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) activeColor else Color(0xFFD1D5DB)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color(0xFF4B5563),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
