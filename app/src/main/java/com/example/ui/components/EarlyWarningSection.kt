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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetailedEarlyWarning
import com.example.model.District
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.WeatherAlertSettings
import com.example.util.WeatherNotificationHelper

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
    weatherAlertSettings: WeatherAlertSettings,
    onUpdateWeatherAlertSettings: (WeatherAlertSettings) -> Unit,
    onTestWeatherAlertNotification: (android.content.Context) -> Unit,
    onTestSuddenSevereWeatherNotification: (android.content.Context) -> Unit = {},
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
        // Severe Weather Notification Settings Card (Orange-themed)
        WeatherAlertNotificationSettingsCard(
            currentDistrict = currentDistrict,
            settings = weatherAlertSettings,
            onUpdateSettings = onUpdateWeatherAlertSettings,
            onTestNotification = onTestWeatherAlertNotification,
            onTestSuddenNotification = onTestSuddenSevereWeatherNotification
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
                containerColor = Color(0xFFFEF2F2)
            ),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherAlertNotificationSettingsCard(
    currentDistrict: District,
    settings: WeatherAlertSettings,
    onUpdateSettings: (WeatherAlertSettings) -> Unit,
    onTestNotification: (android.content.Context) -> Unit,
    onTestSuddenNotification: (android.content.Context) -> Unit = {}
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onUpdateSettings(settings.copy(isEnabled = true))
            onTestNotification(context)
        } else {
            onUpdateSettings(settings.copy(isEnabled = false))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_alert_settings_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDBA74))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Orange Theme
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
                            .size(42.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifikasi Cuaca",
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notifikasi Cuaca Buruk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = OrangePrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PUSH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OrangeOnPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "Peringatan dini cuaca ekstrem di Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !WeatherNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateSettings(settings.copy(isEnabled = checked))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OrangePrimary
                    ),
                    modifier = Modifier.testTag("switch_weather_alert_master")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-category filters description
            Text(
                text = "Pilih Jenis Cuaca Buruk yang Diberitahukan:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips in FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Badai Petir
                FilterChip(
                    selected = settings.alertThunderstorm,
                    onClick = {
                        onUpdateSettings(settings.copy(alertThunderstorm = !settings.alertThunderstorm))
                    },
                    label = { Text("⚡ Badai Petir", fontSize = 11.sp) },
                    leadingIcon = if (settings.alertThunderstorm) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = OrangePrimary,
                        selectedLeadingIconColor = OrangePrimary
                    )
                )

                // Angin Kencang
                FilterChip(
                    selected = settings.alertStrongWind,
                    onClick = {
                        onUpdateSettings(settings.copy(alertStrongWind = !settings.alertStrongWind))
                    },
                    label = { Text("💨 Angin Kencang", fontSize = 11.sp) },
                    leadingIcon = if (settings.alertStrongWind) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = OrangePrimary,
                        selectedLeadingIconColor = OrangePrimary
                    )
                )

                // Banjir & Longsor
                FilterChip(
                    selected = settings.alertFloodHeavyRain,
                    onClick = {
                        onUpdateSettings(settings.copy(alertFloodHeavyRain = !settings.alertFloodHeavyRain))
                    },
                    label = { Text("🌧️ Banjir & Longsor", fontSize = 11.sp) },
                    leadingIcon = if (settings.alertFloodHeavyRain) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = OrangePrimary,
                        selectedLeadingIconColor = OrangePrimary
                    )
                )

                // Gelombang Tinggi
                FilterChip(
                    selected = settings.alertHighWaves,
                    onClick = {
                        onUpdateSettings(settings.copy(alertHighWaves = !settings.alertHighWaves))
                    },
                    label = { Text("🌊 Gelombang Tinggi", fontSize = 11.sp) },
                    leadingIcon = if (settings.alertHighWaves) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = OrangePrimary,
                        selectedLeadingIconColor = OrangePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location Relevance Selection Strip
            Surface(
                color = Color(0xFFFFF7ED),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lokasi Peringatan Relevan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = if (settings.onlySelectedDistrict) {
                                    "Khusus wilayah ${currentDistrict.name}"
                                } else {
                                    "Seluruh 15 Kecamatan di Kab. Mukomuko"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }

                    Switch(
                        checked = settings.onlySelectedDistrict,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(onlySelectedDistrict = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OrangePrimary
                        ),
                        modifier = Modifier.testTag("switch_district_relevance")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Notification Trigger Button (Orange theme with preview info)
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !WeatherNotificationHelper.hasNotificationPermission(context)
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onTestNotification(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_test_weather_alert_notification"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = OrangeOnPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tes Notifikasi Peringatan Cuaca (Tema Oranye)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sudden Severe Weather Push Notification Button
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !WeatherNotificationHelper.hasNotificationPermission(context)
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onTestSuddenNotification(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_test_sudden_weather_push_notification"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Icon(
                    imageVector = Icons.Default.Thunderstorm,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simulasi Push Notifikasi Cuaca Buruk Mendadak (${currentDistrict.name})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
