package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyItem
import com.example.model.WeatherFullReport
import com.example.ui.theme.OrangePrimary
import kotlin.math.max
import kotlin.math.min

/**
 * Kartu Ringkasan Parameter Meteorologi Kabupaten Mukomuko
 * Dilengkapi grafik tren harian suhu (°C) dan curah hujan (mm) berbasis Canvas Native Jetpack Compose.
 */
@Composable
fun MeteorologySummaryCard(
    report: WeatherFullReport,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dailyList = report.daily
    val current = report.current
    val district = report.district

    val maxTemp = dailyList.maxOfOrNull { it.maxTemp } ?: (current.temperature + 2.0)
    val minTemp = dailyList.minOfOrNull { it.minTemp } ?: (current.temperature - 3.0)
    val totalRain7Days = dailyList.sumOf { it.precipitationSum }
    val avgHumidity = current.humidity

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("meteorology_summary_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = OrangePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Parameter Meteorologi Harian",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "${district.name} • Radar & Sensor BMKG",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .height(24.dp)
                        .wrapContentWidth()
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "TREN 7 HARI",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Mini Metrics Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Max Temp
                MetricPill(
                    label = "Suhu Max",
                    value = "%.1f°C".format(maxTemp),
                    color = Color(0xFFEA580C),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                // Metric 2: Min Temp
                MetricPill(
                    label = "Suhu Min",
                    value = "%.1f°C".format(minTemp),
                    color = Color(0xFF0284C7),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                // Metric 3: Total Rain
                MetricPill(
                    label = "Hujan 7-Hr",
                    value = "%.1f mm".format(totalRain7Days),
                    color = Color(0xFF2563EB),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                // Metric 4: Humidity
                MetricPill(
                    label = "Kelembapan",
                    value = "$avgHumidity%",
                    color = Color(0xFF16A34A),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Native Compose Canvas Chart
            Text(
                text = "Grafik Tren Fluktuasi Suhu (°C) & Presipitasi Hujan (mm)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))

            MeteorologyCanvasChart(
                dailyList = dailyList,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend 1: Temp Line
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEA580C),
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Suhu Maksimal (°C)",
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Legend 2: Rain Bars
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(width = 10.dp, height = 7.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Curah Hujan (mm)",
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MeteorologyCanvasChart(
    dailyList: List<DailyItem>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val items = dailyList.take(7)
    if (items.isEmpty()) return

    val gridColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val labelColor = if (isDark) android.graphics.Color.rgb(148, 163, 184) else android.graphics.Color.rgb(100, 116, 139)
    val tempColor = Color(0xFFEA580C)
    val rainBarColor = Color(0xFF38BDF8).copy(alpha = 0.75f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 16f
        val paddingRight = 16f
        val paddingTop = 20f
        val paddingBottom = 26f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val count = items.size
        val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

        // Find min and max for temperatures
        val maxT = (items.maxOfOrNull { it.maxTemp } ?: 34.0).toFloat()
        val minT = (items.minOfOrNull { it.minTemp } ?: 20.0).toFloat()
        val tempRange = max(maxT - minT, 4f)

        // Find max rain
        val maxR = max(items.maxOfOrNull { it.precipitationSum.toFloat() } ?: 10f, 5f)

        // Draw horizontal grid lines
        for (i in 0..3) {
            val y = paddingTop + (chartHeight * (i / 3f))
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        // Draw Rainfall Bars in background
        val barWidth = min(stepX * 0.38f, 22f)
        items.forEachIndexed { index, day ->
            val cx = paddingLeft + (index * stepX)
            val rainVal = day.precipitationSum.toFloat()
            val barHeight = (rainVal / maxR) * (chartHeight * 0.65f)
            val barTop = paddingTop + chartHeight - barHeight

            drawRoundRect(
                color = rainBarColor,
                topLeft = Offset(cx - (barWidth / 2f), barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Compute temperature points
        val tempPoints = items.mapIndexed { index, day ->
            val cx = paddingLeft + (index * stepX)
            val normalized = (day.maxTemp.toFloat() - minT) / tempRange
            val cy = paddingTop + chartHeight - (normalized * chartHeight)
            Offset(cx, cy)
        }

        // Draw Temperature Curve
        if (tempPoints.size > 1) {
            val path = Path().apply {
                moveTo(tempPoints.first().x, tempPoints.first().y)
                for (i in 0 until tempPoints.size - 1) {
                    val p0 = tempPoints[i]
                    val p1 = tempPoints[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = path,
                color = tempColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // Draw Points & Labels
        val textPaint = android.graphics.Paint().apply {
            color = labelColor
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val tempTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(234, 88, 12)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        items.forEachIndexed { index, day ->
            val pt = tempPoints[index]

            // Outer ring
            drawCircle(
                color = Color.White,
                radius = 5.5f,
                center = pt
            )
            // Inner dot
            drawCircle(
                color = tempColor,
                radius = 3.5f,
                center = pt
            )

            // Temp label above point
            drawContext.canvas.nativeCanvas.drawText(
                "${day.maxTemp.toInt()}°",
                pt.x,
                pt.y - 8f,
                tempTextPaint
            )

            // Day label below chart
            val shortDay = day.dayName.take(3)
            drawContext.canvas.nativeCanvas.drawText(
                shortDay,
                pt.x,
                height - 4f,
                textPaint
            )
        }
    }
}
