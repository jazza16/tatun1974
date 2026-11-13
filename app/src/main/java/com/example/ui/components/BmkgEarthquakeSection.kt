package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BmkgEarthquake
import com.example.model.EarthquakeUrgency
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.util.EarthquakeNotificationHelper
import com.example.util.GoogleMapsHelper
import java.util.Locale

/**
 * Redesigned BMKG Real-Time Earthquake UI modeled after WRS-NTWC
 * (Warning Receiver System - National Tsunami Warning Center: inatews.bmkg.go.id/wrs).
 * Delivers intuitive, attractive earthquake intelligence, interactive GIS map viewer,
 * tsunami warning level indicators, shakemap inspection, and event logs.
 */
@Composable
fun BmkgEarthquakeSection(
    latestEarthquake: BmkgEarthquake?,
    recentEarthquakes: List<BmkgEarthquake>,
    isLoading: Boolean,
    isNotificationEnabled: Boolean,
    onToggleNotification: (Boolean) -> Unit,
    onTestNotification: (Context) -> Unit,
    onRefresh: () -> Unit,
    onCall117: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) }

    // Android 13+ Notification permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleNotification(true)
            onTestNotification(context)
        } else {
            onToggleNotification(false)
        }
    }

    // Radar pulse animation for operational status
    val infiniteTransition = rememberInfiniteTransition(label = "wrs_status_pulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmkg_earthquake_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. WRS-NTWC Official Console Header Card
        WrsConsoleHeaderCard(
            radarPulse = radarPulse,
            isLoading = isLoading,
            isNotificationEnabled = isNotificationEnabled,
            onRefresh = onRefresh,
            onToggleNotification = { checked ->
                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !EarthquakeNotificationHelper.hasNotificationPermission(context)
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onToggleNotification(checked)
                }
            },
            onTestSirene = {
                WrsAlertSoundHelper.playWrsAlarmTone(context, isTsunamiAlert = false)
                onTestNotification(context)
            },
            onOpenWrsPortal = {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://inatews.bmkg.go.id/wrs/index.html")
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )

        // 2. Sub-Tab Switcher (Peta WRS & Gempa, Shakemap, 15 Kejadian, Sistem & SOP)
        WrsNavigationTabRow(
            selectedTab = selectedSubTab,
            onSelectTab = { selectedSubTab = it },
            recentCount = recentEarthquakes.size
        )

        // 3. Dynamic Sub-Tab Content
        when (selectedSubTab) {
            0 -> {
                // TAB 0: Peta WRS & Gempa Terkini
                if (latestEarthquake != null) {
                    WrsMainEarthquakeView(
                        earthquake = latestEarthquake,
                        onCall117 = onCall117
                    )
                } else {
                    EmptyEarthquakePlaceholder(isLoading = isLoading, onRefresh = onRefresh)
                }
            }
            1 -> {
                // TAB 1: Peta Guncangan (Shakemap BMKG)
                if (latestEarthquake != null) {
                    WrsShakemapSection(earthquake = latestEarthquake)
                } else {
                    EmptyEarthquakePlaceholder(isLoading = isLoading, onRefresh = onRefresh)
                }
            }
            2 -> {
                // TAB 2: Daftar 15 Kejadian Gempa Terkini (Event List)
                WrsRecentEventListCard(earthquakes = recentEarthquakes)
            }
            3 -> {
                // TAB 3: Sistem Peringatan, Sirene WRS, & SOP Tsunami
                WrsSystemAndSopCard(
                    onTestSirene = { WrsAlertSoundHelper.playWrsAlarmTone(context, isTsunamiAlert = true) },
                    onCall117 = onCall117
                )
            }
        }
    }
}

@Composable
private fun WrsConsoleHeaderCard(
    radarPulse: Float,
    isLoading: Boolean,
    isNotificationEnabled: Boolean,
    onRefresh: () -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onTestSirene: () -> Unit,
    onOpenWrsPortal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bmkg_earthquake_header_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top branding row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFDC2626).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "WRS-NTWC",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WRS-NTWC",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Operational Mode Live LED
                            Surface(
                                color = WeatherGoodGreen.copy(alpha = radarPulse),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "MODE OPERASIONAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "InaTEWS BMKG • Warning Receiver System",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("btn_refresh_earthquake")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OrangePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Perbarui Data WRS",
                            tint = OrangePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification toggle & Direct Link strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification setting
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Alarm Dering",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = onToggleNotification,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("switch_earthquake_notifications")
                        )
                    }
                }

                // Official WRS Portal Link Button
                Button(
                    onClick = onOpenWrsPortal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_open_wrs_web")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "inatews.bmkg.go.id/wrs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun WrsNavigationTabRow(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    recentCount: Int
) {
    val subTabs = listOf(
        "🗺️ Peta WRS & Gempa",
        "📉 Peta Shakemap",
        "📋 15 Kejadian ($recentCount)",
        "🚨 Sirene & SOP"
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = OrangePrimary,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = OrangePrimary
                )
            }
        },
        divider = {},
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        subTabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Tab(
                selected = isSelected,
                onClick = { onSelectTab(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            )
        }
    }
}

@Composable
private fun WrsMainEarthquakeView(
    earthquake: BmkgEarthquake,
    onCall117: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Tsunami Warning Level Card
        WrsTsunamiThreatCard(earthquake = earthquake)

        // Interactive Geospatial GIS Map Viewer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_wrs_map_viewer"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peta Geospasial WRS-NTWC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Pinch Zoom & Pan Aktif",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The GIS Canvas Map
                WrsNtwcMapView(
                    earthquake = earthquake,
                    height = 320.dp
                )
            }
        }

        // Comprehensive Earthquake Parameter Matrix Card
        WrsParameterMatrixCard(
            earthquake = earthquake,
            onCall117 = onCall117
        )
    }
}

@Composable
private fun WrsTsunamiThreatCard(earthquake: BmkgEarthquake) {
    val isTsunamiPotential = earthquake.potentialTsunami.contains("berpotensi tsunami", ignoreCase = true) &&
            !earthquake.potentialTsunami.contains("tidak", ignoreCase = true)

    val warningLevel = when {
        isTsunamiPotential && earthquake.magnitude >= 7.0 -> "AWAS (WARNING)"
        isTsunamiPotential && earthquake.magnitude >= 6.5 -> "SIAGA (ADVISORY)"
        isTsunamiPotential -> "WASPADA (WATCH)"
        else -> "TIDAK BERPOTENSI TSUNAMI"
    }

    val warningBg = when {
        isTsunamiPotential && earthquake.magnitude >= 7.0 -> Color(0xFFFEF2F2)
        isTsunamiPotential -> Color(0xFFFFF7ED)
        else -> Color(0xFFF0FDF4)
    }

    val warningBorder = when {
        isTsunamiPotential && earthquake.magnitude >= 7.0 -> Color(0xFFFCA5A5)
        isTsunamiPotential -> Color(0xFFFDBA74)
        else -> Color(0xFFBBF7D0)
    }

    val warningTextCol = when {
        isTsunamiPotential && earthquake.magnitude >= 7.0 -> Color(0xFFDC2626)
        isTsunamiPotential -> Color(0xFFEA580C)
        else -> Color(0xFF16A34A)
    }

    Surface(
        color = warningBg,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, warningBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTsunamiPotential) Icons.Default.Waves else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = warningTextCol,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATUS ANCAMAN TSUNAMI:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningTextCol.copy(alpha = 0.85f)
                    )
                }

                Surface(
                    color = warningTextCol.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "InaTEWS BMKG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningTextCol,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = warningLevel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = warningTextCol
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isTsunamiPotential) {
                    "Warga pesisir pantai Mukomuko dan sekitarnya diimbau segera melakukan evakuasi mandiri ke tempat aman/dataran tinggi serta mengikuti arahan BPBD."
                } else {
                    "Hasil pemodelan numerik BMKG InaTEWS menunjukkan gempa bumi ini tidak memicu kenaikan gelombang tsunami di pesisir Mukomuko."
                },
                fontSize = 11.sp,
                color = if (isTsunamiPotential) Color(0xFF991B1B) else Color(0xFF166534),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun WrsParameterMatrixCard(
    earthquake: BmkgEarthquake,
    onCall117: () -> Unit
) {
    val context = LocalContext.current
    val depthVal = earthquake.depth.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 15
    val depthCategory = when {
        depthVal < 60 -> "Dangkal (< 60 km)"
        depthVal <= 300 -> "Menengah (60 - 300 km)"
        else -> "Dalam (> 300 km)"
    }
    val depthColor = when {
        depthVal < 60 -> Color(0xFFDC2626)
        depthVal <= 300 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_latest_earthquake"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: OT & Region Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PARAMETER OT BMKG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${earthquake.date} • ${earthquake.timeWib}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Metrics Grid (Magnitude, Depth, Distance to Mukomuko)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Magnitude Metric Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MAGNITUDO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", earthquake.magnitude),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Skala Richter",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }

                // Depth Metric Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = depthColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, depthColor.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KEDALAMAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = depthColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = earthquake.depth,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = depthColor
                        )
                        Text(
                            text = depthCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = depthColor.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }

                // Proximity to Mukomuko Metric Box
                Surface(
                    modifier = Modifier.weight(1.1f),
                    color = if (earthquake.isNearMukomuko) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (earthquake.isNearMukomuko) Color(0xFFFCD34D) else Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "JARAK KE MUKOMUKO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (earthquake.isNearMukomuko) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "~${earthquake.distanceKmToMukomuko} km",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = if (earthquake.isNearMukomuko) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (earthquake.isNearMukomuko) "Radius Dekat" else "Radius Aman",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (earthquake.isNearMukomuko) Color(0xFFB45309) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Epicenter Geographic Region
            Text(
                text = "Pusat Episentrum Gempa:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatEarthquakeRegion(earthquake.region),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Geodetic Coordinates
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Koordinat: ${String.format(Locale.US, "%.2f", earthquake.latitude)} LS, ${String.format(Locale.US, "%.2f", earthquake.longitude)} BT",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Felt Scale (MMI Intensity) if provided
            if (earthquake.feltDescription != "-") {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guncangan Dirasakan (Skala MMI):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = earthquake.feltDescription,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keterangan: Skala II (dirasakan beberapa orang) • Skala III (getaran nyata dalam rumah) • Skala IV (gerabah pecah, jendela berderit).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disclaimer Box WRS-NTWC
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Diseminasi Otomatis WRS BMKG: Pada beberapa menit pertama, parameter dapat diperbarui setelah dianalisis ulang seismolog.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Google Maps Episentrum Button
            Button(
                onClick = {
                    GoogleMapsHelper.openEarthquakeEpicenter(
                        context = context,
                        latitude = earthquake.latitude,
                        longitude = earthquake.longitude,
                        magnitude = String.format(Locale.US, "%.1f", earthquake.magnitude),
                        wilayah = formatEarthquakeRegion(earthquake.region)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_open_earthquake_maps")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka Episentrum di Google Maps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Emergency call button
            Button(
                onClick = onCall117,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Panggilan Darurat BPBD Mukomuko (117)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WrsShakemapSection(earthquake: BmkgEarthquake) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_shakemap_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peta Guncangan (Shakemap BMKG)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = OrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Zoom In / Out Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Peta kontur pemodelan intensitas percepatan tanah (PGA) BMKG di wilayah terdampak.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            ZoomableShakemapViewer(
                earthquake = earthquake,
                height = 320.dp
            )
        }
    }
}

@Composable
private fun WrsRecentEventListCard(earthquakes: List<BmkgEarthquake>) {
    val context = LocalContext.current
    var filterType by remember { mutableStateOf("ALL") } // ALL, NEAR, FELT
    var isExpanded by remember { mutableStateOf(false) }

    val filteredList = when (filterType) {
        "NEAR" -> earthquakes.filter { it.isNearMukomuko }
        "FELT" -> earthquakes.filter { it.feltDescription != "-" }
        else -> earthquakes
    }

    val displayList = if (isExpanded) filteredList else filteredList.take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_recent_earthquakes_list"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar 15 Kejadian (WRS Event List)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${filteredList.size} Kejadian",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("Semua (M ≥ 5.0)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = OrangePrimary
                    )
                )
                FilterChip(
                    selected = filterType == "NEAR",
                    onClick = { filterType = "NEAR" },
                    label = { Text("Dekat Mukomuko", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFFDC2626)
                    )
                )
                FilterChip(
                    selected = filterType == "FELT",
                    onClick = { filterType = "FELT" },
                    label = { Text("Dirasakan Warga", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF2563EB)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            displayList.forEachIndexed { index, eq ->
                val magColor = when {
                    eq.magnitude >= 6.0 -> Color(0xFFDC2626)
                    eq.magnitude >= 5.0 -> Color(0xFFEA580C)
                    else -> Color(0xFFD97706)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(magColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", eq.magnitude),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = magColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatEarthquakeRegion(eq.region),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${eq.date} • ${eq.timeWib}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            if (eq.isNearMukomuko) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• Dekat Mukomuko",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = eq.depth,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        IconButton(
                            onClick = {
                                GoogleMapsHelper.openEarthquakeEpicenter(
                                    context = context,
                                    latitude = eq.latitude,
                                    longitude = eq.longitude,
                                    magnitude = String.format(Locale.US, "%.1f", eq.magnitude),
                                    wilayah = formatEarthquakeRegion(eq.region)
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Buka di Google Maps",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (index < displayList.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            if (filteredList.size > 5) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Tampilkan Lebih Sedikit" else "Lihat Semua (${filteredList.size} Gempa)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WrsSystemAndSopCard(
    onTestSirene: () -> Unit,
    onCall117: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_wrs_sop_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sirene & SOP Evakuasi InaTEWS BMKG",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Sirene WRS Button
            Button(
                onClick = onTestSirene,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_test_wrs_siren")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tes Bunyi Sirene Peringatan WRS di HP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Golden Time Guideline Card
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Golden Time Evakuasi Tsunami: 20 Menit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Jika merasakan gempa bumi kuat lebih dari 20 detik atau sulit berdiri, jangan tunggu sirene atau rilis resmi. Segera jauhi pantai Mukomuko dan menuju dataran tinggi > 15 mdpl.",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCall117,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hubungi Call Center 117 BPBD Mukomuko",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyEarthquakePlaceholder(isLoading: Boolean, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = OrangePrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mengunduh data telemetry gempabumi WRS-NTWC...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            } else {
                Text(
                    text = "Data WRS-NTWC belum termuat.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Muat Ulang WRS", fontSize = 11.sp)
                }
            }
        }
    }
}
