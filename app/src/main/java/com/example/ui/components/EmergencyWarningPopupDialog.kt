package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BmkgEarthquake
import com.example.model.DetailedEarlyWarning
import com.example.model.MarineWeather
import com.example.model.WeatherAlert
import com.example.ui.theme.OrangePrimary

/**
 * Dialog Popup Notifikasi Peringatan Dini Kebencanaan SIPAGAR KITA.
 * Muncul secara proaktif ketika terdeteksi peringatan gempa bumi BMKG WRS-NTWC,
 * cuaca buruk ekstrem, atau gelombang tinggi Samudra Hindia di Kabupaten Mukomuko.
 */
@Composable
fun EmergencyWarningPopupDialog(
    weatherAlert: WeatherAlert? = null,
    marineWeather: MarineWeather? = null,
    latestEarthquake: BmkgEarthquake? = null,
    earlyWarnings: List<DetailedEarlyWarning> = emptyList(),
    isDark: Boolean = false,
    onDismiss: () -> Unit,
    onCall117: () -> Unit,
    onOpenEvacuationMap: () -> Unit
) {
    // Pulsing animation for the emergency badge beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlert")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val isHighWave = marineWeather != null && (
        marineWeather.waveHeight >= 2.0 ||
        marineWeather.seaCondition.contains("Tinggi", ignoreCase = true) ||
        marineWeather.fishermanSafety.contains("Bahaya", ignoreCase = true) ||
        marineWeather.fishermanSafety.contains("Waspada", ignoreCase = true)
    )

    val isCriticalEq = latestEarthquake != null && (
        latestEarthquake.magnitude >= 5.0 ||
        latestEarthquake.isNearMukomuko ||
        latestEarthquake.potentialTsunami.contains("tsunami", ignoreCase = true)
    )

    val hasActiveAlerts = weatherAlert != null || isHighWave || isCriticalEq || earlyWarnings.isNotEmpty()

    if (!hasActiveAlerts) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("emergency_warning_popup_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1C130D) else Color(0xFFFFFFFF)
            ),
            border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row: Beacon & Dismiss Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626).copy(alpha = 0.25f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Peringatan",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "PERINGATAN DINI AKTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SIPAGAR KITA • BPBD MUKOMUKO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C1E16) else Color(0xFFFEE2E2))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Dialog",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Perhatian Masyarakat Kabupaten Mukomuko!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF1F2937)
                )

                Text(
                    text = "Terdeteksi potensi bahaya cuaca buruk, gelombang tinggi, atau aktivitas seismik gempa bumi BMKG InaTEWS WRS-NTWC:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFFED7AA) else Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 1. Gempa Bumi BMKG Section (if present & significant)
                if (isCriticalEq && latestEarthquake != null) {
                    WarningCardItem(
                        icon = Icons.Default.Vibration,
                        iconTint = Color(0xFFDC2626),
                        category = "GEMPA BUMI WRS-NTWC BMKG",
                        headline = "M ${latestEarthquake.magnitude} • Kedalaman ${latestEarthquake.depth}",
                        detail = "${latestEarthquake.region} (${latestEarthquake.timeWib} WIB). ${latestEarthquake.potentialTsunami}",
                        badgeText = if (latestEarthquake.isNearMukomuko) "DEKAT MUKOMUKO" else "BMKG TERKINI",
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Cuaca Buruk / Ekstrem Section
                if (weatherAlert != null) {
                    WarningCardItem(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFEA580C),
                        category = "PERINGATAN CUACA BMKG",
                        headline = weatherAlert.title,
                        detail = weatherAlert.message,
                        badgeText = weatherAlert.severity.uppercase(),
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Gelombang Tinggi Samudra Hindia Section
                if (isHighWave && marineWeather != null) {
                    WarningCardItem(
                        icon = Icons.Default.Surfing,
                        iconTint = Color(0xFF0284C7),
                        category = "MARITIM & PESISIR PANTAI",
                        headline = "Tinggi Gelombang ${marineWeather.waveHeight} Meter (${marineWeather.seaCondition})",
                        detail = "${marineWeather.fishermanSafety}. Himbauan nelayan: ${marineWeather.fishermanAdvice}",
                        badgeText = "WASPADA GELOMBANG",
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 4. Detailed Early Warnings list
                if (earlyWarnings.isNotEmpty() && weatherAlert == null) {
                    val firstWarning = earlyWarnings.first()
                    WarningCardItem(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFDC2626),
                        category = firstWarning.category.uppercase(),
                        headline = firstWarning.title,
                        detail = firstWarning.description,
                        badgeText = firstWarning.severity,
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Himbauan Keamanan
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF2B1612) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Himbauan Kesiapsiagaan BPBD:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. Hindari aktivitas di bibir pantai dan muara saat gelombang pasang tinggi.\n2. Waspadai pohon tumbang, banjir luapan sungai, dan tanah longsor.\n3. Tetap tenang dan ikuti arahan resmi Posko PB BPBD Kabupaten Mukomuko.",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFFFEE2E2) else Color(0xFF7F1D1D),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Call 117 & SOP Evakuasi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onCall117()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("popup_btn_call_117")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Telepon 117",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onOpenEvacuationMap()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("popup_btn_sop_evacuation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SOP Evakuasi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF573E32) else Color(0xFFD1D5DB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("popup_btn_dismiss")
                ) {
                    Text(
                        text = "Mengerti & Tetap Siaga",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFED7AA) else Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    category: String,
    headline: String,
    detail: String,
    badgeText: String,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF261814) else Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconTint,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = iconTint
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = detail,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF4B5563),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
