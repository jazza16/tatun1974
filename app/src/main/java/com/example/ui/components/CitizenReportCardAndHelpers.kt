package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.OfficerRoles
import com.example.model.CitizenDisasterReport
import com.example.security.UserRole
import com.example.util.GoogleMapsHelper

/**
 * Reusable Dropdown Row matching the clean underlined layout
 */
@Composable
internal fun ReportDropdownField(
    label: String,
    value: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Casualty counter row with [-] and [+]
 */
@Composable
internal fun CasualtyCounterRow(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (count > 0) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = count > 0, onClick = onDecrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = if (count > 0) Color(0xFFDC2626) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$count Orang",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) Color(0xFFDC2626) else Color(0xFF111827),
                modifier = Modifier.wrapContentWidth(),
                textAlign = TextAlign.Center
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF15803D).copy(alpha = 0.15f),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onIncrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Report Card Item inside the History Table view with interactive status buttons,
 * GPS Photo display with watermark, Google Maps navigation, and officer-gated actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CitizenReportItemCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    officerRole: String = OfficerRoles.PETUGAS_JAGA,
    onUpdateStatus: (String) -> Unit,
    onViewPhoto: (String) -> Unit,
    onDeleteReport: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
    val (statusContainer, statusContent, statusLabel) = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Divalidasi")
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Selesai")
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diproses")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = report.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = report.incidentTime,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isSyncedServer = isReportSyncedWithServer(report)
                    val syncIcon = if (isSyncedServer) Icons.Default.CloudDone else Icons.Default.CloudSync
                    val syncColor = if (isSyncedServer) Color(0xFF16A34A) else Color(0xFFD97706)
                    val syncText = if (isSyncedServer) "Synced" else "Menunggu Sync"

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = syncColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, syncColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .testTag("sync_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = syncText,
                                tint = syncColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = syncText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = syncColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusContainer,
                        border = BorderStroke(0.8.dp, statusContent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(statusContent, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.disasterType,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.village}, Kec. ${report.district}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Detail: ${report.incidentLocation}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            if (!report.photoUri.isNullOrBlank()) {
                val cardContext = LocalContext.current
                val resolvedPhotoModel = remember(report.photoUri) {
                    decodeCitizenPhotoModel(report.photoUri)
                }
                val fallbackCardBitmap = rememberCitizenReportPhotoBitmap(report.photoUri, report.id)
                var isPhotoLoadError by remember(report.photoUri) {
                    mutableStateOf(resolvedPhotoModel == null && fallbackCardBitmap == null)
                }
                if (fallbackCardBitmap != null && isPhotoLoadError) {
                    isPhotoLoadError = false
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    color = if (isPhotoLoadError && fallbackCardBitmap == null) Color(0xFFE5E7EB) else Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("citizen_report_photo_thumbnail")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isPhotoLoadError || fallbackCardBitmap != null) { onViewPhoto(report.photoUri) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPhotoLoadError && fallbackCardBitmap == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE5E7EB))
                                    .testTag("photo_error_placeholder")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak dapat dimuat",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Foto tidak dapat dimuat",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                            }
                        } else {
                            val activeBitmap = fallbackCardBitmap ?: (resolvedPhotoModel as? Bitmap)
                            if (activeBitmap != null) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Foto Bukti Laporan Bencana",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(cardContext)
                                        .data(resolvedPhotoModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Bukti Laporan Bencana",
                                    onSuccess = { isPhotoLoadError = false },
                                    onError = {
                                        if (fallbackCardBitmap == null) {
                                            isPhotoLoadError = true
                                        }
                                    },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(topStart = 8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(
                                        text = "📍 %.5f, %.5f".format(report.latitude, report.longitude),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                    Text(
                                        text = "🛡️ FOTO ASLI BPBD MUKOMUKO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF86EFAC)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onViewPhoto(report.photoUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("btn_zoom_report_photo")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.Black.copy(alpha = 0.68f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Perbesar Foto Layar Penuh",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isPhotoLoadError) {
                    Text(
                        text = "🔍 Sentuh foto atau ikon kaca pembesar untuk mode layar penuh",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("placeholder_no_photo")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = "Warga tidak melampirkan foto",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warga tidak melampirkan foto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF3F4F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Lat: %.5f, Lon: %.5f".format(report.latitude, report.longitude),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    if (report.isGpsVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF16A34A)
                        ) {
                            Text(
                                text = "GPS ASLI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasualtyBadge(label = "Meninggal", count = report.casualtiesDead, color = Color(0xFFDC2626))
                CasualtyBadge(label = "Luka", count = report.casualtiesInjured, color = Color(0xFFD97706))
                CasualtyBadge(label = "Hilang", count = report.casualtiesMissing, color = Color(0xFF4B5563))
            }

            if (report.additionalNotes.isNotBlank()) {
                Text(
                    text = "Catatan: ${report.additionalNotes}",
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Dilaporkan oleh: ${report.reporterName}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )

            if (report.handledByOfficerName.isNotBlank() || report.handledByOfficerNip.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Petugas Penangan Aktif",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val officerLabel = buildString {
                            append("Ditangani: ")
                            append(report.handledByOfficerName.ifBlank { "Petugas Pusdalops BPBD" })
                            if (report.handledByOfficerRole.isNotBlank()) {
                                append(" (${report.handledByOfficerRole})")
                            }
                            if (report.handledByOfficerNip.isNotBlank()) {
                                append(" • NIP ${maskNipLast6(report.handledByOfficerNip)}")
                            }
                        }
                        Text(
                            text = officerLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E40AF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            CitizenReportVerticalFeedbackStepper(report = report)

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        GoogleMapsHelper.openNavigationRoute(
                            context = context,
                            destLat = report.latitude,
                            destLon = report.longitude,
                            destName = "${report.disasterType} - ${report.village}"
                        )
                    },
                    modifier = Modifier.weight(1.3f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rute Google Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        GoogleMapsHelper.openLocation(
                            context = context,
                            latitude = report.latitude,
                            longitude = report.longitude,
                            label = "${report.disasterType} (${report.village})"
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D4ED8)),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pin Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF3F4F6),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val activeUserRole = UserRole.fromRoleString(officerRole)
            val canValidate = isOfficerLoggedIn && UserRole.canValidateReports(officerRole)
            val canComplete = isOfficerLoggedIn && UserRole.canCompleteReports(officerRole)
            val canDelete = isOfficerLoggedIn && UserRole.canDeleteReports(officerRole) && onDeleteReport != null

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    if (!isOfficerLoggedIn) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = if (isOfficerLoggedIn) {
                            "Aksi (${activeUserRole.tierCode}):"
                        } else {
                            "Status (Khusus Petugas):"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficerLoggedIn) Color(activeUserRole.badgeColorHex) else Color(0xFF4B5563)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportStatusChip(
                        label = "Diproses",
                        isSelected = report.status == "Diproses",
                        color = Color(0xFFEA580C)
                    ) {
                        onUpdateStatus("Diproses")
                    }

                    if (canValidate) {
                        ReportStatusChip(
                            label = "Validasi Laporan",
                            isSelected = report.status == "Divalidasi",
                            color = Color(0xFF16A34A),
                            testTag = "btn_history_validate_report_${report.id}"
                        ) {
                            onUpdateStatus("Divalidasi")
                        }
                    }

                    if (canComplete) {
                        ReportStatusChip(
                            label = "Selesai",
                            isSelected = report.status == "Selesai",
                            color = Color(0xFF2563EB),
                            testTag = "btn_history_complete_report_${report.id}"
                        ) {
                            onUpdateStatus("Selesai")
                        }
                    }

                    if (canDelete) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("btn_history_delete_report_${report.id}")
                                .clickable { onDeleteReport?.invoke() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Laporan",
                                    tint = Color(0xFFB91C1C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Hapus Laporan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
internal fun ReportStatusChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color else Color(0xFFF3F4F6),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else Color(0xFFE5E7EB)
        ),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .let { base -> if (testTag != null) base.testTag(testTag) else base }
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF6B7280),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun CasualtyBadge(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (count > 0) color.copy(alpha = 0.12f) else Color(0xFFF3F4F6)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                color = Color(0xFF4B5563),
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) color else Color(0xFF6B7280),
                modifier = Modifier.weight(0.5f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun isReportSyncedWithServer(report: CitizenDisasterReport): Boolean {
    if (report.isSyncedToCloud) return true
    val photo = report.photoUri?.trim().orEmpty()
    val hasRemoteStorageUrl = photo.startsWith("https://", ignoreCase = true) ||
        photo.startsWith("http://", ignoreCase = true)
    val hasServerOfficerHandling = report.handledByOfficerName.isNotBlank() ||
        report.handledByOfficerNip.isNotBlank()
    return hasRemoteStorageUrl || hasServerOfficerHandling
}

internal fun decodeCitizenPhotoModel(photoSource: String?): Any? {
    if (photoSource.isNullOrBlank()) return null
    val trimmed = photoSource.trim()

    if (trimmed.startsWith("data:image", ignoreCase = true) || trimmed.contains("base64,", ignoreCase = true)) {
        return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
    }

    if (
        trimmed.startsWith("file://", ignoreCase = true) ||
        trimmed.startsWith("content://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("android.resource://", ignoreCase = true)
    ) {
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
        }
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.substring(7)
        } else {
            trimmed
        }
        if (normalizedUrl.startsWith("https://", ignoreCase = true)) {
            com.example.data.api.SupabaseClient.getCachedPhotoBitmap(normalizedUrl)?.let { return it }
            com.example.util.ImageUtils.getInlineDataUriForUrl(normalizedUrl)?.let { inlineDataUri ->
                com.example.util.ImageUtils.decodePhotoSourceToBitmap(inlineDataUri)?.let { bmp ->
                    com.example.data.api.SupabaseClient.cachePhotoBitmap(normalizedUrl, bmp)
                    return bmp
                }
            }
        }
        return normalizedUrl
    }

    if (trimmed.startsWith("/")) {
        return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
    }

    return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
}

@Composable
internal fun rememberCitizenReportPhotoBitmap(
    photoSource: String?,
    reportId: String = ""
): Bitmap? {
    val trimmed = photoSource?.trim().orEmpty()
    var resolvedBitmap by remember(trimmed, reportId) {
        val initialModel = decodeCitizenPhotoModel(trimmed)
        val fromModel = initialModel as? Bitmap
        val fromReportCache = if (fromModel == null && reportId.isNotBlank()) {
            com.example.util.ImageUtils.getInlineDataUriForReport(reportId)?.let {
                com.example.util.ImageUtils.decodePhotoSourceToBitmap(it)
            }
        } else {
            null
        }
        mutableStateOf(fromModel ?: fromReportCache)
    }

    LaunchedEffect(trimmed, reportId) {
        if (resolvedBitmap == null &&
            (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true))
        ) {
            val downloaded = com.example.data.api.SupabaseClient.downloadReportPhotoBitmap(trimmed)
            if (downloaded != null) {
                resolvedBitmap = downloaded
            } else if (reportId.isNotBlank()) {
                val backupUri = com.example.util.ImageUtils.getInlineDataUriForReport(reportId)
                if (!backupUri.isNullOrBlank()) {
                    resolvedBitmap = com.example.util.ImageUtils.decodePhotoSourceToBitmap(backupUri)
                }
            }
        }
    }

    return resolvedBitmap
}

@Suppress("DEPRECATION")
internal fun triggerBiometricVibration(context: Context, mode: String) {
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (mode) {
                "SUCCESS" -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 40L, 60L, 90L),
                    intArrayOf(0, 180, 0, 255),
                    -1
                )
                "ERROR" -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 65L, 50L, 65L, 50L, 95L),
                    intArrayOf(0, 220, 0, 220, 0, 255),
                    -1
                )
                else -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 35L, 55L, 45L),
                    intArrayOf(0, 150, 0, 200),
                    -1
                )
            }
            vibrator.vibrate(effect)
        } else {
            when (mode) {
                "SUCCESS" -> vibrator.vibrate(longArrayOf(0L, 40L, 60L, 90L), -1)
                "ERROR" -> vibrator.vibrate(longArrayOf(0L, 65L, 50L, 65L, 50L, 95L), -1)
                else -> vibrator.vibrate(longArrayOf(0L, 35L, 55L, 45L), -1)
            }
        }
    } catch (_: Exception) {
    }
}

@Composable
internal fun BiometricScanningVisualBanner(
    officerName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometricScannerBanner")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserScanLine"
    )

    val iconGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 675, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlowScale"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF0F9FF),
        border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f

                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = (1f - pulseProgress) * 0.45f),
                        radius = maxRadius * (0.45f + 0.55f * pulseProgress),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val secondaryProgress = (pulseProgress + 0.5f) % 1f
                    drawCircle(
                        color = Color(0xFF2563EB).copy(alpha = (1f - secondaryProgress) * 0.35f),
                        radius = maxRadius * (0.45f + 0.55f * secondaryProgress),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 0.78f
                        ),
                        radius = maxRadius * 0.78f,
                        center = center
                    )

                    val yPos = size.height * scanLineY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0284C7),
                                Color(0xFF10B981),
                                Color(0xFF0284C7),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.14f, yPos),
                        end = Offset(size.width * 0.86f, yPos),
                        strokeWidth = 2.2.dp.toPx()
                    )
                }

                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Memindai Sidik Jari",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = iconGlowScale
                            scaleY = iconGlowScale
                        }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Memindai Sensor Biometrik...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0369A1)
                )
                Text(
                    text = "Verifikasi identitas: $officerName",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tempelkan jari pada sensor sidik jari perangkat Anda.",
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    lineHeight = 13.sp
                )
            }
        }
    }
}
