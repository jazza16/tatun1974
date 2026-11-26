package com.example.ui.components

import android.media.AudioAttributes
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.model.MukomukoDistricts
import com.example.security.UserRole
import com.example.siren.FourEyesSirenDraft
import com.example.siren.FourEyesSirenManager
import com.example.siren.FourEyesSirenState
import com.example.siren.SirenAlertType
import com.example.util.EmergencyTonePlayer
import com.example.util.IndonesianTtsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Layar Kendali Sirene Otoritas Berlapis (Four-Eyes Principle) — Master PRD BAB 4.
 *
 * Fitur & Pengamanan:
 * - Terkunci ketat di balik sesi login petugas aktif via [InternalModuleSessionGuard].
 * - Alur state: `DRAFT -> MENUNGGU PERSETUJUAN -> DIAKTIFKAN` (atau `DITOLAK` / `DINONAKTIFKAN`).
 * - Pembuat draf DILARANG menyetujui drafnya sendiri (penolakan otomatis di UI & manager).
 * - Hanya pejabat berwenang (Kepala Pusdalops / Manajer / Kalaksa / Pengawas) yang dapat menyetujui & mengaktifkan.
 * - Pilihan cepat 7 kecamatan pesisir zona rawan tsunami (`isCoastalTsunamiZone`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FourEyesSirenControlScreen(
    officerName: String,
    officerNip: String,
    officerRole: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isOfficerSessionActive = com.example.data.local.OfficerAuthManager(context).isOfficerLoggedIn()
    if (!isOfficerSessionActive) {
        InternalModuleLockedScreen(
            moduleTitle = "Sirene Otoritas Berlapis (Four-Eyes)",
            testTag = "four_eyes_siren_locked",
            onClose = onClose
        )
        return
    }
    BackHandler(onBack = onClose)

    val coroutineScope = rememberCoroutineScope()
        val sirenManager = remember(context) { FourEyesSirenManager.getInstance(context) }
        val drafts by sirenManager.drafts.collectAsStateWithLifecycle()
        val ttsHelper = remember(context) { IndonesianTtsHelper(context) }
        val activeTtsKey by ttsHelper.activeSpeakingKey.collectAsStateWithLifecycle()
        val adaptiveInsets = rememberAdaptiveSystemBarInsets()

        val canCreateDraft = remember(officerRole) { UserRole.canCreateSirenDraft(officerRole) }
        val canApproveSiren = remember(officerRole) { UserRole.canApproveSirenActivation(officerRole) }

        var selectedAlertType by remember { mutableStateOf(SirenAlertType.TSUNAMI) }
        var draftTitle by remember { mutableStateOf("Peringatan Dini Tsunami Pesisir Mukomuko") }
        var draftMessage by remember {
            mutableStateOf(
                "Perhatian! Peringatan dini potensi tsunami di wilayah pesisir Kabupaten Mukomuko. Segera lakukan evakuasi mandiri menuju dataran tinggi minimal 20 meter MDPL."
            )
        }
        var useAllCoastalDistricts by remember { mutableStateOf(true) }
        var customDistrictText by remember { mutableStateOf("") }
        var statusFeedback by remember { mutableStateOf<String?>(null) }
        var draftPendingReject by remember { mutableStateOf<FourEyesSirenDraft?>(null) }
        var rejectReasonText by remember { mutableStateOf("") }

        var isAudioSirenPlaying by remember { mutableStateOf(false) }
        var sirenAudioJob by remember { mutableStateOf<Job?>(null) }

        fun stopAudioSiren() {
            sirenAudioJob?.cancel()
            sirenAudioJob = null
            isAudioSirenPlaying = false
            EmergencyTonePlayer.stop()
            ttsHelper.stop()
        }

        fun playActivatedSirenAudio(draft: FourEyesSirenDraft) {
            stopAudioSiren()
            isAudioSirenPlaying = true
            ttsHelper.speak(
                key = "siren_${draft.draftId}",
                text = "Perhatian. ${draft.title}. Wilayah sasaran: ${draft.targetDistricts.joinToString(", ")}. ${draft.message}"
            )
            sirenAudioJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    repeat(4) {
                        if (!isActive || !isAudioSirenPlaying) return@repeat
                        EmergencyTonePlayer.playToneSuspend(
                            freqHz1 = 920.0,
                            freqHz2 = 1380.0,
                            durationMs = 550,
                            usage = AudioAttributes.USAGE_ALARM,
                            volume = 0.85f
                        )
                        delay(200)
                    }
                } finally {
                    isAudioSirenPlaying = false
                    EmergencyTonePlayer.stop()
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                stopAudioSiren()
                ttsHelper.shutdown()
            }
        }

        val pendingApprovalCount = remember(drafts) {
            drafts.count { it.status == FourEyesSirenState.MENUNGGU_PERSETUJUAN }
        }
        val activeSirenCount = remember(drafts) {
            drafts.count { it.status == FourEyesSirenState.DIAKTIFKAN }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .fillMaxSize()
                .testTag("four_eyes_siren_control_screen"),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(top = adaptiveInsets.topBarInset),
                    title = {
                        Column {
                            Text(
                                text = "Sirene Otoritas Berlapis",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Protokol Four-Eyes • $officerName ($officerRole)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                stopAudioSiren()
                                onClose()
                            },
                            modifier = Modifier.testTag("btn_close_four_eyes_siren")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup Kendali Sirene",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (isAudioSirenPlaying || activeTtsKey != null) {
                            IconButton(
                                onClick = { stopAudioSiren() },
                                modifier = Modifier.testTag("btn_stop_four_eyes_audio")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Matikan Suara Sirene",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (activeSirenCount > 0) Color(0xFFB91C1C) else Color(0xFF0F172A)
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 14.dp,
                    end = 16.dp,
                    bottom = 28.dp + adaptiveInsets.effectiveBottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Kartu Alur Four-Eyes Principle & Ringkasan Status
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ALUR FOUR-EYES PRINCIPLE (ANTI-AKTIVASI TUNGGAL)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FourEyesStagePill("1. DRAFT", Color(0xFF475569), Modifier.weight(1f))
                                Text("→", color = Color.White, fontWeight = FontWeight.Bold)
                                FourEyesStagePill("2. MENUNGGU ($pendingApprovalCount)", Color(0xFFD97706), Modifier.weight(1.3f))
                                Text("→", color = Color.White, fontWeight = FontWeight.Bold)
                                FourEyesStagePill("3. AKTIF ($activeSirenCount)", Color(0xFFDC2626), Modifier.weight(1.1f))
                            }
                            Text(
                                text = "Pembuat draf (Operator Pusdalops) tidak dapat menyetujui drafnya sendiri. Persetujuan & aktivasi sirene wajib dilakukan pejabat berbeda (Kepala Pusdalops / Manajer / Kalaksa / Pengawas).",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 16.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "Wewenang Anda: ${if (canCreateDraft) "Buat Draf ✓" else "Buat Draf ✕"}  •  ${if (canApproveSiren) "Setujui/Aktifkan Pimpinan ✓" else "Setujui/Aktifkan Pimpinan ✕"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (!statusFeedback.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusFeedback.orEmpty(),
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Form Buat Draf Aktivasi Sirene
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Buat Draf Aktivasi Sirene Baru",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedAlertType == SirenAlertType.TSUNAMI,
                                    onClick = {
                                        selectedAlertType = SirenAlertType.TSUNAMI
                                        useAllCoastalDistricts = true
                                        draftTitle = "Peringatan Dini Tsunami Pesisir Mukomuko"
                                        draftMessage = "Perhatian! Peringatan dini potensi tsunami di 7 kecamatan pesisir Kabupaten Mukomuko. Segera evakuasi mandiri ke dataran tinggi minimal 20 meter MDPL."
                                    },
                                    label = { Text("Tsunami Pesisir", fontSize = 12.sp) },
                                    modifier = Modifier.testTag("chip_siren_type_tsunami")
                                )
                                FilterChip(
                                    selected = selectedAlertType == SirenAlertType.EXTREME_WEATHER,
                                    onClick = {
                                        selectedAlertType = SirenAlertType.EXTREME_WEATHER
                                        useAllCoastalDistricts = false
                                        draftTitle = "Peringatan Cuaca Ekstrem & Banjir Bandang"
                                        draftMessage = "Waspada hujan sangat lebat disertai angin kencang di wilayah Kabupaten Mukomuko. Jauhi bantaran sungai dan lereng rawan longsor."
                                    },
                                    label = { Text("Cuaca Ekstrem", fontSize = 12.sp) },
                                    modifier = Modifier.testTag("chip_siren_type_weather")
                                )
                            }

                            OutlinedTextField(
                                value = draftTitle,
                                onValueChange = { draftTitle = it },
                                label = { Text("Judul Peringatan Sirene") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_siren_draft_title")
                            )

                            OutlinedTextField(
                                value = draftMessage,
                                onValueChange = { draftMessage = it },
                                label = { Text("Pesan Suara TTS & Siaran Peringatan") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_siren_draft_message")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Target 7 Kecamatan Zona Pesisir Tsunami",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = MukomukoDistricts.coastalTsunamiDistricts.joinToString(", ") { it.name },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                                Switch(
                                    checked = useAllCoastalDistricts,
                                    onCheckedChange = { useAllCoastalDistricts = it }
                                )
                            }

                            if (!useAllCoastalDistricts) {
                                OutlinedTextField(
                                    value = customDistrictText,
                                    onValueChange = { customDistrictText = it },
                                    label = { Text("Wilayah Kecamatan Sasaran (pisahkan koma)") },
                                    placeholder = { Text("Contoh: Penarik, Selagan Raya, V Koto") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    enabled = canCreateDraft,
                                    onClick = {
                                        val targets = if (useAllCoastalDistricts) {
                                            MukomukoDistricts.coastalTsunamiDistricts.map { it.name }
                                        } else {
                                            customDistrictText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        }
                                        val res = sirenManager.createDraft(
                                            alertType = selectedAlertType,
                                            title = draftTitle,
                                            message = draftMessage,
                                            targetDistricts = targets,
                                            creatorNip = officerNip,
                                            creatorName = officerName,
                                            creatorRole = officerRole,
                                            autoSubmitForApproval = false
                                        )
                                        statusFeedback = res.fold(
                                            onSuccess = { "Draf ${it.draftId} disimpan sebagai DRAFT. Tekan 'Ajukan Persetujuan' agar dapat ditinjau Pimpinan." },
                                            onFailure = { it.message ?: "Gagal menyimpan draf sirene." }
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_save_siren_draft")
                                ) {
                                    Text("Simpan DRAFT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    enabled = canCreateDraft,
                                    onClick = {
                                        val targets = if (useAllCoastalDistricts) {
                                            MukomukoDistricts.coastalTsunamiDistricts.map { it.name }
                                        } else {
                                            customDistrictText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        }
                                        val res = sirenManager.createDraft(
                                            alertType = selectedAlertType,
                                            title = draftTitle,
                                            message = draftMessage,
                                            targetDistricts = targets,
                                            creatorNip = officerNip,
                                            creatorName = officerName,
                                            creatorRole = officerRole,
                                            autoSubmitForApproval = true
                                        )
                                        statusFeedback = res.fold(
                                            onSuccess = { "Draf ${it.draftId} diajukan ke MENUNGGU PERSETUJUAN Pimpinan (Four-Eyes)." },
                                            onFailure = { it.message ?: "Gagal mengajukan draf sirene." }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_submit_siren_draft")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ajukan Persetujuan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. Daftar Draf & Riwayat Aktivasi Sirene
                item {
                    Text(
                        text = "Antrean Persetujuan & Riwayat Sirene (${drafts.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (drafts.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum ada draf aktivasi sirene.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Buat draf baru di atas untuk memulai alur verifikasi berlapis Four-Eyes.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(drafts, key = { it.draftId }) { draft ->
                        val isSelfCreator = draft.createdByNip.replace(Regex("[\\s\\-]+"), "")
                            .equals(officerNip.replace(Regex("[\\s\\-]+"), ""), ignoreCase = true)
                        FourEyesSirenDraftCard(
                            draft = draft,
                            canApproveRole = canApproveSiren,
                            isSelfCreator = isSelfCreator,
                            onSubmitFromDraft = {
                                val res = sirenManager.submitForApproval(draft.draftId, officerNip)
                                statusFeedback = res.fold(
                                    onSuccess = { "Draf ${it.draftId} kini berstatus MENUNGGU PERSETUJUAN." },
                                    onFailure = { it.message ?: "Gagal mengajukan draf." }
                                )
                            },
                            onApproveAndActivate = {
                                val res = sirenManager.approveAndActivate(
                                    draftId = draft.draftId,
                                    approverNip = officerNip,
                                    approverName = officerName,
                                    approverRole = officerRole
                                )
                                res.onSuccess { activated ->
                                    statusFeedback = "SIRENE DIAKTIFKAN oleh $officerName ($officerRole)."
                                    playActivatedSirenAudio(activated)
                                    Toast.makeText(context, "Sirene Four-Eyes diaktifkan!", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    statusFeedback = err.message ?: "Persetujuan ditolak."
                                    Toast.makeText(context, err.message ?: "Persetujuan ditolak.", Toast.LENGTH_LONG).show()
                                }
                            },
                            onReject = {
                                draftPendingReject = draft
                                rejectReasonText = ""
                            },
                            onDeactivate = {
                                stopAudioSiren()
                                val res = sirenManager.deactivateActiveSiren(draft.draftId, officerName)
                                statusFeedback = res.fold(
                                    onSuccess = { "Sirene ${it.draftId} telah dinonaktifkan." },
                                    onFailure = { it.message ?: "Gagal menonaktifkan sirene." }
                                )
                            },
                            onSpeakPreview = {
                                ttsHelper.toggleSpeak(
                                    key = "preview_${draft.draftId}",
                                    text = "${draft.title}. ${draft.message}"
                                )
                            },
                            isSpeaking = activeTtsKey == "preview_${draft.draftId}" || activeTtsKey == "siren_${draft.draftId}"
                        )
                    }
                }
            }
        }

        if (draftPendingReject != null) {
            val target = draftPendingReject!!
            AlertDialog(
                onDismissRequest = { draftPendingReject = null },
                title = { Text("Tolak Draf Sirene", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Masukkan alasan penolakan untuk draf ${target.draftId} (${target.title}):",
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = rejectReasonText,
                            onValueChange = { rejectReasonText = it },
                            label = { Text("Alasan Penolakan") },
                            placeholder = { Text("Contoh: Belum ada konfirmasi peringatan resmi BMKG") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val res = sirenManager.rejectDraft(
                                draftId = target.draftId,
                                reviewerNip = officerNip,
                                reviewerName = officerName,
                                reviewerRole = officerRole,
                                reason = rejectReasonText
                            )
                            statusFeedback = res.fold(
                                onSuccess = { "Draf ${it.draftId} ditolak oleh $officerName." },
                                onFailure = { it.message ?: "Gagal menolak draf." }
                            )
                            draftPendingReject = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Tolak Draf")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { draftPendingReject = null }) {
                        Text("Batal")
                    }
                }
            )
        }
}

@Composable
private fun FourEyesStagePill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FourEyesSirenDraftCard(
    draft: FourEyesSirenDraft,
    canApproveRole: Boolean,
    isSelfCreator: Boolean,
    onSubmitFromDraft: () -> Unit,
    onApproveAndActivate: () -> Unit,
    onReject: () -> Unit,
    onDeactivate: () -> Unit,
    onSpeakPreview: () -> Unit,
    isSpeaking: Boolean
) {
    val badgeColor = when (draft.status) {
        FourEyesSirenState.DRAFT -> Color(0xFF475569)
        FourEyesSirenState.MENUNGGU_PERSETUJUAN -> Color(0xFFD97706)
        FourEyesSirenState.DIAKTIFKAN -> Color(0xFFDC2626)
        FourEyesSirenState.DITOLAK -> Color(0xFF991B1B)
        FourEyesSirenState.DINONAKTIFKAN -> Color(0xFF0F766E)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (draft.status == FourEyesSirenState.DIAKTIFKAN) 2.dp else 1.dp,
            color = badgeColor.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_siren_draft_${draft.draftId}")
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
                Surface(shape = RoundedCornerShape(6.dp), color = badgeColor) {
                    Text(
                        text = draft.status.displayLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = onSpeakPreview,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) "Hentikan TTS" else "Bacakan Pesan Sirene",
                        tint = if (isSpeaking) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = draft.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = draft.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            Text(
                text = "Wilayah: ${draft.targetDistricts.joinToString(", ")}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pembuat Draf: ${draft.createdByName} (${draft.createdByRole}) • ${FourEyesSirenManager.formatTimestamp(draft.createdAtMs)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!draft.approvedByName.isNullOrBlank()) {
                Text(
                    text = "Peninjau: ${draft.approvedByName} (${draft.approvedByRole.orEmpty()}) • ${draft.decisionNote}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }

            when (draft.status) {
                FourEyesSirenState.DRAFT -> {
                    Button(
                        onClick = onSubmitFromDraft,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Ajukan ke MENUNGGU PERSETUJUAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                FourEyesSirenState.MENUNGGU_PERSETUJUAN -> {
                    if (isSelfCreator) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "Four-Eyes Lock: Anda pembuat draf ini. Aktivasi wajib disetujui pejabat berbeda (Kepala Pusdalops / Kalaksa).",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApproveAndActivate,
                            enabled = canApproveRole && !isSelfCreator,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_approve_siren_${draft.draftId}")
                        ) {
                            Text("Setujui & Aktifkan", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            enabled = canApproveRole,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_reject_siren_${draft.draftId}")
                        ) {
                            Text("Tolak", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                FourEyesSirenState.DIAKTIFKAN -> {
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_deactivate_siren_${draft.draftId}")
                    ) {
                        Text("Hentikan / Nonaktifkan Sirene", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                else -> Unit
            }
        }
    }
}
