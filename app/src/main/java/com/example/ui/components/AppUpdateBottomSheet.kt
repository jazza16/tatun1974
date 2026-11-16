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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onOpenEvacuationGuide: () -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bpbd_logo),
                        contentDescription = "Logo BPBD Mukomuko",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, OrangePrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pusat Pembaruan Aplikasi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SIPAGAR KITA v${updateState.currentVersion} • Rilis Resmi",
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
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenOnboarding()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
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
                                .heightIn(min = 48.dp)
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

                        Button(
                            onClick = {
                                onDismiss()
                                onOpenEvacuationGuide()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_open_sop_evakuasi_from_sheet"),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SOP Evakuasi (Panduan Offline)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preserved Wallpaper HD Badges (migrated from Login Screen)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = OrangePrimary.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, OrangePrimary)
                                ) {
                                    Text(
                                        text = "WALLPAPER HD 9:16",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lanskap Alam & Mitigasi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onShowStartupWallpaper()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_view_full_photo_info"),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = OrangeOnPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lihat Foto Penuh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangeOnPrimary)
                                }

                                val context = androidx.compose.ui.platform.LocalContext.current
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val wm = android.app.WallpaperManager.getInstance(context)
                                            val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.img_mukomuko_wallpaper)
                                            if (bmp != null) {
                                                wm.setBitmap(bmp)
                                                android.widget.Toast.makeText(context, "Wallpaper HD berhasil dipasang di layar HP!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Gagal memasang: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_set_wallpaper_info"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = OrangePrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pasang di HP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                                }
                            }
                        }
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
            NewFeaturesExplanationSection(
                currentVersion = updateState.currentVersion,
                isDark = isActuallyDark
            )

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

            // --- 6. IDENTITAS & MAKNA AKRONIM RESMI SIPAGAR KITA ---
            SipagarKitaAcronymCard(isDark = isActuallyDark)

            Spacer(modifier = Modifier.height(16.dp))

            // --- 7. VERSION CHANGELOG HISTORY ---
            VersionChangelogSection(updateState = updateState, isDark = isActuallyDark)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

                Column(modifier = Modifier.weight(1f)) {
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

            // 3-way Segmented Button (Terang, Gelap, Otomatis) wrapped in FlowRow for large font scales
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Light Mode Option
                ThemeModePill(
                    label = "Terang",
                    icon = Icons.Default.LightMode,
                    isSelected = isDarkMode == false,
                    modifier = Modifier.widthIn(min = 90.dp),
                    onClick = { onSetThemeMode(false) },
                    testTag = "theme_option_light"
                )

                // Dark Mode Option
                ThemeModePill(
                    label = "Gelap",
                    icon = Icons.Default.DarkMode,
                    isSelected = isDarkMode == true,
                    modifier = Modifier.widthIn(min = 90.dp),
                    onClick = { onSetThemeMode(true) },
                    testTag = "theme_option_dark"
                )

                // System Option
                ThemeModePill(
                    label = "Sistem",
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = isDarkMode == null,
                    modifier = Modifier.widthIn(min = 90.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isActuallyDark) OrangePrimaryDark else OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Kontras Tinggi Aktif: Seluruh notifikasi oranye & teks memenuhi standar keterbacaan WCAG AAA.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActuallyDark) WarningAlertTextDark else WarningAlertTextLight,
                        modifier = Modifier
                            .weight(1f)
                            .padding(12.dp)
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
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
                color = contentColor,
                maxLines = 1
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
    val isUpToDate = updateState.currentVersion == updateState.latestVersion
    @OptIn(ExperimentalLayoutApi::class)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("online_update_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(36.dp)
                            .background(
                                if (isUpToDate) Color(0xFF16A34A).copy(alpha = 0.15f)
                                else OrangePrimary.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUpToDate) Icons.Default.VerifiedUser else Icons.Default.NewReleases,
                            contentDescription = "Pembaruan",
                            tint = if (isUpToDate) Color(0xFF16A34A) else OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Versi & Rilis",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Terakhir dicek: ${updateState.lastCheckedTime}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                Surface(
                    color = if (isUpToDate) Color(0xFF16A34A).copy(alpha = 0.15f)
                    else OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUpToDate) "Tersedia Versi Terbaru"
                        else "Update Tersedia",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUpToDate) Color(0xFF16A34A) else OrangePrimary,
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "v${updateState.latestVersion}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCheckUpdate,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
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

                if (!isUpToDate && updateState.isUpdateAvailable) {
                    Button(
                        onClick = onApplyUpdate,
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
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
        }
    }
}

@Composable
private fun NewFeaturesExplanationSection(
    currentVersion: String = com.example.data.local.AppUpdatePreferences.DEFAULT_LATEST_VERSION,
    isDark: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("new_features_explanation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(32.dp)
                            .background(OrangePrimary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Fitur Baru",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Panduan Fitur & Rilis Resmi v$currentVersion",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF78350F)
                        )
                        Text(
                            text = "Biometric Auth, Play Integrity Server & Laporan Harian PDF",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFDC5A2) else Color(0xFF92400E)
                        )
                    }
                }

                Surface(
                    color = OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Tutup" else "Detail",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Concise Bullet Points
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    "• Play Integrity API Classic: Evaluasi keaslian hardware dan token anti-tamper",
                    "• Google Play In-App Update: Pustaka pembaruan versi resmi otomatis dalam aplikasi",
                    "• Multi-Device Sync & Latensi: Deteksi latensi jaringan dan sinkronisasi real-time",
                    "• Ruang Kerja Terproteksi: Validasi sesi backend Firestore sebelum akses diberikan"
                ).forEach { summaryPoint ->
                    Text(
                        text = summaryPoint,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFFED7AA) else Color(0xFF78350F),
                        lineHeight = 15.sp
                    )
                }
            }

            // Expanded Full Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeatureDetailItem(
                        emoji = "🛡️",
                        title = "Keamanan Hardware Play Integrity Classic",
                        desc = "Memverifikasi integritas fisik perangkat ponsel petugas dengan cryptographic nonce unik guna mencegah pembajakan akun dan rekayasa perangkat lunak.",
                        isDark = isDark
                    )
                    FeatureDetailItem(
                        emoji = "🔄",
                        title = "Pembaruan In-App Google Play",
                        desc = "Mendukung instalasi pembaruan aplikasi secara resmi dan aman langsung dari Google Play Store tanpa memerlukan berkas eksternal.",
                        isDark = isDark
                    )
                    FeatureDetailItem(
                        emoji = "👥",
                        title = "Proteksi Data Personil BPBD",
                        desc = "Struktur personil dan penugasan dinas diamankan oleh verifikasi sandi resmi bertingkat untuk menjaga kerahasiaan identitas operasional.",
                        isDark = isDark
                    )
                    FeatureDetailItem(
                        emoji = "⚡",
                        title = "Sinkronisasi Multi-Ponsel & Alarm Siaga",
                        desc = "Seluruh unit piket posko dan personil TRC lapangan terhubung serempak dengan audio-alarm otomatis saat ada aduan kedaruratan masuk.",
                        isDark = isDark
                    )
                    FeatureDetailItem(
                        emoji = "🗺️",
                        title = "Peta Geospasial GIS & Megathrust WRS-NTWC",
                        desc = "Pemantauan zona kerentanan bencana pesisir Samudra Hindia Mukomuko dan visualisasi sebaran laporan warga dengan koordinat GPS presisi.",
                        isDark = isDark
                    )
                    FeatureDetailItem(
                        emoji = "📞",
                        title = "Pusat Bantuan & Kontak Penting 117",
                        desc = "Akses terpadu satu ketukan ke Call Center Bebas Pulsa 117 BPBD, Posko Pusdalops, IGD Rumah Sakit, dan jajaran Polsek se-Mukomuko.",
                        isDark = isDark
                    )
                }
            }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            text = "Riwayat Catatan Rilis Versi",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        updateState.changelogs.forEach { log ->
            CollapsibleVersionCard(log = log, isDark = isDark)
        }
    }
}

@Composable
private fun CollapsibleVersionCard(
    log: com.example.model.VersionChangelog,
    isDark: Boolean
) {
    var isExpanded by remember { mutableStateOf(log.isLatest) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                                fontSize = 11.sp,
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.releaseDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Persempit" else "Buka",
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${log.highlights.size} catatan pembaruan • Ketuk untuk rincian",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
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
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
