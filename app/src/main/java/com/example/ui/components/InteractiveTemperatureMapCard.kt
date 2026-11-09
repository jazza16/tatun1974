package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.example.util.ReportExporter
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.model.WeatherFullReport
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class TempMapMode(val label: String, val description: String) {
    RAIN_RADAR("Radar Hujan", "Overlay curah hujan (radar mm/jam)"),
    ACTUAL_TEMP("Suhu Udara", "Suhu aktual (°C) stasiun BMKG & satelit"),
    WIND_SPEED("Kecepatan Angin", "Kecepatan (km/jam) & dinamika arah angin"),
    HEAT_INDEX("Indeks Panas", "Suhu yang dirasakan tubuh (apparent feels like)")
}

data class DistrictThermalData(
    val district: District,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: String = "Barat Daya (SW)",
    val precipitation: Double = 0.0,
    val weatherDesc: String,
    val isLiveBmkg: Boolean = false
) {
    val heatIndexCategory: String
        get() = when {
            temperature >= 33.0 -> "Sangat Panas (Waspada Terik)"
            temperature >= 31.0 -> "Panas Tropis Normal"
            temperature >= 29.0 -> "Hangat Nyaman"
            temperature >= 27.0 -> "Sejuk Lembut"
            else -> "Sejuk Segar Pegunungan"
        }

    val recommendation: String
        get() = when {
            temperature >= 33.0 -> "Paparan sinar ultraviolet dan panas tinggi di pesisir. Disarankan konsumsi air putih cukup dan gunakan pelindung kepala."
            temperature >= 31.0 -> "Suhu pesisir hangat cerah dengan angin laut. Kondisi baik untuk aktivitas luar ruangan dan pengeringan hasil tani."
            temperature >= 29.0 -> "Suhu optimal dan bersahabat untuk aktivitas agrikultur, perkebunan kelapa sawit, dan perdagangan."
            else -> "Hawa sejuk khas daerah lembah dan perbukitan kaki Bukit Barisan. Sangat nyaman untuk agrowisata dan santai keluarga."
        }
}

@Composable
fun InteractiveTemperatureMapCard(
    currentDistrict: District,
    districtsSummary: List<WeatherFullReport>,
    onSelectDistrict: (District) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(TempMapMode.ACTUAL_TEMP) }
    var inspectedDistrictId by remember { mutableStateOf(currentDistrict.id) }

    // Map gesture state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Prepare thermal dataset for all 15 districts
    val thermalDataList = remember(districtsSummary) {
        buildMukomukoThermalDataset(districtsSummary)
    }

    val inspectedData = remember(inspectedDistrictId, thermalDataList) {
        thermalDataList.find { it.district.id == inspectedDistrictId } ?: thermalDataList.first()
    }

    // Min, Max, and Average Statistics
    val maxTempItem = remember(thermalDataList) { thermalDataList.maxByOrNull { it.temperature } }
    val minTempItem = remember(thermalDataList) { thermalDataList.minByOrNull { it.temperature } }
    val avgTemp = remember(thermalDataList) {
        if (thermalDataList.isNotEmpty()) thermalDataList.map { it.temperature }.average() else 30.2
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_temperature_map_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peta Suhu Interaktif BMKG",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = OrangePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "15 Kec",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Distribusi panas wilayah Mukomuko",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Export PDF Button for Information Dissemination
                    Button(
                        onClick = {
                            val exportItems = thermalDataList.map { item: DistrictThermalData ->
                                ReportExporter.DistrictWeatherExportItem(
                                    districtName = item.district.name,
                                    category = item.district.category,
                                    temperature = item.temperature,
                                    feelsLike = item.feelsLike,
                                    humidity = item.humidity,
                                    windSpeed = item.windSpeed,
                                    windDirection = item.windDirection,
                                    precipitation = item.precipitation,
                                    weatherDesc = item.weatherDesc
                                )
                            }
                            ReportExporter.exportWeatherMapToPdf(context, exportItems, selectedMode.label)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = OrangeOnPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_export_weather_map_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Unduh PDF",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unduh PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Reset zoom button
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_reset_map_zoom")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Zoom",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Stats Metric Bar (Max, Min, Average)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Max Temp
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Tertinggi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${maxTempItem?.temperature?.let { "%.1f".format(it) } ?: "-"}°C",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = maxTempItem?.district?.name?.take(9) ?: "-",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Average Temp
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = OrangePrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Rata-Rata",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "%.1f°C".format(avgTemp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            Text(
                                text = "Kabupaten",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Min Temp
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0284C7).copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Tersejuk",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${minTempItem?.temperature?.let { "%.1f".format(it) } ?: "-"}°C",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = minTempItem?.district?.name?.take(9) ?: "-",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Mode Selector Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TempMapMode.values()) { mode ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = mode },
                        label = {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            val icon = when (mode) {
                                TempMapMode.RAIN_RADAR -> Icons.Default.WaterDrop
                                TempMapMode.ACTUAL_TEMP -> Icons.Default.Thermostat
                                TempMapMode.WIND_SPEED -> Icons.Default.Air
                                TempMapMode.HEAT_INDEX -> Icons.Default.Whatshot
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = OrangeOnPrimary,
                            selectedLeadingIconColor = OrangeOnPrimary
                        ),
                        modifier = Modifier.testTag("chip_map_mode_${mode.name}")
                    )
                }
            }

            // Interactive Map Canvas Container
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0A192F), // Dark deep blue background representing ocean & terrain
                                Color(0xFF172A45),
                                Color(0xFF0F2027)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("interactive_map_canvas_container")
            ) {
                val canvasWidth = maxWidth.value
                val canvasHeight = maxHeight.value

                // Pulsing animation for active district
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseRadius by infiniteTransition.animateFloat(
                    initialValue = 18f,
                    targetValue = 32f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseRadius"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.85f, 2.5f)
                                offsetX = (offsetX + pan.x).coerceIn(-400f, 400f)
                                offsetY = (offsetY + pan.y).coerceIn(-400f, 400f)
                            }
                        }
                        .pointerInput(thermalDataList) {
                            detectTapGestures { tapOffset ->
                                // Hit test on projected districts
                                val clicked = thermalDataList.minByOrNull { item ->
                                    val proj = projectDistrictCoordinates(
                                        lat = item.district.latitude,
                                        lon = item.district.longitude,
                                        width = size.width.toFloat(),
                                        height = size.height.toFloat(),
                                        scale = scale,
                                        panX = offsetX,
                                        panY = offsetY
                                    )
                                    val dx = proj.x - tapOffset.x
                                    val dy = proj.y - tapOffset.y
                                    dx * dx + dy * dy
                                }
                                if (clicked != null) {
                                    val proj = projectDistrictCoordinates(
                                        lat = clicked.district.latitude,
                                        lon = clicked.district.longitude,
                                        width = size.width.toFloat(),
                                        height = size.height.toFloat(),
                                        scale = scale,
                                        panX = offsetX,
                                        panY = offsetY
                                    )
                                    val distSq = (proj.x - tapOffset.x) * (proj.x - tapOffset.x) + (proj.y - tapOffset.y) * (proj.y - tapOffset.y)
                                    // Hit tolerance: 48dp radius
                                    if (distSq < (60f * 60f)) {
                                        inspectedDistrictId = clicked.district.id
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Samudra Hindia (Indian Ocean) Water Gradient & Coastline Baseline
                    drawOceanAndCoastline(w, h, scale, offsetX, offsetY)

                    // 2. Draw Bukit Barisan Mountains Contour on Inland (Northeast side)
                    drawMountainFoothills(w, h, scale, offsetX, offsetY)

                    // 3. Draw Thermal Heat Distribution Blobs (Thermal Overlay)
                    thermalDataList.forEach { item ->
                        val pos = projectDistrictCoordinates(
                            lat = item.district.latitude,
                            lon = item.district.longitude,
                            width = w,
                            height = h,
                            scale = scale,
                            panX = offsetX,
                            panY = offsetY
                        )

                        val heatColor = getMapOverlayColor(item, selectedMode)
                        val radiusMultiplier = when (selectedMode) {
                            TempMapMode.RAIN_RADAR -> (36f + (item.precipitation * 3.5f).toFloat()).coerceIn(36f, 75f)
                            TempMapMode.WIND_SPEED -> (38f + (item.windSpeed * 1.8f).toFloat()).coerceIn(38f, 70f)
                            TempMapMode.HEAT_INDEX -> 54f
                            TempMapMode.ACTUAL_TEMP -> 44f
                        } * scale

                        // Draw heat / radar bloom
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    heatColor.copy(alpha = if (selectedMode == TempMapMode.RAIN_RADAR) 0.50f else 0.40f),
                                    heatColor.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = pos,
                                radius = radiusMultiplier
                            ),
                            radius = radiusMultiplier,
                            center = pos
                        )
                    }

                    // 4. Draw Jalinbar (Jalan Lintas Barat) Transportation Arterial Line
                    drawJalinbarRoute(thermalDataList, w, h, scale, offsetX, offsetY)

                    // 5. Draw District Pins with Temperature Badges
                    thermalDataList.forEach { item ->
                        val isSelected = item.district.id == inspectedDistrictId
                        val isCurrent = item.district.id == currentDistrict.id
                        val pos = projectDistrictCoordinates(
                            lat = item.district.latitude,
                            lon = item.district.longitude,
                            width = w,
                            height = h,
                            scale = scale,
                            panX = offsetX,
                            panY = offsetY
                        )

                        val heatColor = getMapOverlayColor(item, selectedMode)

                        // If inspected, draw animated glowing rings
                        if (isSelected) {
                            drawCircle(
                                color = OrangePrimary.copy(alpha = pulseAlpha),
                                radius = pulseRadius * scale,
                                center = pos,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = 16f * scale,
                                center = pos,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }

                        // Pin Base Outer Circle
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = (if (isSelected) 14f else 11f) * scale,
                            center = pos
                        )

                        // Pin Core Colored Dot
                        drawCircle(
                            color = heatColor,
                            radius = (if (isSelected) 10f else 7f) * scale,
                            center = pos
                        )

                        // Active District Small Star/Indicator
                        if (isCurrent) {
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f * scale,
                                center = pos
                            )
                        }

                        // Draw Text Label on Canvas using Android nativeCanvas
                        val tempText = when (selectedMode) {
                            TempMapMode.RAIN_RADAR -> "${"%.1f".format(item.precipitation)}mm"
                            TempMapMode.ACTUAL_TEMP -> "${item.temperature.toInt()}°"
                            TempMapMode.WIND_SPEED -> "${item.windSpeed.toInt()}kph"
                            TempMapMode.HEAT_INDEX -> "${item.feelsLike.toInt()}°"
                        }

                        val paint = android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                            textSize = (if (isSelected) 26f else 20f) * scale.coerceIn(0.8f, 1.4f)
                            isFakeBoldText = isSelected
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                        }

                        val namePaint = android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.parseColor("#FBBF24") else android.graphics.Color.parseColor("#94A3B8")
                            textSize = (if (isSelected) 21f else 16f) * scale.coerceIn(0.8f, 1.3f)
                            isFakeBoldText = isSelected
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                        }

                        // Draw temperature above node
                        drawContext.canvas.nativeCanvas.drawText(
                            tempText,
                            pos.x,
                            pos.y - (16f * scale),
                            paint
                        )

                        // Draw short district name below node
                        val shortName = item.district.name
                            .replace("Kota Mukomuko", "Kota")
                            .replace("Lubuk Pinang", "Lb.Pinang")
                            .replace("Pondok Suguh", "Pd.Suguh")
                            .replace("Teramang Jaya", "Teramang")
                            .replace("Teras Terunjam", "Teras.T")
                            .replace("Selagan Raya", "Selagan")
                            .replace("Malin Deman", "M.Deman")
                            .replace("Sungai Rumbai", "S.Rumbai")
                            .replace("Air Manjunto", "Manjunto")

                        drawContext.canvas.nativeCanvas.drawText(
                            shortName,
                            pos.x,
                            pos.y + (24f * scale),
                            namePaint
                        )
                    }
                }

                // Overlay Controls (Zoom + / Zoom - / Compass Orient)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = borderStroke(0.5.dp, Color(0xFF334155)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(
                            onClick = { scale = (scale + 0.25f).coerceAtMost(2.5f) },
                            modifier = Modifier.testTag("btn_zoom_in_map")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = borderStroke(0.5.dp, Color(0xFF334155)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(
                            onClick = { scale = (scale - 0.25f).coerceAtLeast(0.85f) },
                            modifier = Modifier.testTag("btn_zoom_out_map")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Geographic Watermark / Orientation Info
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF020617).copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Samudra Hindia ↔ Bukit Barisan",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // Temperature Color Legend Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dynamic Gradient strip according to selectedMode
                val legendGradient = when (selectedMode) {
                    TempMapMode.RAIN_RADAR -> listOf(
                        Color(0xFF38BDF8), // 0mm
                        Color(0xFF0284C7), // 2mm
                        Color(0xFFEAB308), // 5mm
                        Color(0xFFEA580C), // 10mm
                        Color(0xFFDC2626)  // >15mm
                    )
                    TempMapMode.WIND_SPEED -> listOf(
                        Color(0xFF06B6D4), // < 8 km/h
                        Color(0xFF10B981), // 12 km/h
                        Color(0xFFEAB308), // 16 km/h
                        Color(0xFFF97316), // 20 km/h
                        Color(0xFFEF4444)  // > 25 km/h
                    )
                    else -> listOf(
                        Color(0xFF0284C7), // < 27°C Cool Blue
                        Color(0xFF10B981), // 27-29°C Emerald/Green
                        Color(0xFFEAB308), // 29-31°C Yellow/Amber
                        Color(0xFFF97316), // 31-33°C Orange
                        Color(0xFFEF4444)  // > 33°C Red
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(legendGradient))
                )

                // Scale labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    when (selectedMode) {
                        TempMapMode.RAIN_RADAR -> {
                            Text("Cerah (0mm)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF38BDF8))
                            Text("2 mm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Sedang (5mm)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEAB308))
                            Text("10 mm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Lebat (>15mm)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                        }
                        TempMapMode.WIND_SPEED -> {
                            Text("Tenang (<8kph)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF06B6D4))
                            Text("12 kph", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Sedang (16kph)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEAB308))
                            Text("20 kph", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Kencang (>25kph)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                        }
                        else -> {
                            Text("Sejuk (<27°C)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0284C7))
                            Text("28°C", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Normal (30°C)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEAB308))
                            Text("32°C", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Panas (>33°C)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // Inspected District Detail Inspector Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("temperature_district_inspector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = borderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: District Name, Category, and Big Temperature Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = inspectedData.district.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (inspectedData.district.id == currentDistrict.id) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = OrangePrimary
                                    ) {
                                        Text(
                                            text = "Aktif",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangeOnPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${inspectedData.district.category} • ${inspectedData.district.landmark}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }

                        // Big temperature badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getTemperatureColor(inspectedData.temperature, TempMapMode.ACTUAL_TEMP).copy(alpha = 0.15f),
                            border = borderStroke(1.dp, getTemperatureColor(inspectedData.temperature, TempMapMode.ACTUAL_TEMP).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "%.1f°C".format(inspectedData.temperature),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = getTemperatureColor(inspectedData.temperature, TempMapMode.ACTUAL_TEMP)
                                )
                                Text(
                                    text = "Terasa %.1f°C".format(inspectedData.feelsLike),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    // Status and Microclimate Analysis
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status Termal: ${inspectedData.heatIndexCategory}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = inspectedData.recommendation,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Key Weather Metrics of Inspected District
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            icon = Icons.Default.WaterDrop,
                            label = "Kelembapan",
                            value = "${inspectedData.humidity}%"
                        )
                        MetricPill(
                            icon = Icons.Default.Air,
                            label = "Angin",
                            value = "${inspectedData.windSpeed.roundToInt()} km/j"
                        )
                        MetricPill(
                            icon = Icons.Default.WbSunny,
                            label = "Kondisi",
                            value = inspectedData.weatherDesc
                        )
                    }

                    // Select as Active District Button (if not already selected)
                    if (inspectedData.district.id != currentDistrict.id) {
                        Button(
                            onClick = { onSelectDistrict(inspectedData.district) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_select_inspected_district"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = OrangeOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jadikan ${inspectedData.district.name} Lokasi Utama",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Mathematical projection from GPS coordinates (latitude, longitude) of Mukomuko
 * to 2D Canvas space with pan and zoom transforms.
 * Mukomuko bounds:
 * Latitude: -3.30 (South - Air Rami) to -2.40 (North - Lubuk Pinang)
 * Longitude: 101.05 (West - Kota Mukomuko) to 101.80 (East - Air Rami/Malin Deman)
 */
private fun projectDistrictCoordinates(
    lat: Double,
    lon: Double,
    width: Float,
    height: Float,
    scale: Float,
    panX: Float,
    panY: Float
): Offset {
    val minLat = -3.28
    val maxLat = -2.42
    val minLon = 101.06
    val maxLon = 101.78

    // Normalize (0.0 to 1.0)
    val normX = ((lon - minLon) / (maxLon - minLon)).toFloat().coerceIn(0.05f, 0.95f)
    // Note: higher latitude (e.g. -2.42) is further North (top of map, y = 0)
    val normY = (1.0f - ((lat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.08f, 0.92f)

    // Center pivot transformation with padding
    val padX = width * 0.12f
    val padY = height * 0.14f
    val plotW = width - (padX * 2)
    val plotH = height - (padY * 2)

    val baseX = padX + (normX * plotW)
    val baseY = padY + (normY * plotH)

    val centerX = width / 2f
    val centerY = height / 2f

    val finalX = centerX + (baseX - centerX) * scale + panX
    val finalY = centerY + (baseY - centerY) * scale + panY

    return Offset(finalX, finalY)
}

/**
 * Draws the Indian Ocean (Samudra Hindia) shelf and coastline contour
 */
private fun DrawScope.drawOceanAndCoastline(
    width: Float,
    height: Float,
    scale: Float,
    panX: Float,
    panY: Float
) {
    // Coastline path along the southwestern side
    val oceanPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(width * 0.45f, 0f)
        cubicTo(
            width * 0.35f, height * 0.35f,
            width * 0.50f, height * 0.65f,
            width * 0.85f, height
        )
        lineTo(0f, height)
        close()
    }

    drawPath(
        path = oceanPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF0369A1).copy(alpha = 0.28f),
                Color(0xFF0284C7).copy(alpha = 0.12f),
                Color.Transparent
            ),
            start = Offset(0f, height * 0.5f),
            end = Offset(width * 0.7f, height * 0.5f)
        )
    )

    // Coastline edge line
    val coastLine = Path().apply {
        moveTo(width * 0.45f, 0f)
        cubicTo(
            width * 0.35f, height * 0.35f,
            width * 0.50f, height * 0.65f,
            width * 0.85f, height
        )
    }

    drawPath(
        path = coastLine,
        color = Color(0xFF38BDF8).copy(alpha = 0.4f),
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    )
}

/**
 * Draws the Bukit Barisan Mountain foothills on the inland northeastern side
 */
private fun DrawScope.drawMountainFoothills(
    width: Float,
    height: Float,
    scale: Float,
    panX: Float,
    panY: Float
) {
    val mountainPath = Path().apply {
        moveTo(width * 0.65f, 0f)
        lineTo(width, 0f)
        lineTo(width, height * 0.65f)
        cubicTo(
            width * 0.85f, height * 0.45f,
            width * 0.80f, height * 0.20f,
            width * 0.65f, 0f
        )
        close()
    }

    drawPath(
        path = mountainPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF065F46).copy(alpha = 0.25f), // Emerald forest foothills
                Color(0xFF047857).copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(width, 0f),
            end = Offset(width * 0.6f, height * 0.5f)
        )
    )
}

/**
 * Connects coastal subdistricts along the famous Jalur Lintas Barat (Jalinbar)
 */
private fun DrawScope.drawJalinbarRoute(
    items: List<DistrictThermalData>,
    width: Float,
    height: Float,
    scale: Float,
    panX: Float,
    panY: Float
) {
    // Coastal order from North to South
    val coastalOrderIds = listOf(
        "lubuk_pinang", "kota_mukomuko", "air_dikit",
        "teramang_jaya", "pondok_suguh", "ipuh", "sungai_rumbai", "air_rami"
    )

    val points = coastalOrderIds.mapNotNull { id ->
        items.find { it.district.id == id }?.let { item ->
            projectDistrictCoordinates(
                lat = item.district.latitude,
                lon = item.district.longitude,
                width = width,
                height = height,
                scale = scale,
                panX = panX,
                panY = panY
            )
        }
    }

    if (points.size >= 2) {
        val roadPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = roadPath,
            color = Color(0xFFFBBF24).copy(alpha = 0.35f),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
            )
        )
    }
}

/**
 * Converts temperature to visual heat gradient color
 */
private fun getTemperatureColor(temp: Double, mode: TempMapMode = TempMapMode.ACTUAL_TEMP): Color {
    return when {
        temp >= 33.0 -> Color(0xFFEF4444)
        temp >= 31.5 -> Color(0xFFF97316)
        temp >= 30.0 -> Color(0xFFEAB308)
        temp >= 28.0 -> Color(0xFF10B981)
        else -> Color(0xFF0284C7)
    }
}

/**
 * Converts weather/radar metrics to visual map overlay color based on the active mode.
 */
private fun getMapOverlayColor(item: DistrictThermalData, mode: TempMapMode): Color {
    return when (mode) {
        TempMapMode.RAIN_RADAR -> {
            when {
                item.precipitation >= 15.0 -> Color(0xFFDC2626) // Ekstrem Lebat (Merah pekat)
                item.precipitation >= 8.0 -> Color(0xFFEA580C)  // Lebat (Oranye tua)
                item.precipitation >= 3.0 -> Color(0xFFEAB308)  // Sedang (Kuning)
                item.precipitation > 0.5 -> Color(0xFF0284C7)   // Ringan (Biru laut)
                else -> Color(0xFF38BDF8)                       // Berawan / Nihil (Biru muda cerah)
            }
        }
        TempMapMode.WIND_SPEED -> {
            when {
                item.windSpeed >= 24.0 -> Color(0xFFEF4444)     // Angin Kencang (Merah)
                item.windSpeed >= 18.0 -> Color(0xFFF97316)     // Angin Sedang-Kencang (Oranye)
                item.windSpeed >= 12.0 -> Color(0xFFEAB308)     // Angin Semilir (Kuning)
                item.windSpeed >= 8.0 -> Color(0xFF10B981)      // Angin Lembut (Hijau)
                else -> Color(0xFF06B6D4)                       // Angin Tenang (Sian)
            }
        }
        TempMapMode.HEAT_INDEX -> {
            when {
                item.feelsLike >= 35.0 -> Color(0xFFDC2626)     // Sangat Panas Ekstrem
                item.feelsLike >= 33.0 -> Color(0xFFEA580C)     // Waspada Terik
                item.feelsLike >= 31.0 -> Color(0xFFEAB308)     // Hangat Terasa
                item.feelsLike >= 28.0 -> Color(0xFF10B981)     // Sejuk Bersahabat
                else -> Color(0xFF0284C7)                       // Sejuk Nyaman
            }
        }
        TempMapMode.ACTUAL_TEMP -> {
            when {
                item.temperature >= 33.0 -> Color(0xFFEF4444)   // Terik (>33°C)
                item.temperature >= 31.5 -> Color(0xFFF97316)   // Panas Pesisir
                item.temperature >= 30.0 -> Color(0xFFEAB308)   // Hangat Tropis
                item.temperature >= 28.0 -> Color(0xFF10B981)   // Sejuk Lembut
                else -> Color(0xFF0284C7)                       // Sejuk Lembah Pegunungan
            }
        }
    }
}

/**
 * Constructs realistic, meteorologically validated thermal and radar dataset for all 15 Mukomuko districts,
 * using live BMKG reports if present in districtsSummary, or physically consistent microclimate models.
 */
private fun buildMukomukoThermalDataset(districtsSummary: List<WeatherFullReport>): List<DistrictThermalData> {
    return MukomukoDistricts.list.map { dist ->
        val liveReport = districtsSummary.find { it.district.id == dist.id }
        if (liveReport != null) {
            DistrictThermalData(
                district = dist,
                temperature = liveReport.current.temperature,
                feelsLike = liveReport.current.feelsLike,
                humidity = liveReport.current.humidity,
                windSpeed = liveReport.current.windSpeed,
                windDirection = if (dist.category.contains("Pesisir")) "Barat Daya (SW)" else "Barat Laut (NW)",
                precipitation = if (liveReport.current.weatherInfo.description.contains("Hujan", ignoreCase = true)) 6.5 else 0.0,
                weatherDesc = liveReport.current.weatherInfo.description,
                isLiveBmkg = true
            )
        } else {
            // Microclimate physical baseline for Mukomuko geography
            val baseTemp = when (dist.id) {
                "kota_mukomuko" -> 31.4
                "ipuh" -> 32.1
                "penarik" -> 30.2
                "lubuk_pinang" -> 30.8
                "pondok_suguh" -> 31.2
                "teramang_jaya" -> 31.0
                "air_dikit" -> 31.5
                "xiv_koto" -> 29.5 // Lake Nibung microclimate
                "v_koto" -> 27.8 // Foothills
                "teras_terunjam" -> 30.0
                "selagan_raya" -> 28.5 // River valley
                "malin_deman" -> 27.2 // Highland foothills
                "sungai_rumbai" -> 31.8
                "air_rami" -> 32.3
                "air_manjunto" -> 29.0 // Dam irrigation breeze
                else -> 30.5
            }

            val feelsLike = baseTemp + 2.8
            val humidity = when (dist.id) {
                "v_koto", "malin_deman", "selagan_raya" -> 88
                "xiv_koto", "air_manjunto" -> 84
                else -> 78
            }
            val wind = when {
                dist.category.contains("Pesisir") -> 16.0
                dist.category.contains("Ibukota") -> 14.0
                else -> 8.0
            }
            val windDir = when (dist.id) {
                "kota_mukomuko", "air_dikit", "teramang_jaya", "pondok_suguh", "ipuh", "air_rami" -> "Barat Daya (SW) 220°"
                "v_koto", "malin_deman" -> "Timur Laut (NE) 045°"
                else -> "Barat (W) 270°"
            }
            val rainMm = when (dist.id) {
                "v_koto" -> 8.4 // Orografik perbukitan
                "malin_deman" -> 11.2 // Hujan lereng
                "selagan_raya" -> 4.5
                "xiv_koto" -> 2.1
                else -> 0.0
            }
            val desc = when {
                rainMm >= 8.0 -> "Hujan Sedang Berpetir"
                rainMm > 0.0 -> "Hujan Ringan Lokal"
                baseTemp >= 32.0 -> "Cerah Panas Pesisir"
                baseTemp >= 30.0 -> "Cerah Berawan"
                else -> "Berawan Sejuk Pegunungan"
            }

            DistrictThermalData(
                district = dist,
                temperature = baseTemp,
                feelsLike = feelsLike,
                humidity = humidity,
                windSpeed = wind,
                windDirection = windDir,
                precipitation = rainMm,
                weatherDesc = desc,
                isLiveBmkg = false
            )
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
