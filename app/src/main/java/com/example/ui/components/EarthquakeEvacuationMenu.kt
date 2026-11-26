package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import com.example.util.EmergencyTonePlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.EmergencyGoBagItemEntity
import com.example.data.repository.CitizenPreparednessRepository
import com.example.data.repository.GoBagExpiryStatus
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherWarningOrange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Menu Kesiapsiagaan dan Pelaksanaan Evakuasi Mandiri Gempa Bumi & Tsunami
 * Standar PUSDALOPS-PB BPBD Kabupaten Mukomuko.
 */
@Composable
fun EarthquakeEvacuationMenu(
    onCall117: () -> Unit,
    onCallPoskoMukomuko: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedEvacPhaseTab by remember { mutableIntStateOf(0) } // 0: Sebelum, 1: Saat Gempa, 2: Sesudah

    // Emergency whistle sound state
    var isWhistleActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var whistleJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            whistleJob?.cancel()
            whistleJob = null
            EmergencyTonePlayer.stop()
        }
    }

    fun toggleWhistle() {
        if (isWhistleActive) {
            whistleJob?.cancel()
            whistleJob = null
            isWhistleActive = false
            EmergencyTonePlayer.stop()
        } else {
            isWhistleActive = true
            whistleJob = coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    while (isActive && isWhistleActive) {
                        EmergencyTonePlayer.playToneSuspend(
                            freqHz1 = 2600.0,
                            freqHz2 = 3100.0,
                            durationMs = 400,
                            usage = AudioAttributes.USAGE_ALARM,
                            volume = 0.90f
                        )
                        delay(150)
                        EmergencyTonePlayer.playToneSuspend(
                            freqHz1 = 2200.0,
                            freqHz2 = 2850.0,
                            durationMs = 400,
                            usage = AudioAttributes.USAGE_ALARM,
                            volume = 0.90f
                        )
                        delay(250)
                    }
                } catch (_: Throwable) {
                    // Ignore sound output failures
                } finally {
                    EmergencyTonePlayer.stop()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("earthquake_evacuation_menu"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner Kesiapsiagaan BPBD Mukomuko
        EvacuationHeaderCard(
            isWhistleActive = isWhistleActive,
            onToggleWhistle = { toggleWhistle() },
            onCall117 = onCall117
        )

        // 2. Rumus Emas 20-20-20 Pesisir Mukomuko (Megathrust Hazard)
        Rule202020Card()

        // 3. Panduan Tiga Langkah Emas: DROP, COVER, HOLD ON
        DropCoverHoldOnCard()

        // 4. Tahapan Pelaksanaan Evakuasi Mandiri (Sebelum, Saat, Sesudah)
        EvacuationPhasesTabsCard(
            selectedTab = selectedEvacPhaseTab,
            onTabSelected = { selectedEvacPhaseTab = it }
        )

        // 5. Checklist Interaktif Tas Siaga Bencana (TSB)
        EmergencyBagChecklistCard()

        // 6. Titik Kumpul & Lokasi Aman Evakuasi di Kabupaten Mukomuko
        MukomukoEvacuationAssemblyPointsCard(
            onCallPoskoMukomuko = onCallPoskoMukomuko
        )

        // 7. Kontak Siaga & Bantuan Cepat
        EvacuationEmergencyContactsCard(
            onCall117 = onCall117,
            onCallPoskoMukomuko = onCallPoskoMukomuko
        )
    }
}

@Composable
private fun EvacuationHeaderCard(
    isWhistleActive: Boolean,
    onToggleWhistle: () -> Unit,
    onCall117: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val headerGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E293B)  // Slate 800
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_evac_header"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Color(0xFFEA580C)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = OrangePrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PUSDALOPS-PB BPBD MUKOMUKO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "SIAGA GEMPA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bpbd_logo),
                        contentDescription = "BPBD Mukomuko",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFEA580C), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kesiapsiagaan & Evakuasi Mandiri Gempa",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prosedur keselamatan warga saat guncangan gempa bumi di wilayah pesisir & daratan Mukomuko.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Tool Buttons (Whistle Siren & 117 Call)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sinyal Suara / Peluit Darurat
                    Button(
                        onClick = onToggleWhistle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWhistleActive) Color(0xFFDC2626) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 42.dp)
                            .testTag("btn_toggle_emergency_whistle")
                    ) {
                        Icon(
                            imageVector = if (isWhistleActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWhistleActive) "Matikan Sirene" else "Bunyikan Peluit SOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Call Center 117
                    Button(
                        onClick = onCall117,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .heightIn(min = 42.dp)
                            .testTag("btn_evac_header_117")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Panggil 117",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Rule202020Card() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_rule_202020"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEA580C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Surfing,
                        contentDescription = "Tsunami Rule",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Rumus Emas 20-20-20 (Pesisir Mukomuko)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF7C2D12)
                    )
                    Text(
                        text = "Panduan evakuasi cepat jika gempa terasa di dekat pantai",
                        fontSize = 11.sp,
                        color = Color(0xFF9A3412)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuleItemBox(
                    number = "20",
                    unit = "Detik",
                    description = "Goncangan gempa berlangsung ≥ 20 detik atau sulit berdiri",
                    modifier = Modifier.weight(1f)
                )

                RuleItemBox(
                    number = "20",
                    unit = "Menit",
                    description = "Waktu emas maksimal evakuasi mandiri segera",
                    modifier = Modifier.weight(1f)
                )

                RuleItemBox(
                    number = "20",
                    unit = "Meter",
                    description = "Tinggi tempat evakuasi aman di atas laut / bukit",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Segera lari menjauhi pantai ke arah timur/perbukitan! JANGAN MENUNGGU SIRINE ATAU AIR SURUT.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleItemBox(
    number: String,
    unit: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFDBA74))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFC2410C)
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFEA580C)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF431407),
                lineHeight = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DropCoverHoldOnCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_drop_cover_hold_on"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2563EB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Drop Cover Hold On",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tiga Aksi Emas: Drop, Cover, Hold On",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Protokol perlindungan diri seketika gempa mengguncang",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 1: DROP
            StepActionRow(
                badgeNumber = "1",
                badgeTitle = "DROP (Merunduk)",
                actionText = "Segera berlutut atau rendahkan tubuh ke lantai. Jangan berdiri karena goncangan bisa menjatuhkan dan mencederai Anda.",
                badgeBg = Color(0xFFEFF6FF),
                badgeColor = Color(0xFF1D4ED8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 2: COVER (Berlindung)
            StepActionRow(
                badgeNumber = "2",
                badgeTitle = "COVER (Berlindung)",
                actionText = "Masuk ke bawah meja yang kokoh. Lindungi kepala dan tengkuk menggunakan kedua tangan, tas tebal, atau bantal. Jauhi jendela kaca dan lemari tinggi.",
                badgeBg = Color(0xFFFEF3C7),
                badgeColor = Color(0xFFB45309)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 3: HOLD ON (Bertahan)
            StepActionRow(
                badgeNumber = "3",
                badgeTitle = "HOLD ON (Bertahan)",
                actionText = "Pegang erat kaki meja sampai getaran gempa berhenti total. Jika meja bergeser, ikut bergerak bersamanya sambil tetap terlindung.",
                badgeBg = Color(0xFFECFDF5),
                badgeColor = Color(0xFF047857)
            )
        }
    }
}

@Composable
private fun StepActionRow(
    badgeNumber: String,
    badgeTitle: String,
    actionText: String,
    badgeBg: Color,
    badgeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeNumber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badgeTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun EvacuationPhasesTabsCard(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_evac_phases"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tahapan Lengkap Evakuasi Mandiri",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ketahui langkah tepat sebelum, saat, dan sesudah bencana gempa bumi",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = OrangePrimary
                        )
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Text(
                            text = "Pra-Bencana",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) OrangePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Text(
                            text = "Saat Gempa",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) OrangePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    text = {
                        Text(
                            text = "Pasca-Gempa",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) OrangePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // Fase Pra-Bencana
                    PhaseGuideItem(
                        title = "1. Tentukan Titik Kumpul Keluarga",
                        desc = "Sepakati lokasi terbuka bebas bahaya (seperti lapangan RT atau taman depan) sebagai titik temu jika anggota keluarga terpencar saat gempa."
                    )
                    PhaseGuideItem(
                        title = "2. Amankan Perabotan Berat Rumah",
                        desc = "Kaitkan lemari piring, rak buku tinggi, dan kulkas ke dinding. Jauhkan tempat tidur dari jendela kaca dan lampu gantung besar."
                    )
                    PhaseGuideItem(
                        title = "3. Siapkan Tas Siaga Bencana (TSB)",
                        desc = "Letakkan tas siaga di tempat yang mudah dijangkau saat melarikan diri, misalnya dekat pintu keluar utama rumah."
                    )
                    PhaseGuideItem(
                        title = "4. Pelajari Rute Evakuasi Bebas Reruntuhan",
                        desc = "Pastikan jalan keluar rumah tidak terhalang barang menumpuk dan kunci pintu utama mudah dibuka dari dalam."
                    )
                }
                1 -> {
                    // Fase Saat Gempa
                    PhaseGuideItem(
                        title = "1. JANGAN PANIK & JANGAN BERLARIAN",
                        desc = "Kepanikan menyebabkan desak-desakan dan salah langkah. Tarik napas dalam dan lakukan Drop, Cover, Hold On segera."
                    )
                    PhaseGuideItem(
                        title = "2. Jika di Dalam Gedung/Rumah",
                        desc = "Tetap di dalam hingga guncangan reda. Jangan terburu-buru keluar saat gempa masih berlangsung karena bahaya genteng, pecahan kaca, dan plester dinding jatuh."
                    )
                    PhaseGuideItem(
                        title = "3. JANGAN PERNAH GUNAKAN LIFT",
                        desc = "Gunakan hanya tangga darurat. Lift berisiko macet total akibat pemadaman listrik otomatis saat gempa."
                    )
                    PhaseGuideItem(
                        title = "4. Jika Sedang di Luar Ruangan",
                        desc = "Jauhi bangunan tinggi, tiang listrik kabel tegangan tinggi, pohon kelapa, papan reklame, dan tebing rawan longsor."
                    )
                    PhaseGuideItem(
                        title = "5. Jika Sedang Berkendara",
                        desc = "Nyalakan lampu hazard, kurangi kecepatan perlahan dan menepi ke bahu jalan. Jangan berhenti di atas atau di bawah jembatan."
                    )
                }
                2 -> {
                    // Fase Pasca-Gempa
                    PhaseGuideItem(
                        title = "1. Waspadai Gempa Susulan (Aftershocks)",
                        desc = "Gempa susulan sering terjadi beberapa menit hingga beberapa jam setelah gempa utama. Tetap siaga di ruang terbuka."
                    )
                    PhaseGuideItem(
                        title = "2. Periksa Kebocoran Gas & Korsleting Listrik",
                        desc = "Matikan regulator tabung LPG dan turunkan saklar meteran listrik (MCB). Jangan nyalakan korek api atau saklar lampu jika mencium bau gas."
                    )
                    PhaseGuideItem(
                        title = "3. Evakuasi dengan Berjalan Kaki Cepat",
                        desc = "Ambil Tas Siaga Bencana dan berjalan cepat menuju titik kumpul aman. Hindari menggunakan mobil/motor agar tidak membuat kemacetan jalur evakuasi darurat."
                    )
                    PhaseGuideItem(
                        title = "4. Jangan Masuki Bangunan yang Retak",
                        desc = "Struktur bangunan retak bisa runtuh seketika jika terjadi gempa susulan kecil. Tunggu pemeriksaan resmi dari Tim TRC BPBD."
                    )
                    PhaseGuideItem(
                        title = "5. Pantau Informasi Resmi BPBD & BMKG",
                        desc = "Jangan mudah percaya berita bohong (hoaks). Pantau rilis resmi dari Pusdalops BPBD Mukomuko dan BMKG."
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseGuideItem(
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(OrangePrimary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/**
 * Checklist Interaktif Tas Siaga Bencana (Emergency Go-Bag) Persisten SQLite Room (Master PRD BAB 2).
 * Mendukung 10 barang wajib, `+ Tambah Barang Kustom`, penghapusan item kustom, serta
 * pelacakan tanggal kedaluwarsa (obat-obatan, makanan, air minum).
 */
@Composable
private fun EmergencyBagChecklistCard() {
    val context = LocalContext.current
    val repo = remember(context) { CitizenPreparednessRepository.fromContext(context) }
    val coroutineScope = rememberCoroutineScope()

    val dbItems by repo.goBagItemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(repo) {
        repo.ensureSeeded()
    }

    val items = remember(dbItems) {
        if (dbItems.isNotEmpty()) dbItems else CitizenPreparednessRepository.DEFAULT_GO_BAG_ITEMS
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var customDesc by remember { mutableStateOf("") }
    var customExpiry by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }

    var editingExpiryItem by remember { mutableStateOf<EmergencyGoBagItemEntity?>(null) }
    var editingExpiryInput by remember { mutableStateOf("") }
    var editingExpiryError by remember { mutableStateOf<String?>(null) }

    val checkedCount = items.count { it.isChecked }
    val progress = if (items.isNotEmpty()) checkedCount.toFloat() / items.size else 0f
    val percent = (progress * 100).toInt()

    val expiryEvaluations = remember(items) {
        items.associate { it.id to CitizenPreparednessRepository.evaluateExpiry(it.expiryDateIso) }
    }
    val expiredCount = expiryEvaluations.values.count { it.status == GoBagExpiryStatus.EXPIRED }
    val expiringSoonCount = expiryEvaluations.values.count { it.status == GoBagExpiryStatus.EXPIRING_SOON }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_emergency_bag_checklist"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF0D9488), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Tas Siaga Bencana",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Checklist Tas Siaga Bencana (TSB)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tersimpan otomatis di SQLite • Pantau kedaluwarsa obat & pangan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peringatan Kedaluwarsa Logistik Tas Siaga
            if (expiredCount > 0 || expiringSoonCount > 0) {
                val isCriticalExpiry = expiredCount > 0
                Surface(
                    color = if (isCriticalExpiry) Color(0xFFDC2626).copy(alpha = 0.12f) else Color(0xFFD97706).copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (isCriticalExpiry) Color(0xFFDC2626).copy(alpha = 0.4f) else Color(0xFFD97706).copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_gobag_expiry_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan Kedaluwarsa Tas Siaga",
                            tint = if (isCriticalExpiry) Color(0xFFB91C1C) else Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                if (expiredCount > 0) append("$expiredCount item sudah KEDALUWARSA. ")
                                if (expiringSoonCount > 0) append("$expiringSoonCount item segera kedaluwarsa (≤30 hari). ")
                                append("Segera perbarui isi Tas Siaga Anda.")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCriticalExpiry) Color(0xFFB91C1C) else Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Progress Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kesiapan Tas Siaga:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$checkedCount / ${items.size} Item ($percent%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (percent >= 80) WeatherGoodGreen else OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (percent >= 80) WeatherGoodGreen else OrangePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol + Tambah Barang Kustom
            OutlinedButton(
                onClick = {
                    customTitle = ""
                    customDesc = ""
                    customExpiry = ""
                    customError = null
                    showAddDialog = true
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF0D9488)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_add_custom_gobag_item")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Barang Kustom Tas Siaga",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Tambah Barang Kustom (Obat/Bayi/Lansia)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checklist Items (Persisten SQLite Room)
            items.forEachIndexed { index, item ->
                val expiryEval = expiryEvaluations[item.id]
                    ?: CitizenPreparednessRepository.evaluateExpiry(item.expiryDateIso)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                repo.setItemChecked(item.id, !item.isChecked)
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .testTag("gobag_item_row_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                repo.setItemChecked(item.id, isChecked)
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = OrangePrimary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isChecked) WeatherGoodGreen else MaterialTheme.colorScheme.onSurface
                            )
                            if (item.isCustom) {
                                Surface(
                                    color = Color(0xFF0D9488).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "KUSTOM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 14.sp
                        )

                        if (expiryEval.status != GoBagExpiryStatus.NONE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val (badgeBg, badgeFg) = when (expiryEval.status) {
                                GoBagExpiryStatus.EXPIRED -> Color(0xFFDC2626).copy(alpha = 0.14f) to Color(0xFFB91C1C)
                                GoBagExpiryStatus.EXPIRING_SOON -> Color(0xFFD97706).copy(alpha = 0.16f) to Color(0xFFB45309)
                                GoBagExpiryStatus.SAFE -> WeatherGoodGreen.copy(alpha = 0.14f) to Color(0xFF15803D)
                                GoBagExpiryStatus.NONE -> Color.Transparent to Color.Transparent
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("gobag_expiry_badge_${item.id}")
                            ) {
                                Text(
                                    text = expiryEval.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Tombol Atur / Ubah Tanggal Kedaluwarsa
                    IconButton(
                        onClick = {
                            editingExpiryItem = item
                            editingExpiryInput = item.expiryDateIso.orEmpty()
                            editingExpiryError = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_edit_expiry_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Atur tanggal kedaluwarsa ${item.title}",
                            tint = when (expiryEval.status) {
                                GoBagExpiryStatus.EXPIRED -> Color(0xFFDC2626)
                                GoBagExpiryStatus.EXPIRING_SOON -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (item.isCustom) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    repo.deleteCustomItem(item.id)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_delete_custom_gobag_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus barang kustom ${item.title}",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Dialog Tambah Barang Kustom Tas Siaga
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Tambah Barang Kustom Tas Siaga",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = {
                            customTitle = it
                            customError = null
                        },
                        label = { Text("Nama Barang (mis. Susu Bayi / Inhaler)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_gobag_title")
                    )
                    OutlinedTextField(
                        value = customDesc,
                        onValueChange = { customDesc = it },
                        label = { Text("Catatan / Jumlah (Opsional)") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_gobag_desc")
                    )
                    OutlinedTextField(
                        value = customExpiry,
                        onValueChange = {
                            customExpiry = it
                            customError = null
                        },
                        label = { Text("Tanggal Kedaluwarsa YYYY-MM-DD (Opsional)") },
                        placeholder = { Text("Contoh: 2027-06-30") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_gobag_expiry")
                    )
                    if (customError != null) {
                        Text(
                            text = customError.orEmpty(),
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = repo.addCustomItem(
                                title = customTitle,
                                description = customDesc,
                                expiryDateIso = customExpiry.takeIf { it.isNotBlank() }
                            )
                            result.fold(
                                onSuccess = { showAddDialog = false },
                                onFailure = { customError = it.message ?: "Gagal menyimpan barang kustom." }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    modifier = Modifier.testTag("btn_save_custom_gobag_item")
                ) {
                    Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Atur / Perbarui Tanggal Kedaluwarsa Item Tas Siaga
    val targetExpiryItem = editingExpiryItem
    if (targetExpiryItem != null) {
        AlertDialog(
            onDismissRequest = { editingExpiryItem = null },
            title = {
                Text(
                    text = "Tanggal Kedaluwarsa",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = targetExpiryItem.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = editingExpiryInput,
                        onValueChange = {
                            editingExpiryInput = it
                            editingExpiryError = null
                        },
                        label = { Text("Format YYYY-MM-DD (kosongkan untuk hapus)") },
                        placeholder = { Text("Contoh: 2027-08-15") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_gobag_expiry")
                    )
                    if (editingExpiryError != null) {
                        Text(
                            text = editingExpiryError.orEmpty(),
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = repo.updateItemExpiry(
                                itemId = targetExpiryItem.id,
                                expiryDateIso = editingExpiryInput.takeIf { it.isNotBlank() }
                            )
                            res.fold(
                                onSuccess = { editingExpiryItem = null },
                                onFailure = { editingExpiryError = it.message ?: "Format tanggal tidak valid." }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.testTag("btn_save_gobag_expiry")
                ) {
                    Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExpiryItem = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun MukomukoEvacuationAssemblyPointsCard(
    onCallPoskoMukomuko: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_evac_assembly_points"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF7C3AED), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Titik Kumpul",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Titik Kumpul & Lokasi Aman Kabupaten Mukomuko",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lokasi dataran tinggi dan titik kumpul terkoordinasi BPBD",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AssemblyPointItem(
                name = "Posko Utama PUSDALOPS-PB BPBD Mukomuko",
                location = "Jl. Danau Nibung, Kompleks Perkantoran Pemda Mukomuko",
                type = "Pusat Komando & Tenda Darurat Utama"
            )

            AssemblyPointItem(
                name = "Lapangan Merdeka Kota Mukomuko",
                location = "Pusat Kota Mukomuko (Area Terbuka Hijau Bebas Bangunan Tinggi)",
                type = "Titik Kumpul Evakuasi Sementara (TES)"
            )

            AssemblyPointItem(
                name = "Arah Evakuasi Tsunami: Daratan Timur / Bukit Trans",
                location = "Jalur perkebunan & jalan poros arah timur menjauhi Pantai Abrasi",
                type = "Elevasi Aman > 20 Meter dpl"
            )

            AssemblyPointItem(
                name = "Halaman Kantor Camat di 15 Kecamatan",
                location = "Kota Mukomuko, Ipuh, Teramang Jaya, Penarik, Lubuk Pinang, dll.",
                type = "Posko Penampungan Kecamatan"
            )
        }
    }
}

@Composable
private fun AssemblyPointItem(
    name: String,
    location: String,
    type: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = location,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Surface(
                    color = OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EvacuationEmergencyContactsCard(
    onCall117: () -> Unit,
    onCallPoskoMukomuko: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_evac_emergency_contacts"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CrisisAlert,
                    contentDescription = "Darurat",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kontak Evakuasi Darurat BPBD Mukomuko",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Layanan siaga tanggap darurat bencana 24 Jam bebas pulsa",
                        fontSize = 11.sp,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall117,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call Center 117",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCallPoskoMukomuko,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Call Center 112",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
