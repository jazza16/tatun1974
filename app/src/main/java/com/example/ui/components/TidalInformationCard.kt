package com.example.ui.components

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.TidalData
import com.example.model.TidalDataHelper

/**
 * Kartu Info Pasang Surut & Pasang Naik Dinamis Samudra Hindia Mukomuko
 * Dilengkapi animasi ombak air dinamis, indikator status waktu nyata,
 * dan panduan interaktif pop-up untuk nelayan & warga pesisir.
 */
@Composable
fun TidalInformationCard(
    district: District,
    modifier: Modifier = Modifier
) {
    val tidalData = remember(district) { TidalDataHelper.getTidalForecastForDistrict(district) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Wave animation setup
    val infiniteTransition = rememberInfiniteTransition(label = "tidal_wave_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_horizontal_phase"
    )

    val verticalWaveSway by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_vertical_sway"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tidal_information_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = "Pasang Surut",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Info Pasang Surut Air Laut",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pesisir ${district.name} • Samudra Hindia",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Interactive Guide Button
                OutlinedButton(
                    onClick = { showGuideDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Panduan",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Panduan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DYNAMIC WATER LEVEL WAVE ANIMATION BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 115.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                // Animated Water Wave Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Water level baseline based on percent
                    val waterY = height * (1f - (0.25f + tidalData.waterLevelPercent * 0.55f)) + verticalWaveSway

                    val wavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, waterY)

                        val waveFrequency = 1.8f
                        val waveAmplitude = 9f
                        val step = 10f
                        var x = 0f
                        while (x <= width) {
                            val relativeX = x / width
                            val y = waterY + Math.sin((relativeX * waveFrequency * 2 * Math.PI + waveOffset).toDouble()).toFloat() * waveAmplitude
                            lineTo(x, y)
                            x += step
                        }

                        lineTo(width, height)
                        close()
                    }

                    // Draw gradient wave
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0284C7).copy(alpha = 0.85f),
                                Color(0xFF0369A1),
                                Color(0xFF0C4A6E)
                            )
                        )
                    )
                }

                // Overlay Status & Numbers
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (tidalData.isRisingTide) Color(0xFF166534).copy(alpha = 0.9f) else Color(0xFF7C2D12).copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tidalData.isRisingTide) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (tidalData.isRisingTide) Color(0xFF86EFAC) else Color(0xFFFDBA74),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tidalData.tideMovementText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = tidalData.lunarPhaseName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFDE68A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Ketinggian Muka Air Saat Ini",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${tidalData.currentWaterLevelMeters}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = " meter",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp).weight(0.5f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Event Berikutnya:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "${tidalData.nextEventTime} (${tidalData.nextEventType})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-GRID TIDAL EVENTS TODAY (PASANG NAIK & SURUT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pasang Naik Pagi
                TidalEventPill(
                    title = "Pasang Naik 1",
                    time = tidalData.highTideMorningTime,
                    height = tidalData.highTideMorningHeight,
                    isHigh = true,
                    modifier = Modifier.weight(1f)
                )

                // Pasang Surut Siang
                TidalEventPill(
                    title = "Pasang Surut 1",
                    time = tidalData.lowTideNoonTime,
                    height = tidalData.lowTideNoonHeight,
                    isHigh = false,
                    modifier = Modifier.weight(1f)
                )

                // Pasang Naik Sore
                TidalEventPill(
                    title = "Pasang Naik 2",
                    time = tidalData.highTideEveningTime,
                    height = tidalData.highTideEveningHeight,
                    isHigh = true,
                    modifier = Modifier.weight(1f)
                )

                // Pasang Surut Malam
                TidalEventPill(
                    title = "Pasang Surut 2",
                    time = tidalData.lowTideNightTime,
                    height = tidalData.lowTideNightHeight,
                    isHigh = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estuarine & Fisherman Crossing Advisory Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBoat,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kondisi Muara & Perahu Nelayan:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tidalData.estuarineCrossingAdvice,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    // Interactive Pop-up Guide ('Info Pasang Surut')
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Panduan Pasang Surut Nelayan & Pesisir Mukomuko",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Siklus Ganda Harian (Semi-Diurnal):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = "Pesisir Mukomuko mengalami 2 kali pasang naik dan 2 kali pasang surut setiap 24 jam dengan jeda waktu sekitar 6 jam antar puncak.",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "2. Waktu Aman Melintasi Pintu Muara Sungai:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        text = "Muara Manjunto, Muara Ipuh, dan Muara Teramang sering mengalami pendangkalan pasir. Waktu terbaik perahu nelayan katir/tempel keluar/masuk adalah saat air pasang (tinggi muka air >= 1.5 meter). Hindari melintas saat surut terendah agar lunas perahu tidak kandas.",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "3. Pasang Purnama (Spring Tide) & Banjir Rob:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = "Saat bulan purnama dan bulan baru, pasang naik mencapai titik tertinggi (hingga 2.15 meter). Waspadai potensi luapan air laut ke pemukiman nelayan (rob) dan abrasi jalan lintas barat Koto Jaya - Pasar Teramang.",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Mengerti", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun TidalEventPill(
    title: String,
    time: String,
    height: String,
    isHigh: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHigh) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = height,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHigh) Color(0xFF38BDF8) else Color(0xFFFBBF24)
            )
        }
    }
}
