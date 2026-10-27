package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WeatherFullReport
import java.util.Locale

@Composable
fun HeroWeatherCard(
    report: WeatherFullReport,
    modifier: Modifier = Modifier
) {
    val current = report.current
    val district = report.district
    val weatherInfo = current.weatherInfo

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_weather_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEA580C), // Vivid Orange
                            Color(0xFFF97316), // Warm Tangerine
                            Color(0xFFC2410C)  // Deep Amber
                        )
                    )
                )
        ) {
            // Background illustration with gentle scrim
            Image(
                painter = painterResource(id = R.drawable.mukomuko_hero),
                contentDescription = "Pantai Mukomuko",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp)),
                alpha = 0.22f
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header inside card: District & Landmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = district.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${district.category} • ${district.landmark}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    if (report.isFromCache) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Offline Cache",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Temperature & Condition Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${current.temperature.toInt()}",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 68.sp
                            )
                            Text(
                                text = "°C",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Text(
                            text = "Terasa seperti ${current.feelsLike.toInt()}°C",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }

                    // Condition Badge & Dynamic Animated Weather Icon
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                DynamicWeatherIcon(
                                    weatherCode = current.weatherCode,
                                    isDay = current.isDay,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = weatherInfo.conditionText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val todayDaily = report.daily.firstOrNull()
                        if (todayDaily != null) {
                            Text(
                                text = "Maks: ${todayDaily.maxTemp.toInt()}° • Min: ${todayDaily.minTemp.toInt()}°",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick 4-stat metrics grid with rich visual indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kelembaban with visual level bar
                    StatPillWithIndicator(
                        icon = Icons.Default.WaterDrop,
                        label = "Kelembapan",
                        value = "${current.humidity}%",
                        subLabel = if (current.humidity >= 85) "Tinggi" else if (current.humidity >= 65) "Normal" else "Kering",
                        indicatorRatio = (current.humidity / 100f).coerceIn(0f, 1f),
                        indicatorColor = Color(0xFF67E8F9),
                        modifier = Modifier.weight(1f)
                    )

                    // Kecepatan Angin with intensity level
                    val windRatio = (current.windSpeed.toFloat() / 50f).coerceIn(0f, 1f)
                    val windStatus = when {
                        current.windSpeed >= 35 -> "Kencang"
                        current.windSpeed >= 18 -> "Sedang"
                        else -> "Tenang"
                    }
                    StatPillWithIndicator(
                        icon = Icons.Default.Air,
                        label = "Kecepatan",
                        value = "${current.windSpeed.toInt()} km/j",
                        subLabel = windStatus,
                        indicatorRatio = windRatio,
                        indicatorColor = if (current.windSpeed >= 35) Color(0xFFEF4444) else if (current.windSpeed >= 18) Color(0xFFFBBF24) else Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )

                    // Arah Angin with Rotating Compass Arrow
                    StatPillWindDirection(
                        windDegree = current.windDirection,
                        compassDirection = current.windDirectionCompass,
                        modifier = Modifier.weight(1f)
                    )

                    // Gelombang Laut
                    StatPillWithIndicator(
                        icon = Icons.Default.Surfing,
                        label = "Gelombang",
                        value = "${report.marine.waveHeight} m",
                        subLabel = report.marine.seaCondition.take(6),
                        indicatorRatio = (report.marine.waveHeight.toFloat() / 4.0f).coerceIn(0f, 1f),
                        indicatorColor = if (report.marine.waveHeight >= 2.0f) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Short advisory banner with dynamic compass needle
                Surface(
                    color = Color.Black.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Kompas Angin",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(current.windDirection.toFloat())
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Arah Angin: ${current.windDirectionCompass} (${current.windDirection}°) • Kecepatan: ${current.windSpeed.toInt()} km/j • Kelembapan: ${current.humidity}%",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPillWithIndicator(
    icon: ImageVector,
    label: String,
    value: String,
    subLabel: String,
    indicatorRatio: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Visual Mini Gauge / Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(indicatorRatio)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(indicatorColor)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = indicatorColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatPillWindDirection(
    windDegree: Int,
    compassDirection: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Arah Angin",
                tint = Color(0xFFFDE047),
                modifier = Modifier
                    .size(16.dp)
                    .rotate(windDegree.toFloat())
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = compassDirection,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Arah Angin",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$windDegree°",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFDE047)
            )
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
