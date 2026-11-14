package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.model.WeatherCodeHelper
import com.example.model.WeatherFullReport
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherSunGold

@Composable
fun AllDistrictsView(
    districtsSummary: List<WeatherFullReport>,
    selectedDistrict: District,
    favoriteDistricts: List<String>,
    onSelectDistrict: (District) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("all_districts_view")
    ) {
        val columnCount = when {
            maxWidth < 600.dp -> 1
            maxWidth < 840.dp -> 2
            else -> 3
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OrangePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "15 Kecamatan di Kabupaten Mukomuko",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prakiraan cuaca spesifik pesisir & hulu Taman Nasional Kerinci Seblat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Adaptive layout: render in rows of `columnCount`
            val chunkedDistricts = MukomukoDistricts.list.chunked(columnCount)

            chunkedDistricts.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { dist ->
                        val report = districtsSummary.find { it.district.id == dist.id }
                        val isCurrentSelected = dist.id == selectedDistrict.id
                        val isFav = favoriteDistricts.contains(dist.id)
                        val weatherInfo = report?.current?.weatherInfo ?: WeatherCodeHelper.getWeatherInfo(1)
                        val tempDisplay = report?.current?.temperature?.toInt()?.toString() ?: "30"

                        Box(modifier = Modifier.weight(1f)) {
                            DistrictCardItem(
                                dist = dist,
                                isCurrentSelected = isCurrentSelected,
                                isFav = isFav,
                                weatherInfo = weatherInfo,
                                tempDisplay = tempDisplay,
                                onSelectDistrict = onSelectDistrict
                            )
                        }
                    }

                    // Fill remaining empty slots in last row if needed
                    val emptySlots = columnCount - rowItems.size
                    for (i in 0 until emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictCardItem(
    dist: District,
    isCurrentSelected: Boolean,
    isFav: Boolean,
    weatherInfo: com.example.model.WeatherInfo,
    tempDisplay: String,
    onSelectDistrict: (District) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectDistrict(dist) }
            .testTag("district_item_${dist.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrentSelected) 2.dp else 1.dp,
            color = if (isCurrentSelected) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dist.name,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrentSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isCurrentSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFav) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorit",
                            tint = WeatherSunGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = OrangePrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = dist.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dist.landmark,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Condition icon & text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = weatherInfo.icon,
                    contentDescription = weatherInfo.conditionText,
                    tint = weatherInfo.color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$tempDisplay°C",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCurrentSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isCurrentSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

