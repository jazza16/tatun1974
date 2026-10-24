package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

/**
 * Wallpaper Awal & Analisis Alam-Kebencanaan Kabupaten Mukomuko
 * Menggunakan foto background beresolusi tinggi (img_mukomuko_wallpaper)
 * yang menggambarkan lanskap pesisir Samudra Hindia, potensi bencana abrasi/gempa/tsunami,
 * dan kesiapsiagaan mitigasi TRC BPBD Kabupaten Mukomuko.
 */
@Composable
fun StartupSplashWallpaperScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissDelayMs: Long = 3200L
) {
    var showExplanationSheet by remember { mutableStateOf(false) }
    var isPureWallpaperMode by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        val progress = remember { Animatable(0f) }
        val logoScale = remember { Animatable(0.75f) }
        val contentAlpha = remember { Animatable(0f) }

        // Subtle pulsing glow around the BPBD logo
        val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        // Drive entry animation; pause auto-dismiss if user opened the explanation sheet or pure wallpaper mode
        LaunchedEffect(showExplanationSheet, isPureWallpaperMode) {
            if (!showExplanationSheet && !isPureWallpaperMode) {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(500, easing = LinearEasing)
                )
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween((autoDismissDelayMs - 600).toInt(), easing = LinearEasing)
                )
                delay(200)
                if (!showExplanationSheet && !isPureWallpaperMode) {
                    onDismiss()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isPureWallpaperMode) {
                            isPureWallpaperMode = false
                        }
                    }
                )
                .testTag("startup_splash_wallpaper_screen")
        ) {
            // 1. High-Resolution Background Wallpaper
            Image(
                painter = painterResource(id = R.drawable.img_mukomuko_wallpaper),
                contentDescription = "Foto Latar HD Kondisi Alam, Potensi Bencana, & Penanganan BPBD Mukomuko",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("startup_wallpaper_photo")
            )

            // 2. High-contrast Vignette Overlay (lighter in pure wallpaper mode)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (isPureWallpaperMode) {
                                listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.60f)
                                )
                            } else {
                                listOf(
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.65f),
                                    Color(0xFF0F172A).copy(alpha = 0.94f)
                                )
                            }
                        )
                    )
            )

            // Top action bar in pure wallpaper mode
            if (isPureWallpaperMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "Mode Wallpaper Penuh (Ketuk layar untuk kembali)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { isPureWallpaperMode = false },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. Main Center Content: Logo, Titles, Context Card & Navigation (hidden in pure mode)
            if (!isPureWallpaperMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Middle Section: Logo with Pulsing Halo and App Branding
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        // Pulsing Glow & Logo Container
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(124.dp)
                        ) {
                            // Outer Pulsing Glow Circle
                            Box(
                                modifier = Modifier
                                    .size(118.dp)
                                    .scale(glowScale)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(alpha = glowAlpha))
                            )

                            // Outer Border Ring
                            Box(
                                modifier = Modifier
                                    .size(106.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(2.dp, OrangePrimary, CircleShape)
                            )

                            // Official BPBD Logo Image
                            Image(
                                painter = painterResource(id = R.drawable.ic_bpbd_logo),
                                contentDescription = "Logo BPBD Mukomuko",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .scale(logoScale.value)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .testTag("startup_bpbd_logo")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title & Department Branding
                        Text(
                            text = "PUSDALOPS-PB BPBD",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "KABUPATEN MUKOMUKO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Kondisi Alam, Potensi Kebencanaan\n& Kesiapsiagaan Mitigasi Pesisir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Disaster & Natural Landscape Context Card (Clickable to open detailed sheet)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFEA580C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showExplanationSheet = true }
                                .testTag("card_wallpaper_disaster_context")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEA580C).copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Waves,
                                        contentDescription = null,
                                        tint = Color(0xFFFB923C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Lanskap Alam & Potensi Bencana",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFB923C)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = OrangePrimary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Detail",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Samudra Hindia, Abrasi Pantai, Jalur Evakuasi Tsunami & Siaga TRC BPBD",
                                        fontSize = 10.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Detail Penjelasan",
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Section: Action buttons (View Clean Wallpaper, Read Detail, Enter Dashboard)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isPureWallpaperMode = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_view_pure_wallpaper"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lihat Foto Penuh", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = { showExplanationSheet = true },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(40.dp)
                                    .testTag("btn_read_disaster_analysis"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analisis Bencana", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar & Telemetry Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (progress.value < 0.95f) "Menyiapkan telemetri cuaca & maritim Mukomuko..." else "Sistem siap digunakan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress.value },
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = OrangePrimary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Enter App Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_enter_app"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = OrangeOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Buka Dashboard Aplikasi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. Modal Bottom Sheet for Detailed Explanation of Natural Conditions, Disaster Hazards & BPBD Handling
            if (showExplanationSheet) {
                MukomukoDisasterDetailSheet(onDismiss = { showExplanationSheet = false })
            }
        }
    }
}

/**
 * Bottom Sheet modal explaining the high-resolution photo:
 * 1. Natural conditions of Mukomuko
 * 2. Key disaster risks (abrasion, tsunami Megathrust, floods)
 * 3. Mitigation & handling strategies by BPBD Mukomuko
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MukomukoDisasterDetailSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangePrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kondisi Alam & Mitigasi Bencana",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kabupaten Mukomuko, Provinsi Bengkulu",
                            fontSize = 12.sp,
                            color = OrangePrimary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thumbnail Banner of Wallpaper Photo
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, OrangePrimary)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mukomuko_wallpaper),
                        contentDescription = "Dokumentasi Alam & Penanganan Bencana Mukomuko",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Text(
                        text = "Foto Dokumentasi Resolusi Tinggi: Samudra Hindia, Tanggul Abrasi, Rambu Tsunami & Armada Rescue BPBD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. KONDISI ALAM
            DisasterInfoSectionCard(
                icon = Icons.Default.Terrain,
                title = "1. Kondisi Geografis & Karakter Alam",
                accentColor = Color(0xFF38BDF8),
                items = listOf(
                    "Garis Pantai Panjang: Membentang sepanjang ±150 km di pesisir barat Pulau Sumatra yang berhadapan langsung secara terbuka dengan Samudra Hindia.",
                    "Bentang Alam Dataran Rendah ke Perbukitan: Memiliki 15 kecamatan yang bervariasi dari garis pantai, muara sungai (Sungai Selagan & Manjuto), hingga kaki jajaran Bukit Barisan.",
                    "Iklim Tropis Basah: Curah hujan tahunan yang tinggi dengan pergantian angin muson barat dan timur yang memicu dinamika cuaca maritim ekstrem."
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. POTENSI BENCANA
            DisasterInfoSectionCard(
                icon = Icons.Default.Warning,
                title = "2. Potensi Ancaman Bencana Utama",
                accentColor = Color(0xFFF87171),
                items = listOf(
                    "Gempa Bumi & Megathrust Mentawai: Terletak di zona seismik aktif pertemuan Lempeng Tektonik Indo-Australia dan Eurasia berpotensi gempa tektonik berkekuatan besar.",
                    "Risiko Tsunami Pesisir: Garis pantai yang terbuka tanpa pelindung pulau penghalang di depannya membuat pesisir Mukomuko rentan terhadap terpaan gelombang tsunami.",
                    "Abrasi Pantai Kritis: Gelombang Samudra Hindia yang kuat mengikis bibir pantai pesisir, mengancam permukiman nelayan dan badan Jalan Lintas Barat (Jalinbar) Sumatra.",
                    "Banjir Luapan Sungai: Aliran Sungai Selagan, Sungai Manjuto, dan Sungai Air Dikit berisiko meluap ke permukiman dan perkebunan saat curah hujan ekstrem di hulu.",
                    "Gelombang Tinggi & Cuaca Maritim: Tinggi gelombang 2.5 - 4.0 meter yang membahayakan aktivitas nelayan tradisional Mukomuko."
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. LANGKAH PENANGANAN & MITIGASI BPBD
            DisasterInfoSectionCard(
                icon = Icons.Default.Emergency,
                title = "3. Penanganan & Kesiapsiagaan BPBD Mukomuko",
                accentColor = Color(0xFF4ADE80),
                items = listOf(
                    "Pembangunan Tanggul Abrasi (Revetment & Tetrapod): Struktur batu gajah masif dan tetrapod beton pemecah gelombang di sepanjang garis pantai kritis untuk melindungi jalan raya dan pemukiman warga.",
                    "Rambu & Jalur Evakuasi Tsunami: Pemasangan plang rambu hijau-kuning penunjuk arah evakuasi darurat menuju tempat evakuasi sementara (TES) dan dataran tinggi aman.",
                    "Sistem Peringatan Dini Real-Time: Integrasi telemetri BMKG WRS-NTWC, radar cuaca BMKG, dan pemantauan sensor gempa bumi otomatis di aplikasi ini.",
                    "Kesiapsiagaan Tim Reaksi Cepat (TRC): Posko PUSDALOPS BPBD 24 jam dengan armada kendaraan rescue 4x4, perahu karet penyelamat air, dan Call Center Darurat 117.",
                    "Edukasi & Simulasi Mitigasi: Sosialisasi desa tangguh bencana (Destana) dan kesadaran masyarakat pesisir dalam menghadapi ancaman bencana."
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Tutup Penjelasan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DisasterInfoSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accentColor: Color,
    items: List<String>
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(5.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
