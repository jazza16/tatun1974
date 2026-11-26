package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfficerAuthManager
import com.example.security.DeviceSecuritySession
import com.example.security.OfficerLoginActivity
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

/**
 * Diagnostik Sinkronisasi Multi-Perangkat BPBD:
 * Memantau latensi API Firestore, status detak jantung (heartbeat) 5 unit HP pengujian,
 * pemeriksaan integritas checksum (SHA-256), log resolusi konflik, dan akses Database Explorer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SyncDiagnosticsSection(
    latencyMs: Long,
    isSlowNetwork: Boolean,
    activeDevices: List<com.example.data.sync.DeviceHeartbeat>,
    conflictLogs: List<com.example.data.local.SyncConflictLogEntity>,
    integrityResult: com.example.data.sync.IntegrityCheckResult?,
    isCheckingIntegrity: Boolean,
    securitySessions: List<DeviceSecuritySession> = emptyList(),
    loginActivities: List<OfficerLoginActivity> = emptyList(),
    isSyncingSecurity: Boolean = false,
    onSyncSecurityNow: () -> Unit = {},
    onVerifySecuritySession: (DeviceSecuritySession, Boolean, String) -> Unit = { _, _, _ -> },
    onRevokeSecuritySession: (DeviceSecuritySession) -> Unit = {},
    onRunIntegrityCheck: () -> Unit,
    onTestPing: () -> Unit,
    onExportConflictLogsCsv: () -> Unit,
    onOpenDatabaseExplorer: () -> Unit,
    onOpenFormLaporanHarian: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 0. PLAY INTEGRITY CLASSIC BACKEND SECURITY SESSIONS & ALL OFFICER LOGIN ACTIVITIES CARD
        PlayIntegritySecuritySessionsCard(
            securitySessions = securitySessions,
            loginActivities = loginActivities,
            isSyncing = isSyncingSecurity,
            onSyncNow = onSyncSecurityNow,
            onVerifySecuritySession = onVerifySecuritySession,
            onRevokeSecuritySession = onRevokeSecuritySession
        )

        // 0B. SUPABASE STORAGE IMAGE UPLOAD PATH & BUCKET POLICY DIAGNOSTIC CARD
        SupabaseStorageDiagnosticCard()

        // 1. LATENCY METRIC CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_sync_latency")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981)).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pemantauan Latensi Firestore",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Waktu bolak-balik respon API cloud",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSlowNetwork) Color(0xFFDC2626) else Color(0xFF059669),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(
                                text = if (isSlowNetwork) "JARINGAN LAMBAT" else "NORMAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Response Time", fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (latencyMs > 0) "$latencyMs" else "-",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = onTestPing,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .align(Alignment.Bottom)
                            .testTag("btn_test_latency_ping")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Uji Ping Sekarang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. ACTIVE CONNECTED DEVICES (HEARTBEAT TIMELINE)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_active_devices_heartbeat")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detak Jantung Perangkat (${activeDevices.size} HP Aktif)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "5 HP Pengujian",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeDevices.isEmpty()) {
                    Text(
                        text = "Belum ada heartbeat perangkat lain terdeteksi dalam 5 menit terakhir.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeDevices.forEach { dev ->
                            val isGuest = dev.isGuestMode()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isGuest) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isGuest) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = dev.deviceModel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isGuest) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                                            ) {
                                                Text(
                                                    text = if (isGuest) "MODE WARGA" else "PETUGAS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isGuest) Color(0xFFB45309) else Color(0xFF1D4ED8),
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isGuest) {
                                                "Pengunjung / Warga • ID: ${dev.deviceId.take(8)}"
                                            } else {
                                                "Petugas: ${dev.officerName} • ID: ${dev.deviceId.take(8)}"
                                            },
                                            fontSize = 11.sp,
                                            color = if (isGuest) Color(0xFF92400E) else Color(0xFF64748B),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (dev.status == "SLOW_NET") Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = if (dev.status == "SLOW_NET") "SLOW NET" else "ONLINE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dev.status == "SLOW_NET") Color(0xFFB45309) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${dev.latencyMs} ms",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. INTEGRITY CHECK (SHA-256 CHECKSUM COMPARISON)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_checksum_integrity")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pemeriksaan Integritas Data (Checksum)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bandingkan hashing SHA-256 antara Room Database SQLite lokal dan Cloud Firestore untuk mendeteksi rekaman yang terlewat atau tidak konsisten.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (integrityResult != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (integrityResult.isConsistent) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.dp,
                            if (integrityResult.isConsistent) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (integrityResult.isConsistent) "✅ KONSISTEN (100% SINKRON)" else "⚠️ DITEMUKAN PERBEDAAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (integrityResult.isConsistent) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                                Text(
                                    text = "Lokal: ${integrityResult.localCount} | Cloud: ${integrityResult.remoteCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Checksum Lokal: ${integrityResult.localChecksum}\nChecksum Cloud: ${integrityResult.remoteChecksum}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569)
                            )
                            if (integrityResult.discrepancies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tindakan Resolusi Otomatis (${integrityResult.resolvedDiscrepancyCount} laporan diselaraskan):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFB45309)
                                )
                                integrityResult.discrepancies.take(3).forEach { d ->
                                    Text("• $d", fontSize = 11.sp, color = Color(0xFF78350F))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onRunIntegrityCheck,
                    enabled = !isCheckingIntegrity,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_run_integrity_check")
                ) {
                    if (isCheckingIntegrity) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa Checksum...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jalankan Pengecekan Integritas (Checksum)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. CONFLICT RESOLUTION LOGS & TOOLS
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_conflict_logs_section")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Resolusi Konflik (${conflictLogs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Riwayat resolusi otomatis saat dua perangkat petugas mengubah laporan yang sama secara bersamaan.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportConflictLogsCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_export_conflict_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor CSV Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = onOpenDatabaseExplorer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_open_database_explorer")
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Database Explorer", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                val localContext = LocalContext.current
                val officerAuth = remember(localContext) { OfficerAuthManager(localContext) }
                val isOfficerLoggedIn = officerAuth.isOfficerLoggedIn()
                val officerRole = officerAuth.getOfficerRole()
                val hasSpecialAssignment = officerRole.contains("Penugasan Khusus", ignoreCase = true)
                if (
                    isOfficerLoggedIn && (
                        officerRole.contains("Manajer", ignoreCase = true) ||
                        officerRole.contains("Administrasi", ignoreCase = true) ||
                        officerRole.contains("Operator", ignoreCase = true) ||
                        hasSpecialAssignment
                    )
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = {
                            val intent = Intent(localContext, com.example.FormLaporanActivity::class.java)
                            localContext.startActivity(intent)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cardBuatLaporan")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Buat Laporan Harian (Form Resmi)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Buka FormLaporanActivity dengan 4 Tab (Kejadian, Dampak, Log, Simpan)",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuthDialog(
    actionTitle: String,
    officerNip: String,
    officerName: String,
    officerRole: String,
    onDismiss: () -> Unit,
    onAuthorized: (com.example.security.AdminTemporaryAccessToken) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRetryableError by remember { mutableStateOf(false) }

    val executeVerification: () -> Unit = {
        val trimmed = inputPassword.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Masukkan PIN Master Admin terlebih dahulu."
            isRetryableError = false
        } else {
            isVerifying = true
            errorMessage = null
            isRetryableError = false
            coroutineScope.launch {
                val result = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
                    context = context,
                    masterPassword = trimmed,
                    officerNip = officerNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    actionTitle = actionTitle
                )
                isVerifying = false
                when (result) {
                    is com.example.security.AdminVerificationResult.Authorized -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        onAuthorized(result.accessToken)
                    }
                    is com.example.security.AdminVerificationResult.Denied -> {
                        errorMessage = result.message
                        isRetryableError = result.canRetry
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF2F2),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Verifikasi Admin Server-Side",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Otorisasi Master Admin",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            val adminPinKeyboardActions = rememberFormKeyboardActions(onDoneCallback = executeVerification)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Akses ke \"$actionTitle\" diverifikasi langsung ke server Cloud Firestore (app_settings/admin_config). " +
                        "Jika PIN valid, Token Akses Sementara (10 menit) diterbitkan dan setiap percobaan dicatat di Audit Log.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )
                if (officerNip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Petugas: ${officerName.ifBlank { "-" }} (NIP: $officerNip)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = {
                        inputPassword = it
                        errorMessage = null
                        isRetryableError = false
                    },
                    label = { Text("PIN Master Admin", fontSize = 11.sp) },
                    placeholder = { Text("Masukkan PIN Master Admin", fontSize = 11.sp) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = adminPinKeyboardActions,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Sembunyikan PIN" else "Tampilkan PIN",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_master_admin_pin")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRetryableError) Color(0xFFFFFBEB) else Color(0xFFFEF2F2),
                        border = BorderStroke(
                            1.dp,
                            if (isRetryableError) Color(0xFFF59E0B) else Color(0xFFFECACA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_admin_auth_error")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = if (isRetryableError) Color(0xFF92400E) else Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                            if (isRetryableError) {
                                OutlinedButton(
                                    onClick = executeVerification,
                                    enabled = !isVerifying,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_retry_admin_pin")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Coba Lagi verifikasi server",
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Coba Lagi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = executeVerification,
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRetryableError) Color(0xFFEA580C) else Color(0xFF15803D)
                ),
                modifier = Modifier.testTag("btn_verify_master_admin_pin")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Memverifikasi...")
                } else {
                    Text(if (isRetryableError) "Coba Lagi" else "Verifikasi Server")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text("Batal")
            }
        }
    )
}
