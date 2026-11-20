package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.BucketAccessPolicyStatus
import com.example.data.api.SupabaseClient
import com.example.data.api.SupabaseUploadDiagnosticReport
import kotlinx.coroutines.launch

/**
 * Interactive officer diagnostic panel for verifying the Supabase Storage (`reports` bucket)
 * image upload path, JPEG byte transmission integrity (size + SHA-256), and public vs authenticated
 * bucket access policy.
 */
@Composable
fun SupabaseStorageDiagnosticCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<SupabaseUploadDiagnosticReport?>(null) }
    var showRawLog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_supabase_storage_diagnostic")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val statusTint = when (report?.bucketPolicyStatus) {
                        BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED -> Color(0xFF059669)
                        BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY -> Color(0xFFD97706)
                        null -> Color(0xFF0284C7)
                        else -> Color(0xFFDC2626)
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusTint.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (report?.bucketPolicyStatus) {
                                    BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED -> Icons.Default.CloudDone
                                    BucketAccessPolicyStatus.NOT_CONFIGURED,
                                    BucketAccessPolicyStatus.UPLOAD_FAILED,
                                    BucketAccessPolicyStatus.ACCESS_DENIED_OR_MISCONFIGURED,
                                    BucketAccessPolicyStatus.BYTE_MISMATCH_CORRUPTED -> Icons.Default.CloudOff
                                    else -> Icons.Default.CloudUpload
                                },
                                contentDescription = "Diagnostik Supabase Storage",
                                tint = statusTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Diagnostik Upload Foto Supabase",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verifikasi path bucket 'reports', integritas byte JPEG & policy akses",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isRunning) return@Button
                        isRunning = true
                        coroutineScope.launch {
                            report = SupabaseClient.diagnoseImageUploadPath(
                                reportId = "diag_${System.currentTimeMillis() % 100000}",
                                cleanupProbeObject = true
                            )
                            isRunning = false
                        }
                    },
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_run_supabase_diagnostic")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa Storage...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uji Path & Policy Bucket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (report != null) {
                    OutlinedButton(
                        onClick = {
                            val current = report ?: return@OutlinedButton
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Diagnostik_Supabase_Storage_Reports.txt")
                                putExtra(Intent.EXTRA_TEXT, current.formattedLog)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Log Diagnostik Supabase"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_share_supabase_diagnostic_log")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan Log Diagnostik",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            report?.let { diag ->
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                val badgeColor = when (diag.bucketPolicyStatus) {
                    BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED -> Color(0xFF059669)
                    BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Status Policy: ${diag.bucketPolicyStatus.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Path: ${diag.bucketName}/${diag.objectPath}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Byte Terkirim: ${if (diag.bytesTransmittedVerified) "UTUH (${diag.expectedByteSize} B, SHA-256 cocok)" else "BELUM TERVERIFIKASI (${diag.expectedByteSize} B)"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Public GET: ${diag.publicHttpStatus ?: "-"} (${if (diag.publicAccessAllowed) "IZIN PUBLIK" else "DITOLAK"}) • Auth GET: ${diag.authenticatedHttpStatus ?: "-"} (${if (diag.authenticatedAccessAllowed) "IZIN AUTH" else "DITOLAK"})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = diag.recommendation,
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showRawLog = !showRawLog },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_toggle_supabase_raw_log")
                ) {
                    Text(
                        text = if (showRawLog) "Sembunyikan Log Lengkap (Logcat: SupabaseStorageDiag)" else "Lihat Log Diagnostik Lengkap",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showRawLog) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = diag.formattedLog,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
