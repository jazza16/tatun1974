package com.example.ui.components

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import com.example.model.BmkgEarthquake
import com.example.model.EarthquakeUrgency
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.util.EarthquakeNotificationHelper
import java.util.Locale

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

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
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

    // Pulsing indicator for live BMKG feed
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmkg_earthquake_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header with Live Status & Notification Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bmkg_earthquake_header_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFDC2626).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Gempa Bumi",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Rilis Gempa BMKG",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Live pulse badge
                                Surface(
                                    color = Color(0xFFDC2626).copy(alpha = liveAlpha),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pusat Gempa Nasional BMKG Indonesia",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
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
                                contentDescription = "Perbarui Data Gempa",
                                tint = OrangePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification setting toggle strip
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notifikasi Gempa Real-Time",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "Dering & getar otomatis bila ada rilis BMKG",
                                    fontSize = 10.sp,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }

                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { checked ->
                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !EarthquakeNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleNotification(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            ),
                            modifier = Modifier.testTag("switch_earthquake_notifications")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button to trigger immediate test notification on user's device
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !EarthquakeNotificationHelper.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onTestNotification(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_notification"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tes Notifikasi Gempa di HP Ini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangePrimary
                    )
                }
            }
        }

        // Latest Earthquake Detail Card
        if (latestEarthquake != null) {
            LatestEarthquakeCard(
                earthquake = latestEarthquake,
                onCall117 = onCall117
            )
        }

        // Recent Earthquakes List (M 5.0+)
        if (recentEarthquakes.isNotEmpty()) {
            RecentEarthquakesListCard(earthquakes = recentEarthquakes)
        }
    }
}

@Composable
private fun LatestEarthquakeCard(
    earthquake: BmkgEarthquake,
    onCall117: () -> Unit
) {
    val urgencyBgColor = when (earthquake.urgencyLevel) {
        EarthquakeUrgency.CRITICAL -> Color(0xFFFEF2F2)
        EarthquakeUrgency.WARNING -> Color(0xFFFFF7ED)
        EarthquakeUrgency.ADVISORY -> Color(0xFFF0F9FF)
    }

    val urgencyBorderColor = when (earthquake.urgencyLevel) {
        EarthquakeUrgency.CRITICAL -> Color(0xFFFCA5A5)
        EarthquakeUrgency.WARNING -> Color(0xFFFDBA74)
        EarthquakeUrgency.ADVISORY -> Color(0xFFBAE6FD)
    }

    val urgencyPrimaryColor = when (earthquake.urgencyLevel) {
        EarthquakeUrgency.CRITICAL -> Color(0xFFDC2626)
        EarthquakeUrgency.WARNING -> Color(0xFFEA580C)
        EarthquakeUrgency.ADVISORY -> Color(0xFF0284C7)
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
            // Header: Status badge & Proximity to Mukomuko
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = urgencyPrimaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GEMPA TERKINI BMKG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = urgencyPrimaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${earthquake.date} • ${earthquake.timeWib}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Highlight: Magnitude, Depth, and Proximity
            Surface(
                color = urgencyBgColor,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, urgencyBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "M ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = urgencyPrimaryColor
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", earthquake.magnitude),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = urgencyPrimaryColor
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Kedalaman: ${earthquake.depth}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Distance badge to Mukomuko
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (earthquake.isNearMukomuko) Color(0xFFDC2626) else Color.Gray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (earthquake.isNearMukomuko) {
                                    "~${earthquake.distanceKmToMukomuko} km dari Mukomuko (Dekat)"
                                } else {
                                    "~${earthquake.distanceKmToMukomuko} km dari Mukomuko"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (earthquake.isNearMukomuko) FontWeight.Bold else FontWeight.Normal,
                                color = if (earthquake.isNearMukomuko) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Tsunami potential indicator
                    Box(
                        modifier = Modifier
                            .background(
                                if (earthquake.potentialTsunami.contains("tidak", ignoreCase = true)) {
                                    WeatherGoodGreen.copy(alpha = 0.15f)
                                } else {
                                    Color(0xFFDC2626).copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = earthquake.potentialTsunami,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (earthquake.potentialTsunami.contains("tidak", ignoreCase = true)) {
                                WeatherGoodGreen
                            } else {
                                Color(0xFFDC2626)
                            },
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Epicenter & Coordinates
            Text(
                text = "Pusat Gempa:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = earthquake.region,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Felt description if available
            if (earthquake.feltDescription != "-") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Dirasakan (Skala MMI):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = earthquake.feltDescription,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Zoomable BMKG Shakemap Feature (Peta Guncangan yang bisa di Zoom In & Zoom Out)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peta Guncangan (Shakemap BMKG):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = OrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Zoom In / Out Aktif",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            ZoomableShakemapViewer(
                earthquake = earthquake,
                height = 320.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mitigation action buttons & 117
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall117,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call Center 117",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentEarthquakesListCard(
    earthquakes: List<BmkgEarthquake>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayList = if (isExpanded) earthquakes else earthquakes.take(3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_recent_earthquakes_list"),
        shape = RoundedCornerShape(20.dp),
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
                    text = "Riwayat Gempa M 5.0+ BMKG",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${earthquakes.size} Kejadian",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    // Magnitude circle badge
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
                            text = eq.region,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${eq.date} • ${eq.timeWib}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (eq.isNearMukomuko) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Dekat Mukomuko",
                                    fontSize = 10.sp,
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
                    }
                }

                if (index < displayList.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    )
                }
            }

            if (earthquakes.size > 3) {
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
                        text = if (isExpanded) "Tampilkan Lebih Sedikit" else "Lihat Semua (${earthquakes.size} Gempa)",
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
