package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.DalaPdfExportWrapper
import com.example.viewmodel.DalaAssessmentStateManager
import com.example.viewmodel.DalaAutosaveStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DalaOrange = Color(0xFFEA580C)
private val DalaOrangeDark = Color(0xFFC2410C)
private val DalaGreen = Color(0xFF15803D)

private enum class DalaTab(val stepNumber: Int, val shortTitle: String) {
    CASUALTIES(1, "Korban & Pengungsi"),
    DAMAGES(2, "Kerusakan & Kerugian"),
    URGENT_NEEDS(3, "Kebutuhan Mendesak"),
    SUMMARY(4, "Laporan & Rekap")
}

/**
 * Layar utama Modul "Pasca Bencana / DALA (Data dan Informasi Bencana)".
 *
 * Fitur:
 * - Navigasi 4 Sub-Tab Stepper (Korban & Pengungsi, Kerusakan & Kerugian, Kebutuhan Mendesak, Laporan & Rekap).
 * - Autosave lokal (SharedPreferences JSON) secara real-time dengan indikator status pada AppBar.
 * - Ekspor PDF standar BNPB melalui DalaPdfExportWrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DalaAssessmentScreen(
    officerName: String = "",
    officerNip: String = "",
    initialDistrict: String = "Kec. Kota Mukomuko",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stateManager = remember(officerName, officerNip, initialDistrict) {
        DalaAssessmentStateManager(
            context = context,
            initialOfficerName = officerName,
            initialOfficerNip = officerNip,
            initialDistrict = initialDistrict
        )
    }

    val assessmentState by stateManager.state.collectAsStateWithLifecycle()
    val autosaveStatus by stateManager.autosaveStatus.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    val tabs = DalaTab.entries

    // Pastikan draft tersimpan sebelum menutup layar lewat tombol Back sistem
    BackHandler {
        stateManager.persistNow(markSubmitted = false)
        onClose()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Pasca Bencana / DALA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AutosaveStatusBadge(status = autosaveStatus)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                stateManager.persistNow(markSubmitted = false)
                                onClose()
                            },
                            modifier = Modifier.testTag("btn_dala_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Modul DALA",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("btn_dala_more_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi Data Simulasi & Reset",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Muat Data Simulasi BPBD") },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        stateManager.loadDummySimulation()
                                        Toast.makeText(context, "Data simulasi kaji cepat DALA dimuat.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kosongkan Form Baru") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        stateManager.clearFormToEmpty()
                                        Toast.makeText(context, "Form DALA dikosongkan untuk kejadian baru.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DalaOrange,
                        titleContentColor = Color.White
                    )
                )

                // Step-by-Step 4 Sub-Tab Bar
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = DalaOrange,
                    edgePadding = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dala_tab_row")
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("dala_tab_${tab.stepNumber}"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) DalaOrange else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = tab.stepNumber.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = tab.shortTitle,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (selectedTabIndex > 0) selectedTabIndex -= 1 },
                        enabled = selectedTabIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_dala_prev_step")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sebelumnya", fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = "Langkah ${selectedTabIndex + 1}/${tabs.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DalaOrangeDark
                    )

                    if (selectedTabIndex < tabs.lastIndex) {
                        Button(
                            onClick = { selectedTabIndex += 1 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DalaOrange),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_dala_next_step")
                        ) {
                            Text("Lanjut", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                stateManager.persistNow(markSubmitted = false)
                                onClose()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DalaGreen),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_dala_finish")
                        ) {
                            Text("Selesai", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tabs[selectedTabIndex]) {
                DalaTab.CASUALTIES -> DalaCasualtiesTab(
                    state = assessmentState,
                    stateManager = stateManager
                )
                DalaTab.DAMAGES -> DalaDamagesTab(
                    state = assessmentState,
                    stateManager = stateManager
                )
                DalaTab.URGENT_NEEDS -> DalaUrgentNeedsTab(
                    state = assessmentState,
                    stateManager = stateManager
                )
                DalaTab.SUMMARY -> DalaSummaryTab(
                    state = assessmentState,
                    isExportingPdf = isExportingPdf,
                    onSaveDraft = {
                        stateManager.persistNow(markSubmitted = false)
                        Toast.makeText(context, "Draft Kaji Cepat DALA berhasil disimpan di perangkat.", Toast.LENGTH_SHORT).show()
                    },
                    onSubmitToPusdalops = {
                        stateManager.persistNow(markSubmitted = true)
                        Toast.makeText(context, "Laporan DALA berhasil dikirim ke Pusdalops BPBD!", Toast.LENGTH_LONG).show()
                    },
                    onExportPdf = {
                        if (!isExportingPdf) {
                            isExportingPdf = true
                            stateManager.persistNow(markSubmitted = false)
                            coroutineScope.launch {
                                try {
                                    DalaPdfExportWrapper.exportAndSharePdf(context, stateManager.state.value)
                                } finally {
                                    isExportingPdf = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AutosaveStatusBadge(status: DalaAutosaveStatus) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val (label, icon) = when (status) {
        is DalaAutosaveStatus.Idle -> "Autosave Lokal Aktif" to Icons.Default.CloudDone
        is DalaAutosaveStatus.Saving -> "Menyimpan otomatis..." to Icons.Default.Sync
        is DalaAutosaveStatus.Saved -> "Tersimpan otomatis ${timeFormatter.format(Date(status.timestampMs))}" to Icons.Default.CloudDone
        is DalaAutosaveStatus.Submitted -> "Terkirim Pusdalops ${timeFormatter.format(Date(status.timestampMs))}" to Icons.Default.CloudDone
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("badge_dala_autosave_status")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
