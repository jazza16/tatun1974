package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CommunityWeatherReport
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherRainBlue
import com.example.ui.theme.WeatherSunGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityReportsSection(
    reports: List<CommunityWeatherReport>,
    currentDistrict: District,
    isDialogOpen: Boolean,
    onOpenDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    onSubmitReport: (districtId: String, districtName: String, reporterName: String, condition: String, notes: String) -> Unit,
    onUpvote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("community_reports_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Banner with Call to Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(OrangePrimary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Laporan Warga",
                            tint = OrangeOnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Laporan Langsung Masyarakat",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Pantau kondisi riil cuaca dari warga di 15 kecamatan Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_report_dialog"),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = OrangeOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kirim Laporan Cuaca Wilayah Anda",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeOnPrimary
                    )
                }
            }
        }

        // List of Reports
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada laporan dari warga hari ini. Jadilah yang pertama melaporkan!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            reports.forEach { report ->
                ReportCardItem(
                    report = report,
                    onUpvote = { onUpvote(report.id) }
                )
            }
        }
    }

    // New Report Dialog
    if (isDialogOpen) {
        CreateReportDialog(
            currentDistrict = currentDistrict,
            onDismiss = onDismissDialog,
            onSubmit = onSubmitReport
        )
    }
}

@Composable
private fun ReportCardItem(
    report: CommunityWeatherReport,
    onUpvote: () -> Unit
) {
    val conditionColor = when {
        report.weatherCondition.contains("Hujan", ignoreCase = true) -> WeatherRainBlue
        report.weatherCondition.contains("Ombak", ignoreCase = true) || report.weatherCondition.contains("Angin", ignoreCase = true) -> OrangePrimary
        report.weatherCondition.contains("Banjir", ignoreCase = true) || report.weatherCondition.contains("Genangan", ignoreCase = true) -> Color(0xFFDC2626)
        else -> WeatherSunGold
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: District Badge & Weather Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.districtName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                }

                Surface(
                    color = conditionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = report.weatherCondition,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = conditionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notes / Citizen Observation
            Text(
                text = report.notes,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Reporter info, relative time, and upvote button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.reporterName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatRelativeTime(report.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onUpvote),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvote",
                            tint = OrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${report.upvotes} Membantu",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateReportDialog(
    currentDistrict: District,
    onDismiss: () -> Unit,
    onSubmit: (districtId: String, districtName: String, reporterName: String, condition: String, notes: String) -> Unit
) {
    var selectedDist by remember { mutableStateOf(currentDistrict) }
    var isDistrictDropdownOpen by remember { mutableStateOf(false) }
    var reporterName by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("Hujan Deras") }
    var notes by remember { mutableStateOf("") }

    val conditions = listOf(
        "Cerah Berawan",
        "Hujan Ringan",
        "Hujan Deras",
        "Hujan Petir",
        "Angin Kencang",
        "Ombak Tinggi",
        "Genangan Banjir"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Laporan Cuaca Warga",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // District selector
                Column {
                    Text(
                        text = "Pilih Kecamatan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isDistrictDropdownOpen = true },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDist.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isDistrictDropdownOpen,
                            onDismissRequest = { isDistrictDropdownOpen = false }
                        ) {
                            MukomukoDistricts.list.forEach { dist ->
                                DropdownMenuItem(
                                    text = { Text(dist.name) },
                                    onClick = {
                                        selectedDist = dist
                                        isDistrictDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Reporter name field
                OutlinedTextField(
                    value = reporterName,
                    onValueChange = { reporterName = it },
                    label = { Text("Nama Anda / Panggilan") },
                    placeholder = { Text("Contoh: Warga Nelayan Ipuh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Condition chips
                Column {
                    Text(
                        text = "Kondisi Cuaca Saat Ini:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        conditions.forEach { cond ->
                            val isChosen = selectedCondition == cond
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedCondition = cond },
                                color = if (isChosen) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = cond,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) OrangeOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan Lapangan") },
                    placeholder = { Text("Contoh: Hujan lebat disertai angin kencang sejak 30 menit lalu...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (notes.isNotBlank()) {
                        onSubmit(
                            selectedDist.id,
                            selectedDist.name,
                            reporterName.ifBlank { "Warga Mukomuko" },
                            selectedCondition,
                            notes.trim()
                        )
                    }
                },
                enabled = notes.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Kirim Laporan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (60 * 1000)
    return when {
        mins < 1 -> "Baru saja"
        mins < 60 -> "$mins menit lalu"
        mins < 1440 -> "${mins / 60} jam lalu"
        else -> "${mins / 1440} hari lalu"
    }
}
