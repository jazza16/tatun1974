package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.DeviceSecuritySession
import com.example.security.OfficerLoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cachedSecurityDateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID"))
}

private fun formatSecurityTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "-"
    return try {
        cachedSecurityDateFormat.get()?.format(Date(timestampMs)) ?: timestampMs.toString()
    } catch (_: Exception) {
        timestampMs.toString()
    }
}

private fun formatLoginMethodLabel(method: String): String = when (method.uppercase()) {
    "PIN_CLOUD" -> "PIN Cloud"
    "BIOMETRIK" -> "Biometrik"
    "BIOMETRIK_SPLASH" -> "Biometrik Splash"
    "VERIFIKASI_ADMIN" -> "Verifikasi Admin"
    "SYNC_SESI_AKTIF" -> "Sinkron Sesi"
    "PLAY_INTEGRITY" -> "Play Integrity"
    "UBAH_PIN" -> "Ubah PIN"
    "ADMIN_OTORISASI" -> "Otorisasi Admin"
    else -> method.replace("_", " ").ifBlank { "PIN Cloud" }
}

private fun formatTokenSourceLabel(source: String): String = when (source.uppercase()) {
    "FIRESTORE_SESSION" -> "Sesi Terverifikasi Server (Firestore)"
    "PLAY_INTEGRITY_CLASSIC" -> "Play Integrity Classic (Hardware)"
    "FIREBASE_APP_CHECK" -> "Firebase App Check"
    "EMULATOR_SANDBOX_FALLBACK" -> "Emulator Sandbox Fallback"
    "LOCAL_FALLBACK" -> "Fallback Integritas Lokal"
    "DEVICE_AUDIT_HASH" -> "Digest Audit Perangkat"
    "LOCAL_AUDIT_RECORD" -> "Sinkron Log Audit Lokal"
    else -> source.replace("_", " ").ifBlank { "Tidak Diketahui" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayIntegritySecuritySessionsCard(
    securitySessions: List<DeviceSecuritySession>,
    loginActivities: List<OfficerLoginActivity>,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onVerifySecuritySession: (DeviceSecuritySession, Boolean, String) -> Unit,
    onRevokeSecuritySession: (DeviceSecuritySession) -> Unit
) {
    val context = LocalContext.current
    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0 = Sesi Perangkat, 1 = Aktivitas Login
    var selectedActivityFilter by remember { mutableStateOf("ALL") } // ALL, SUCCESS, FALLBACK, FAILED
    var sessionPendingRevoke by remember { mutableStateOf<DeviceSecuritySession?>(null) }
    var sessionPendingInspect by remember { mutableStateOf<DeviceSecuritySession?>(null) }
    var activityPendingInspect by remember { mutableStateOf<OfficerLoginActivity?>(null) }

    val filteredActivities = remember(loginActivities, selectedActivityFilter) {
        when (selectedActivityFilter) {
            "SUCCESS" -> loginActivities.filter { it.loginStatus.equals("SUCCESS", ignoreCase = true) }
            "FALLBACK" -> loginActivities.filter { it.loginStatus.equals("WARNING_FALLBACK", ignoreCase = true) }
            "FAILED" -> loginActivities.filter {
                val st = it.loginStatus.uppercase()
                st == "FAILED" || st == "BLOCKED" || st == "REVOKED"
            }
            else -> loginActivities
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_play_integrity_sessions")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF15803D).copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Keamanan Perangkat Play Integrity Classic",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keamanan Perangkat & Sesi (Play Integrity Classic)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sinkronisasi seluruh aktivitas login petugas, keaslian hardware & token",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF15803D).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .testTag("btn_sync_security_sessions")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF15803D)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sinkronkan Aktivitas Login",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "Sinkron..." else "Sinkron",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary badges row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "${securitySessions.size} Perangkat / Sesi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "${loginActivities.size} Aktivitas Login",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                val verifiedCount = securitySessions.count { it.adminVerified }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (verifiedCount > 0) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "$verifiedCount Terverifikasi Admin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (verifiedCount > 0) Color(0xFF047857) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                val isFirestoreActive = securitySessions.isNotEmpty() || loginActivities.isNotEmpty()
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFirestoreActive) Color(0xFFECFDF5) else Color(0xFFF1F5F9)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (isFirestoreActive) Color(0xFF047857) else Color(0xFF475569),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFirestoreActive) "Cloud Firestore Aktif" else "Siap Sinkron Cloud",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFirestoreActive) Color(0xFF047857) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Tabs: Sesi Perangkat vs Aktivitas Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedSectionTab == 0) Color(0xFF15803D) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clickable { selectedSectionTab = 0 }
                        .testTag("tab_security_sessions_devices")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (selectedSectionTab == 0) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sesi Perangkat (${securitySessions.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSectionTab == 0) Color.White else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedSectionTab == 1) Color(0xFF1D4ED8) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clickable { selectedSectionTab = 1 }
                        .testTag("tab_security_login_activities")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (selectedSectionTab == 1) Color.White else Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aktivitas Login (${loginActivities.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSectionTab == 1) Color.White else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSectionTab == 0) {
                // TAB 0: SESI PERANGKAT
                if (securitySessions.isEmpty()) {
                    EmptySecurityStateBox(
                        message = "Belum ada sesi Play Integrity tersimpan di backend Firestore.\nPetugas akan divalidasi dan dicatat otomatis saat login PIN atau Biometrik."
                    )
                } else {
                    var showAllSessions by remember { mutableStateOf(false) }
                    val visibleSessions = remember(securitySessions, showAllSessions) {
                        if (showAllSessions) securitySessions else securitySessions.take(15)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleSessions.forEach { session ->
                            DeviceSecuritySessionItemCard(
                                session = session,
                                onInspectClick = { sessionPendingInspect = session },
                                onRevokeClick = { sessionPendingRevoke = session }
                            )
                        }
                        if (!showAllSessions && securitySessions.size > 15) {
                            OutlinedButton(
                                onClick = { showAllSessions = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_show_all_security_sessions")
                            ) {
                                Text(
                                    text = "Tampilkan semua (${securitySessions.size} perangkat)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: AKTIVITAS LOGIN PETUGAS (CHRONOLOGICAL STREAM)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "ALL" to "Semua (${loginActivities.size})",
                        "SUCCESS" to "Berhasil",
                        "FALLBACK" to "Fallback",
                        "FAILED" to "Gagal/Blokir"
                    ).forEach { (key, label) ->
                        val active = selectedActivityFilter == key
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable { selectedActivityFilter = key }
                                .testTag("filter_login_activity_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredActivities.isEmpty()) {
                    EmptySecurityStateBox(
                        message = "Belum ada catatan aktivitas login pada kategori ini.\nSeluruh login PIN, Biometrik, gagal login, dan verifikasi admin akan tersinkron otomatis di sini."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredActivities.take(30).forEach { activity ->
                            LoginActivityItemCard(
                                activity = activity,
                                onClick = { activityPendingInspect = activity }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Inspeksi Hardware & Token Play Integrity untuk Sesi Perangkat
    sessionPendingInspect?.let { targetSession ->
        val officerHistory = remember(targetSession.officerNip, loginActivities) {
            loginActivities.filter {
                it.officerNip.equals(targetSession.officerNip, ignoreCase = true)
            }.take(10)
        }
        DeviceSecurityInspectionDialog(
            session = targetSession,
            officerActivities = officerHistory,
            onDismiss = { sessionPendingInspect = null },
            onApproveVerify = { note ->
                sessionPendingInspect = null
                onVerifySecuritySession(targetSession, true, note)
            },
            onBlockSession = { note ->
                sessionPendingInspect = null
                onVerifySecuritySession(targetSession, false, note)
            },
            onRevokeDelete = {
                sessionPendingInspect = null
                sessionPendingRevoke = targetSession
            },
            onCopyProof = { text ->
                copyAuditProofToClipboard(context, "Bukti_Play_Integrity_${targetSession.officerNip}", text)
            }
        )
    }

    // Dialog Detail Inspeksi untuk 1 Aktivitas Login
    activityPendingInspect?.let { targetActivity ->
        val matchedSession = remember(targetActivity.officerNip, targetActivity.activityId, securitySessions) {
            securitySessions.firstOrNull {
                it.officerNip.equals(targetActivity.officerNip, ignoreCase = true)
            } ?: DeviceSecuritySession(
                sessionId = targetActivity.sessionId.ifBlank { "sess_${targetActivity.officerNip}" },
                officerNip = targetActivity.officerNip,
                officerName = targetActivity.officerName,
                officerRole = targetActivity.officerRole,
                integrityToken = targetActivity.tokenHash,
                tokenHash = targetActivity.tokenHash,
                classicNonce = targetActivity.classicNonce,
                verdict = targetActivity.verdict,
                deviceVerdict = targetActivity.deviceVerdict,
                deviceId = targetActivity.deviceId,
                deviceModel = targetActivity.deviceModel,
                osVersion = targetActivity.osVersion,
                sdkInt = targetActivity.sdkInt,
                isVerified = targetActivity.accessGranted,
                accessGranted = targetActivity.accessGranted,
                timestamp = targetActivity.timestamp,
                tokenLength = targetActivity.tokenLength,
                tokenSource = targetActivity.tokenSource,
                deviceBrand = targetActivity.deviceBrand,
                deviceManufacturer = targetActivity.deviceBrand,
                deviceHardware = targetActivity.deviceHardware,
                deviceFingerprintHash = targetActivity.deviceFingerprintHash,
                isEmulator = targetActivity.isEmulator,
                appVersion = targetActivity.appVersion,
                loginMethod = targetActivity.loginMethod,
                loginStatus = targetActivity.loginStatus,
                loginAction = targetActivity.action,
                loginCount = 1,
                lastActivityDetail = targetActivity.detail
            )
        }
        LoginActivityDetailDialog(
            activity = targetActivity,
            linkedSession = matchedSession,
            onDismiss = { activityPendingInspect = null },
            onOpenOfficerSession = { sess ->
                activityPendingInspect = null
                sessionPendingInspect = sess
            },
            onCopyProof = { text ->
                copyAuditProofToClipboard(context, "Bukti_Login_${targetActivity.officerNip}", text)
            }
        )
    }

    // Konfirmasi Cabut Akses Perangkat
    sessionPendingRevoke?.let { targetSession ->
        AlertDialog(
            onDismissRequest = { sessionPendingRevoke = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                )
            },
            title = {
                Text(
                    text = "Cabut Akses Perangkat?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Yakin ingin mencabut akses perangkat ini?\n\n" +
                        "Petugas: ${targetSession.officerName} (NIP: ${targetSession.officerNip})\n" +
                        "Perangkat: ${targetSession.deviceModel}\n\n" +
                        "Sesi perangkat akan dinonaktifkan/dihapus dari Firestore dan akses petugas ke Ruang Kerja dicabut.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionPendingRevoke = null
                        onRevokeSecuritySession(targetSession)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_revoke_security_session")
                ) {
                    Text("Cabut Akses", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { sessionPendingRevoke = null },
                    modifier = Modifier.testTag("btn_cancel_revoke_security_session")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun EmptySecurityStateBox(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceSecuritySessionItemCard(
    session: DeviceSecuritySession,
    onInspectClick: () -> Unit,
    onRevokeClick: () -> Unit
) {
    val isStrongIntegrity = (session.verdict == "MEETS_STRONG_INTEGRITY" ||
        session.verdict == com.example.security.SecurityManager.VERDICT_SERVER_VERIFIED) && !session.isEmulator
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (session.accessGranted) Color(0xFFF8FAFC) else Color(0xFFFEF2F2),
        border = BorderStroke(
            1.dp,
            when {
                !session.accessGranted -> Color(0xFFFECACA)
                session.adminVerified -> Color(0xFF86EFAC)
                else -> Color(0xFFE2E8F0)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspectClick() }
            .testTag("card_security_session_${session.officerNip}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "${session.officerName.ifBlank { "Petugas Pusdalops" }} (${session.officerRole.ifBlank { "Petugas" }})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "NIP: ${session.officerNip} • ${session.deviceModel.ifBlank { "Perangkat Android" }}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Login: ${formatLoginMethodLabel(session.loginMethod)} (${session.loginCount}x) • ${formatSecurityTimestamp(session.timestamp)}",
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (session.accessGranted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (session.accessGranted) "AKSES DIIZINKAN" else "AKSES DICABUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (session.accessGranted) Color(0xFF15803D) else Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hardware & Admin Verification badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (session.isEmulator) Color(0xFFFFEDD5) else Color(0xFFE0F2FE)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isEmulator) Icons.Default.DeveloperMode else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (session.isEmulator) Color(0xFFC2410C) else Color(0xFF0369A1),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (session.isEmulator) "EMULATOR / SANDBOX" else "HARDWARE FISIK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.isEmulator) Color(0xFFC2410C) else Color(0xFF0369A1)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (session.adminVerified) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (session.adminVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (session.adminVerified) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (session.adminVerified) "TERVERIFIKASI ADMIN" else "BELUM DIVERIFIKASI ADMIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (session.adminVerified) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Play Integrity Classic Token & Nonce Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isStrongIntegrity) Color(0xFFECFDF5) else Color(0xFFEFF6FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Verdict: ${session.verdict}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isStrongIntegrity) Color(0xFF047857) else Color(0xFF1D4ED8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Nonce: ${session.classicNonce.take(12).ifBlank { "-" }}...",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sumber: ${formatTokenSourceLabel(session.tokenSource)} (${session.tokenLength} char) • Hash: ${session.tokenHash.take(16).ifBlank { "-" }}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "HW: ${session.osVersion} • ID: ${session.deviceId.take(14).ifBlank { "-" }} • FP: ${session.deviceFingerprintHash.take(12).ifBlank { "-" }}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (session.adminVerified && session.adminVerifiedBy.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diverifikasi oleh ${session.adminVerifiedBy} (${formatSecurityTimestamp(session.adminVerifiedAt)})",
                    fontSize = 10.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onInspectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .testTag("btn_inspect_security_session_${session.officerNip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Inspeksi & Verifikasi Perangkat",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Inspeksi & Verifikasi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onRevokeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .wrapContentWidth()
                        .testTag("btn_revoke_security_session_${session.officerNip}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cabut Akses Perangkat",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cabut Akses",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class LoginActivityVisualStyle(
    val bgColor: Color,
    val borderColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val badgeLabel: String,
    val iconVec: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginActivityItemCard(
    activity: OfficerLoginActivity,
    onClick: () -> Unit
) {
    val statusUpper = activity.loginStatus.uppercase()
    val style = when (statusUpper) {
        "SUCCESS" -> LoginActivityVisualStyle(
            Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFFDCFCE7), Color(0xFF15803D), "BERHASIL", Icons.Default.CheckCircle
        )
        "WARNING_FALLBACK" -> LoginActivityVisualStyle(
            Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFFEF3C7), Color(0xFFB45309), "FALLBACK", Icons.Default.Warning
        )
        "BLOCKED", "REVOKED" -> LoginActivityVisualStyle(
            Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFFFEE2E2), Color(0xFFDC2626),
            if (statusUpper == "REVOKED") "DICABUT" else "DIBLOKIR", Icons.Default.Block
        )
        else -> LoginActivityVisualStyle(
            Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFFFEE2E2), Color(0xFFB91C1C), "GAGAL", Icons.Default.ErrorOutline
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = style.bgColor,
        border = BorderStroke(1.dp, style.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_login_activity_${activity.activityId}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    Icon(
                        imageVector = style.iconVec,
                        contentDescription = null,
                        tint = style.badgeText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.officerName.ifBlank { "Petugas (${activity.officerNip})" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NIP: ${activity.officerNip} • ${formatLoginMethodLabel(activity.loginMethod)}",
                            fontSize = 10.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = style.badgeBg
                ) {
                    Text(
                        text = style.badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = style.badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (activity.detail.isNotBlank()) {
                Text(
                    text = activity.detail,
                    fontSize = 11.sp,
                    color = Color(0xFF334155),
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${activity.deviceModel.ifBlank { "Perangkat" }} (${if (activity.isEmulator) "Emulator" else "Fisik"}) • ${activity.verdict}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatSecurityTimestamp(activity.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun DeviceSecurityInspectionDialog(
    session: DeviceSecuritySession,
    officerActivities: List<OfficerLoginActivity>,
    onDismiss: () -> Unit,
    onApproveVerify: (String) -> Unit,
    onBlockSession: (String) -> Unit,
    onRevokeDelete: () -> Unit,
    onCopyProof: (String) -> Unit
) {
    var verificationNote by remember(session.sessionId) {
        mutableStateOf(session.adminVerificationNote)
    }

    val auditProofText = remember(session) {
        buildString {
            appendLine("=== BUKTI VERIFIKASI PLAY INTEGRITY CLASSIC (SIPAGAR KITA BPBD) ===")
            appendLine("Petugas        : ${session.officerName} (${session.officerRole})")
            appendLine("NIP            : ${session.officerNip}")
            appendLine("Session ID     : ${session.sessionId}")
            appendLine("Metode Login   : ${formatLoginMethodLabel(session.loginMethod)} (${session.loginCount}x)")
            appendLine("Status Login   : ${session.loginStatus} (${session.loginAction})")
            appendLine("Waktu Login    : ${formatSecurityTimestamp(session.timestamp)}")
            appendLine("--- KEASLIAN HARDWARE PERANGKAT ---")
            appendLine("Tipe Lingkungan: ${if (session.isEmulator) "EMULATOR / SANDBOX" else "PERANGKAT FISIK ASLI"}")
            appendLine("Model          : ${session.deviceModel}")
            appendLine("Brand/Pabrikan : ${session.deviceBrand} / ${session.deviceManufacturer}")
            appendLine("Hardware Board : ${session.deviceHardware}")
            appendLine("OS / SDK       : ${session.osVersion} (API ${session.sdkInt})")
            appendLine("Android ID     : ${session.deviceId}")
            appendLine("FingerprintSHA : ${session.deviceFingerprintHash}")
            appendLine("Versi Aplikasi : ${session.appVersion}")
            appendLine("--- TOKEN & KRIPTOGRAFI PLAY INTEGRITY CLASSIC ---")
            appendLine("Verdict Utama  : ${session.verdict}")
            appendLine("Device Verdict : ${session.deviceVerdict}")
            appendLine("Sumber Token   : ${session.tokenSource} (${session.tokenLength} karakter)")
            appendLine("Token Hash     : ${session.tokenHash}")
            appendLine("Classic Nonce  : ${session.classicNonce}")
            appendLine("Status Akses   : ${if (session.accessGranted) "DIIZINKAN" else "DICABUT/DIBLOKIR"}")
            appendLine("Verifikasi Adm : ${if (session.adminVerified) "TERVERIFIKASI oleh ${session.adminVerifiedBy}" else "BELUM DIVERIFIKASI"}")
        }
    }

    BalancedModalDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = 14.dp,
        extraVerticalMargin = 14.dp
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_device_security_inspection")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Inspeksi Hardware & Token",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Play Integrity API Classic • Verifikasi Admin",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onCopyProof(auditProofText) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.heightIn(min = 34.dp).testTag("btn_copy_security_audit_proof")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Bukti Audit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // 1. Identitas & Status Sesi
                InspectionSectionBlock(
                    title = "1. Identitas Petugas & Aktivitas Login",
                    icon = Icons.Default.Fingerprint,
                    accent = Color(0xFF15803D)
                ) {
                    InspectionKeyValueRow("Nama Petugas", session.officerName.ifBlank { "-" })
                    InspectionKeyValueRow("NIP / ID", session.officerNip)
                    InspectionKeyValueRow("Jabatan", session.officerRole.ifBlank { "-" })
                    InspectionKeyValueRow("Metode Login", "${formatLoginMethodLabel(session.loginMethod)} (${session.loginCount}x)")
                    InspectionKeyValueRow("Status Sesi", if (session.accessGranted) "AKSES DIIZINKAN" else "AKSES DIBLOKIR / DICABUT")
                    InspectionKeyValueRow("Waktu Login", formatSecurityTimestamp(session.timestamp))
                    if (session.lastActivityDetail.isNotBlank()) {
                        InspectionKeyValueRow("Keterangan", session.lastActivityDetail)
                    }
                }

                // 2. Keaslian Hardware Perangkat
                InspectionSectionBlock(
                    title = "2. Keaslian Hardware Perangkat",
                    icon = Icons.Default.PhoneAndroid,
                    accent = if (session.isEmulator) Color(0xFFC2410C) else Color(0xFF0369A1)
                ) {
                    InspectionKeyValueRow(
                        "Tipe Perangkat",
                        if (session.isEmulator) "EMULATOR / SANDBOX (Perlu Pengawasan)" else "PERANGKAT FISIK ASLI"
                    )
                    InspectionKeyValueRow("Model Perangkat", session.deviceModel.ifBlank { "-" })
                    InspectionKeyValueRow("Brand / Pabrikan", "${session.deviceBrand.ifBlank { "-" }} / ${session.deviceManufacturer.ifBlank { "-" }}")
                    InspectionKeyValueRow("Hardware / Board", session.deviceHardware.ifBlank { "-" })
                    InspectionKeyValueRow("Sistem Operasi", "${session.osVersion} (API ${session.sdkInt})")
                    InspectionKeyValueRow("Android Device ID", session.deviceId.ifBlank { "-" }, mono = true)
                    InspectionKeyValueRow("Fingerprint SHA-256", session.deviceFingerprintHash.ifBlank { "-" }, mono = true)
                    InspectionKeyValueRow("Versi Aplikasi", session.appVersion.ifBlank { "-" })
                }

                // 3. Token & Kriptografi Play Integrity Classic
                InspectionSectionBlock(
                    title = "3. Token Play Integrity Classic & Nonce",
                    icon = Icons.Default.Key,
                    accent = Color(0xFF1D4ED8)
                ) {
                    InspectionKeyValueRow("Verdict Utama", session.verdict)
                    InspectionKeyValueRow("Device Verdict", session.deviceVerdict)
                    InspectionKeyValueRow("Sumber Token", "${formatTokenSourceLabel(session.tokenSource)} (${session.tokenLength} char)")
                    InspectionKeyValueRow("Token Hash", session.tokenHash.ifBlank { "-" }, mono = true)
                    InspectionKeyValueRow("Classic Nonce", session.classicNonce.ifBlank { "-" }, mono = true)
                    InspectionKeyValueRow(
                        "Cuplikan Token",
                        if (session.integrityToken.length > 36) "${session.integrityToken.take(36)}..." else session.integrityToken.ifBlank { "-" },
                        mono = true
                    )
                    InspectionKeyValueRow("Session ID", session.sessionId, mono = true)
                }

                // 4. Riwayat Aktivitas Login Petugas Ini
                if (officerActivities.isNotEmpty()) {
                    InspectionSectionBlock(
                        title = "4. Riwayat Login Petugas (${officerActivities.size} Terakhir)",
                        icon = Icons.Default.History,
                        accent = MaterialTheme.colorScheme.primary
                    ) {
                        officerActivities.take(5).forEach { act ->
                            Text(
                                text = "• [${act.loginStatus}] ${formatLoginMethodLabel(act.loginMethod)} — ${formatSecurityTimestamp(act.timestamp)} (${act.deviceModel})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Catatan Verifikasi Admin
                OutlinedTextField(
                    value = verificationNote,
                    onValueChange = { verificationNote = it },
                    label = { Text("Catatan Verifikasi Admin (Opsional)", fontSize = 12.sp) },
                    placeholder = { Text("Misal: Perangkat dinas terkonfirmasi asli", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_verification_note")
                )

                // Admin Decision Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApproveVerify(verificationNote.trim()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("btn_admin_verify_approve_session")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifikasi & Izinkan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { onBlockSession(verificationNote.trim()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("btn_admin_block_session")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Blokir Akses", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRevokeDelete,
                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cabut & Hapus", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                    ) {
                        Text("Tutup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginActivityDetailDialog(
    activity: OfficerLoginActivity,
    linkedSession: DeviceSecuritySession?,
    onDismiss: () -> Unit,
    onOpenOfficerSession: (DeviceSecuritySession) -> Unit,
    onCopyProof: (String) -> Unit
) {
    val proof = remember(activity) {
        buildString {
            appendLine("=== BUKTI AKTIVITAS LOGIN PETUGAS (PLAY INTEGRITY CLASSIC) ===")
            appendLine("Activity ID    : ${activity.activityId}")
            appendLine("Petugas        : ${activity.officerName} (${activity.officerRole})")
            appendLine("NIP            : ${activity.officerNip}")
            appendLine("Metode / Aksi  : ${formatLoginMethodLabel(activity.loginMethod)} / ${activity.action}")
            appendLine("Status         : ${activity.loginStatus}")
            appendLine("Waktu          : ${formatSecurityTimestamp(activity.timestamp)}")
            appendLine("Detail         : ${activity.detail}")
            appendLine("Perangkat      : ${activity.deviceModel} (${if (activity.isEmulator) "EMULATOR" else "FISIK"})")
            appendLine("Hardware       : ${activity.deviceBrand} / ${activity.deviceHardware}")
            appendLine("OS / SDK       : ${activity.osVersion} (API ${activity.sdkInt})")
            appendLine("Android ID     : ${activity.deviceId}")
            appendLine("FingerprintSHA : ${activity.deviceFingerprintHash}")
            appendLine("Verdict        : ${activity.verdict} (${activity.deviceVerdict})")
            appendLine("Sumber Token   : ${activity.tokenSource} (${activity.tokenLength} char)")
            appendLine("Token Hash     : ${activity.tokenHash}")
            appendLine("Classic Nonce  : ${activity.classicNonce}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF3B82F6)
            )
        },
        title = {
            Text(
                text = "Detail Aktivitas Login & Token",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InspectionKeyValueRow("Petugas", "${activity.officerName} (${activity.officerNip})")
                InspectionKeyValueRow("Metode / Aksi", "${formatLoginMethodLabel(activity.loginMethod)} • ${activity.action}")
                InspectionKeyValueRow("Status", activity.loginStatus)
                InspectionKeyValueRow("Waktu", formatSecurityTimestamp(activity.timestamp))
                InspectionKeyValueRow("Lingkungan", if (activity.isEmulator) "EMULATOR / SANDBOX" else "HARDWARE FISIK")
                InspectionKeyValueRow("Perangkat", "${activity.deviceModel} (${activity.deviceHardware.ifBlank { "-" }})")
                InspectionKeyValueRow("OS / SDK", activity.osVersion)
                InspectionKeyValueRow("Device ID", activity.deviceId, mono = true)
                InspectionKeyValueRow("Fingerprint", activity.deviceFingerprintHash.ifBlank { "-" }, mono = true)
                InspectionKeyValueRow("Verdict", activity.verdict)
                InspectionKeyValueRow("Sumber Token", "${formatTokenSourceLabel(activity.tokenSource)} (${activity.tokenLength} char)")
                InspectionKeyValueRow("Token Hash", activity.tokenHash.ifBlank { "-" }, mono = true)
                InspectionKeyValueRow("Classic Nonce", activity.classicNonce.ifBlank { "-" }, mono = true)
                if (activity.detail.isNotBlank()) {
                    InspectionKeyValueRow("Keterangan", activity.detail)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (linkedSession != null) {
                    Button(
                        onClick = { onOpenOfficerSession(linkedSession) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Verifikasi Sesi", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(onClick = { onCopyProof(proof) }) {
                    Text("Salin Bukti", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup", fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun InspectionSectionBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
            }
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun InspectionKeyValueRow(
    label: String,
    value: String,
    mono: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

private fun copyAuditProofToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Bukti audit Play Integrity berhasil disalin ke clipboard.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Gagal menyalin bukti audit.", Toast.LENGTH_SHORT).show()
    }
}
