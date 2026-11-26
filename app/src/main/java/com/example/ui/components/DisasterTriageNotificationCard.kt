package com.example.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.DisasterTriageLevel
import com.example.util.DisasterTriageNotificationManager
import com.example.util.DisasterTriageSignal
import com.example.util.TriageNotificationSettings

/**
 * Kartu Pengaturan & Pantauan Langsung Notifikasi Siaga Bencana Berbasis Triase Pewarnaan
 * (Hijau Aman, Kuning Waspada, Oranye Siaga, Merah Awas) untuk seluruh fitur SIPAGAR KITA.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisasterTriageNotificationCard(
    currentDistrict: District,
    settings: TriageNotificationSettings,
    activeSignals: List<DisasterTriageSignal>,
    onUpdateSettings: (TriageNotificationSettings) -> Unit,
    onTestTriageNotification: (Context, DisasterTriageLevel) -> Unit,
    onNavigateToTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingTestLevel by remember { mutableStateOf<DisasterTriageLevel?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onUpdateSettings(settings.copy(isEnabled = true))
            pendingTestLevel?.let { lvl ->
                onTestTriageNotification(context, lvl)
                pendingTestLevel = null
            }
        } else {
            pendingTestLevel = null
        }
    }

    val highestSignal = remember(activeSignals) {
        activeSignals.maxByOrNull { it.level.priorityScore }
    }
    val headerAccentColor = Color(highestSignal?.level?.colorHex ?: DisasterTriageLevel.ORANYE_SIAGA.colorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("disaster_triage_notification_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, headerAccentColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(headerAccentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifikasi Triase Siaga Bencana",
                            tint = headerAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifikasi Triase Siaga Bencana",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Aktif di ${currentDistrict.name} • Notifikasi gempa khusus lokal & dirasakan di Provinsi Bengkulu",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                SipagarSliderSwitch(
                    checked = settings.isEnabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !DisasterTriageNotificationManager.hasNotificationPermission(context)
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateSettings(settings.copy(isEnabled = checked))
                        }
                    },
                    checkedThumbColor = headerAccentColor,
                    modifier = Modifier.testTag("switch_triage_notification_master")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Color Triage Standard Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Standar Triase Pewarnaan Notifikasi:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TriageLegendPill("Hijau • Aman (Hening)", Color(DisasterTriageLevel.HIJAU_AMAN.colorHex))
                        TriageLegendPill("Kuning • Waspada", Color(DisasterTriageLevel.KUNING_WASPADA.colorHex))
                        TriageLegendPill("Oranye • Siaga", Color(DisasterTriageLevel.ORANYE_SIAGA.colorHex))
                        TriageLegendPill("Merah • Awas", Color(DisasterTriageLevel.MERAH_AWAS.colorHex))
                    }
                }
            }

            // Live Module Triage Indicators
            if (activeSignals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Status Triase Modul Terpantau Saat Ini:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSignals.forEach { signal ->
                        val chipColor = Color(signal.level.colorHex)
                        Surface(
                            color = chipColor.copy(alpha = 0.13f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clickable { onNavigateToTab(signal.resolvedTargetTab) }
                                .testTag("triage_status_chip_${signal.source.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${signal.source.displayName}: ${signal.level.code}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(12.dp))

            // Threshold Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ambang Minimum Pemicu Notifikasi:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val thresholdOptions = listOf(
                    DisasterTriageLevel.KUNING_WASPADA to "🟡 Kuning (Waspada+)",
                    DisasterTriageLevel.ORANYE_SIAGA to "🟠 Oranye (Siaga+)",
                    DisasterTriageLevel.MERAH_AWAS to "🔴 Merah (Awas)"
                )
                thresholdOptions.forEach { (level, label) ->
                    val isSelected = settings.minimumLevel == level
                    val levelColor = Color(level.colorHex)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onUpdateSettings(settings.copy(minimumLevel = level))
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = levelColor.copy(alpha = 0.18f),
                            selectedLabelColor = levelColor,
                            selectedLeadingIconColor = levelColor
                        ),
                        modifier = Modifier.testTag("chip_triage_threshold_${level.code.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lokasi Peringatan Relevan (Kecamatan terpilih vs Seluruh 15 Kecamatan)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                            contentDescription = "Lokasi Peringatan Relevan",
                            tint = headerAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lokasi Peringatan Relevan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.onlySelectedDistrict) {
                                    "Khusus wilayah ${currentDistrict.name}"
                                } else {
                                    "Seluruh 15 Kecamatan di Kab. Mukomuko"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    SipagarSliderSwitch(
                        checked = settings.onlySelectedDistrict,
                        onCheckedChange = {
                            onUpdateSettings(settings.copy(onlySelectedDistrict = it))
                        },
                        checkedThumbColor = headerAccentColor,
                        modifier = Modifier.testTag("switch_district_relevance")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preview / Test Buttons per Triage Color
            Text(
                text = "Uji Coba Notifikasi per Warna Triase:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val testLevels = listOf(
                    DisasterTriageLevel.KUNING_WASPADA to "Tes Kuning",
                    DisasterTriageLevel.ORANYE_SIAGA to "Tes Oranye",
                    DisasterTriageLevel.MERAH_AWAS to "Tes Merah"
                )
                testLevels.forEach { (level, label) ->
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !DisasterTriageNotificationManager.hasNotificationPermission(context)
                            ) {
                                pendingTestLevel = level
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTestTriageNotification(context, level)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_test_triage_${level.code.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(level.colorHex),
                            contentColor = OrangeOnPrimary
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TriageLegendPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
