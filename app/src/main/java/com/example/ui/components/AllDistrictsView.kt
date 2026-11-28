package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatWeatherTimestamp(timestampMs: Long, shortFormat: Boolean = false): String {
    if (timestampMs <= 0L) return "-"
    return try {
        val pattern = if (shortFormat) "dd/MM HH:mm 'WIB'" else "dd MMM yyyy, HH:mm 'WIB'"
        val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        "-"
    }
}

@Composable
fun AllDistrictsView(
    districtsSummary: List<WeatherFullReport>,
    selectedDistrict: District,
    favoriteDistricts: List<String>,
    onSelectDistrict: (District) -> Unit,
    onOpenKrbRtrwDistrict: ((District) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val latestTimestamp = remember(districtsSummary) {
        districtsSummary.maxOfOrNull { it.lastUpdated } ?: 0L
    }
    val isAnyFromOfflineCache = remember(districtsSummary) {
        districtsSummary.isNotEmpty() && districtsSummary.any { it.isFromCache }
    }

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
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
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
                                text = "Cuaca aktual, KRB 2026–2030, Shelter RPB 2025–2029 & Hierarki Perda RTRW No. 12/2025",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (latestTimestamp > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAnyFromOfflineCache) {
                                OrangePrimary.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isAnyFromOfflineCache) Icons.Default.CloudOff else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (isAnyFromOfflineCache) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAnyFromOfflineCache) {
                                            "Cache Offline Room • Data terakhir: ${formatWeatherTimestamp(latestTimestamp)}"
                                        } else {
                                            "Tersimpan di Room • Diperbarui: ${formatWeatherTimestamp(latestTimestamp)}"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAnyFromOfflineCache) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
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
                        val timestampText = report?.lastUpdated?.let { formatWeatherTimestamp(it, shortFormat = true) }
                        val isFromCache = report?.isFromCache == true

                        Box(modifier = Modifier.weight(1f)) {
                            DistrictCardItem(
                                dist = dist,
                                isCurrentSelected = isCurrentSelected,
                                isFav = isFav,
                                weatherInfo = weatherInfo,
                                tempDisplay = tempDisplay,
                                timestampText = timestampText,
                                isFromCache = isFromCache,
                                onSelectDistrict = onSelectDistrict,
                                onOpenKrbRtrwDistrict = onOpenKrbRtrwDistrict
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DistrictCardItem(
    dist: District,
    isCurrentSelected: Boolean,
    isFav: Boolean,
    weatherInfo: com.example.model.WeatherInfo,
    tempDisplay: String,
    timestampText: String?,
    isFromCache: Boolean,
    onSelectDistrict: (District) -> Unit,
    onOpenKrbRtrwDistrict: ((District) -> Unit)? = null
) {
    val krbProfile = remember(dist.id, dist.name) {
        com.example.model.MukomukoKrb2026Catalog.getDistrictProfile(dist)
    }
    val rtrwProfile = remember(dist.id, dist.name) {
        com.example.model.MukomukoRtrw2025Catalog.getDistrictSpatialProfile(dist)
    }

    Card(
        onClick = { onSelectDistrict(dist) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("district_item_${dist.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrentSelected) 1.5.dp else 1.dp,
            color = if (isCurrentSelected) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                                softWrap = false
                            )
                        }

                        if (isFromCache) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Cache",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
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

                    if (!timestampText.isNullOrBlank() && timestampText != "-") {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update: $timestampText",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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

            // Sinkronisasi KRB 2026-2030, RPB 2025-2029 & Perda RTRW No. 12/2025 per Kecamatan (Bisa Buka-Tutup seperti Catatan Rilis)
            val rpbShelters = androidx.compose.runtime.remember(dist.id) {
                com.example.model.MukomukoRpb2025Catalog.getSheltersForDistrict(dist)
            }
            var isKrbRtrwExpanded by androidx.compose.runtime.remember(dist.id) {
                androidx.compose.runtime.mutableStateOf(false)
            }
            Surface(
                onClick = { isKrbRtrwExpanded = !isKrbRtrwExpanded },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isKrbRtrwExpanded) 1.2.dp else 1.dp,
                    color = if (isKrbRtrwExpanded) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("district_toggle_krb_rtrw_${dist.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kebijakan KRB, RPB & RTRW (${rtrwProfile.settlementHierarchyCode} • Risiko ${krbProfile.multiHazardRiskClass})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = if (isKrbRtrwExpanded) "Tutup ▲" else "Buka ▼",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = isKrbRtrwExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            Text(
                                text = "• KRB 2026–2030: Risiko Multibahaya ${krbProfile.multiHazardRiskClass} (${String.format(Locale.GERMANY, "%,.2f", krbProfile.areaHa)} Ha) • IKD ${String.format(Locale.US, "%.3f", krbProfile.multiHazardCapacityIndex)} (${krbProfile.multiHazardCapacityClass})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = if (rpbShelters.isNotEmpty()) {
                                    "• RPB 2025–2029 (Shelter Resmi): ${rpbShelters.joinToString("; ") { it.siteName }} [Kode 1.06.06.2.01.0007]"
                                } else {
                                    "• RPB 2025–2029: Terhubung ke 11 titik pengungsi resmi & jaringan 681,37 Km jalan evakuasi"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "• Perda RTRW No. 12/2025: ${rtrwProfile.settlementHierarchyCode} – ${rtrwProfile.urbanOrRuralCenterName} (${rtrwProfile.article10Reference})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                            if (onOpenKrbRtrwDistrict != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OrangePrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onOpenKrbRtrwDistrict(dist) }
                                            .testTag("district_open_krb_${dist.id}")
                                    ) {
                                        Text(
                                            text = "Buka Tab KRB, RPB & RTRW ↗",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

