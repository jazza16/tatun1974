package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary

data class QuickActionItemData(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color = Color.White,
    val testTag: String
)

/**
 * Material 3 Speed Dial Floating Action Button for Mukomuko Fast Actions.
 * Features:
 * - High-reliability click handling (no gesture conflicts)
 * - Tap anywhere outside on scrim to dismiss
 * - Dockable on bottom-right or bottom-left
 * - Direct quick links to: Laporan Warga, Prakiraan Cuaca, BPBD 117, Kompas, Peta Bencana, Maritim, Panduan
 */
@Composable
fun QuickActionsFab(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectCitizenReport: () -> Unit = {},
    onSelectForecast: () -> Unit = {},
    onSelectMaritimeGuide: () -> Unit = {},
    onSelectEvacuationProcedures: () -> Unit = {},
    onSelectDisasterMap: () -> Unit = {},
    onCall117: () -> Unit = {},
    onOpenCompass: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    onSelectFormLaporan: () -> Unit = {},
    parentMaxWidth: Dp = 380.dp,
    parentMaxHeight: Dp = 700.dp,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Docking position: bottom-right (default) or bottom-left
    var isDockedLeft by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "fab_rotation"
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val officerAuth = remember { com.example.data.local.OfficerAuthManager(context) }
    val isOfficerLoggedIn = officerAuth.isOfficerLoggedIn()
    val officerRole = officerAuth.getOfficerRole()
    val hasSpecialAssignment = officerRole.contains("Penugasan Khusus", ignoreCase = true)

    val actionItems = buildList {
        if (
            isOfficerLoggedIn && (
                officerRole.contains("Manajer", ignoreCase = true) ||
                officerRole.contains("Administrasi", ignoreCase = true) ||
                officerRole.contains("Operator", ignoreCase = true) ||
                hasSpecialAssignment
            )
        ) {
            add(QuickActionItemData("form_laporan", "Form Laporan Harian", Icons.Default.Edit, Color(0xFF1976D2), Color.White, "cardBuatLaporan"))
        }
        add(QuickActionItemData("citizen_report", "Laporan Warga Bencana", Icons.Default.Edit, Color(0xFFEA580C), Color.White, "action_citizen_report"))
        add(QuickActionItemData("forecast", "Prakiraan Cuaca", Icons.Default.CalendarMonth, Color(0xFF2563EB), Color.White, "action_forecast"))
        add(QuickActionItemData("dial_117", "Darurat 117 BPBD", Icons.Default.PhoneInTalk, Color(0xFFDC2626), Color.White, "action_dial_117"))
        add(QuickActionItemData("compass", "Kompas Evakuasi", Icons.Default.Explore, Color(0xFF16A34A), Color.White, "action_open_compass"))
        add(QuickActionItemData("disaster_map", "Peta Bencana Mukomuko", Icons.Default.Map, Color(0xFF0D9488), Color.White, "action_disaster_map"))
        add(QuickActionItemData("maritime", "Keselamatan Maritim", Icons.Default.Surfing, Color(0xFF0284C7), Color.White, "action_maritime_safety_guide"))
        add(QuickActionItemData("onboarding", "Panduan Aplikasi", Icons.Default.AutoAwesome, Color(0xFF8B5CF6), Color.White, "action_open_onboarding"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("quick_actions_fab_container")
    ) {
        // Scrim backdrop when menu is open to tap-to-dismiss
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onToggleExpand()
                    }
            )
        }

        // Action Menu Stack and Main FAB aligned at Bottom-End or Bottom-Start
        Column(
            modifier = Modifier
                .align(if (isDockedLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start = if (isDockedLeft) 16.dp else 0.dp,
                    end = if (!isDockedLeft) 16.dp else 0.dp,
                    bottom = 20.dp
                ),
            horizontalAlignment = if (isDockedLeft) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Speed Dial Action Items (Shown above the FAB when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(150)) + scaleOut(targetScale = 0.85f)
            ) {
                Column(
                    horizontalAlignment = if (isDockedLeft) Alignment.Start else Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // Position dock switcher pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.clickable {
                            isDockedLeft = !isDockedLeft
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDockedLeft) "Pindah ke Kanan" else "Pindah ke Kiri",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        }
                    }

                    // Speed dial items
                    actionItems.forEach { item ->
                        QuickActionSpeedDialItem(
                            item = item,
                            isDockedLeft = isDockedLeft,
                            onClick = {
                                onToggleExpand()
                                when (item.id) {
                                    "form_laporan" -> onSelectFormLaporan()
                                    "citizen_report" -> onSelectCitizenReport()
                                    "forecast" -> onSelectForecast()
                                    "dial_117" -> onCall117()
                                    "compass" -> onOpenCompass()
                                    "disaster_map" -> onSelectDisasterMap()
                                    "maritime" -> onSelectMaritimeGuide()
                                    "onboarding" -> onOpenOnboarding()
                                }
                            }
                        )
                    }
                }
            }

            // Primary Floating Action Button (Clean, 100% Reliable Tap Handler)
            FloatingActionButton(
                onClick = onToggleExpand,
                shape = CircleShape,
                containerColor = OrangePrimary,
                contentColor = OrangeOnPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("main_quick_actions_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.FlashOn,
                        contentDescription = if (isExpanded) "Tutup Menu Aksi Cepat" else "Buka Menu Aksi Cepat",
                        tint = OrangeOnPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionSpeedDialItem(
    item: QuickActionItemData,
    isDockedLeft: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(item.testTag)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDockedLeft) {
                Surface(
                    shape = CircleShape,
                    color = item.containerColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = item.contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            } else {
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = item.containerColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = item.contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
