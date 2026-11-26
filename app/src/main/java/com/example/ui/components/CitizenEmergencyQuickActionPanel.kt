package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EmergencyQuickTileSpec(
    val testTag: String,
    val title: String,
    val subtitle: String,
    val badgeText: String?,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val iconContainerColor: Color,
    val borderColor: Color,
    val onClick: () -> Unit
)

/**
 * Panel 4 Aksi Darurat 1-Ketuk untuk Layar Beranda Warga ("Hari Ini").
 * Dirancang agar lansia, warga awam, dan pengguna dalam kondisi panik dapat
 * langsung menghubungi 117, melapor bencana, memeriksa peringatan dini/gempa,
 * atau membuka panduan evakuasi offline tanpa harus menelusuri banyak tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CitizenEmergencyQuickActionPanel(
    activeAlertCount: Int = 0,
    hasNearEarthquake: Boolean = false,
    onCall117: () -> Unit,
    onOpenQuickReport: () -> Unit,
    onOpenEarlyWarning: () -> Unit,
    onOpenEvacuationGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val fontScale = LocalDensity.current.fontScale

    val warningBadge = when {
        hasNearEarthquake && activeAlertCount > 0 -> "GEMPA + $activeAlertCount INFO"
        hasNearEarthquake -> "GEMPA TERDETEKSI"
        activeAlertCount > 0 -> "$activeAlertCount PERINGATAN"
        else -> null
    }

    val tiles = listOf(
        EmergencyQuickTileSpec(
            testTag = "quick_action_emergency_117",
            title = "Darurat Pusdalops 117",
            subtitle = "Telepon bebas pulsa 24 jam",
            badgeText = "DARURAT",
            icon = Icons.Default.PhoneInTalk,
            containerColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFDC2626),
            contentColor = Color.White,
            iconContainerColor = Color.White.copy(alpha = 0.22f),
            borderColor = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
            onClick = onCall117
        ),
        EmergencyQuickTileSpec(
            testTag = "quick_action_report_disaster",
            title = "Lapor Bencana Cepat",
            subtitle = "Kirim lokasi GPS & foto kejadian",
            badgeText = "1 KETUK",
            icon = Icons.Default.Campaign,
            containerColor = if (isDark) Color(0xFF7C2D12) else Color(0xFFEA580C),
            contentColor = Color.White,
            iconContainerColor = Color.White.copy(alpha = 0.22f),
            borderColor = if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C),
            onClick = onOpenQuickReport
        ),
        EmergencyQuickTileSpec(
            testTag = "quick_action_early_warning",
            title = "Peringatan Dini & Gempa",
            subtitle = "Info BMKG & status cuaca ekstrem",
            badgeText = warningBadge,
            icon = if (hasNearEarthquake) Icons.Default.Vibration else Icons.Default.Warning,
            containerColor = if (isDark) {
                if (activeAlertCount > 0 || hasNearEarthquake) Color(0xFF78350F) else Color(0xFF1E3A8A)
            } else {
                if (activeAlertCount > 0 || hasNearEarthquake) Color(0xFFD97706) else Color(0xFF1D4ED8)
            },
            contentColor = Color.White,
            iconContainerColor = Color.White.copy(alpha = 0.22f),
            borderColor = if (activeAlertCount > 0 || hasNearEarthquake) Color(0xFFF59E0B) else Color(0xFF2563EB),
            onClick = onOpenEarlyWarning
        ),
        EmergencyQuickTileSpec(
            testTag = "quick_action_evacuation_guide",
            title = "Panduan Evakuasi Offline",
            subtitle = "Langkah selamat gempa & tsunami",
            badgeText = "OFFLINE",
            icon = Icons.Default.Shield,
            containerColor = if (isDark) Color(0xFF134E4A) else Color(0xFF0F766E),
            contentColor = Color.White,
            iconContainerColor = Color.White.copy(alpha = 0.22f),
            borderColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
            onClick = onOpenEvacuationGuide
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF334155) else Color(0xFFFED7AA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("citizen_emergency_quick_action_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = "AKSI CEPAT TANGGAP DARURAT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Layanan utama warga Mukomuko dalam 1 ketukan",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "SIAGA 24 JAM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFFECACA) else Color(0xFFB91C1C),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val useSingleColumn = maxWidth < 280.dp || (fontScale >= 1.6f && maxWidth < 480.dp)
                val useFourColumns = maxWidth >= 680.dp && fontScale < 1.5f
                val columns = when {
                    useSingleColumn -> 1
                    useFourColumns -> 4
                    else -> 2
                }
                val rows = tiles.chunked(columns)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEach { rowTiles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTiles.forEach { tile ->
                                EmergencyQuickTileCard(
                                    spec = tile,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - rowTiles.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyQuickTileCard(
    spec: EmergencyQuickTileSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = spec.containerColor,
        border = BorderStroke(1.dp, spec.borderColor),
        shadowElevation = 2.dp,
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "${spec.title}. ${spec.subtitle}"
            }
            .clickable(role = Role.Button, onClick = spec.onClick)
            .testTag(spec.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(spec.iconContainerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = spec.contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!spec.badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.24f)
                    ) {
                        Text(
                            text = spec.badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = spec.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = spec.contentColor,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = spec.subtitle,
                fontSize = 11.sp,
                color = spec.contentColor.copy(alpha = 0.92f),
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
