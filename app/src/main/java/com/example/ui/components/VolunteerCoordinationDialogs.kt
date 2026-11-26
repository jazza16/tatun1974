package com.example.ui.components

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MukomukoVolunteerPoskoCatalog
import com.example.data.local.VolunteerEntity
import com.example.data.local.VolunteerPrivacyHelper
import com.example.data.local.VolunteerStatus
import com.example.util.VolunteerAssignmentPdfGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun VolunteerStatusPipelineBadge(statusEnum: VolunteerStatus) {
    val steps = listOf(
        VolunteerStatus.PENDING,
        VolunteerStatus.VERIFIED,
        VolunteerStatus.ASSIGNED,
        VolunteerStatus.DONE
    )
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (statusEnum == VolunteerStatus.REJECTED) {
                Text(
                    text = "DITOLAK",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDC2626),
                    maxLines = 1
                )
            } else {
                steps.forEachIndexed { index, step ->
                    val isCurrent = step == statusEnum
                    Text(
                        text = step.code,
                        fontSize = 8.5.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isCurrent) Color(0xFFEA580C) else Color(0xFF94A3B8),
                        maxLines = 1
                    )
                    if (index < steps.lastIndex) {
                        Text(
                            text = "→",
                            fontSize = 8.5.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * MODAL PENUGASAN RELAWAN:
 * Saat Admin selesai memasukkan data penugasan (lokasi & shift) dan menekan tombol "Simpan Penugasan",
 * sistem otomatis memicu fungsi pembuatan file PDF "SURAT TUGAS RELAWAN".
 */
@Composable
internal fun VolunteerAssignmentModalDialog(
    volunteer: VolunteerEntity,
    onDismiss: () -> Unit,
    onConfirmAssign: (district: String, poskoName: String, shift: String) -> Unit
) {
    val initialPosko = remember(volunteer) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.firstOrNull {
            it.districtName.equals(volunteer.assignedDistrict.ifBlank { volunteer.homeDistrict }, ignoreCase = true)
        } ?: MukomukoVolunteerPoskoCatalog.POSKO_LIST.first()
    }

    var selectedPosko by remember { mutableStateOf(initialPosko) }
    var customPoskoName by remember {
        mutableStateOf(volunteer.assignedPoskoName.ifBlank { initialPosko.poskoName })
    }
    var selectedShift by remember {
        mutableStateOf(
            volunteer.assignedShift.ifBlank { MukomukoVolunteerPoskoCatalog.SHIFT_OPTIONS.first() }
        )
    }
    var poskoDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("modal_assign_volunteer"),
        title = {
            Column {
                Text(
                    text = "Penugasan Relawan & Cetak Surat Tugas PDF",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${volunteer.fullName} (NIK: ${volunteer.nik}) • ${volunteer.skillsList().joinToString(", ")}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Pilih Kecamatan & Titik Posko BPBD:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { poskoDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("dropdown_assign_posko_district")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kec. ${selectedPosko.districtName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = poskoDropdownExpanded,
                            onDismissRequest = { poskoDropdownExpanded = false }
                        ) {
                            MukomukoVolunteerPoskoCatalog.POSKO_LIST.forEach { posko ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Kec. ${posko.districtName}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = posko.poskoName,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPosko = posko
                                        customPoskoName = posko.poskoName
                                        poskoDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customPoskoName,
                    onValueChange = { customPoskoName = it },
                    label = { Text("Lokasi / Nama Posko Penugasan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_assign_posko_name")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. Atur Shift (Jam Bertugas):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    MukomukoVolunteerPoskoCatalog.SHIFT_OPTIONS.forEach { shiftOption ->
                        val isSelected = selectedShift == shiftOption
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFFFEDD5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedShift = shiftOption }
                                .testTag("shift_option_$shiftOption")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFEA580C) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = shiftOption,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF9A3412) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Menekan 'Simpan Penugasan' otomatis men-generate PDF SURAT TUGAS RELAWAN resmi BPBD Mukomuko.",
                            fontSize = 10.sp,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmAssign(
                        selectedPosko.districtName,
                        customPoskoName.trim().ifBlank { selectedPosko.poskoName },
                        selectedShift
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                modifier = Modifier.testTag("btn_confirm_volunteer_assignment")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Penugasan", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * HALAMAN / MODAL DETAIL RELAWAN:
 * Menampilkan dua tombol besar [ CHECK-IN POSKO ] (Hijau) & [ CHECK-OUT POSKO ] (Merah),
 * status kehadiran real-time ("Sedang Bertugas di Posko ... sejak 08.00 WIB"),
 * serta tombol "Unduh Surat Tugas" (PDF).
 */
@Composable
internal fun VolunteerDetailModalDialog(
    volunteer: VolunteerEntity,
    isAdminView: Boolean,
    onDismiss: () -> Unit,
    onCheckInPosko: () -> Unit,
    onCheckOutPosko: () -> Unit,
    onDownloadSuratTugas: () -> Unit
) {
    val statusEnum = volunteer.statusEnum()
    val displayedNik = VolunteerPrivacyHelper.maskNik(volunteer.nik, isAdminView)
    val displayedPhone = VolunteerPrivacyHelper.maskPhone(volunteer.phoneNumber, isAdminView)
    val isCheckedIn = volunteer.isCurrentlyCheckedInAtPosko()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("modal_detail_volunteer"),
        title = {
            Column {
                Text(
                    text = "Detail & Kehadiran Posko Relawan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${volunteer.fullName} • ${volunteer.organization}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NIK (Primary Key): $displayedNik",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ID Registrasi: ${volunteer.id} • HP: $displayedPhone",
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Keahlian: ${volunteer.skillsList().joinToString(", ")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                        Text(
                            text = "Status: ${statusEnum.code} (${statusEnum.displayLabel})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                if (statusEnum == VolunteerStatus.ASSIGNED) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCheckedIn) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.5.dp,
                            if (isCheckedIn) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "STATUS KEHADIRAN REAL-TIME:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCheckedIn) Color(0xFF047857) else Color(0xFFB45309)
                            )
                            Text(
                                text = volunteer.realtimeAttendanceStatusText(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCheckedIn) Color(0xFF065F46) else Color(0xFF92400E),
                                modifier = Modifier.testTag("detail_realtime_attendance_text")
                            )
                            if (volunteer.waktuCheckIn != null) {
                                Text(
                                    text = "Timestamp waktu_checkin: ${VolunteerPrivacyHelper.formatWibDateTime(volunteer.waktuCheckIn)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                            if (volunteer.waktuCheckOut != null) {
                                Text(
                                    text = "Timestamp waktu_checkout: ${VolunteerPrivacyHelper.formatWibDateTime(volunteer.waktuCheckOut)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onCheckInPosko,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_detail_checkin_posko")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ CHECK-IN POSKO ]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onCheckOutPosko,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_detail_checkout_posko")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ CHECK-OUT POSKO ]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onDownloadSuratTugas,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_detail_download_surat_tugas")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unduh Surat Tugas (PDF)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

/**
 * DIALOG PRATINJAU & UNDUH SURAT TUGAS DIGITAL (PDF)
 */
@Composable
internal fun SuratTugasDigitalPreviewDialog(
    volunteer: VolunteerEntity,
    officerName: String,
    onDismiss: () -> Unit,
    onDownloadPdfFile: () -> Unit
) {
    val letterNumber = remember(volunteer) {
        volunteer.suratTugasNumber.ifBlank {
            VolunteerAssignmentPdfGenerator.buildLetterNumber(volunteer.nik)
        }
    }
    val dateStr = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_preview_surat_tugas_pdf"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Surat Tugas Digital (PDF)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StandardKopSuratHeader(compact = true)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SURAT TUGAS RELAWAN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Nomor: $letterNumber",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Nama Lengkap : ${volunteer.fullName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("NIK : ${volunteer.nik}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Organisasi : ${volunteer.organization}", fontSize = 11.sp)
                        Text("Keahlian : ${volunteer.skillsList().joinToString(", ")}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Lokasi Posko : ${volunteer.effectivePoskoName()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Kecamatan : Kec. ${volunteer.effectivePoskoDistrict()}, Kab. Mukomuko", fontSize = 11.sp)
                        Text("Shift Bertugas : ${volunteer.assignedShift.ifBlank { "Shift Pagi (08:00 - 16:00 WIB)" }}", fontSize = 11.sp)
                        Text("Tanggal : $dateStr", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DITANDATANGANI SECARA DIGITAL — PUSDALOPS-PB BPBD MUKOMUKO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Pejabat Penugas: ${officerName.uppercase()} • ID: ${volunteer.id}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownloadPdfFile,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("btn_confirm_download_surat_tugas_pdf")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unduh Surat Tugas (PDF)", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

internal fun copyVolunteerPhotoToInternalStorage(
    context: Context,
    sourceUri: Uri,
    prefix: String
): String? {
    return try {
        val dir = File(context.filesDir, "volunteer_attachments").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val targetFile = File(dir, "${prefix}_${stamp}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile.absolutePath
    } catch (_: Exception) {
        sourceUri.toString()
    }
}

/**
 * Panel Unduh Daftar Data Seluruh Relawan (Khusus Sesi Aktif Petugas):
 * Digunakan untuk arsip resmi, pelaporan secara berjenjang kepada pihak yang berkompeten,
 * maupun dalam pengakhiran waktu Penanggulangan Bencana sebagai Dokumentasi Kegiatan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VolunteerActiveSessionArchiveExportCard(
    totalVolunteers: Int,
    officerName: String,
    onExportPdf: (purpose: String) -> Unit,
    onExportCsv: (purpose: String) -> Unit
) {
    val purposeOptions = remember {
        listOf(
            "Arsip & Pelaporan Berjenjang kepada Pihak Berkompeten",
            "Dokumentasi Kegiatan Pengakhiran Waktu Penanggulangan Bencana",
            "Arsip Gabungan Pelaporan Berjenjang & Dokumentasi Pasca Tanggap Darurat"
        )
    }
    var selectedPurpose by rememberSaveable { mutableStateOf(purposeOptions.first()) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF0D9488)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_volunteer_active_session_export")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0D9488).copy(alpha = 0.22f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF2DD4BF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "UNDUH DAFTAR DATA SELURUH RELAWAN ($totalVolunteers PERSONEL)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Arsip Pelaporan Berjenjang & Dokumentasi Pengakhiran Penanggulangan Bencana",
                            fontSize = 9.5.sp,
                            color = Color(0xFF99F6E4),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF065F46),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "SESI AKTIF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Unduh daftar lengkap seluruh relawan (NIK KTP utuh, Organisasi, Keahlian, Posko Penugasan, Waktu Check-in/Out, & Nomor Surat Tugas) untuk pelaporan berjenjang kepada pimpinan/instansi berkompeten atau dokumentasi pengakhiran operasi penanggulangan bencana.",
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 14.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pilih Peruntukan Dokumen:",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    purposeOptions.forEachIndexed { idx, option ->
                        val isSelected = selectedPurpose == option
                        val shortLabel = when (idx) {
                            0 -> "Pelaporan Berjenjang"
                            1 -> "Pengakhiran Penanggulangan Bencana"
                            else -> "Arsip & Dokumentasi Lengkap"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPurpose = option },
                            label = {
                                Text(
                                    text = shortLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("chip_volunteer_export_purpose_$idx")
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExportPdf(selectedPurpose) },
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_download_all_volunteers_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Unduh PDF Data Seluruh Relawan",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unduh PDF Relawan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { onExportCsv(selectedPurpose) },
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_download_all_volunteers_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Unduh CSV Excel Data Seluruh Relawan",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unduh CSV (Excel)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
