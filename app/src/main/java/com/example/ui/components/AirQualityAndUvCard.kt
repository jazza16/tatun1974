package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AirQuality
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSunGold

@Composable
fun AirQualityAndUvCard(
    airQuality: AirQuality,
    uvIndex: Double,
    modifier: Modifier = Modifier
) {
    val aqiStatusColor = when (airQuality.status) {
        "Baik" -> WeatherGoodGreen
        "Sedang" -> WeatherSunGold
        else -> Color(0xFFDC2626)
    }

    val uvStatus = when {
        uvIndex >= 11.0 -> "Ekstrem" to Color(0xFF7C3AED)
        uvIndex >= 8.0 -> "Sangat Tinggi" to Color(0xFFDC2626)
        uvIndex >= 6.0 -> "Tinggi" to OrangePrimary
        uvIndex >= 3.0 -> "Sedang" to WeatherSunGold
        else -> "Rendah" to WeatherGoodGreen
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("air_quality_uv_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Kualitas Udara",
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kualitas Udara & Indeks Sinar UV",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two main cards: AQI and UV Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // AQI Box
                Surface(
                    color = aqiStatusColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Indeks AQI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Surface(
                                color = aqiStatusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = airQuality.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = aqiStatusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${airQuality.aqi}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = aqiStatusColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "PM2.5: ${airQuality.pm25} µg/m³\nPM10: ${airQuality.pm10} µg/m³",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 13.sp
                        )
                    }
                }

                // UV Box
                Surface(
                    color = uvStatus.second.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Indeks Sinar UV",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Surface(
                                color = uvStatus.second.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = uvStatus.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = uvStatus.second,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${uvIndex.toInt()}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = uvStatus.second
                            )
                            Text(
                                text = " / 12+",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (uvIndex >= 8.0) "Gunakan tabir surya SPF 30+ dan topi" else "Aman berkegiatan di luar ruangan",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advisory Note
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Masks,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = airQuality.recommendation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
