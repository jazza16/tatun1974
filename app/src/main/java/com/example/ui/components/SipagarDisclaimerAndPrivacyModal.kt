package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DisclaimerConsentManager
import com.example.ui.theme.OrangePrimary

private const val BPBD_OFFICIAL_ADDRESS =
    "Jl. Imam Bonjol, Komplek Perkantoran Pemda Kab. Mukomuko, Prov. Bengkulu 38365"
private const val BPBD_OFFICIAL_PHONE = "Call Center 117 / 112"
private const val BPBD_OFFICIAL_EMAIL = "bpbdmuko@gmail.com"
private const val BPBD_OFFICIAL_DPO_ROLE =
    "Sekretaris / Koordinator Pusdalops-PB BPBD Kabupaten Mukomuko"

private data class QuickDisclaimerBullet(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val body: String
)

private data class LegalClauseSection(
    val number: String,
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val footerNote: String? = null
)

/**
 * Pop-up Disklaimer Singkat (Bagian B) yang muncul sebelum pengguna memulai aplikasi SIPAGAR KITA
 * pada pemakaian pertama (atau saat versi Disklaimer diperbarui).
 *
 * Fitur kunci:
 * - Menggunakan `BalancedModalDialog` agar jarak atas & bawah layar selalu seimbang dan kartu utuh.
 * - Tombol panggilan darurat Call Center 117 dan 112 dapat ditekan langsung bahkan sebelum
 *   menyetujui disklaimer.
 * - Tautan `[Disklaimer Lengkap]` dan `[Kebijakan Privasi]` dapat langsung dibuka in-app tanpa
 *   keluar dari alur persetujuan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SipagarFirstLaunchDisclaimerDialog(
    onAcceptAndContinue: () -> Unit,
    onCallEmergency: (String) -> Unit
) {
    var isAgreed by remember { mutableStateOf(false) }
    var openedFullDocumentTab by remember { mutableStateOf<Int?>(null) }

    val quickItems = remember {
        listOf(
            QuickDisclaimerBullet(
                icon = Icons.Default.Info,
                iconTint = Color(0xFF0284C7),
                title = "Sumber Data Resmi",
                body = "Informasi bersumber dari BMKG, BNPB (inaRISK), Badan Geologi/PVMBG, NASA FIRMS, OpenStreetMap, dan BPBD Kabupaten Mukomuko, serta dapat berubah sewaktu-waktu."
            ),
            QuickDisclaimerBullet(
                icon = Icons.Default.Warning,
                iconTint = Color(0xFFD97706),
                title = "Bukan Pengganti Peringatan Resmi",
                body = "Aplikasi ini bukan pengganti peringatan dan instruksi resmi petugas. Dalam keadaan darurat, ikuti arahan petugas di lapangan."
            ),
            QuickDisclaimerBullet(
                icon = Icons.Default.Cloud,
                iconTint = Color(0xFF2563EB),
                title = "Prakiraan Bukan Kepastian",
                body = "Prakiraan cuaca dan potensi bencana adalah perkiraan ilmiah berdasarkan data yang tersedia, bukan kepastian kejadian."
            ),
            QuickDisclaimerBullet(
                icon = Icons.Default.ReportProblem,
                iconTint = Color(0xFFDC2626),
                title = "Cross-Check Wilayah & Bukan Penyelamatan Seketika",
                body = "Mengingat luas wilayah Kab. Mukomuko, jarak tempuh, ketersediaan SDM, dan efisiensi anggaran, setiap laporan wajib melalui cross-check (pembuktian terbalik) oleh Petugas Piket Resmi BPBD ke Kades/Lurah/Camat via telepon/komunikasi resmi sebelum pengerahan tim. Jika nyawa terancam, segera hubungi Call Center 117 (BPBD/BNPB) atau Nomor Tunggal Panggilan Darurat 112 (Diskominfo Kab. Mukomuko) secara langsung."
            ),
            QuickDisclaimerBullet(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                iconTint = Color(0xFF15803D),
                title = "Panduan Evakuasi Bersifat Umum",
                body = "Panduan evakuasi bersifat umum dan kondisi lapangan dapat berubah. Utamakan keselamatan dan kondisi di sekitar Anda."
            ),
            QuickDisclaimerBullet(
                icon = Icons.Default.SignalWifiOff,
                iconTint = Color(0xFF7C3AED),
                title = "Keterbatasan Jaringan & Sistem",
                body = "Jaringan, listrik, atau sistem dapat terganggu sewaktu-waktu, jadi pantau juga kanal resmi BPBD, BMKG, radio, dan pengeras suara lingkungan."
            ),
            QuickDisclaimerBullet(
                icon = Icons.Default.PrivacyTip,
                iconTint = Color(0xFF0F766E),
                title = "Pelindungan Data Pribadi (UU PDP)",
                body = "Data pribadi Anda dikelola secara terbatas dan aman sesuai UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi dan Kebijakan Privasi SIPAGAR KITA."
            )
        )
    }

    BalancedModalDialog(
        onDismissRequest = { /* Wajib menyetujui sebelum memulai aplikasi */ },
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        horizontalMargin = 14.dp,
        extraVerticalMargin = 14.dp
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_first_launch_disclaimer")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Resmi BPBD Mukomuko
                Surface(
                    color = OrangePrimary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bpbd_logo),
                            contentDescription = "Logo BPBD Kabupaten Mukomuko",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, OrangePrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Penting Sebelum Menggunakan SIPAGAR KITA",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = "Disklaimer Resmi • BPBD Kabupaten Mukomuko",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangePrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                // Area Isi yang Dapat Digulir
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    quickItems.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(item.iconTint.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.body,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Tautan Cepat Baca Lengkap (Disklaimer Lengkap & Kebijakan Privasi) — 2 Kolom Presisi Sejajar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { openedFullDocumentTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, OrangePrimary.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_popup_open_full_disclaimer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Disklaimer Lengkap",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { openedFullDocumentTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, Color(0xFF0F766E).copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_popup_open_privacy_policy")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Kebijakan Privasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Baris Panggilan Darurat Langsung — 2 Kolom Presisi & Simetris (Call Center 117 & 112)
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "Jalur Panggilan Darurat Langsung (Ketuk untuk Menelepon):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmergencyQuickDialChip(
                                    label = "Call Center 117",
                                    subLabel = "BPBD / BNPB (24 Jam)",
                                    bgColor = Color(0xFFDC2626),
                                    testTagName = "chip_disclaimer_call_117",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCallEmergency("117") }
                                )
                                EmergencyQuickDialChip(
                                    label = "Call Center 112",
                                    subLabel = "Tunggal • Diskominfo MMK",
                                    bgColor = Color(0xFF1D4ED8),
                                    testTagName = "chip_disclaimer_call_112",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCallEmergency("112") }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Menempel Rapi: Checkbox Persetujuan Eksplisit + Tombol Lanjutkan
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isAgreed = !isAgreed }
                            .padding(vertical = 4.dp)
                            .testTag("row_checkbox_accept_disclaimer"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAgreed,
                            onCheckedChange = { isAgreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF15803D)),
                            modifier = Modifier.testTag("checkbox_accept_disclaimer")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Saya telah membaca dan menyetujui ")
                                withStyle(
                                    SpanStyle(
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Disklaimer Lengkap")
                                }
                                append(" dan ")
                                withStyle(
                                    SpanStyle(
                                        color = Color(0xFF0F766E),
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Kebijakan Privasi")
                                }
                                append(" SIPAGAR KITA.")
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onAcceptAndContinue,
                        enabled = isAgreed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_accept_disclaimer_continue")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lanjutkan ke SIPAGAR KITA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal Baca Lengkap yang dibuka langsung dari Pop-up Awal
    val activeDocTab = openedFullDocumentTab
    if (activeDocTab != null) {
        SipagarLegalDocumentsDialog(
            initialTab = activeDocTab,
            consentDateLabel = null,
            onDismiss = { openedFullDocumentTab = null },
            onCallEmergency = onCallEmergency
        )
    }
}

@Composable
private fun EmergencyQuickDialChip(
    label: String,
    subLabel: String,
    bgColor: Color,
    testTagName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subLabel,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dialog Dokumen Resmi Lengkap (Bagian A: Disklaimer Lengkap & Bagian C: Kebijakan Privasi UU PDP).
 * Dapat diakses kapan saja dari Menu Info/Pengaturan maupun dari Pop-up Disklaimer Awal.
 */
@Composable
fun SipagarLegalDocumentsDialog(
    initialTab: Int = 0, // 0 = Disklaimer Lengkap, 1 = Kebijakan Privasi
    consentDateLabel: String? = null,
    onDismiss: () -> Unit,
    onCallEmergency: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val scrollState = rememberScrollState()

    val disclaimerClauses = remember { buildFullDisclaimerClauses() }
    val privacyClauses = remember { buildFullPrivacyPolicyClauses() }

    BalancedModalDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = 14.dp,
        extraVerticalMargin = 14.dp
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_legal_documents")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedTab == 0) OrangePrimary.copy(alpha = 0.14f)
                                else Color(0xFF0F766E).copy(alpha = 0.14f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Gavel else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (selectedTab == 0) OrangePrimary else Color(0xFF0F766E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == 0) "Disklaimer Lengkap" else "Kebijakan Privasi (UU PDP)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SIPAGAR KITA • Diperbarui ${DisclaimerConsentManager.LAST_UPDATED_DISPLAY} (v${DisclaimerConsentManager.CURRENT_DISCLAIMER_VERSION})",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_legal_documents")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Dokumen",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                // Segmented Switcher: Disklaimer Lengkap vs Kebijakan Privasi
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegalDocTabPill(
                            title = "Disklaimer Lengkap",
                            selected = selectedTab == 0,
                            activeColor = OrangePrimary,
                            testTagName = "tab_legal_disclaimer",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 0 }
                        )
                        LegalDocTabPill(
                            title = "Kebijakan Privasi",
                            selected = selectedTab == 1,
                            activeColor = Color(0xFF0F766E),
                            testTagName = "tab_legal_privacy",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 1 }
                        )
                    }
                }

                if (!consentDateLabel.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Anda telah menyetujui ketentuan ini pada $consentDateLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                // Konten Dokumen Lengkap (Scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pembuka Dokumen
                    Surface(
                        color = if (selectedTab == 0) OrangePrimary.copy(alpha = 0.08f) else Color(0xFF0F766E).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedTab == 0) {
                                "SIPAGAR KITA dikelola oleh Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko untuk membantu masyarakat dalam kesiapsiagaan, kedaruratan, dan pasca bencana. Dengan menggunakan aplikasi ini, Anda dianggap telah membaca dan memahami seluruh ketentuan berikut."
                            } else {
                                "Kebijakan Privasi ini menjelaskan secara transparan bagaimana BPBD Kabupaten Mukomuko mengumpulkan, memverifikasi, menyimpan, melindungi, dan menghapus data pribadi Anda sesuai UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi (UU PDP) dan kebijakan privasi platform Android."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    val activeClauses = if (selectedTab == 0) disclaimerClauses else privacyClauses
                    activeClauses.forEach { clause ->
                        LegalClauseCard(
                            clause = clause,
                            accentColor = if (selectedTab == 0) OrangePrimary else Color(0xFF0F766E)
                        )
                    }

                    // Tombol Pindah Dokumen & Kontak Darurat
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Kontak Resmi & Jalur Darurat BPBD Kabupaten Mukomuko",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alamat: $BPBD_OFFICIAL_ADDRESS\nLayanan Darurat: Call Center 117 (BPBD/BNPB) & Nomor Tunggal Panggilan Darurat 112 (dikelola Diskominfo Kab. Mukomuko) • Email: $BPBD_OFFICIAL_EMAIL",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onCallEmergency("117") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_legal_call_117")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Center 117", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Button(
                                    onClick = { onCallEmergency("112") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_legal_call_112")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Center 112", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Pinned
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedTab = if (selectedTab == 0) 1 else 0 },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Baca Privasi" else "Baca Disklaimer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) OrangePrimary else Color(0xFF0F766E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_legal_documents_done")
                    ) {
                        Text("Tutup", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalDocTabPill(
    title: String,
    selected: Boolean,
    activeColor: Color,
    testTagName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) activeColor else Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegalClauseCard(
    clause: LegalClauseSection,
    accentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = clause.number,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = clause.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            clause.paragraphs.forEach { paragraph ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = paragraph,
                    fontSize = 11.8.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
                )
            }

            if (clause.bullets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    clause.bullets.forEach { bullet ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = bullet,
                                fontSize = 11.5.sp,
                                lineHeight = 16.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (!clause.footerNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = clause.footerNote,
                    fontSize = 11.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    color = accentColor
                )
            }
        }
    }
}

private fun buildFullDisclaimerClauses(): List<LegalClauseSection> = listOf(
    LegalClauseSection(
        number = "Pasal 1",
        title = "Sumber dan Sifat Informasi",
        paragraphs = listOf(
            "Data dan informasi dalam aplikasi SIPAGAR KITA bersumber dari BMKG, BNPB (inaRISK), Badan Geologi/PVMBG, NASA FIRMS (titik panas/karhutla), OpenStreetMap (peta dasar), BPBD Kabupaten Mukomuko, serta sumber resmi terkait lainnya. Setiap data dilengkapi keterangan sumber dan waktu pembaruan. Informasi dapat berubah sewaktu-waktu sesuai perkembangan kondisi di lapangan."
        )
    ),
    LegalClauseSection(
        number = "Pasal 2",
        title = "Bukan Pengganti Peringatan Resmi",
        paragraphs = listOf(
            "Informasi di aplikasi ini bersifat pendukung dan tidak menggantikan peringatan dini, imbauan, maupun perintah resmi dari BMKG, BPBD, dan petugas berwenang. Peringatan dini cuaca dan gempa resmi bersumber dari BMKG (sesuai UU No. 31 Tahun 2009), dan aplikasi ini hanya menyebarluaskannya. Dalam keadaan darurat, utamakan instruksi petugas di lapangan."
        )
    ),
    LegalClauseSection(
        number = "Pasal 3",
        title = "Cuaca dan Potensi Bencana",
        paragraphs = listOf(
            "Prakiraan cuaca, gelombang laut, titik panas, peta rawan, dan kajian risiko merupakan perkiraan berdasarkan data dan metode ilmiah yang tersedia. Informasi ini bukan kepastian bahwa suatu kejadian akan atau tidak akan terjadi."
        )
    ),
    LegalClauseSection(
        number = "Pasal 4",
        title = "Panduan Evakuasi Mandiri dan Kesiapsiagaan",
        paragraphs = listOf(
            "Panduan evakuasi dan kompas digital bersifat umum. Kondisi jalur, jembatan, titik kumpul, dan lokasi aman dapat berubah akibat dampak bencana. Pengguna bertanggung jawab menilai kondisi sekitarnya dan mengutamakan keselamatan diri serta orang lain."
        )
    ),
    LegalClauseSection(
        number = "Pasal 5",
        title = "Pasca Bencana",
        paragraphs = listOf(
            "Informasi bantuan, pendataan kerusakan (JITUPASNA/DALA), dan layanan pemulihan mengikuti prosedur resmi BPBD Kabupaten Mukomuko dan instansi terkait. Informasi di aplikasi tidak menjamin pemberian bantuan atau status penerima tertentu."
        )
    ),
    LegalClauseSection(
        number = "Pasal 6",
        title = "Keterbatasan Teknis",
        paragraphs = listOf(
            "Aplikasi dapat terganggu atau terlambat menyampaikan informasi akibat gangguan jaringan seluler, listrik, perangkat, satelit GPS, atau sistem server. Jangan menjadikan aplikasi ini sebagai satu-satunya sarana informasi kebencanaan. Pantau juga kanal resmi BPBD, BMKG, radio, dan pengeras suara lingkungan."
        )
    ),
    LegalClauseSection(
        number = "Pasal 7",
        title = "Laporan, Metode Pembuktian, dan Konten dari Pengguna",
        paragraphs = listOf(
            "Pengguna bertanggung jawab penuh atas kebenaran laporan, foto, dan informasi yang dikirim.",
            "Metode Pembuktian Kebenaran Kejadian (Cross-Check / Pembuktian Terbalik): Untuk membuktikan ada atau tidaknya suatu peristiwa kejadian (kebenaran laporan dari warga), BPBD Kabupaten Mukomuko—dalam hal ini Petugas Piket Resmi Pusdalops-PB—melakukan pencocokan koordinat lokasi GPS serta foto kamera langsung berstempel GPS, dan melaksanakan pemeriksaan silang (cross-check / pembuktian terbalik) atas laporan dimaksud kepada Kepala Desa, Lurah, dan/atau Camat setempat melalui telepon maupun fasilitas komunikasi resmi lainnya, selaku penanggung jawab dan pemangku kepentingan wilayah secara administratif dalam Kabupaten Mukomuko.",
            "Pertimbangan Operasional: Prosedur cross-check/pembuktian terbalik wajib dilakukan sebelum tindak lanjut pengerahan personel dan peralatan ke lapangan, mengingat cakupan luasan wilayah Kabupaten Mukomuko, jarak tempuh menuju lokasi kejadian, ketersediaan sumber daya manusia (SDM) petugas, serta demi menjaga efisiensi dan akuntabilitas penggunaan anggaran daerah agar penanganan tepat sasaran.",
            "Dilarang menyebarkan hoaks, memalsukan lokasi (fake GPS) atau foto, dan mengirim konten yang melanggar hukum. BPBD berhak memverifikasi, menolak, atau menghapus laporan yang tidak sesuai atau tidak dapat dibuktikan kebenarannya. Status laporan dapat dilacak melalui ID Laporan.",
            "Fitur Kirim Laporan bukan layanan penyelamatan seketika dan tidak menjamin waktu respons tertentu. Jika nyawa terancam, segera hubungi Call Center Darurat 117 (BPBD/BNPB), Nomor Tunggal Panggilan Darurat 112 (yang dikelola oleh Dinas Kominfo Kabupaten Mukomuko), atau petugas terdekat secara langsung."
        )
    ),
    LegalClauseSection(
        number = "Pasal 8",
        title = "Pelindungan Data Pribadi",
        paragraphs = listOf(
            "Pengelolaan data pribadi pengguna (termasuk pelapor warga, pendaftaran relawan, dan akun petugas BPBD) dilakukan sesuai UU No. 27 Tahun 2022 tentang Pelindungan Data Pribadi (UU PDP) dan diatur secara rinci pada tab Kebijakan Privasi dalam aplikasi ini."
        )
    ),
    LegalClauseSection(
        number = "Pasal 9",
        title = "Hak Cipta dan Konten Pihak Ketiga",
        paragraphs = listOf(
            "Konten, peta, dan data dari pihak ketiga (BMKG, BNPB, Badan Geologi, NASA FIRMS, OpenStreetMap) tetap menjadi hak pemilik aslinya. Penggunaan ulang informasi dari aplikasi ini wajib mencantumkan sumber resmi."
        )
    ),
    LegalClauseSection(
        number = "Pasal 10",
        title = "Batas Tanggung Jawab",
        paragraphs = listOf(
            "BPBD Kabupaten Mukomuko berupaya menyajikan informasi yang akurat dan mutakhir. Sepanjang diizinkan peraturan perundang-undangan, BPBD tidak bertanggung jawab atas kerugian langsung maupun tidak langsung yang timbul dari penggunaan informasi tanpa mengikuti peringatan dan instruksi resmi petugas, atau akibat gangguan teknis di luar kendali pengelola."
        )
    ),
    LegalClauseSection(
        number = "Pasal 11",
        title = "Perubahan Ketentuan",
        paragraphs = listOf(
            "Disklaimer ini dapat diperbarui sewaktu-waktu sesuai perkembangan fitur dan regulasi. Perubahan materiil akan diberitahukan melalui aplikasi dan dapat memerlukan persetujuan ulang sebelum melanjutkan."
        )
    ),
    LegalClauseSection(
        number = "Pasal 12",
        title = "Kontak dan Pengaduan",
        paragraphs = listOf(
            "BPBD Kabupaten Mukomuko: $BPBD_OFFICIAL_ADDRESS | Email: $BPBD_OFFICIAL_EMAIL | Call Center Kebencanaan: 117 (BPBD/BNPB) | Nomor Tunggal Panggilan Darurat: 112 (dikelola oleh Dinas Kominfo Kabupaten Mukomuko)."
        )
    )
)

private fun buildFullPrivacyPolicyClauses(): List<LegalClauseSection> = listOf(
    LegalClauseSection(
        number = "Pasal 1",
        title = "Pengendali Data Pribadi",
        paragraphs = listOf(
            "Badan Penanggulangan Bencana Daerah (BPBD) Kabupaten Mukomuko, beralamat di $BPBD_OFFICIAL_ADDRESS.",
            "Pejabat/Petugas Pelindungan Data Pribadi: $BPBD_OFFICIAL_DPO_ROLE | Email: $BPBD_OFFICIAL_EMAIL | Layanan Darurat: Call Center 117 & Nomor Tunggal Panggilan Darurat 112 (Diskominfo Kab. Mukomuko)."
        )
    ),
    LegalClauseSection(
        number = "Pasal 2",
        title = "Data yang Kami Kumpulkan (Sesuai Fitur Aktual)",
        paragraphs = listOf(
            "Kami hanya mengumpulkan data yang benar-benar diperlukan sesuai peran penggunaan Anda di SIPAGAR KITA:"
        ),
        bullets = listOf(
            "A. Modul Laporan Bencana Warga: Nama pelapor (opsional, boleh inisial/sebutan; aplikasi tidak meminta nomor telepon atau email untuk melapor), data kejadian (jenis bencana, waktu, lokasi, kecamatan, desa/kelurahan), koordinat GPS yang Anda kunci sendiri melalui tombol formulir, foto kamera langsung berstempel GPS & waktu (pilihan galeri dinonaktifkan), jumlah korban berupa angka (bukan identitas individu), deskripsi kejadian, dan ID Laporan unik.",
            "B. Modul Pendaftaran Relawan Kebencanaan (hanya bila Anda mendaftar sebagai relawan): NIK 16 digit (untuk mencegah data ganda & penerbitan Surat Tugas resmi), nama lengkap, nomor HP/WA, organisasi, keahlian, kecamatan domisili, foto diri/selfie, foto sertifikat (opsional), serta waktu Check-in/Check-out posko.",
            "C. Modul Ruang Kerja Petugas / ASN BPBD (khusus petugas terdaftar): NIP/Nomor Registrasi, nama, jabatan/peran, unit/regu jaga, nomor kontak dinas, hash PIN kriptografis terenkripsi di server cloud (PIN mentah tidak disimpan di perangkat), serta catatan waktu & model perangkat login terakhir untuk audit keamanan. Autentikasi sidik jari/biometrik diproses sepenuhnya secara lokal oleh sistem Android perangkat Anda.",
            "D. Data Teknis Keamanan: Model perangkat (untuk log keamanan sesi petugas), token notifikasi Firebase Cloud Messaging (FCM) khusus perangkat petugas yang login, serta verifikasi keaslian aplikasi melalui Google Play Integrity."
        ),
        footerNote = "Mohon tidak memotret wajah korban atau pelat kendaraan, dan jangan menuliskan nama, NIK, atau nomor telepon korban pada kolom deskripsi Laporan Warga."
    ),
    LegalClauseSection(
        number = "Pasal 3",
        title = "Tujuan Penggunaan Data",
        bullets = listOf(
            "Menyampaikan peringatan dini, informasi cuaca, gempa, titik panas, dan potensi bencana sesuai wilayah Anda.",
            "Menerima, memverifikasi kebenaran, dan menindaklanjuti laporan kejadian bencana, termasuk mencegah laporan palsu (hoaks).",
            "Mengelola koordinasi, verifikasi, penugasan, dan penerbitan Surat Tugas resmi bagi relawan kebencanaan terdaftar.",
            "Mendukung penanganan darurat, evakuasi, pelaporan harian Pusdalops, dan pemulihan pasca bencana.",
            "Menjaga keamanan akses, keandalan sistem, serta menyusun statistik kebencanaan dalam bentuk anonim/agregat."
        ),
        footerNote = "Data pribadi Anda TIDAK PERNAH digunakan untuk iklan, pemasaran, atau kepentingan komersial."
    ),
    LegalClauseSection(
        number = "Pasal 3A",
        title = "Metode Pembuktian & Verifikasi Laporan Warga",
        paragraphs = listOf(
            "Untuk membuktikan ada atau tidaknya suatu peristiwa kejadian (kebenaran laporan dari warga) tanpa mewajibkan nomor telepon atau email pelapor, serta mempertimbangkan cakupan luasan wilayah Kabupaten Mukomuko, jarak tempuh, ketersediaan SDM petugas, dan efisiensi anggaran daerah sebelum tindak lanjut ke lapangan, BPBD Kabupaten Mukomuko melalui Petugas Piket Resmi Pusdalops-PB melakukan:"
        ),
        bullets = listOf(
            "Mencocokkan titik koordinat GPS yang dikunci pelapor dengan wilayah kejadian.",
            "Memeriksa foto kamera langsung yang memuat stempel koordinat GPS dan waktu.",
            "Melakukan pemeriksaan silang (cross-check / pembuktian terbalik) atas laporan dimaksud kepada Kepala Desa, Lurah, dan/atau Camat setempat melalui telepon serta fasilitas komunikasi resmi lainnya selaku penanggung jawab dan pemangku kepentingan wilayah secara administratif dalam Kabupaten Mukomuko."
        ),
        footerNote = "Laporan yang tidak dapat dibuktikan atau diverifikasi tidak ditindaklanjuti dengan pengerahan lapangan dan dapat dihapus. Pemalsuan lokasi (fake GPS) atau foto dilarang."
    ),
    LegalClauseSection(
        number = "Pasal 3B",
        title = "Pelacakan Status dengan ID Laporan",
        paragraphs = listOf(
            "Setelah laporan terkirim, Anda memperoleh ID Laporan (format LAP-MMK-...) untuk memantau status melalui fitur Lacak ID. Simpan ID tersebut dan jangan bagikan kepada pihak yang tidak berkepentingan.",
            "Demi melindungi privasi, tampilan Lacak ID untuk umum hanya menampilkan ID Laporan, jenis bencana, wilayah desa/kecamatan, 4 tahapan status (Pending, Diproses, Divalidasi, Selesai), waktu perubahan status, serta petugas penangan—tanpa menampilkan foto bukti maupun koordinat GPS presisi kepada publik."
        )
    ),
    LegalClauseSection(
        number = "Pasal 4",
        title = "Dasar Pemrosesan Data (Pasal 20 UU PDP)",
        paragraphs = listOf(
            "Pemrosesan data pribadi dilakukan berdasarkan satu atau lebih dasar yang sah menurut UU No. 27 Tahun 2022, yaitu: (1) persetujuan eksplisit Anda; (2) pemenuhan kewajiban hukum; (3) pelaksanaan tugas dalam rangka kepentingan umum dan pelayanan publik penanggulangan bencana; serta (4) pemenuhan perlindungan kepentingan vital keselamatan jiwa dalam keadaan darurat."
        )
    ),
    LegalClauseSection(
        number = "Pasal 5",
        title = "Pengungkapan kepada Pihak Lain & Transfer Data Cloud",
        bullets = listOf(
            "Kepala Desa, Lurah, dan Camat setempat selaku penanggung jawab dan pemangku kepentingan wilayah administratif di Kabupaten Mukomuko: melalui telepon maupun fasilitas komunikasi resmi lainnya, sebatas informasi yang diperlukan untuk cross-check pembuktian kejadian (koordinat, foto lokasi GPS, waktu, dan jenis kejadian) tanpa membagikan identitas pelapor.",
            "Instansi penanggulangan bencana terkait (BNPB, BMKG, TNI/Polri, Basarnas, Dinas Kesehatan, Dinas Sosial, dan perangkat daerah terkait) sebatas kebutuhan penanganan darurat.",
            "Penyedia layanan teknis infrastruktur: Google Firebase (Cloud Firestore untuk basis data cloud, Firebase Authentication, Firebase Cloud Messaging, dan Google Play Integrity) serta OpenStreetMap (peta dasar) yang terikat standar kerahasiaan dan keamanan industri.",
            "Pihak lain apabila diwajibkan oleh peraturan perundang-undangan atau perintah resmi yang sah."
        ),
        footerNote = "Transfer Data Lintas Negara (Pasal 56 UU PDP): Karena penyimpanan cloud menggunakan infrastruktur Google Firebase, data dapat diproses pada pusat data Google Cloud di luar wilayah Indonesia dengan perlindungan enkripsi transmisi HTTPS/TLS dan kontrol akses berbasis peran."
    ),
    LegalClauseSection(
        number = "Pasal 6",
        title = "Keamanan Data & Penanganan Insiden",
        paragraphs = listOf(
            "Kami menerapkan langkah teknis dan organisasi yang wajar, meliputi enkripsi saat transmisi (HTTPS/TLS), pembatasan hak akses berbasis peran (Manajer, Pengawas, Operator), enkripsi hash PIN petugas, pencatatan log audit, dan pencadangan data.",
            "Tidak ada sistem elektronik yang sepenuhnya bebas risiko. Apabila terjadi kegagalan pelindungan data pribadi, BPBD Kabupaten Mukomuko akan menyampaikan pemberitahuan tertulis kepada pengguna terdampak dan pihak berwenang paling lambat 3 × 24 jam sesuai ketentuan UU PDP."
        )
    ),
    LegalClauseSection(
        number = "Pasal 7",
        title = "Masa Penyimpanan dan Penghapusan Data",
        bullets = listOf(
            "Data akun petugas dan relawan disimpan selama akun/penugasan aktif atau hingga diajukan penonaktifan/penghapusan.",
            "Data laporan kejadian bencana dan arsip kebencanaan disimpan selama 5 (lima) tahun untuk keperluan penanganan, evaluasi, pertanggungjawaban publik, dan kearsipan sesuai ketentuan kearsipan pemerintah daerah.",
            "Setelah masa retensi berakhir, data pribadi akan dihapus secara permanen atau dianonimkan untuk keperluan statistik dan kajian sejarah kebencanaan."
        )
    ),
    LegalClauseSection(
        number = "Pasal 8",
        title = "Hak Anda sebagai Subjek Data (UU PDP)",
        bullets = listOf(
            "Memperoleh informasi kejelasan identitas, dasar hukum, dan tujuan pemrosesan data Anda.",
            "Melengkapi, memperbarui, dan/atau memperbaiki kesalahan atau ketidakakuratan data pribadi.",
            "Mengakses dan memperoleh salinan data pribadi Anda.",
            "Mengakhiri pemrosesan, menghapus, dan/atau memusnahkan data pribadi Anda.",
            "Menarik kembali persetujuan pemrosesan data pribadi.",
            "Mengajukan keberatan atau menunda/membatasi pemrosesan secara proporsional.",
            "Menggugat dan menerima ganti rugi atas pelanggaran pemrosesan data pribadi sesuai peraturan perundang-undangan."
        ),
        footerNote = "Ajukan permohonan melalui Email $BPBD_OFFICIAL_EMAIL, Call Center 117, atau langsung ke Kantor BPBD Kab. Mukomuko. Kami menanggapi permohonan dalam waktu paling lambat 3 × 24 jam (3 hari kerja) sesuai UU PDP, sepanjang tidak dibatasi kewajiban hukum atau kepentingan darurat."
    ),
    LegalClauseSection(
        number = "Pasal 9",
        title = "Izin Perangkat",
        bullets = listOf(
            "Izin Lokasi (GPS): Digunakan hanya saat aplikasi aktif untuk menampilkan info wilayah Anda, peta rawan, dan mengunci koordinat saat melapor (bukan pelacakan terus-menerus di latar belakang).",
            "Izin Kamera: Digunakan untuk mengambil foto kejadian langsung berstempel GPS pada Laporan Warga serta foto registrasi relawan.",
            "Izin Notifikasi: Digunakan untuk peringatan dini gempa/cuaca ekstrem dan notifikasi laporan masuk bagi petugas Pusdalops.",
            "Penyimpanan Lokal: Aplikasi tidak meminta izin membaca seluruh galeri/memori perangkat Anda; akses penyimpanan lokal hanya digunakan saat Anda mengunduh atau membagikan dokumen PDF laporan atau Surat Tugas."
        ),
        footerNote = "Anda dapat mengubah izin kapan saja melalui Pengaturan perangkat Android Anda."
    ),
    LegalClauseSection(
        number = "Pasal 10",
        title = "Pelindungan Anak",
        paragraphs = listOf(
            "Aplikasi informasi kebencanaan ini dapat diakses masyarakat umum, namun pengiriman laporan kejadian maupun pendaftaran relawan oleh pengguna di bawah usia 18 (delapan belas) tahun wajib dilakukan dengan pendampingan dan persetujuan orang tua atau wali serta mengutamakan kepentingan terbaik anak."
        )
    ),
    LegalClauseSection(
        number = "Pasal 11",
        title = "Perubahan Kebijakan Privasi",
        paragraphs = listOf(
            "Kebijakan Privasi ini dapat diperbarui sewaktu-waktu mengikuti pembaruan fitur aplikasi atau aturan pelaksana UU PDP. Perubahan penting akan diberitahukan secara jelas melalui aplikasi."
        )
    ),
    LegalClauseSection(
        number = "Pasal 12",
        title = "Kontak Pelindungan Data",
        paragraphs = listOf(
            "BPBD Kabupaten Mukomuko | $BPBD_OFFICIAL_ADDRESS | Email: $BPBD_OFFICIAL_EMAIL | Call Center Kebencanaan: 117 | Nomor Tunggal Panggilan Darurat: 112 (Diskominfo Kab. Mukomuko)."
        )
    )
)
