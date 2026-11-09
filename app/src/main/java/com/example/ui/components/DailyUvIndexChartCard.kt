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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSunGold
import java.util.Calendar

data class UvHourPoint(
    val timeLabel: String,
    val hour: Int,
    val uvValue: Float
)

@Composable
fun DailyUvIndexChartCard(
    maxUvIndex: Double,
    modifier: Modifier = Modifier
) {
    // Generate daily UV index distribution curve (06:00 - 18:00) scaled to maxUvIndex
    val hourlyUvPoints = remember(maxUvIndex) {
        val peak = maxUvIndex.toFloat().coerceAtLeast(6.0f)
        listOf(
            UvHourPoint("06.00", 6, 0.2f),
            UvHourPoint("08.00", 8, peak * 0.28f),
            UvHourPoint("10.00", 10, peak * 0.72f),
            UvHourPoint("12.00", 12, peak * 0.98f),
            UvHourPoint("13.00", 13, peak * 1.0f), // Peak around midday in tropical Mukomuko
            UvHourPoint("14.00", 14, peak * 0.82f),
            UvHourPoint("16.00", 16, peak * 0.35f),
            UvHourPoint("18.00", 18, 0.3f)
        )
    }

    val currentCalendar = remember { Calendar.getInstance() }
    val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCalendar.get(Calendar.MINUTE)

    val currentUv = remember(currentHour, hourlyUvPoints) {
        when {
            currentHour < 6 || currentHour >= 18 -> 0.0f
            currentHour in 6..7 -> hourlyUvPoints[0].uvValue + (hourlyUvPoints[1].uvValue - hourlyUvPoints[0].uvValue) * (currentHour - 6 + currentMinute / 60f) / 2f
            currentHour in 8..9 -> hourlyUvPoints[1].uvValue + (hourlyUvPoints[2].uvValue - hourlyUvPoints[1].uvValue) * (currentHour - 8 + currentMinute / 60f) / 2f
            currentHour in 10..11 -> hourlyUvPoints[2].uvValue + (hourlyUvPoints[3].uvValue - hourlyUvPoints[2].uvValue) * (currentHour - 10 + currentMinute / 60f) / 2f
            currentHour in 12..13 -> hourlyUvPoints[3].uvValue + (hourlyUvPoints[4].uvValue - hourlyUvPoints[3].uvValue) * (currentHour - 12 + currentMinute / 60f)
            currentHour in 14..15 -> hourlyUvPoints[5].uvValue - (hourlyUvPoints[5].uvValue - hourlyUvPoints[6].uvValue) * (currentHour - 14 + currentMinute / 60f) / 2f
            currentHour in 16..17 -> hourlyUvPoints[6].uvValue - (hourlyUvPoints[6].uvValue - hourlyUvPoints[7].uvValue) * (currentHour - 16 + currentMinute / 60f) / 2f
            else -> 0.0f
        }
    }

    val (uvCategory, categoryColor) = when {
        maxUvIndex >= 11.0 -> "Ekstrem" to Color(0xFF7C3AED)
        maxUvIndex >= 8.0 -> "Sangat Tinggi" to Color(0xFFDC2626)
        maxUvIndex >= 6.0 -> "Tinggi" to OrangePrimary
        maxUvIndex >= 3.0 -> "Sedang" to WeatherSunGold
        else -> "Rendah" to WeatherGoodGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_uv_chart_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(OrangePrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Grafik Indeks Sinar UV",
                        tint = OrangePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Harian Indeks UV",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = categoryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Maks $uvCategory",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Paparan sinar UV pesisir Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Peak Exposure Times Warning Banner
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan Waktu Puncak",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Waktu Paparan Puncak: 10.00 – 14.00 WIB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                        Text(
                            text = "Radiasi UV mencapai titik tertinggi (${String.format("%.1f", maxUvIndex)}). Wajib kenakan tabir surya & pelindung mata.",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Native Jetpack Compose Canvas Chart for UV Hourly Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val paddingLeft = 36.dp.toPx()
                    val paddingRight = 18.dp.toPx()
                    val paddingTop = 24.dp.toPx()
                    val paddingBottom = 28.dp.toPx()

                    val chartWidth = canvasWidth - paddingLeft - paddingRight
                    val chartHeight = canvasHeight - paddingTop - paddingBottom
                    val maxChartUv = 12.0f

                    // 1. Shaded Peak Exposure Time Band (10:00 to 14:00)
                    // Index 2 is 10:00, index 5 is 14:00
                    val peakStartX = paddingLeft + (2f / (hourlyUvPoints.size - 1)) * chartWidth
                    val peakEndX = paddingLeft + (5f / (hourlyUvPoints.size - 1)) * chartWidth

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFEE2E2).copy(alpha = 0.65f),
                                Color(0xFFFEF3C7).copy(alpha = 0.25f)
                            ),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        ),
                        topLeft = Offset(peakStartX, paddingTop),
                        size = Size(peakEndX - peakStartX, chartHeight)
                    )

                    // Dashed borders for peak zone
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color(0xFFF87171).copy(alpha = 0.7f),
                        start = Offset(peakStartX, paddingTop),
                        end = Offset(peakStartX, paddingTop + chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color(0xFFF87171).copy(alpha = 0.7f),
                        start = Offset(peakEndX, paddingTop),
                        end = Offset(peakEndX, paddingTop + chartHeight),
                        strokeWidth = 1.5f,
                        pathEffect = dashEffect
                    )

                    // 2. Horizontal Grid Lines & Y-axis labels
                    val yLevels = listOf(0f, 3f, 6f, 8f, 11f)
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        isAntiAlias = true
                    }

                    yLevels.forEach { lvl ->
                        val y = paddingTop + chartHeight - (lvl / maxChartUv) * chartHeight
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(paddingLeft, y),
                            end = Offset(canvasWidth - paddingRight, y),
                            strokeWidth = 1f
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            lvl.toInt().toString(),
                            8.dp.toPx(),
                            y + 8f,
                            textPaint
                        )
                    }

                    // 3. Build Smooth Bezier Curve Path
                    val points = hourlyUvPoints.mapIndexed { idx, point ->
                        val x = paddingLeft + (idx.toFloat() / (hourlyUvPoints.size - 1)) * chartWidth
                        val y = paddingTop + chartHeight - (point.uvValue.coerceIn(0f, maxChartUv) / maxChartUv) * chartHeight
                        Offset(x, y)
                    }

                    val strokePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        strokePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            strokePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }

                        // Close fill path
                        fillPath.lineTo(points.last().x, paddingTop + chartHeight)
                        fillPath.lineTo(points.first().x, paddingTop + chartHeight)
                        fillPath.close()

                        // Draw Gradient Area under Curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    categoryColor.copy(alpha = 0.38f),
                                    WeatherSunGold.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Draw Curve Stroke
                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    WeatherGoodGreen,
                                    WeatherSunGold,
                                    categoryColor,
                                    WeatherSunGold,
                                    WeatherGoodGreen
                                )
                            ),
                            style = Stroke(width = 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Draw Point dots and X-axis Time Labels
                        val timeLabelPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        points.forEachIndexed { idx, pt ->
                            val ptColor = when {
                                hourlyUvPoints[idx].uvValue >= 8.0f -> Color(0xFFDC2626)
                                hourlyUvPoints[idx].uvValue >= 6.0f -> OrangePrimary
                                hourlyUvPoints[idx].uvValue >= 3.0f -> WeatherSunGold
                                else -> WeatherGoodGreen
                            }

                            // Dot
                            drawCircle(
                                color = Color.White,
                                radius = 4.5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = ptColor,
                                radius = 3.dp.toPx(),
                                center = pt
                            )

                            // X Label
                            drawContext.canvas.nativeCanvas.drawText(
                                hourlyUvPoints[idx].timeLabel,
                                pt.x,
                                canvasHeight - 4.dp.toPx(),
                                timeLabelPaint
                            )
                        }

                        // 4. Highlight Peak Point
                        val peakIdx = 4 // 13:00
                        if (peakIdx < points.size) {
                            val peakPt = points[peakIdx]
                            drawCircle(
                                color = Color(0xFFDC2626).copy(alpha = 0.3f),
                                radius = 9.dp.toPx(),
                                center = peakPt
                            )
                            drawCircle(
                                color = Color(0xFFDC2626),
                                radius = 4.5.dp.toPx(),
                                center = peakPt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // UV Standard Scale & Health Recommendations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UvScaleLegendItem(label = "0-2 Rendah", color = WeatherGoodGreen)
                UvScaleLegendItem(label = "3-5 Sedang", color = WeatherSunGold)
                UvScaleLegendItem(label = "6-7 Tinggi", color = OrangePrimary)
                UvScaleLegendItem(label = "8-10 S. Tinggi", color = Color(0xFFDC2626))
                UvScaleLegendItem(label = "11+ Ekstrem", color = Color(0xFF7C3AED))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Protection Checklist based on Peak UV Level
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Panduan Proteksi Sinar UV Mukomuko:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Gunakan tabir surya (sunscreen) ber-SPF minimal 30+ setiap 2 jam.\n" +
                                "• Kenakan kacamata hitam berfilter anti-UV (UV-400) dan topi bertepi lebar.\n" +
                                "• Utamakan berada di tempat teduh selama rentang paparan puncak (10.00 - 14.00 WIB).\n" +
                                "• Bagi nelayan pantai dan petani sawit, lindungi kulit dengan pakaian lengan panjang berbahan sejuk.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UvScaleLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}
