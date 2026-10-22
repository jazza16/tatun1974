package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.util.CompassSensorManager
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive onboarding guide dialog shown to new users on first application launch.
 * Introduces key pillars: Weather Forecasts, Maritime Ocean Data, and Digital Compass with Evacuation SOP.
 */
@Composable
fun InteractiveOnboardingDialog(
    onComplete: () -> Unit,
    onOpenCompassDetailed: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onComplete,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("interactive_onboarding_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .widthIn(max = 600.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bpbd_logo),
                            contentDescription = "Logo BPBD",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, OrangePrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Panduan Aplikasi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    if (pagerState.currentPage < 3) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.clickable { onComplete() }
                        ) {
                            Text(
                                text = "Lewati",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("btn_skip_onboarding")
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier.testTag("btn_close_onboarding")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> OnboardingWelcomePage()
                        1 -> OnboardingWeatherForecastPage()
                        2 -> OnboardingMaritimeDataPage()
                        3 -> OnboardingCompassPage(onOpenCompassDetailed = onOpenCompassDetailed)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Row: Indicator Dots & Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pager Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width = if (isSelected) 24.dp else 8.dp
                            val color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    // Next / Finish Button
                    if (pagerState.currentPage < 3) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_next_onboarding")
                        ) {
                            Text("Lanjut", color = OrangeOnPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = OrangeOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_get_started_onboarding")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Sekarang", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide 0: Selamat Datang di Cuaca Mukomuko
 */
@Composable
private fun OnboardingWelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Image
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_onboarding_guide),
                contentDescription = "Ilustrasi Cuaca & Maritim Mukomuko",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = OrangePrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "PUSDALOPS BPBD KABUPATEN MUKOMUKO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Informasi Cuaca & Mitigasi Bencana Pesisir Terpadu",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aplikasi resmi terpercaya untuk memantau prakiraan cuaca 15 kecamatan, gelombang laut pesisir Mukomuko, dan panduan evakuasi darurat mandiri.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Feature Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HighlightPill(
                icon = Icons.Default.WbSunny,
                title = "Cuaca 15 Kec.",
                desc = "3 Hari Akurat",
                modifier = Modifier.weight(1f)
            )
            HighlightPill(
                icon = Icons.Default.Surfing,
                title = "Data Maritim",
                desc = "Ombak Pesisir",
                modifier = Modifier.weight(1f)
            )
            HighlightPill(
                icon = Icons.Default.Explore,
                title = "Kompas Darurat",
                desc = "Jalur Evakuasi",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HighlightPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Slide 1: Prakiraan Cuaca 15 Kecamatan & Radar Ekstrem
 */
@Composable
private fun OnboardingWeatherForecastPage() {
    var sampleConditionIndex by remember { mutableIntStateOf(0) }
    val conditions = listOf(
        Triple("Cerah", "31°C", Color(0xFF16A34A)),
        Triple("Hujan Lebat", "26°C", Color(0xFF0284C7)),
        Triple("Badai Petir (AWAS)", "24°C", Color(0xFFDC2626))
    )
    val current = conditions[sampleConditionIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(OrangePrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Prakiraan Cuaca 15 Kecamatan",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Dapatkan informasi cuaca harian dan prakiraan 3 hari ke depan untuk seluruh kecamatan di Kabupaten Mukomuko, lengkap dengan status peringatan dini BMKG.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Live Preview Weather Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kota Mukomuko (Contoh)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = current.third,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = current.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = current.second,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kelembaban: 82% • Angin: 18 km/j Barat Daya",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Icon(
                        imageVector = if (sampleConditionIndex == 0) Icons.Default.WbSunny else if (sampleConditionIndex == 1) Icons.Default.CloudQueue else Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = current.third,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ketuk untuk mencoba simulasi peringatan cuaca BMKG:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    conditions.forEachIndexed { idx, cond ->
                        OutlinedButton(
                            onClick = { sampleConditionIndex = idx },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (sampleConditionIndex == idx) {
                                ButtonDefaults.outlinedButtonColors(containerColor = cond.third.copy(alpha = 0.15f))
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = cond.first.split(" ")[0],
                                fontSize = 10.sp,
                                fontWeight = if (sampleConditionIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide 2: Data Maritim & Keselamatan Nelayan Pesisir
 */
@Composable
private fun OnboardingMaritimeDataPage() {
    var waveLevelIndex by remember { mutableIntStateOf(0) }
    val waveLevels = listOf(
        Triple("0.8 meter", "AMAN UNTUK SEMUA PERAHU", Color(0xFF16A34A)),
        Triple("2.2 meter", "WASPADA PERAHU NELAYAN KECIL", Color(0xFFEAB308)),
        Triple("3.8 meter", "BAHAYA GELOMBANG TINGGI (LARANGAN)", Color(0xFFDC2626))
    )
    val currentWave = waveLevels[waveLevelIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF0284C7).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Surfing,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Data Maritim & Keselamatan Nelayan",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Pantau kondisi pesisir Samudra Hindia Mukomuko: prakiraan tinggi gelombang 7 hari, pasang surut air laut, dan rekomendasi keselamatan berlayar nelayan.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Wave Gauge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulasi Tinggi Gelombang",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentWave.first,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = currentWave.third
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (waveLevelIndex + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = currentWave.third,
                    trackColor = currentWave.third.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = currentWave.third.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (waveLevelIndex == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = currentWave.third,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentWave.second,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentWave.third
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pilih tinggi gelombang untuk melihat rekomendasi:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    waveLevels.forEachIndexed { idx, wave ->
                        OutlinedButton(
                            onClick = { waveLevelIndex = idx },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (waveLevelIndex == idx) {
                                ButtonDefaults.outlinedButtonColors(containerColor = wave.third.copy(alpha = 0.15f))
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = wave.first,
                                fontSize = 10.sp,
                                fontWeight = if (waveLevelIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide 3: Kompas Digital & SOP Jalur Evakuasi Bencana
 */
@Composable
private fun OnboardingCompassPage(
    onOpenCompassDetailed: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { CompassSensorManager(context) }

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    val state by sensorManager.compassState.collectAsState()
    var manualDegree by remember { mutableFloatStateOf(0f) }

    val displayAzimuth = (state.azimuthDegrees + manualDegree) % 360f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF16A34A).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Kompas & Evakuasi Mandiri",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Gunakan sensor orientasi ponsel untuk mengetahui arah mata angin dan menemukan jalur evakuasi aman menuju dataran tinggi saat potensi gempa/tsunami.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive Mini Compass Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Arah Saat Ini: ${displayAzimuth.toInt()}° ${state.cardinalDirection}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Text(
                            text = if (state.hasHardwareSensor) "Sensor Kompas Terhubung" else "Geser untuk memutar kompas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Surface(
                        color = Color(0xFF16A34A),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "EVAKUASI: TIMUR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Compass Bezel
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, OrangePrimary, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                manualDegree = (manualDegree + dragAmount.x * 0.5f) % 360f
                                sensorManager.setManualHeading(displayAzimuth)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Needle rotated by -displayAzimuth
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Jarum Kompas",
                        tint = OrangePrimary,
                        modifier = Modifier
                            .size(54.dp)
                            .rotate(-displayAzimuth)
                    )

                    // Cardinal labels
                    Text("U", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OrangePrimary, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                    Text("S", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                    Text("B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                    Text("T", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Titik Kumpul Aman: TES Tsunami Dataran Tinggi (1.4 km)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF16A34A)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenCompassDetailed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_detailed_compass")
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Kompas Layar Penuh", fontSize = 12.sp)
                }
            }
        }
    }
}
