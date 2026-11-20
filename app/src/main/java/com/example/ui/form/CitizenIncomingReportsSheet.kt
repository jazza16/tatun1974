package com.example.ui.form

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.data.local.LaporanWargaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model terpadu untuk item Laporan Warga Masuk yang ditampilkan pada Modal Bottom Sheet
 * saat tombol hijau "DARI WARGA" ditekan di layar Form Laporan Harian (Tab Kejadian).
 */
data class IncomingCitizenReportItem(
    val id: String,
    val reporterName: String,
    val reporterPhone: String,
    val dateText: String,
    val timeText: String,
    val disasterType: String,
    val district: String,
    val village: String,
    val locationDetail: String,
    val coordinates: String,
    val description: String,
    val photoUrl: String?,
    @DrawableRes val fallbackDrawableRes: Int = R.drawable.img_mukomuko_disaster_wallpaper,
    val casualtiesDead: Int = 0,
    val casualtiesInjured: Int = 0,
    val status: String = "Baru Masuk",
    val isGpsVerified: Boolean = true,
    val isUnreadOrUnprocessed: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    fun fullFormattedLocation(): String {
        val parts = mutableListOf<String>()
        if (locationDetail.isNotBlank()) parts.add(locationDetail.trim())
        if (village.isNotBlank() && !locationDetail.contains(village, ignoreCase = true)) {
            parts.add("Desa ${village.trim()}")
        }
        if (district.isNotBlank() && !locationDetail.contains(district, ignoreCase = true)) {
            parts.add("Kec. ${district.trim()}")
        }
        return parts.joinToString(", ").ifBlank { "Kec. $district" }
    }
}

/**
 * Penyedia data & pengelola status baca/proses untuk Daftar Laporan Warga Masuk.
 * Menggabungkan data dari Room (`citizen_disaster_reports` dan `laporan_warga`) serta
 * menyediakan data laporan warga lengkap dengan foto dokumentasi jika database masih kosong.
 */
object CitizenIncomingReportsProvider {
    private const val PREFS_NAME = "sipagar_citizen_autofill_prefs"
    private const val KEY_PROCESSED_REPORT_IDS = "processed_citizen_report_ids"

    fun getProcessedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PROCESSED_REPORT_IDS, emptySet())?.toSet() ?: emptySet()
    }

    suspend fun markReportAsUsedByOfficer(
        context: Context,
        database: AppDatabase,
        report: IncomingCitizenReportItem
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_PROCESSED_REPORT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(report.id)
        prefs.edit().putStringSet(KEY_PROCESSED_REPORT_IDS, current).apply()

        val officerAuth = com.example.data.local.OfficerAuthManager(context)
        val authMgr = runCatching { com.example.data.auth.AuthManager.getInstance(context) }.getOrNull()
        val activeOfficerName = officerAuth.getOfficerName().ifBlank { authMgr?.getDisplayName().orEmpty() }
        val activeOfficerNip = officerAuth.getOfficerBadgeId().ifBlank { authMgr?.getBadgeOrUid().orEmpty() }
        val activeOfficerRole = officerAuth.getOfficerRole()
        val now = System.currentTimeMillis()

        try {
            val existingEntity = database.weatherDao().getCitizenReportById(report.id)
            if (existingEntity != null) {
                // Jangan pernah menurunkan status laporan yang sudah Divalidasi atau Selesai kembali menjadi Diproses!
                val currentNormalized = com.example.model.CitizenReportStatusHelper.normalizeStatus(existingEntity.status)
                val targetStatus = if (com.example.model.CitizenReportStatusHelper.statusRank(currentNormalized) >=
                    com.example.model.CitizenReportStatusHelper.statusRank(com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI)
                ) {
                    currentNormalized
                } else {
                    com.example.model.CitizenReportStatusHelper.STATUS_DIPROSES
                }

                database.weatherDao().updateCitizenReportStatus(
                    reportId = report.id,
                    newStatus = targetStatus,
                    updatedAt = now,
                    officerName = activeOfficerName,
                    officerNip = activeOfficerNip,
                    officerRole = activeOfficerRole,
                    isSynced = false
                )

                val fallbackModel = com.example.model.CitizenDisasterReport(
                    id = existingEntity.id,
                    disasterType = existingEntity.disasterType,
                    incidentTime = existingEntity.incidentTime,
                    incidentLocation = existingEntity.incidentLocation,
                    district = existingEntity.district,
                    village = existingEntity.village,
                    latitude = existingEntity.latitude,
                    longitude = existingEntity.longitude,
                    gpsAccuracyMeters = existingEntity.gpsAccuracyMeters,
                    isGpsVerified = existingEntity.isGpsVerified,
                    casualtiesDead = existingEntity.casualtiesDead,
                    casualtiesInjured = existingEntity.casualtiesInjured,
                    casualtiesMissing = existingEntity.casualtiesMissing,
                    photoUri = existingEntity.photoUri,
                    reporterName = existingEntity.reporterName,
                    reporterPhone = existingEntity.reporterPhone,
                    additionalNotes = existingEntity.additionalNotes,
                    timestamp = existingEntity.timestamp,
                    status = targetStatus,
                    updatedAt = now,
                    handledByOfficerName = activeOfficerName,
                    handledByOfficerNip = activeOfficerNip,
                    handledByOfficerRole = activeOfficerRole,
                    isSyncedToCloud = false
                )
                val cloudRes = com.example.data.local.FirestoreHelper.updateReportStatusInCloudSuspend(
                    reportId = report.id,
                    newStatus = targetStatus,
                    notes = null,
                    updatedAt = now,
                    officerName = activeOfficerName,
                    officerNip = activeOfficerNip,
                    officerRole = activeOfficerRole,
                    fallbackReport = fallbackModel
                )
                if (cloudRes.isSuccess) {
                    database.weatherDao().updateCitizenReportSyncState(report.id, true)
                }
            }
        } catch (_: Exception) {
        }
        if (report.id.startsWith("LW-")) {
            val numericId = report.id.removePrefix("LW-").toIntOrNull()
            if (numericId != null) {
                try {
                    val existing = database.laporanWargaDao().getLaporanWargaById(numericId)
                    if (existing != null) {
                        database.laporanWargaDao().updateLaporanWarga(existing.copy(status = "Diproses Petugas"))
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    suspend fun loadIncomingCitizenReports(
        context: Context,
        database: AppDatabase
    ): List<IncomingCitizenReportItem> = withContext(Dispatchers.IO) {
        val processedIds = getProcessedIds(context)
        val pkg = context.packageName
        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        val fallbackDrawables = listOf(
            R.drawable.img_mukomuko_disaster_wallpaper,
            R.drawable.mukomuko_hero,
            R.drawable.img_mukomuko_wallpaper,
            R.drawable.wallpaper_startup_bpbd
        )

        // 0. Sinkronkan lebih dulu data laporan warga terbaru dari Cloud Firestore agar "DARI WARGA" selalu sinkron dengan laporan warga nyata
        try {
            val fetchRes = com.example.data.local.FirestoreHelper.fetchCitizenReportsFromServerSuspend()
            val remoteReports = fetchRes.getOrNull().orEmpty()
            if (remoteReports.isNotEmpty()) {
                val localMap = database.weatherDao().getAllCitizenDisasterReportsSync().associateBy { it.id }
                for (remote in remoteReports) {
                    val existing = localMap[remote.id]
                    val localUpdatedAt = existing?.effectiveUpdatedAt() ?: 0L
                    val remoteUpdatedAt = remote.effectiveUpdatedAt()
                    val remoteBringsPhoto = com.example.util.ImageUtils.isCloudPortablePhotoUri(remote.photoUri) &&
                        !com.example.util.ImageUtils.isCloudPortablePhotoUri(existing?.photoUri)
                    val shouldUpsert = existing == null ||
                        !existing.isSyncedToCloud ||
                        remoteUpdatedAt >= localUpdatedAt ||
                        remoteBringsPhoto
                    if (shouldUpsert) {
                        database.weatherDao().insertCitizenDisasterReport(
                            CitizenDisasterReportEntity(
                                id = remote.id,
                                disasterType = remote.disasterType,
                                incidentTime = remote.incidentTime,
                                incidentLocation = remote.incidentLocation,
                                district = remote.district,
                                village = remote.village,
                                latitude = remote.latitude,
                                longitude = remote.longitude,
                                gpsAccuracyMeters = remote.gpsAccuracyMeters,
                                isGpsVerified = remote.isGpsVerified,
                                casualtiesDead = remote.casualtiesDead,
                                casualtiesInjured = remote.casualtiesInjured,
                                casualtiesMissing = remote.casualtiesMissing,
                                photoUri = com.example.util.ImageUtils.selectBestPhotoUri(remote.photoUri, existing?.photoUri),
                                reporterName = remote.reporterName,
                                reporterPhone = remote.reporterPhone,
                                additionalNotes = remote.additionalNotes,
                                timestamp = remote.timestamp,
                                status = com.example.model.CitizenReportStatusHelper.normalizeStatus(remote.status),
                                updatedAt = remoteUpdatedAt,
                                handledByOfficerName = remote.handledByOfficerName.ifBlank { existing?.handledByOfficerName.orEmpty() },
                                handledByOfficerNip = remote.handledByOfficerNip.ifBlank { existing?.handledByOfficerNip.orEmpty() },
                                handledByOfficerRole = remote.handledByOfficerRole.ifBlank { existing?.handledByOfficerRole.orEmpty() },
                                isSyncedToCloud = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Tetap tampilkan data Room lokal apabila perangkat sedang offline
        }

        val combined = LinkedHashMap<String, IncomingCitizenReportItem>()

        // 1. Muat dari tabel citizen_disaster_reports (Laporan Warga real-time & sinkronisasi Cloud)
        val citizenEntities: List<CitizenDisasterReportEntity> = try {
            database.weatherDao().getAllCitizenDisasterReportsSync()
        } catch (_: Exception) {
            emptyList()
        }

        citizenEntities.forEachIndexed { index, entity ->
            val parsedDateAndTime = splitIncidentTime(entity.incidentTime, todayStr)
            val fallbackRes = fallbackDrawables[index % fallbackDrawables.size]
            val resolvedPhoto = entity.photoUri?.takeIf { it.isNotBlank() }
                ?: "android.resource://$pkg/$fallbackRes"
            val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(entity.status)
            val isAlreadyProcessed = entity.id in processedIds ||
                com.example.model.CitizenReportStatusHelper.isResolved(normalizedStatus)

            combined[entity.id] = IncomingCitizenReportItem(
                id = entity.id,
                reporterName = entity.reporterName.ifBlank { "Warga Kab. Mukomuko" },
                reporterPhone = entity.reporterPhone.ifBlank { "-" },
                dateText = parsedDateAndTime.first,
                timeText = parsedDateAndTime.second,
                disasterType = entity.disasterType.ifBlank { "Banjir" },
                district = entity.district.ifBlank { "Kota Mukomuko" },
                village = entity.village.ifBlank { "Pasar Mukomuko" },
                locationDetail = entity.incidentLocation.ifBlank { "Kec. ${entity.district}" },
                coordinates = String.format(Locale.US, "%.4f, %.4f", entity.latitude, entity.longitude),
                description = entity.additionalNotes.ifBlank {
                    "Dilaporkan kejadian ${entity.disasterType} oleh ${entity.reporterName} di ${entity.incidentLocation}, Desa ${entity.village}, Kec. ${entity.district}."
                },
                photoUrl = resolvedPhoto,
                fallbackDrawableRes = fallbackRes,
                casualtiesDead = entity.casualtiesDead,
                casualtiesInjured = entity.casualtiesInjured,
                status = if (entity.id in processedIds) "Sudah Dipakai ($normalizedStatus)" else normalizedStatus,
                isGpsVerified = entity.isGpsVerified,
                isUnreadOrUnprocessed = !isAlreadyProcessed,
                timestampMillis = entity.effectiveUpdatedAt()
            )
        }

        // 2. Muat dari tabel laporan_warga (Room lokal)
        val localWargaEntities: List<LaporanWargaEntity> = try {
            database.laporanWargaDao().getAllLaporanWargaSync()
        } catch (_: Exception) {
            emptyList()
        }

        localWargaEntities.forEachIndexed { idx, lw ->
            val key = "LW-${lw.id}"
            val fallbackRes = fallbackDrawables[(idx + 1) % fallbackDrawables.size]
            val resolvedPhoto = lw.fotoUri?.takeIf { it.isNotBlank() }
                ?: "android.resource://$pkg/$fallbackRes"
            val isAlreadyProcessed = key in processedIds ||
                lw.status.contains("Diproses", ignoreCase = true) ||
                lw.status.contains("Selesai", ignoreCase = true)

            val coords = lw.koordinat.takeIf { it.isNotBlank() }
                ?: if (lw.latitude != 0.0 || lw.longitude != 0.0) {
                    String.format(Locale.US, "%.4f, %.4f", lw.latitude, lw.longitude)
                } else {
                    "-2.5830, 101.1240"
                }

            combined[key] = IncomingCitizenReportItem(
                id = key,
                reporterName = lw.namaPelapor.ifBlank { "Warga Kab. Mukomuko" },
                reporterPhone = lw.nomorKontak.ifBlank { "-" },
                dateText = lw.tanggal.ifBlank { todayStr },
                timeText = lw.waktu.ifBlank { "08:30 WIB" },
                disasterType = lw.jenisBencana.ifBlank { "Banjir" },
                district = lw.kecamatan.ifBlank { "Kota Mukomuko" },
                village = lw.desa.ifBlank { "Pasar Mukomuko" },
                locationDetail = lw.lokasi.ifBlank { "Kec. ${lw.kecamatan}" },
                coordinates = coords,
                description = lw.deskripsi.ifBlank {
                    "Laporan kejadian ${lw.jenisBencana} dari masyarakat di ${lw.lokasi}."
                },
                photoUrl = resolvedPhoto,
                fallbackDrawableRes = fallbackRes,
                casualtiesDead = 0,
                casualtiesInjured = 0,
                status = if (isAlreadyProcessed) "Sudah Dipakai" else lw.status.ifBlank { "Diproses" },
                isGpsVerified = true,
                isUnreadOrUnprocessed = !isAlreadyProcessed,
                timestampMillis = lw.timestamp
            )
        }

        combined.values.sortedWith(
            compareByDescending<IncomingCitizenReportItem> { it.isUnreadOrUnprocessed }
                .thenByDescending { it.timestampMillis }
        )
    }

    private fun splitIncidentTime(raw: String, fallbackDate: String): Pair<String, String> {
        if (raw.isBlank()) return Pair(fallbackDate, "08:30 WIB")
        val parts = raw.split(",")
        return if (parts.size >= 2) {
            Pair(parts[0].trim(), parts[1].trim())
        } else if (raw.contains("WIB", ignoreCase = true)) {
            Pair(fallbackDate, raw.trim())
        } else {
            Pair(raw.trim(), "08:30 WIB")
        }
    }
}

/**
 * Kartu Header Petugas Jaga + Tombol Hijau "DARI WARGA" (lengkap dengan Badge Angka Merah laporan baru)
 * + Tombol Oranye "UPDATE KEJADIAN" + Pratinjau Foto Warga Terlampir jika sudah di-autofill.
 */
@Composable
fun DariWargaHeaderCard(
    officerName: String,
    unreadCitizenCount: Int,
    attachedCitizenReport: IncomingCitizenReportItem?,
    onOpenDariWargaSheet: () -> Unit,
    onUpdateKejadianKemarin: () -> Unit,
    onClearAttachedCitizenReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFullPhotoDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Petugas Jaga:",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = officerName.ifBlank { "Petugas Pusdalops Aktif" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }

                if (unreadCitizenCount > 0) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.clickable { onOpenDariWargaSheet() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Text(
                                text = "$unreadCitizenCount Laporan Warga Baru",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }

            // Baris Tombol Sejajar: DARI WARGA (Hijau + Badge Angka Merah) & UPDATE KEJADIAN (Oranye)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onOpenDariWargaSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_dari_warga"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Tarik Laporan Dari Warga",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DARI WARGA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Badge Angka Merah di sudut kanan atas tombol "DARI WARGA"
                    if (unreadCitizenCount > 0) {
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-6).dp)
                                .testTag("badge_dari_warga_count")
                        ) {
                            Text(
                                text = if (unreadCitizenCount > 99) "99+" else unreadCitizenCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Button(
                    onClick = onUpdateKejadianKemarin,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_update_kejadian"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Update Kejadian",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPDATE KEJADIAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Jika petugas sudah memilih "Gunakan Data Ini" dari modal, tampilkan pratinjau data & foto warga di dalam form!
            AnimatedVisibility(visible = attachedCitizenReport != null) {
                if (attachedCitizenReport != null) {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_attached_citizen_report")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Autofill Aktif dari Laporan Warga",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }

                                IconButton(
                                    onClick = onClearAttachedCitizenReport,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus lampiran laporan warga",
                                        tint = Color(0xFF166534),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CitizenReportPhotoThumbnail(
                                    photoUrl = attachedCitizenReport.photoUrl,
                                    fallbackDrawableRes = attachedCitizenReport.fallbackDrawableRes,
                                    contentDescription = "Foto laporan ${attachedCitizenReport.reporterName}",
                                    modifier = Modifier
                                        .size(width = 88.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showFullPhotoDialog = true }
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${attachedCitizenReport.disasterType} • Kec. ${attachedCitizenReport.district}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pelapor: ${attachedCitizenReport.reporterName} (${attachedCitizenReport.timeText})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = attachedCitizenReport.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF374151),
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

    if (showFullPhotoDialog && attachedCitizenReport != null) {
        CitizenReportFullPhotoDialog(
            report = attachedCitizenReport,
            onDismiss = { showFullPhotoDialog = false }
        )
    }
}

/**
 * Modal Bottom Sheet (Material 3) untuk memilih Laporan Warga Masuk dan meng-autofill Form Laporan Harian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenIncomingReportsModalBottomSheet(
    reports: List<IncomingCitizenReportItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onUseReportData: (IncomingCitizenReportItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CitizenIncomingReportsSheetContent(
            reports = reports,
            isLoading = isLoading,
            onDismiss = onDismiss,
            onRefresh = onRefresh,
            onUseReportData = onUseReportData
        )
    }
}

/**
 * Konten Daftar Laporan Warga Masuk yang dapat dipakai baik di Compose ModalBottomSheet
 * maupun di dalam BottomSheetDialog pada FormTab1Fragment (XML/ViewPager2).
 */
@Composable
fun CitizenIncomingReportsSheetContent(
    reports: List<IncomingCitizenReportItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onUseReportData: (IncomingCitizenReportItem) -> Unit
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Semua, 1 = Belum Diproses (Baru), 2 = Sudah Dipakai
    var photoPreviewReport by remember { mutableStateOf<IncomingCitizenReportItem?>(null) }

    val unreadCount = remember(reports) { reports.count { it.isUnreadOrUnprocessed } }
    val filteredReports = remember(reports, selectedFilter) {
        when (selectedFilter) {
            1 -> reports.filter { it.isUnreadOrUnprocessed }
            2 -> reports.filter { !it.isUnreadOrUnprocessed }
            else -> reports
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(Color(0xFFF8FAFC))
            .testTag("sheet_daftar_laporan_warga")
    ) {
        // Header Modal
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Daftar Laporan Warga Masuk",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$unreadCount Baru",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Pilih laporan untuk mengisi otomatis (autofill) Informasi Dasar Kejadian",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang Laporan Warga",
                                tint = Color(0xFF1976D2)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Modal",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("Semua (${reports.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("Belum Diproses ($unreadCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = {
                            val usedCount = reports.size - unreadCount
                            Text("Sudah Dipakai ($usedCount)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF15803D),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF16A34A))
                    Text(
                        text = "Memuat daftar laporan warga & foto dokumentasi...",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        } else if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Tidak ada laporan warga pada kategori filter ini",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    OutlinedButton(onClick = { selectedFilter = 0 }) {
                        Text("Tampilkan Semua Laporan")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredReports, key = { it.id }) { item ->
                    CitizenReportListItemCard(
                        item = item,
                        onPreviewPhoto = { photoPreviewReport = item },
                        onUseDataClick = { onUseReportData(item) }
                    )
                }
            }
        }
    }

    if (photoPreviewReport != null) {
        CitizenReportFullPhotoDialog(
            report = photoPreviewReport!!,
            onDismiss = { photoPreviewReport = null }
        )
    }
}

/**
 * Kartu setiap item Laporan Warga Masuk:
 * Menampilkan Foto Warga (dengan loading & error fallback), Nama Pelapor, Waktu, Jenis Bencana,
 * Lokasi, Deskripsi, serta Tombol "Gunakan Data Ini".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CitizenReportListItemCard(
    item: IncomingCitizenReportItem,
    onPreviewPhoto: () -> Unit,
    onUseDataClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("citizen_report_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (item.isUnreadOrUnprocessed) 1.5.dp else 1.dp,
            color = if (item.isUnreadOrUnprocessed) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Baris Atas: Jenis Bencana + Status Baru / Waktu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.disasterType,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                if (item.isUnreadOrUnprocessed) {
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "BARU • BELUM DIPROSES",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "SUDAH DIPAKAI",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Foto Dokumentasi Warga dengan penanganan Loading & Error Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    .clickable { onPreviewPhoto() }
            ) {
                CitizenReportPhotoThumbnail(
                    photoUrl = item.photoUrl,
                    fallbackDrawableRes = item.fallbackDrawableRes,
                    contentDescription = "Foto bencana dari ${item.reporterName}",
                    modifier = Modifier.fillMaxSize()
                )

                // Label Zoom di pojok kanan bawah foto
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Lihat Foto",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (item.isGpsVerified) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "GPS: ${item.coordinates}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Informasi Nama Pelapor & Waktu Kejadian
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Pelapor: ${item.reporterName}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.dateText} • ${item.timeText}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Lokasi & Kecamatan
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 1.dp)
                )
                Text(
                    text = item.fullFormattedLocation(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }

            // Deskripsi Kejadian dari Warga
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Tombol "Gunakan Data Ini"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.reporterPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.reporterPhone,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Button(
                    onClick = onUseDataClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("btn_gunakan_data_ini_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gunakan Data Ini",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Widget gambar khusus untuk menampilkan foto laporan warga menggunakan Coil `SubcomposeAsyncImage`
 * dengan penanganan lengkap:
 * 1. Loading state (indikator proses muat + teks keterangan).
 * 2. Error / Placeholder fallback (menampilkan visual fallback lokal + badge keterangan jika URL foto gagal dimuat).
 */
@Composable
fun CitizenReportPhotoThumbnail(
    photoUrl: String?,
    @DrawableRes fallbackDrawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (photoUrl.isNullOrBlank()) {
        // Jika URL foto kosong, tampilkan gambar placeholder lokal dengan label jelas
        Box(modifier = modifier.background(Color(0xFFE2E8F0))) {
            Image(
                painter = painterResource(id = fallbackDrawableRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Foto Dokumentasi Lapangan",
                        color = Color.White,
                        fontSize = 9.5.sp
                    )
                }
            }
        }
        return
    }

    val resolvedModel = remember(photoUrl) {
        com.example.ui.components.decodeCitizenPhotoModel(photoUrl)
    }

    if (resolvedModel is android.graphics.Bitmap) {
        Image(
            bitmap = resolvedModel.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedModel ?: photoUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        text = "Memuat foto warga...",
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        },
        error = {
            // Penanganan error: tampilkan gambar placeholder/fallback lokal + pemberitahuan bahwa URL gagal dimuat
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE2E8F0))
            ) {
                Image(
                    painter = painterResource(id = fallbackDrawableRes),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = Color(0xFFFECACA),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Pratinjau Lokal (Foto Jaringan Offline)",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    )
}

/**
 * Dialog pratinjau foto ukuran penuh saat petugas menekan foto pada daftar atau kartu autofill.
 */
@Composable
fun CitizenReportFullPhotoDialog(
    report: IncomingCitizenReportItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.disasterType,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Pelapor: ${report.reporterName} • ${report.dateText} ${report.timeText}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Foto"
                        )
                    }
                }

                CitizenReportPhotoThumbnail(
                    photoUrl = report.photoUrl,
                    fallbackDrawableRes = report.fallbackDrawableRes,
                    contentDescription = "Foto penuh laporan warga",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Text(
                    text = report.fullFormattedLocation(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = report.description,
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}
