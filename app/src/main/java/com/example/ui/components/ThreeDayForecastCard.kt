package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyItem
import com.example.model.District
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherRainBlue

@Composable
fun ThreeDayForecastCard(
    district: District,
    dailyList: List<DailyItem>,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val threeDays = dailyList.take(3)
    if (threeDays.isEmpty()) return

    val cardBg = MaterialTheme.colorScheme.surface
    val titleColor = if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("three_day_forecast_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Orange Accents
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
                            .size(34.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Prakiraan 3 Hari",
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prakiraan Cuaca 3 Hari",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kecamatan ${district.name}, Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = OrangePrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "3 HARI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Day Cards Grid / Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                threeDays.forEachIndexed { index, day ->
                    val dayLabel = when (index) {
                        0 -> "Hari Ini"
                        1 -> "Besok"
                        2 -> "Lusa"
                        else -> day.dayName
                    }

                    ThreeDayColumnItem(
                        dayLabel = dayLabel,
                        day = day,
                        isToday = index == 0,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Info Strip for Farmers & Coastal Communities
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) OrangePrimary.copy(alpha = 0.12f) else Color(0xFFFFF7ED),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) OrangePrimary.copy(alpha = 0.25f) else Color(0xFFFFEDD5)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ringkasan: Suhu rerata 24°–32°C. Perhatikan potensi angin sore pesisir Mukomuko.",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreeDayColumnItem(
    dayLabel: String,
    day: DailyItem,
    isToday: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val weatherInfo = day.weatherInfo
    val itemBg = MaterialTheme.colorScheme.surface

    val itemBorder = if (isToday) {
        androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag("three_day_item_${dayLabel.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = itemBg,
        border = itemBorder
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day Badge
            Text(
                text = dayLabel,
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isToday) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            Text(
                text = day.date.takeLast(5),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        weatherInfo.color.copy(alpha = if (isDark) 0.25f else 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                DynamicWeatherIcon(
                    weatherCode = day.weatherCode,
                    isDay = true,
                    size = 30.dp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = weatherInfo.conditionText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature High / Low
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${day.maxTemp.toInt()}°",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = " / ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${day.minTemp.toInt()}°",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rain Probability Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(WeatherRainBlue.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Hujan",
                    tint = WeatherRainBlue,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${day.rainProbabilityMax}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeatherRainBlue,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Wind Speed
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Angin",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${day.windSpeedMax.toInt()} km/j",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}
