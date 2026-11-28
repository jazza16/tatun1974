package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.AppUsageGuideRepository
import com.example.model.AppUsageGuide
import com.example.model.UsageGuideStep
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

private enum class UsageGuideAudience {
    WARGA,
    PETUGAS
}

/**
 * Dialog Panduan Penggunaan Aplikasi SIPAGAR KITA secara ringkas untuk Warga & Petugas.
 * Konten disinkronkan secara dinamis dari Cloud Firestore (`app_settings/usage_guide`)
 * dengan cache offline sehingga pembaruan panduan tampil di HP asli tanpa update/reinstall APK.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveOnboardingDialog(
    onComplete: () -> Unit,
    onOpenCompassDetailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adaptiveInsets = rememberAdaptiveSystemBarInsets()
    val isDark = isSystemInDarkTheme()

    val officerAuth = remember { OfficerAuthManager(context) }
    val isOfficerActive = remember { officerAuth.isOfficerLoggedIn() }
    val isAdminActive = remember { officerAuth.isAdministratorSessionActive() }

    var selectedAudience by remember {
        mutableStateOf(if (isOfficerActive) UsageGuideAudience.PETUGAS else UsageGuideAudience.WARGA)
    }

    var guide by remember { mutableStateOf(AppUsageGuideRepository.getCachedOrDefaultGuide(context)) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncedFromCloud by remember { mutableStateOf(guide.source == AppUsageGuide.SOURCE_FIRESTORE) }
    var syncStatusText by remember {
        mutableStateOf("Memeriksa pembaruan panduan dari Cloud BPBD...")
    }
    var showLiveEditor by remember { mutableStateOf(false) }

    suspend fun refreshFromCloud() {
        isSyncing = true
        val result = AppUsageGuideRepository.fetchLatestGuide(context)
        guide = result.guide
        syncedFromCloud = result.syncedFromCloud
        syncStatusText = result.statusMessage
        isSyncing = false
    }

    LaunchedEffect(Unit) {
        refreshFromCloud()
    }

    val activeSteps = if (selectedAudience == UsageGuideAudience.WARGA) {
        guide.citizenSteps
    } else {
        guide.officerSteps
    }
    val activeSummary = if (selectedAudience == UsageGuideAudience.WARGA) {
        guide.citizenSummary
    } else {
        guide.officerSummary
    }
    val roleAccentColor = if (selectedAudience == UsageGuideAudience.WARGA) {
        OrangePrimary
    } else {
        Color(0xFF1D4ED8)
    }

    Dialog(
        onDismissRequest = onComplete,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("interactive_onboarding_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 620.dp)
                        .padding(
                            top = adaptiveInsets.topBarInset,
                            bottom = adaptiveInsets.effectiveBottomInset
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // 1. Header Bar
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
                                contentDescription = "Logo BPBD Kabupaten Mukomuko",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, OrangePrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = guide.releaseTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SIPAGAR KITA v${BuildConfig.VERSION_NAME} • Warga & Petugas",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_close_onboarding")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Panduan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Cloud Live-Sync Status Card (Updates without APK reinstall)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = OrangePrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (syncedFromCloud) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (syncedFromCloud) Color(0xFF16A34A) else OrangePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = syncStatusText,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (!isSyncing) {
                                            coroutineScope.launch { refreshFromCloud() }
                                        }
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("btn_refresh_usage_guide")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Segarkan Panduan dari Cloud",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GuideTagBadge(
                                    text = if (syncedFromCloud) "Sinkron Cloud Aktif" else "Siap Offline",
                                    backgroundColor = if (syncedFromCloud) Color(0xFF16A34A).copy(alpha = 0.14f) else OrangePrimary.copy(alpha = 0.12f),
                                    textColor = if (syncedFromCloud) {
                                        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                    } else {
                                        if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)
                                    }
                                )
                                GuideTagBadge(
                                    text = "Tanpa Update APK",
                                    backgroundColor = Color(0xFF0284C7).copy(alpha = 0.12f),
                                    textColor = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                )
                                if (isAdminActive) {
                                    OutlinedButton(
                                        onClick = { showLiveEditor = true },
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("btn_edit_usage_guide_admin"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, OrangePrimary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit Live Cloud",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangePrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Role Switcher: Untuk Warga vs Untuk Petugas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleTabButton(
                            selected = selectedAudience == UsageGuideAudience.WARGA,
                            title = "Untuk Warga",
                            icon = Icons.Default.Groups,
                            accentColor = OrangePrimary,
                            onClick = { selectedAudience = UsageGuideAudience.WARGA },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("tab_guide_citizen")
                        )
                        RoleTabButton(
                            selected = selectedAudience == UsageGuideAudience.PETUGAS,
                            title = "Untuk Petugas",
                            icon = Icons.Default.Security,
                            accentColor = Color(0xFF1D4ED8),
                            onClick = { selectedAudience = UsageGuideAudience.PETUGAS },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("tab_guide_officer")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Scrollable Guide Content
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        item(key = "role_summary") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = roleAccentColor.copy(alpha = if (isDark) 0.16f else 0.08f)
                                ),
                                border = BorderStroke(1.dp, roleAccentColor.copy(alpha = 0.3f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedAudience == UsageGuideAudience.WARGA) {
                                            "Ringkasan Panduan Warga (Masyarakat Umum)"
                                        } else {
                                            "Ringkasan Panduan Petugas (Pusdalops / TRC / Posko)"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else roleAccentColor
                                    )
                                    Text(
                                        text = activeSummary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = activeSteps,
                            key = { index, step -> "${selectedAudience.name}_${index}_${step.title}" }
                        ) { index, step ->
                            UsageGuideStepCard(
                                displayIndex = index + 1,
                                step = step,
                                accentColor = roleAccentColor,
                                isDark = isDark
                            )
                        }

                        item(key = "dynamic_update_note") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Panduan ini terhubung dengan Cloud Firestore BPBD (${guide.guideVersion}). Pembaruan langkah penggunaan otomatis tersinkron ke perangkat tanpa perlu membongkar atau menginstal ulang APK.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Bottom Footer Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenCompassDetailed,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_open_detailed_compass"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kompas",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_get_started_onboarding"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OrangeOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tutup & Mulai",
                                color = OrangeOnPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLiveEditor && isAdminActive) {
        UsageGuideLiveEditorDialog(
            initialGuide = guide,
            onDismiss = { showLiveEditor = false },
            onPublished = { publishedGuide ->
                guide = publishedGuide
                syncedFromCloud = true
                syncStatusText = "Berhasil diterbitkan ke Cloud (${publishedGuide.guideVersion})."
                showLiveEditor = false
                Toast.makeText(
                    context,
                    "Panduan Penggunaan Live Cloud berhasil diperbarui untuk semua pengguna.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
private fun RoleTabButton(
    selected: Boolean,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else accentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsageGuideStepCard(
    displayIndex: Int,
    step: UsageGuideStep,
    accentColor: Color,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayIndex.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = step.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (step.moduleTag.isNotBlank()) {
                        GuideTagBadge(
                            text = step.moduleTag,
                            backgroundColor = accentColor.copy(alpha = if (isDark) 0.24f else 0.12f),
                            textColor = if (isDark) Color.White else accentColor
                        )
                    }
                }

                Text(
                    text = step.description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideTagBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
