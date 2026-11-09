package com.example.ui.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSeaCyan
import java.util.Locale

data class WaveHistoryPoint(
    val dateLabel: String,     // e.g. "10 Okt", "11 Okt"
    val fullDate: String,      // "2026-10-10"
    val waveHeight: Float,     // in meters, e.g. 1.2f
    val swellPeriod: Float,    // in seconds, e.g. 9.5f
    val windSpeedKnots: Float, // in knots
    val seaCondition: String   // "Tenang", "Sedang", "Tinggi"
)

object MockMukomukoWaveHistory {
    fun getHistory7Days(): List<WaveHistoryPoint> = listOf(
        WaveHistoryPoint("10 Okt", "10 Oktober 2026", 0.8f, 7.5f, 8.2f, "Tenang (Aman Nelayan)"),
        WaveHistoryPoint("11 Okt", "11 Oktober 2026", 1.1f, 8.0f, 10.5f, "Tenang - Sedang"),
        WaveHistoryPoint("12 Okt", "12 Oktober 2026", 1.5f, 9.2f, 14.1f, "Sedang (Waspada Katir)"),
        WaveHistoryPoint("13 Okt", "13 Oktober 2026", 2.4f, 11.0f, 18.6f, "Tinggi (Peringatan Gelombang)"),
        WaveHistoryPoint("14 Okt", "14 Oktober 2026", 2.8f, 12.5f, 22.0f, "Sangat Tinggi (Bahaya Laut)"),
        WaveHistoryPoint("15 Okt", "15 Oktober 2026", 1.9f, 10.0f, 16.2f, "Sedang - Tinggi"),
        WaveHistoryPoint("16 Okt", "16 Oktober 2026", 1.2f, 8.5f, 11.0f, "Sedang (Kondusif)")
    )

    fun getHistory14Days(): List<WaveHistoryPoint> = listOf(
        WaveHistoryPoint("03 Okt", "03 Oktober 2026", 0.7f, 6.8f, 7.5f, "Tenang"),
        WaveHistoryPoint("04 Okt", "04 Oktober 2026", 0.9f, 7.2f, 8.8f, "Tenang"),
        WaveHistoryPoint("05 Okt", "05 Oktober 2026", 1.2f, 8.1f, 11.2f, "Sedang"),
        WaveHistoryPoint("06 Okt", "06 Oktober 2026", 1.8f, 9.5f, 15.0f, "Sedang"),
        WaveHistoryPoint("07 Okt", "07 Oktober 2026", 2.1f, 10.2f, 17.5f, "Waspada Gelombang"),
        WaveHistoryPoint("08 Okt", "08 Oktober 2026", 1.6f, 8.8f, 13.0f, "Sedang"),
        WaveHistoryPoint("09 Okt", "09 Oktober 2026", 1.0f, 7.8f, 9.5f, "Tenang"),
        WaveHistoryPoint("10 Okt", "10 Oktober 2026", 0.8f, 7.5f, 8.2f, "Tenang"),
        WaveHistoryPoint("11 Okt", "11 Oktober 2026", 1.1f, 8.0f, 10.5f, "Tenang"),
        WaveHistoryPoint("12 Okt", "12 Oktober 2026", 1.5f, 9.2f, 14.1f, "Sedang"),
        WaveHistoryPoint("13 Okt", "13 Oktober 2026", 2.4f, 11.0f, 18.6f, "Tinggi"),
        WaveHistoryPoint("14 Okt", "14 Oktober 2026", 2.8f, 12.5f, 22.0f, "Sangat Tinggi"),
        WaveHistoryPoint("15 Okt", "15 Oktober 2026", 1.9f, 10.0f, 16.2f, "Sedang"),
        WaveHistoryPoint("16 Okt", "16 Oktober 2026", 1.2f, 8.5f, 11.0f, "Sedang")
    )
}

@Composable
fun MarineWaveHistoryChart(
    district: District,
    modifier: Modifier = Modifier
) {
    var selectedRangeIndex by remember { mutableIntStateOf(0) } // 0: 7 Hari, 1: 14 Hari
    val dataPoints = remember(selectedRangeIndex) {
        if (selectedRangeIndex == 0) MockMukomukoWaveHistory.getHistory7Days()
        else MockMukomukoWaveHistory.getHistory14Days()
    }

    var selectedPointIndex by remember(dataPoints) {
        mutableStateOf<Int?>(dataPoints.size - 1)
    }

    val selectedPoint = selectedPointIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.last()

    // Smooth entry animation for the chart curve
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedRangeIndex) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("marine_wave_history_chart"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with badge
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
                            .size(36.dp)
                            .background(WeatherSeaCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Grafik Gelombang",
                            tint = WeatherSeaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Tren Gelombang Historis",
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
                                color = WeatherSeaCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Historis BMKG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WeatherSeaCyan,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                            Text(
                                text = "Pesisir Samudra Hindia • ${district.name}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Range Selector Pills (7 Hari / 14 Hari)
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    val ranges = listOf("7 Hari", "14 Hari")
                    ranges.forEachIndexed { idx, label ->
                        val isSel = selectedRangeIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) OrangePrimary else Color.Transparent)
                                .clickable { selectedRangeIndex = idx }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) OrangeOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Tooltip Callout Card
            Surface(
                color = when {
                    selectedPoint.waveHeight >= 2.5f -> Color(0xFFFEF2F2)
                    selectedPoint.waveHeight >= 1.25f -> Color(0xFFFFF7ED)
                    else -> Color(0xFFF0FDF4)
                },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        selectedPoint.waveHeight >= 2.5f -> Color(0xFFFCA5A5)
                        selectedPoint.waveHeight >= 1.25f -> Color(0xFFFDBA74)
                        else -> Color(0xFFBBF7D0)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedPoint.fullDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Status: ${selectedPoint.seaCondition}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                selectedPoint.waveHeight >= 2.5f -> Color(0xFFDC2626)
                                selectedPoint.waveHeight >= 1.25f -> OrangePrimary
                                else -> WeatherGoodGreen
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", selectedPoint.waveHeight),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                selectedPoint.waveHeight >= 2.5f -> Color(0xFFDC2626)
                                selectedPoint.waveHeight >= 1.25f -> OrangePrimary
                                else -> WeatherGoodGreen
                            },
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = " meter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 3.dp).weight(0.5f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wave Chart Canvas Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val widthPerPoint = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                            val tappedIndex = ((offset.x + widthPerPoint / 2) / widthPerPoint)
                                .toInt()
                                .coerceIn(0, dataPoints.size - 1)
                            selectedPointIndex = tappedIndex
                        }
                    }
                    .pointerInput(dataPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val widthPerPoint = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                            val draggedIndex = ((change.position.x + widthPerPoint / 2) / widthPerPoint)
                                .toInt()
                                .coerceIn(0, dataPoints.size - 1)
                            selectedPointIndex = draggedIndex
                        }
                    }
            ) {
                val maxWave = 3.5f
                val minWave = 0.0f

                val orangePrimary = OrangePrimary
                val seaCyan = WeatherSeaCyan
                val textMutedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (dataPoints.size - 1).coerceAtLeast(1)

                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = height - topPadding - bottomPadding

                    fun getY(waveMeters: Float): Float {
                        val normalized = (waveMeters - minWave) / (maxWave - minWave)
                        return height - bottomPadding - (normalized * chartHeight * animationProgress.value)
                    }

                    // 1. Draw Threshold Reference Lines
                    // Safe line (1.25m - Batas Aman Perahu Katir)
                    val ySafe = getY(1.25f)
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.5f),
                        start = Offset(0f, ySafe),
                        end = Offset(width, ySafe),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Danger line (2.5m - Batas Gelombang Tinggi BMKG)
                    val yDanger = getY(2.5f)
                    drawLine(
                        color = Color(0xFFEF4444).copy(alpha = 0.6f),
                        start = Offset(0f, yDanger),
                        end = Offset(width, yDanger),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // 2. Build Cubic Bezier Curve Path & Area Path
                    val linePath = Path()
                    val areaPath = Path()

                    val points = dataPoints.mapIndexed { index, point ->
                        Offset(index * spacing, getY(point.waveHeight))
                    }

                    if (points.isNotEmpty()) {
                        linePath.moveTo(points.first().x, points.first().y)
                        areaPath.moveTo(points.first().x, height - bottomPadding)
                        areaPath.lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                            linePath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p1.x, p1.y
                            )
                            areaPath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p1.x, p1.y
                            )
                        }

                        areaPath.lineTo(points.last().x, height - bottomPadding)
                        areaPath.close()

                        // Fill under curve with oceanic sunset gradient
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    orangePrimary.copy(alpha = 0.35f),
                                    seaCyan.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = height - bottomPadding
                            )
                        )

                        // Draw Curve Stroke
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(seaCyan, orangePrimary, Color(0xFFEA580C))
                            ),
                            style = Stroke(
                                width = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // 3. Draw Selected Data Point & Vertical Guide Line
                    selectedPointIndex?.let { selIdx ->
                        if (selIdx in points.indices) {
                            val selPoint = points[selIdx]

                            // Vertical scrubber line
                            drawLine(
                                color = orangePrimary.copy(alpha = 0.7f),
                                start = Offset(selPoint.x, topPadding),
                                end = Offset(selPoint.x, height - bottomPadding),
                                strokeWidth = 1.8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )

                            // Outer halo
                            drawCircle(
                                color = orangePrimary.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = selPoint
                            )
                            // White border
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = selPoint
                            )
                            // Inner solid core
                            drawCircle(
                                color = orangePrimary,
                                radius = 4.dp.toPx(),
                                center = selPoint
                            )
                        }
                    }
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEachIndexed { idx, pt ->
                    val isSel = selectedPointIndex == idx
                    Text(
                        text = pt.dateLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) OrangePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend Row for Thresholds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = Color(0xFF10B981),
                    label = "Aman Katir (≤ 1.25m)"
                )
                LegendItem(
                    color = Color(0xFFF59E0B),
                    label = "Sedang (1.25 - 2.5m)"
                )
                LegendItem(
                    color = Color(0xFFEF4444),
                    label = "Waspada (> 2.5m)"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}
