package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppUpdateState
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangePrimaryContainerDark
import com.example.ui.theme.OrangePrimaryDark
import com.example.ui.theme.WarningAlertBgDark
import com.example.ui.theme.WarningAlertBgLight
import com.example.ui.theme.WarningAlertBorderDark
import com.example.ui.theme.WarningAlertBorderLight
import com.example.ui.theme.WarningAlertTextDark
import com.example.ui.theme.WarningAlertTextLight
import com.example.ui.theme.WarningAlertTitleDark
import com.example.ui.theme.WarningAlertTitleLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateBottomSheet(
    updateState: AppUpdateState,
    isDarkMode: Boolean?,
    onDismiss: () -> Unit,
    onSetThemeMode: (Boolean?) -> Unit,
    onCheckUpdateOnline: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onSimulateOlderVersion: () -> Unit = {},
    onSimulatePublishNextVersion: () -> Unit = {},
    onResetLatestRelease: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    onOpenCompass: () -> Unit = {},
    onShowStartupWallpaper: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val systemInDark = isSystemInDarkTheme()
    val isActuallyDark = isDarkMode ?: systemInDark

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bpbd_logo),
                        contentDescription = "Logo BPBD Mukomuko",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, OrangePrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pusat Pembaruan Aplikasi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cuaca Mukomuko v${updateState.currentVersion}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_update_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. THEME MODE TOGGLE CARD (LIGHT / DARK / SYSTEM) ---
            ThemeToggleCard(
                isDarkMode = isDarkMode,
                isActuallyDark = isActuallyDark,
                onSetThemeMode = onSetThemeMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. USER GUIDE & COMPASS TOOLS CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guide_and_tools_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Panduan & Alat Keselamatan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pelajari kembali fitur utama aplikasi atau gunakan kompas digital evakuasi darurat.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenOnboarding()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reopen_onboarding"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = OrangePrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Panduan Fitur", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onOpenCompass()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_compass_from_sheet"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kompas Digital", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onDismiss()
                            onShowStartupWallpaper()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_show_startup_wallpaper"),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OrangeOnPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wallpaper HD & Mitigasi Bencana Mukomuko",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeOnPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. ONLINE APP UPDATE SECTION ---
            OnlineUpdateCard(
                updateState = updateState,
                isDark = isActuallyDark,
                onCheckUpdate = onCheckUpdateOnline,
                onApplyUpdate = onDownloadUpdate,
                onSimulateOlderVersion = onSimulateOlderVersion,
                onSimulatePublishNextVersion = onSimulatePublishNextVersion,
                onResetLatestRelease = onResetLatestRelease
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. EXPLANATION OF NEW FEATURES (Penjelasan Fitur Update yang Baru) ---
            NewFeaturesExplanationSection(isDark = isActuallyDark)

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. DEVELOPER INFORMATION SECTION ("TnT@74") ---
            DeveloperInfoCard(
                developerName = updateState.developerName,
                developerRole = updateState.developerRole,
                organization = updateState.organization,
                developerEmail = updateState.developerEmail,
                developerTelegram = updateState.developerTelegram,
                isDark = isActuallyDark,
                onOpenWallpaper = {
                    onDismiss()
                    onShowStartupWallpaper()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 6. VERSION CHANGELOG HISTORY ---
            VersionChangelogSection(updateState = updateState, isDark = isActuallyDark)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeToggleCard(
    isDarkMode: Boolean?,
    isActuallyDark: Boolean,
    onSetThemeMode: (Boolean?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_mode_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isActuallyDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActuallyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Tema",
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Mode Tampilan Aplikasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActuallyDark) "Mode Gelap aktif (Kontras Tinggi)" else "Mode Terang aktif (Cerah & Hangat)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-way Segmented Button (Terang, Gelap, Otomatis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Light Mode Option
                ThemeModePill(
                    label = "Terang",
                    icon = Icons.Default.LightMode,
                    isSelected = isDarkMode == false,
                    modifier = Modifier.weight(1f),
                    onClick = { onSetThemeMode(false) },
                    testTag = "theme_option_light"
                )

                // Dark Mode Option
                ThemeModePill(
                    label = "Gelap",
                    icon = Icons.Default.DarkMode,
                    isSelected = isDarkMode == true,
                    modifier = Modifier.weight(1f),
                    onClick = { onSetThemeMode(true) },
                    testTag = "theme_option_dark"
                )

                // System Option
                ThemeModePill(
                    label = "Sistem",
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = isDarkMode == null,
                    modifier = Modifier.weight(1f),
                    onClick = { onSetThemeMode(null) },
                    testTag = "theme_option_system"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contrast Assurance Indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isActuallyDark) WarningAlertBgDark else WarningAlertBgLight,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isActuallyDark) WarningAlertBorderDark else WarningAlertBorderLight
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isActuallyDark) OrangePrimaryDark else OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kontras Tinggi Aktif: Seluruh notifikasi oranye & teks memenuhi standar keterbacaan WCAG AAA.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActuallyDark) WarningAlertTextDark else WarningAlertTextLight
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) OrangePrimary else Color.Transparent,
        label = "themePillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) OrangeOnPrimary else MaterialTheme.colorScheme.onSurface,
        label = "themePillText"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun OnlineUpdateCard(
    updateState: AppUpdateState,
    isDark: Boolean,
    onCheckUpdate: () -> Unit,
    onApplyUpdate: () -> Unit,
    onSimulateOlderVersion: () -> Unit = {},
    onSimulatePublishNextVersion: () -> Unit = {},
    onResetLatestRelease: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("online_update_card"),
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
                            .size(36.dp)
                            .background(
                                if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A).copy(alpha = 0.15f)
                                else Color(0xFF0284C7).copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Icons.Default.VerifiedUser else Icons.Default.NewReleases,
                            contentDescription = "Pembaruan",
                            tint = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A) else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Status Versi & Rilis",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Terakhir dicek: ${updateState.lastCheckedTime}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    color = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A).copy(alpha = 0.15f)
                    else OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (updateState.isPermanentlyInstalled && updateState.isUpdated) "Terkini v${updateState.currentVersion} (Permanen)"
                        else "Update v${updateState.latestVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A) else OrangePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Version Comparison Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Versi Terpasang",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "v${updateState.currentVersion}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rilis Online Terbaru",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "v${updateState.latestVersion}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }

            // OTA Architecture Assurance Strip
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = if (updateState.isPermanentlyInstalled && updateState.isUpdated) {
                    Color(0xFF16A34A).copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Icons.Default.CheckCircle else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A) else OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (updateState.isPermanentlyInstalled && updateState.isUpdated) {
                                "Perangkat Berada Pada Rilis Terbaru (Tetap & Permanen)"
                            } else {
                                "Sistem OTA Aktif (Tanpa Offline Update)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (updateState.isPermanentlyInstalled && updateState.isUpdated) Color(0xFF16A34A) else OrangePrimary
                        )
                        Text(
                            text = if (updateState.isPermanentlyInstalled && updateState.isUpdated) {
                                "Perangkat yang sudah memperbarui tidak berulang meminta update. Cukup sekali saja dan posisi rilis tersimpan permanen di DataStore."
                            } else {
                                "Perangkat yang sebelumnya menginstal versi lama langsung mengenali & memasang v${updateState.latestVersion} secara daring tanpa perantara offline."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Status message
            if (updateState.updateStatusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = updateState.updateStatusMessage,
                    fontSize = 11.sp,
                    color = if (updateState.isUpdated) Color(0xFF16A34A) else OrangePrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Downloading Progress
            if (updateState.isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mengunduh pembaruan online...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OrangePrimary
                        )
                        Text(
                            text = "${(updateState.downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { updateState.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = OrangePrimary,
                        trackColor = OrangePrimary.copy(alpha = 0.2f)
                    )
                }
            }

            // Success message
            if (updateState.isUpdated) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aplikasi berhasil diperbarui ke versi ${updateState.currentVersion} secara daring dan tersimpan permanen!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCheckUpdate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_check_update_online"),
                    enabled = !updateState.isCheckingOnline && !updateState.isDownloading
                ) {
                    if (updateState.isCheckingOnline) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = OrangePrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memeriksa...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cek Daring", fontSize = 12.sp)
                    }
                }

                if (!updateState.isUpdated && updateState.isUpdateAvailable) {
                    Button(
                        onClick = onApplyUpdate,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_download_update"),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        enabled = !updateState.isDownloading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = OrangeOnPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasang OTA v${updateState.latestVersion}", fontSize = 12.sp, color = OrangeOnPrimary)
                    }
                }
            }

            // Developer / QA Simulation Section
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pengujian Alur Pembaruan Daring (OTA)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Sistem menggunakan Semantic Versioning (AppVersionHelper) untuk mendeteksi perangkat versi sebelumnya (v1.2.0) guna memicu notifikasi update ke v1.3.0, serta otomatis berlaku untuk versi berikutnya (v1.4.0, dst) saat dirilis pengembang.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSimulateOlderVersion,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_simulate_v120"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Uji v1.2.0 (Notif v1.3.0)",
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = onSimulatePublishNextVersion,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_simulate_v140"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Uji Rilis v1.4.0",
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onResetLatestRelease,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("btn_reset_latest_release"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (updateState.isUpdated && updateState.isPermanentlyInstalled) Color(0xFF16A34A) else OrangePrimary
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Kunci v1.3.0",
                                fontSize = 10.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewFeaturesExplanationSection(isDark: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("new_features_explanation_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2B1D16) else Color(0xFFFFFBEB)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isDark) OrangePrimary.copy(alpha = 0.5f) else Color(0xFFFDE68A)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(OrangePrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Fitur Baru",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Penjelasan Fitur Update Baru",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF78350F)
                    )
                    Text(
                        text = "Peningkatan sistem visual, performa, dan keselamatan warga",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFFDC5A2) else Color(0xFF92400E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Explanations for v1.3.0
            FeatureDetailItem(
                emoji = "🚨",
                title = "Konsol Gempa WRS-NTWC InaTEWS BMKG",
                desc = "Transformasi total tampilan gempa bumi mengadopsi standar sistem WRS-NTWC BMKG (inatews.bmkg.go.id/wrs): Peta GIS interaktif (Streets, Satelit, Grayscale), animasi gelombang seismik P/S-wave, zona subduksi Sunda Megathrust, status ancaman tsunami nasional, matrix parameter OT, dan pengujian sirene.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "🍃",
                title = "Tab Kualitas Udara (ISPU) & Himbauan BMKG",
                desc = "Pemantauan komprehensif 6 parameter polutan udara (PM2.5, PM10, SO2, CO, O3, NO2) lengkap dengan status ISPU, rilis resmi BMKG, dan himbauan pencegahan kesehatan untuk masyarakat serta kelompok rentan.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "🗺️",
                title = "Peta Shake Map Gempa Interaktif (Zoom In / Out)",
                desc = "Penampil peta guncangan gempa bumi BMKG interaktif dengan gestur pinch-to-zoom, pan navigasi, double tap, dan penampil gambar resolusi tinggi layar penuh (Fullscreen Viewer).",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "✨",
                title = "Dynamic Weather Icon (Ikon Cuaca Dinamis)",
                desc = "Set ikon cuaca hidup beranimasi yang bereaksi dinamis sesuai kondisi cuaca waktu nyata (cerah bersinar, awan bergerak, tetesan hujan turun, badai petir berdenyut).",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "☀️",
                title = "Grafik Kurva Indeks UV & Jam Paparan Puncak",
                desc = "Visualisasi kurva interaktif indeks radiasi ultraviolet matahari harian dengan penanda zona waktu paparan puncak (10.00 - 14.00 WIB) serta protokol perlindungan diri.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "📍",
                title = "Deteksi Otomatis Lokasi GPS 15 Kecamatan",
                desc = "Mendeteksi secara otomatis posisi kecamatan pengguna di wilayah Kabupaten Mukomuko melalui GPS perangkat dengan alur izin lokasi runtime yang ramah.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "📱",
                title = "Optimalisasi Pengguna Ponsel Mode Portrait",
                desc = "Tampilan antarmuka disempurnakan khusus untuk penggunaan ponsel satu tangan dalam posisi tegak (portrait) yang rapi, proporsional, dan tanpa pergeseran elemen.",
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureDetailItem(
                emoji = "🔄",
                title = "Pembaruan Daring Otomatis (OTA In-App Engine)",
                desc = "Sistem pembaruan langsung di dalam aplikasi (OTA) yang mengenali rilis terbaru dan menginstalnya secara permanen tanpa perlu update file manual.",
                isDark = isDark
            )
        }
    }
}

@Composable
private fun FeatureDetailItem(
    emoji: String,
    title: String,
    desc: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) Color(0xFF1E140E) else Color(0xFFFFFFFF),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF573E32)
            )
        }
    }
}

@Composable
private fun VersionChangelogSection(
    updateState: AppUpdateState,
    isDark: Boolean
) {
    Column {
        Text(
            text = "Riwayat Versi Sebelumnya",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        updateState.changelogs.forEach { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Versi ${log.version}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (log.isLatest) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = OrangePrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TERBARU",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (log.isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF16A34A),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TERPASANG",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = log.releaseDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    log.highlights.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
