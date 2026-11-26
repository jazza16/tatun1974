package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DalaAssessmentState
import com.example.model.UrgentNeedCategory
import com.example.model.UrgentNeedItem
import com.example.model.UrgentNeedStatus
import com.example.model.VulnerableGroupCategory
import com.example.viewmodel.DalaAssessmentStateManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val BpbdOrange = Color(0xFFEA580C)
private val BpbdOrangeSoft = Color(0xFFFFF7ED)
private val BpbdOrangeBorder = Color(0xFFFED7AA)
private val AlertRed = Color(0xFFDC2626)
private val AlertRedSoft = Color(0xFFFEF2F2)
private val SafeGreen = Color(0xFF15803D)
private val SafeGreenSoft = Color(0xFFF0FDF4)
private val SlateText = Color(0xFF475569)

/**
 * Komponen Accordion / Collapsible Clean UI agar form lapangan panjang tetap rapi dan fokus.
 */
@Composable
fun DalaAccordionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    initiallyExpanded: Boolean = true,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(testTag) { mutableStateOf(initiallyExpanded) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) primaryColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BpbdOrangeSoft,
                        border = BorderStroke(1.dp, BpbdOrangeBorder),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BpbdOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Tutup bagian $title" else "Buka bagian $title",
                    tint = SlateText
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    content()
                }
            }
        }
    }
}

/**
 * Input angka tervalidasi ketat (digits-only) dengan suffix satuan dan tooltip/keterangan lapangan.
 */
@Composable
fun DalaNumericField(
    label: String,
    valueText: String,
    onDigitsChanged: (String) -> Unit,
    unitSuffix: String,
    helperTooltip: String,
    testTag: String,
    modifier: Modifier = Modifier,
    maxDigits: Int = 7,
    imeAction: ImeAction = ImeAction.Next
) {
    val keyboardActions = rememberFormKeyboardActions()
    Column(modifier = modifier) {
        OutlinedTextField(
            value = valueText,
            onValueChange = { raw ->
                val sanitized = DalaAssessmentStateManager.sanitizeDigitsOnly(raw, maxDigits)
                onDigitsChanged(sanitized)
            },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            suffix = {
                Text(
                    text = unitSuffix,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BpbdOrange
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .formScrollPadding()
                .testTag(testTag)
        )
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SlateText.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = helperTooltip,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = SlateText
            )
        }
    }
}

// =================================================================================================
// TAB 1: KORBAN & PENGUNGSI (CASUALTIES & EVACUEES)
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DalaCasualtiesTab(
    state: DalaAssessmentState,
    stateManager: DalaAssessmentStateManager
) {
    val keyboardActions = rememberFormKeyboardActions()
    var newSiteName by rememberSaveable { mutableStateOf("") }
    var newSiteEvacuees by rememberSaveable { mutableStateOf("") }
    var newSiteNotes by rememberSaveable { mutableStateOf("") }

    var selectedVulnerableCategory by remember { mutableStateOf(VulnerableGroupCategory.BALITA) }
    var vulnerableDropdownExpanded by remember { mutableStateOf(false) }
    var vulnerableCountInput by rememberSaveable { mutableStateOf("") }
    var vulnerableNotesInput by rememberSaveable { mutableStateOf("") }

    val presetLocations = listOf(
        "Balai Desa",
        "Masjid / Musholla",
        "Gedung Sekolah",
        "Lapangan / Tenda BPBD"
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val poskoRepository = remember { com.example.data.repository.PoskoLogisticsRepository.getInstance(context) }
    val poskoUnits by poskoRepository.poskoUnits.collectAsStateWithLifecycle()
    val lockedSnapshot by poskoRepository.dalaLockedSnapshot.collectAsStateWithLifecycle()

    val activePoskoSnapshot = remember(poskoUnits, lockedSnapshot) {
        lockedSnapshot ?: poskoRepository.getLiveAggregatedSnapshot()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dala_tab_casualties_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. PANEL READ-ONLY AUTO-POPULATE PENGUNGSI DARI MODUL POSKO & LOGISTIK (TAHAP 4 PRD)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activePoskoSnapshot.isLocked) Color(0xFF0F172A) else Color(0xFF1E293B)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (activePoskoSnapshot.isLocked) Color(0xFF38BDF8) else BpbdOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_dala_posko_autopopulate_readonly")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activePoskoSnapshot.isLocked) {
                                "DATA PENGUNGSI POSKO (SNAPSHOT TERKUNCI)"
                            } else {
                                "DATA PENGUNGSI POSKO (AUTO-POPULATE)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activePoskoSnapshot.isLocked) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "READ-ONLY",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Incident: ${activePoskoSnapshot.incidentId} (${activePoskoSnapshot.activePoskoCount} Posko Aktif) • Status: ${activePoskoSnapshot.lockedAtIso}",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Pengungsi Terdata Posko:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${activePoskoSnapshot.totalJiwa} Jiwa",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lansia: ${activePoskoSnapshot.lansia} • Ibu Hamil: ${activePoskoSnapshot.ibuHamil} • Balita: ${activePoskoSnapshot.balita} • Disabilitas: ${activePoskoSnapshot.disabilitas}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFDE047)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val snap = poskoRepository.captureAndLockDalaSnapshot(state.officerName)
                            android.widget.Toast.makeText(
                                context,
                                "Snapshot Data Posko (${snap.totalJiwa} Jiwa) dikunci untuk Laporan DaLA!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activePoskoSnapshot.isLocked) Color(0xFF0284C7) else BpbdOrange
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_dala_capture_posko_snapshot")
                    ) {
                        Text(
                            text = if (activePoskoSnapshot.isLocked) {
                                "PERBARUI & KUNCI ULANG SNAPSHOT DATA POSKO"
                            } else {
                                "AMBIL SNAPSHOT DATA POSKO (KUNCI DATA)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Identitas Kejadian (Collapsible agar tidak memenuhi layar)
        item {
            DalaAccordionCard(
                title = "Identitas Kejadian Kaji Cepat",
                subtitle = state.disasterTitle.ifBlank { "Isi nama kejadian & wilayah terdampak" },
                icon = Icons.Default.EditNote,
                badgeText = state.assessmentId,
                initiallyExpanded = false,
                testTag = "accordion_dala_incident_info"
            ) {
                OutlinedTextField(
                    value = state.disasterTitle,
                    onValueChange = { stateManager.updateHeaderInfo(disasterTitle = it) },
                    label = { Text("Nama / Jenis Kejadian Bencana") },
                    placeholder = { Text("Contoh: Banjir Bandang & Longsor") },
                    singleLine = true,
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_dala_disaster_title")
                )
                OutlinedTextField(
                    value = state.district,
                    onValueChange = { stateManager.updateHeaderInfo(district = it) },
                    label = { Text("Kecamatan / Wilayah Terdampak") },
                    placeholder = { Text("Contoh: Kec. Kota Mukomuko") },
                    singleLine = true,
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_dala_district")
                )
            }
        }

        // Korban Jiwa & Pengungsi
        item {
            DalaAccordionCard(
                title = "Data Korban Jiwa & Pengungsi",
                subtitle = "Hanya menerima angka bulat positif sesuai kaji cepat",
                icon = Icons.Default.MedicalServices,
                badgeText = "${state.casualties.totalCasualties} Korban",
                initiallyExpanded = true,
                testTag = "accordion_dala_casualties"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DalaNumericField(
                        label = "Meninggal",
                        valueText = if (state.casualties.dead == 0) "" else state.casualties.dead.toString(),
                        onDigitsChanged = { d ->
                            stateManager.updateCasualties { it.copy(dead = DalaAssessmentStateManager.parseNonNegativeInt(d)) }
                        },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Korban terkonfirmasi MD",
                        testTag = "input_casualty_dead",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    DalaNumericField(
                        label = "Hilang",
                        valueText = if (state.casualties.missing == 0) "" else state.casualties.missing.toString(),
                        onDigitsChanged = { d ->
                            stateManager.updateCasualties { it.copy(missing = DalaAssessmentStateManager.parseNonNegativeInt(d)) }
                        },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Dalam pencarian SAR",
                        testTag = "input_casualty_missing",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DalaNumericField(
                        label = "Luka Berat",
                        valueText = if (state.casualties.heavyInjured == 0) "" else state.casualties.heavyInjured.toString(),
                        onDigitsChanged = { d ->
                            stateManager.updateCasualties { it.copy(heavyInjured = DalaAssessmentStateManager.parseNonNegativeInt(d)) }
                        },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Perlu rawat inap RS",
                        testTag = "input_casualty_heavy",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    DalaNumericField(
                        label = "Luka Ringan",
                        valueText = if (state.casualties.lightInjured == 0) "" else state.casualties.lightInjured.toString(),
                        onDigitsChanged = { d ->
                            stateManager.updateCasualties { it.copy(lightInjured = DalaAssessmentStateManager.parseNonNegativeInt(d)) }
                        },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Rawat jalan / P3K",
                        testTag = "input_casualty_light",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                DalaNumericField(
                    label = "Total Jiwa Mengungsi",
                    valueText = if (state.casualties.totalEvacuees == 0) "" else state.casualties.totalEvacuees.toString(),
                    onDigitsChanged = { d ->
                        stateManager.updateCasualties { it.copy(totalEvacuees = DalaAssessmentStateManager.parseNonNegativeInt(d)) }
                    },
                    unitSuffix = "Jiwa",
                    helperTooltip = "Akumulasi pengungsi di seluruh posko & rumah kerabat",
                    testTag = "input_total_evacuees",
                    imeAction = ImeAction.Done
                )
            }
        }

        // Titik Lokasi Pengungsian (Bisa tambah > 1 titik)
        item {
            DalaAccordionCard(
                title = "Titik Lokasi Pengungsian",
                subtitle = "Tambahkan seluruh posko pengungsian aktif",
                icon = Icons.Default.LocationOn,
                badgeText = "${state.casualties.evacuationSites.size} Titik",
                initiallyExpanded = true,
                testTag = "accordion_dala_evacuation_sites"
            ) {
                if (state.casualties.evacuationSites.isEmpty()) {
                    Text(
                        text = "Belum ada titik pengungsian tercatat. Gunakan formulir di bawah untuk menambahkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.casualties.evacuationSites.forEach { site ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BpbdOrangeSoft,
                                border = BorderStroke(1.dp, BpbdOrangeBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.locationName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${site.evacueeCount} Jiwa" + if (site.notes.isNotBlank()) " • ${site.notes}" else "",
                                            fontSize = 12.sp,
                                            color = SlateText
                                        )
                                    }
                                    IconButton(
                                        onClick = { stateManager.removeEvacuationSite(site.id) },
                                        modifier = Modifier.testTag("btn_remove_site_${site.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus titik ${site.locationName}",
                                            tint = AlertRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Pilih cepat jenis lokasi atau ketik nama titik baru:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetLocations.forEach { preset ->
                        FilterChip(
                            selected = newSiteName.startsWith(preset, ignoreCase = true),
                            onClick = { newSiteName = "$preset " },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = newSiteName,
                    onValueChange = { newSiteName = it },
                    label = { Text("Nama Titik Pengungsian") },
                    placeholder = { Text("Contoh: Balai Desa Tanah Rekah") },
                    singleLine = true,
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_new_evacuation_site_name")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DalaNumericField(
                        label = "Jml Pengungsi",
                        valueText = newSiteEvacuees,
                        onDigitsChanged = { newSiteEvacuees = it },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Di titik ini",
                        testTag = "input_new_evacuation_site_count",
                        modifier = Modifier.weight(1.05f),
                        imeAction = ImeAction.Next
                    )
                    OutlinedTextField(
                        value = newSiteNotes,
                        onValueChange = { newSiteNotes = it },
                        label = { Text("Catatan Singkat", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Fasilitas / kondisi") },
                        singleLine = true,
                        keyboardOptions = formDoneKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .formScrollPadding()
                    )
                }
                Button(
                    onClick = {
                        if (newSiteName.isNotBlank()) {
                            stateManager.addEvacuationSite(
                                locationName = newSiteName,
                                evacueeCount = DalaAssessmentStateManager.parseNonNegativeInt(newSiteEvacuees),
                                notes = newSiteNotes
                            )
                            newSiteName = ""
                            newSiteEvacuees = ""
                            newSiteNotes = ""
                        }
                    },
                    enabled = newSiteName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_evacuation_site")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Titik Pengungsian", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Kelompok Rentan
        item {
            DalaAccordionCard(
                title = "Kelompok Rentan",
                subtitle = "Balita, Ibu Hamil, Lansia & Penyandang Disabilitas",
                icon = Icons.Default.Diversity3,
                badgeText = "${state.casualties.totalVulnerable} Jiwa",
                initiallyExpanded = false,
                testTag = "accordion_dala_vulnerable_groups"
            ) {
                ExposedDropdownMenuBox(
                    expanded = vulnerableDropdownExpanded,
                    onExpandedChange = { vulnerableDropdownExpanded = !vulnerableDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedVulnerableCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori Kelompok Rentan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vulnerableDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("dropdown_vulnerable_category")
                    )
                    ExposedDropdownMenu(
                        expanded = vulnerableDropdownExpanded,
                        onDismissRequest = { vulnerableDropdownExpanded = false }
                    ) {
                        VulnerableGroupCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label) },
                                onClick = {
                                    selectedVulnerableCategory = category
                                    vulnerableDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DalaNumericField(
                        label = "Jumlah",
                        valueText = vulnerableCountInput,
                        onDigitsChanged = { vulnerableCountInput = it },
                        unitSuffix = "Jiwa",
                        helperTooltip = "Total kategori ini",
                        testTag = "input_vulnerable_count",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    OutlinedTextField(
                        value = vulnerableNotesInput,
                        onValueChange = { vulnerableNotesInput = it },
                        label = { Text("Kebutuhan Khusus") },
                        placeholder = { Text("Susu, obat, dll") },
                        singleLine = true,
                        keyboardOptions = formDoneKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .formScrollPadding()
                    )
                }

                OutlinedButton(
                    onClick = {
                        val qty = DalaAssessmentStateManager.parseNonNegativeInt(vulnerableCountInput)
                        if (qty > 0) {
                            stateManager.addOrUpdateVulnerableGroup(
                                category = selectedVulnerableCategory,
                                count = qty,
                                notes = vulnerableNotesInput
                            )
                            vulnerableCountInput = ""
                            vulnerableNotesInput = ""
                        }
                    },
                    enabled = DalaAssessmentStateManager.parseNonNegativeInt(vulnerableCountInput) > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_vulnerable_group")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Kelompok Rentan")
                }

                if (state.casualties.vulnerableGroups.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.casualties.vulnerableGroups.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.category.label}: ${entry.count} Jiwa",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (entry.notes.isNotBlank()) {
                                            Text(entry.notes, fontSize = 11.sp, color = SlateText)
                                        }
                                    }
                                    IconButton(
                                        onClick = { stateManager.removeVulnerableGroup(entry.id) },
                                        modifier = Modifier.testTag("btn_remove_vg_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus ${entry.category.label}",
                                            tint = AlertRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================================
// TAB 2: KERUSAKAN & KERUGIAN (DAMAGES & LOSSES)
// =================================================================================================

@Composable
fun DalaDamagesTab(
    state: DalaAssessmentState,
    stateManager: DalaAssessmentStateManager
) {
    val keyboardActions = rememberFormKeyboardActions()
    val d = state.damages

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dala_tab_damages_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Kategori 1: Rumah
        item {
            DalaAccordionCard(
                title = "Kerusakan Rumah Warga",
                subtitle = "Klasifikasi Rusak Berat (RB), Sedang (RS), Ringan (RR)",
                icon = Icons.Default.HomeWork,
                badgeText = "${d.totalHousesDamaged} Unit",
                initiallyExpanded = true,
                testTag = "accordion_damage_houses"
            ) {
                DalaNumericField(
                    label = "Rumah Rusak Berat (RB)",
                    valueText = if (d.houseHeavy == 0) "" else d.houseHeavy.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(houseHeavy = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Roboh total / struktur utama rusak & tidak layak huni",
                    testTag = "input_house_heavy",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Rumah Rusak Sedang (RS)",
                    valueText = if (d.houseModerate == 0) "" else d.houseModerate.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(houseModerate = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Sebagian struktur rusak, masih dapat diperbaiki",
                    testTag = "input_house_moderate",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Rumah Rusak Ringan (RR)",
                    valueText = if (d.houseLight == 0) "" else d.houseLight.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(houseLight = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Kerusakan non-struktural (atap, plafon, dinding retak)",
                    testTag = "input_house_light",
                    imeAction = ImeAction.Done
                )
            }
        }

        // Kategori 2: Fasilitas Umum
        item {
            DalaAccordionCard(
                title = "Fasilitas Umum & Sosial",
                subtitle = "Sekolah, Puskesmas/Faskes, dan Kantor Pelayanan",
                icon = Icons.Default.AccountBalance,
                badgeText = "${d.totalPublicFacilitiesDamaged} Unit",
                initiallyExpanded = false,
                testTag = "accordion_damage_public_facilities"
            ) {
                DalaNumericField(
                    label = "Sekolah / Pendidikan",
                    valueText = if (d.schoolUnits == 0) "" else d.schoolUnits.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(schoolUnits = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "SD, SMP, SMA, atau PAUD terdampak",
                    testTag = "input_damage_school",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Puskesmas / Faskes",
                    valueText = if (d.puskesmasUnits == 0) "" else d.puskesmasUnits.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(puskesmasUnits = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Puskesmas, Pustu, atau Polindes terdampak",
                    testTag = "input_damage_puskesmas",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Kantor Pemerintahan / Desa",
                    valueText = if (d.officeUnits == 0) "" else d.officeUnits.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(officeUnits = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Kantor desa, kecamatan, atau layanan publik",
                    testTag = "input_damage_office",
                    imeAction = ImeAction.Done
                )
            }
        }

        // Kategori 3: Infrastruktur
        item {
            DalaAccordionCard(
                title = "Kerusakan Infrastruktur",
                subtitle = "Akses jalan, jembatan penghubung, dan irigasi",
                icon = Icons.Default.Engineering,
                badgeText = "${d.roadMeters}m • ${d.bridgeUnits} Jbt",
                initiallyExpanded = false,
                testTag = "accordion_damage_infrastructure"
            ) {
                DalaNumericField(
                    label = "Jalan Rusak / Terputus",
                    valueText = if (d.roadMeters == 0) "" else d.roadMeters.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(roadMeters = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Meter",
                    helperTooltip = "Panjang total badan jalan amblas / tertimbun",
                    testTag = "input_damage_road",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Jembatan Rusak / Putus",
                    valueText = if (d.bridgeUnits == 0) "" else d.bridgeUnits.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(bridgeUnits = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Unit",
                    helperTooltip = "Jumlah titik jembatan rusak berat atau terputus",
                    testTag = "input_damage_bridge",
                    imeAction = ImeAction.Next
                )
                DalaNumericField(
                    label = "Saluran Irigasi / Tanggul",
                    valueText = if (d.irrigationMeters == 0) "" else d.irrigationMeters.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(irrigationMeters = DalaAssessmentStateManager.parseNonNegativeInt(v)) }
                    },
                    unitSuffix = "Meter",
                    helperTooltip = "Panjang jaringan irigasi jebol / tertutup sedimen",
                    testTag = "input_damage_irrigation",
                    imeAction = ImeAction.Done
                )
            }
        }

        // Estimasi Kerugian (Rp) & Deskripsi Kerusakan
        item {
            DalaAccordionCard(
                title = "Estimasi Kerugian & Deskripsi",
                subtitle = "Taksiran nilai ekonomi (Rp) dan kronologi kerusakan",
                icon = Icons.Default.AttachMoney,
                badgeText = d.formatEstimatedLossRupiah(),
                initiallyExpanded = true,
                testTag = "accordion_damage_losses"
            ) {
                DalaNumericField(
                    label = "Estimasi Kerugian (Rupiah)",
                    valueText = if (d.estimatedLossIdr == 0L) "" else d.estimatedLossIdr.toString(),
                    onDigitsChanged = { v ->
                        stateManager.updateDamages { it.copy(estimatedLossIdr = DalaAssessmentStateManager.parseNonNegativeLong(v)) }
                    },
                    unitSuffix = "Rp",
                    helperTooltip = "Terbaca: ${d.formatEstimatedLossRupiah()} (isi angka saja tanpa titik/koma)",
                    testTag = "input_estimated_loss_idr",
                    maxDigits = 14,
                    imeAction = ImeAction.Next
                )

                OutlinedTextField(
                    value = d.damageDescription,
                    onValueChange = { desc ->
                        stateManager.updateDamages { it.copy(damageDescription = desc) }
                    },
                    label = { Text("Deskripsi Kerusakan Lapangan") },
                    placeholder = { Text("Jelaskan dampak genangan, akses jalan terisolir, kondisi jaringan listrik/air...") },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_damage_description")
                )
            }
        }
    }
}

// =================================================================================================
// TAB 3: KEBUTUHAN MENDESAK (URGENT NEEDS)
// =================================================================================================

@Composable
fun DalaUrgentNeedsTab(
    state: DalaAssessmentState,
    stateManager: DalaAssessmentStateManager
) {
    var showAddNeedDialog by remember { mutableStateOf(false) }
    val pendingCount = state.urgentNeedsPendingCount
    val availableCount = state.urgentNeeds.size - pendingCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dala_tab_urgent_needs_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Ringkasan + Tombol "+ Tambah Kebutuhan"
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BpbdOrangeSoft),
                border = BorderStroke(1.dp, BpbdOrangeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daftar Kebutuhan Mendesak",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ketuk lencana status pada item untuk mengubah Mendesak ↔ Tersedia",
                                fontSize = 11.sp,
                                color = SlateText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeedStatusPill(
                            label = "Total: ${state.urgentNeeds.size}",
                            bgColor = Color.White,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        NeedStatusPill(
                            label = "Mendesak: $pendingCount",
                            bgColor = AlertRedSoft,
                            textColor = AlertRed,
                            modifier = Modifier.weight(1f)
                        )
                        NeedStatusPill(
                            label = "Tersedia: $availableCount",
                            bgColor = SafeGreenSoft,
                            textColor = SafeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { showAddNeedDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_add_urgent_need")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Tambah Kebutuhan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.urgentNeeds.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = SlateText.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Belum ada kebutuhan mendesak tercatat",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tekan tombol \"+ Tambah Kebutuhan\" untuk mencatat logistik, medis, air bersih, tenda, atau alat berat.",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                    }
                }
            }
        } else {
            items(state.urgentNeeds, key = { it.id }) { item ->
                UrgentNeedItemCard(
                    item = item,
                    onToggleStatus = { stateManager.toggleUrgentNeedStatus(item.id) },
                    onDelete = { stateManager.removeUrgentNeed(item.id) }
                )
            }
        }
    }

    if (showAddNeedDialog) {
        AddUrgentNeedDialog(
            onDismiss = { showAddNeedDialog = false },
            onConfirm = { category, quantity, unit, status, notes ->
                stateManager.addUrgentNeed(category, quantity, unit, status, notes)
                showAddNeedDialog = false
            }
        )
    }
}

@Composable
private fun NeedStatusPill(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UrgentNeedItemCard(
    item: UrgentNeedItem,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isUrgent = item.status == UrgentNeedStatus.MENDESAK
    val statusBg = if (isUrgent) AlertRedSoft else SafeGreenSoft
    val statusColor = if (isUrgent) AlertRed else SafeGreen

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_urgent_need_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BpbdOrangeSoft,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = item.category.label,
                                tint = BpbdOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.category.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = BpbdOrange
                        )
                    }
                }

                // Status badge yang bisa ditekan untuk toggle Mendesak / Tersedia
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clickable { onToggleStatus() }
                        .testTag("btn_toggle_need_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Default.PriorityHigh else Icons.Default.CheckCircle,
                            contentDescription = "Ubah status ${item.category.label}",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("btn_delete_need_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus kebutuhan ${item.category.label}",
                        tint = AlertRed
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Text(
                    text = "Keterangan: ${item.notes}",
                    fontSize = 12.sp,
                    color = SlateText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUrgentNeedDialog(
    onDismiss: () -> Unit,
    onConfirm: (UrgentNeedCategory, Int, String, UrgentNeedStatus, String) -> Unit
) {
    val keyboardActions = rememberFormKeyboardActions()
    var category by remember { mutableStateOf(UrgentNeedCategory.LOGISTIK) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("10") }
    var unitText by remember { mutableStateOf(UrgentNeedCategory.LOGISTIK.defaultUnit) }
    var status by remember { mutableStateOf(UrgentNeedStatus.MENDESAK) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Kebutuhan Mendesak",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jenis Kebutuhan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("dropdown_need_category")
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        UrgentNeedCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.label} (${cat.defaultUnit})") },
                                onClick = {
                                    category = cat
                                    unitText = cat.defaultUnit
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = DalaAssessmentStateManager.sanitizeDigitsOnly(it, 7) },
                        label = { Text("Jumlah") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = keyboardActions,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding()
                            .testTag("input_need_quantity")
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Satuan") },
                        singleLine = true,
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding()
                            .testTag("input_need_unit")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Status Ketersediaan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UrgentNeedStatus.entries.forEach { s ->
                            val isSelected = status == s
                            FilterChip(
                                selected = isSelected,
                                onClick = { status = s },
                                label = { Text(s.label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (s == UrgentNeedStatus.MENDESAK) AlertRedSoft else SafeGreenSoft,
                                    selectedLabelColor = if (s == UrgentNeedStatus.MENDESAK) AlertRed else SafeGreen
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan / Spesifikasi") },
                    placeholder = { Text("Contoh: Sembako siap saji untuk 3 hari") },
                    maxLines = 3,
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_need_notes")
                )
            }
        },
        confirmButton = {
            val qty = DalaAssessmentStateManager.parseNonNegativeInt(quantityText)
            Button(
                onClick = { onConfirm(category, qty.coerceAtLeast(1), unitText, status, notes) },
                enabled = qty > 0 && unitText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BpbdOrange),
                modifier = Modifier.testTag("btn_confirm_add_need")
            ) {
                Text("Simpan Item", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
