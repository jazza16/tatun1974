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

                // Quick 4-stat metrics grid with translucent cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        icon = Icons.Default.WaterDrop,
                        label = "Kelembaban",
                        value = "${current.humidity}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        icon = Icons.Default.Air,
                        label = "Angin Pesisir",
                        value = "${current.windSpeed.toInt()} km/j",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        icon = Icons.Default.Surfing,
                        label = "Gelombang",
                        value = "${report.marine.waveHeight} m",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        icon = Icons.Default.Compress,
                        label = "Tekanan",
                        value = "${current.surfacePressure.toInt()} hPa",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Short advisory banner
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Arah Angin",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Angin bertiup dari ${current.windDirectionCompass} • ${weatherInfo.description}",
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
