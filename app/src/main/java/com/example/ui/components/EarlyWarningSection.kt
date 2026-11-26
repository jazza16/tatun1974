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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetailedEarlyWarning
import com.example.model.District

/**
 * Menyisipkan spasi pada teks wilayah pusat gempa BMKG yang menempel tanpa spasi,
 * misalnya "TimurLaut" -> "Timur Laut" dan "HALMAHERASELATAN" -> "HALMAHERA SELATAN".
 */
fun formatEarthquakeRegion(rawRegion: String): String {
    if (rawRegion.isBlank()) return rawRegion
    return rawRegion
        // 1. Pisahkan huruf kecil diikuti huruf kapital (contoh: TimurLaut -> Timur Laut, BaratDaya -> Barat Daya)
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        // 2. Pisahkan kata arah mata angin/geografis kapital yang menempel di akhir kata kapital (contoh: HALMAHERASELATAN -> HALMAHERA SELATAN)
        .replace(
            Regex("([A-Z]{3,})(SELATAN|UTARA|BARAT|TIMUR|TENGAH|TENGGARA|DAYA|LAUT)\\b"),
            "$1 $2"
        )
        // 3. Pisahkan awalan geografis kapital yang menempel di awal kata kapital (contoh: PULAUMOROTAI -> PULAU MOROTAI)
        .replace(
            Regex("\\b(PULAU|KEPULAUAN|KABUPATEN|TELUK|SELAT|PEGUNUNGAN)([A-Z]{3,})"),
            "$1 $2"
        )
        .replace(Regex("\\s+"), " ")
        .trim()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarlyWarningSection(
    warnings: List<DetailedEarlyWarning>,
    currentDistrict: District,
    triageNotificationSettings: com.example.util.TriageNotificationSettings = com.example.util.TriageNotificationSettings(),
    activeTriageSignals: List<com.example.util.DisasterTriageSignal> = emptyList(),
    onUpdateTriageNotificationSettings: (com.example.util.TriageNotificationSettings) -> Unit = {},
    onTestTriageNotification: (android.content.Context, com.example.util.DisasterTriageLevel) -> Unit = { _, _ -> },
    onNavigateToTab: (Int) -> Unit = {},
    latestEarthquake: com.example.model.BmkgEarthquake? = null,
    recentEarthquakes: List<com.example.model.BmkgEarthquake> = emptyList(),
    isLoadingEarthquakes: Boolean = false,
    isNotificationEnabled: Boolean = true,
    onToggleNotification: (Boolean) -> Unit = {},
    onTestNotification: (android.content.Context) -> Unit = {},
    onRefreshEarthquake: () -> Unit = {},
    onCall117: () -> Unit,
    onOpenCallCenterInfo: () -> Unit,
    onOpenEvacuationGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("early_warning_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unified Color-Triage Disaster Readiness Notification Center (Hijau / Kuning / Oranye / Merah)
        DisasterTriageNotificationCard(
            currentDistrict = currentDistrict,
            settings = triageNotificationSettings,
            activeSignals = activeTriageSignals,
            onUpdateSettings = onUpdateTriageNotificationSettings,
            onTestTriageNotification = onTestTriageNotification,
            onNavigateToTab = onNavigateToTab
        )

        // BMKG Real-time Earthquake Section
        BmkgEarthquakeSection(
            latestEarthquake = latestEarthquake,
            recentEarthquakes = recentEarthquakes,
            isLoading = isLoadingEarthquakes,
            isNotificationEnabled = isNotificationEnabled,
            onToggleNotification = onToggleNotification,
            onTestNotification = onTestNotification,
            onRefresh = onRefreshEarthquake,
            onCall117 = onCall117
        )

        // Prominent Call Center 117 Banner
        CallCenter117HeroBanner(
            onCallClick = onCall117,
            onDetailClick = onOpenCallCenterInfo
        )

        // Section Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFDC2626), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Peringatan Dini",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Peringatan Dini Cuaca & Bencana",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Prakiraan BMKG & Pusdalops BPBD Kabupaten Mukomuko",
                        fontSize = 11.sp,
                        color = Color(0xFF7F1D1D)
                    )
                }
            }
        }

        // List of Warnings
        warnings.forEach { warning ->
            val severityColor = when (warning.severity) {
                "AWAS" -> Color(0xFFDC2626)
                "SIAGA" -> Color(0xFFEA580C)
                else -> Color(0xFFD97706)
            }

            val icon = when {
                warning.category.contains("Gelombang", ignoreCase = true) -> Icons.Default.Surfing
                warning.category.contains("Petir", ignoreCase = true) || warning.category.contains("Hujan", ignoreCase = true) -> Icons.Default.Thunderstorm
                else -> Icons.Default.Warning
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title & Severity Badge
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = severityColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = warning.severity,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = severityColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = warning.validUntil,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(severityColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = warning.category,
                                tint = severityColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = warning.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kategori: ${warning.category}",
                                fontSize = 12.sp,
                                color = severityColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = warning.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Affected Districts FlowRow
                    Text(
                        text = "Kecamatan Terdampak:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        warning.affectedDistricts.forEach { distName ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = distName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safety instructions list
                    Surface(
                        color = severityColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = severityColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Langkah Mitigasi & Keselamatan:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            warning.safetyInstructions.forEach { instruction ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = severityColor
                                    )
                                    Text(
                                        text = instruction,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Emergency Contact clickable banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenCallCenterInfo),
                        color = Color(0xFFDC2626).copy(alpha = 0.08f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Kontak Darurat",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call Center 117 (Bebas Pulsa) • ${warning.bpbdContact}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Hubungi >",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}
