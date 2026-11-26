package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Visualisasi Grafik Batang Tren Bulanan Kategori Bencana
 */
@Composable
internal fun DisasterMonthlyBarChartCard(
    reports: List<CitizenDisasterReport>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Grafik Tren Kategori Bencana Bulanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tahun ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val categories = listOf(
                Triple("Banjir", reports.count { it.disasterType.contains("Banjir", true) }, Color(0xFF2563EB)),
                Triple("Badai", reports.count { it.disasterType.contains("Angin", true) || it.disasterType.contains("Badai", true) }, Color(0xFFD97706)),
                Triple("Abrasi", reports.count { it.disasterType.contains("Gelombang", true) || it.disasterType.contains("Abrasi", true) }, Color(0xFF0284C7)),
                Triple("Longsor", reports.count { it.disasterType.contains("Longsor", true) }, Color(0xFFB45309)),
                Triple("Gempa", reports.count { it.disasterType.contains("Gempa", true) }, Color(0xFFDC2626)),
                Triple("Lainnya", reports.count { !it.disasterType.contains("Banjir", true) && !it.disasterType.contains("Angin", true) && !it.disasterType.contains("Gelombang", true) && !it.disasterType.contains("Longsor", true) && !it.disasterType.contains("Gempa", true) }, Color(0xFF64748B))
            )

            val maxCount = (categories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val barWidth = 24.dp.toPx()
                val spacing = (size.width - (categories.size * barWidth)) / (categories.size + 1)
                val chartHeight = size.height - 20f

                categories.forEachIndexed { i, (_, count, color) ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = (count.toFloat() / maxCount) * chartHeight
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                categories.forEach { (name, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "$count",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini Map Canvas with Pin and Pulsing Radar Circle
 */
@Composable
internal fun MiniMapWithPinCanvas(
    latitude: Double,
    longitude: Double,
    disasterType: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pin_radar_anim")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val gridStep = 30f
        var gx = 0f
        while (gx <= size.width) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(gx, 0f),
                end = Offset(gx, size.height),
                strokeWidth = 1f
            )
            gx += gridStep
        }
        var gy = 0f
        while (gy <= size.height) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1f
            )
            gy += gridStep
        }

        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = (1f - (pulseRadius / 35f)).coerceIn(0f, 0.7f)),
            radius = pulseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        drawCircle(
            color = Color(0xFFDC2626),
            radius = 7f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * KPI Metric Card Component
 */
@Composable
internal fun KpiMetricCard(
    title: String,
    count: Int,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Kartu Insiden Petugas Responsif dengan Aksi Validasi & Auto-Pin
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OfficerIncidentReportCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    officerRole: String = OfficerRoles.PETUGAS_JAGA,
    onInspectMapPin: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDeleteReport: (() -> Unit)? = null,
    onOpenNoteEditor: () -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canValidate = UserRole.canValidateReports(officerRole)
    val canComplete = UserRole.canCompleteReports(officerRole)
    val canDelete = UserRole.canDeleteReports(officerRole) && onDeleteReport != null

    val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
    val statusColor = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Color(0xFF16A34A)
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Color(0xFF2563EB)
        else -> Color(0xFFD97706)
    }

    val statusBg = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Color(0xFFF0FDF4)
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Color(0xFFEFF6FF)
        else -> Color(0xFFFFFBEB)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("officer_incident_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = "#${report.id.takeLast(7)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.disasterType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(
                                text = normalizedStatus.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isSyncedServer = isReportSyncedWithServer(report)
                    val syncIcon = if (isSyncedServer) Icons.Default.CheckCircle else Icons.Default.Schedule
                    val syncColor = if (isSyncedServer) Color(0xFF16A34A) else Color(0xFFD97706)
                    val syncText = if (isSyncedServer) "Synced" else "Menunggu Sync"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = syncColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, syncColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .testTag("officer_sync_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = syncText,
                                tint = syncColor,
                                modifier = Modifier.size(10.dp)
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
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text(
                                text = report.district,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(15.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "${report.village}, Kecamatan ${report.district}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    if (report.incidentLocation.isNotBlank()) {
                        Text(
                            text = "Patokan: ${report.incidentLocation}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    Text(
                        text = "Waktu Kejadian: ${report.incidentTime}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            if (report.casualtiesDead > 0 || report.casualtiesInjured > 0 || report.casualtiesMissing > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Korban: ${report.casualtiesDead} Meninggal • ${report.casualtiesInjured} Luka • ${report.casualtiesMissing} Hilang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (!report.photoUri.isNullOrBlank()) {
                val thumbContext = LocalContext.current
                val resolvedPhotoModel = remember(report.photoUri) {
                    decodeCitizenPhotoModel(report.photoUri)
                }
                val fallbackOfficerBitmap = rememberCitizenReportPhotoBitmap(report.photoUri, report.id)
                var isThumbError by remember(report.photoUri) {
                    mutableStateOf(resolvedPhotoModel == null && fallbackOfficerBitmap == null)
                }
                if (fallbackOfficerBitmap != null && isThumbError) {
                    isThumbError = false
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isThumbError && fallbackOfficerBitmap == null) Color(0xFFE5E7EB) else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("officer_report_photo_thumbnail")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isThumbError || fallbackOfficerBitmap != null) { onViewPhoto(report.photoUri) }
                    ) {
                        if (isThumbError && fallbackOfficerBitmap == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE5E7EB))
                                    .testTag("officer_report_photo_error")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak dapat dimuat",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(36.dp)
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
                            val activeBitmap = fallbackOfficerBitmap ?: (resolvedPhotoModel as? android.graphics.Bitmap)
                            if (activeBitmap != null) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Foto Bukti Kejadian dari Warga",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(thumbContext)
                                        .data(resolvedPhotoModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Bukti Kejadian dari Warga",
                                    onSuccess = { isThumbError = false },
                                    onError = {
                                        if (fallbackOfficerBitmap == null) {
                                            isThumbError = true
                                        }
                                    },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(topStart = 8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "🔍 Ketuk untuk layar penuh",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { onViewPhoto(report.photoUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("officer_btn_zoom_report_photo")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.Black.copy(alpha = 0.68f), CircleShape)
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
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("officer_placeholder_no_photo")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = "Warga tidak melampirkan foto",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
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

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (!report.photoUri.isNullOrBlank()) "📷 Lampiran foto bukti tersedia" else "Tanpa lampiran foto",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (!report.photoUri.isNullOrBlank()) Color(0xFF15803D) else Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                OutlinedButton(
                    onClick = onInspectMapPin,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Tinjau Peta", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            }

            if (report.additionalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catatan Tindak Lanjut TRC:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                        Text(
                            text = report.additionalNotes,
                            fontSize = 11.sp,
                            color = Color(0xFF14532D)
                        )
                    }
                }
            }

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
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Petugas Penangan",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val handlerText = buildString {
                            append("Petugas Penangan: ")
                            append(report.handledByOfficerName.ifBlank { "Petugas Pusdalops" })
                            if (report.handledByOfficerRole.isNotBlank()) {
                                append(" (${report.handledByOfficerRole})")
                            }
                            if (report.handledByOfficerNip.isNotBlank()) {
                                append(" • NIP ${maskNipLast6(report.handledByOfficerNip)}")
                            }
                        }
                        Text(
                            text = handlerText,
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
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (canValidate && !report.status.equals("Divalidasi", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Divalidasi") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_validate_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Validasi Laporan",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Validasi Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (canComplete) {
                    if (!report.status.equals("Selesai", ignoreCase = true)) {
                        Button(
                            onClick = { onUpdateStatus("Selesai") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("btn_complete_report_${report.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tandai Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onUpdateStatus("Diproses") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(text = "Buka Kembali", fontSize = 11.sp)
                        }
                    }
                }

                if (canDelete) {
                    Button(
                        onClick = { onDeleteReport?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_delete_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Laporan",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Hapus Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenNoteEditor,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Catatan", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        GoogleMapsHelper.openNavigationRoute(
                            context = context,
                            destLat = report.latitude,
                            destLon = report.longitude,
                            destName = "Bencana ${report.disasterType} - ${report.village}"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Rute Google Maps",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
