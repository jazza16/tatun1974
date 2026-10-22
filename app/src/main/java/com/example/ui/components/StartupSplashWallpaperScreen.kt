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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * Wallpaper Awal Aplikasi (Startup Splash Wallpaper Screen)
 * Menampilkan foto dokumentasi operasional lapangan pemadaman Karhutla & siaga bencana
 * oleh personel PUSDALOPS BPBD Kabupaten Mukomuko sebagai wallpaper latar penuh,
 * bersamaan dengan kemunculan logo resmi BPBD, animasi branding, dan progress peluncuran aplikasi.
 */
@Composable
fun StartupSplashWallpaperScreen(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissDelayMs: Long = 2800L
) {
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

        // Drive entry animation and automatic dismissal
        LaunchedEffect(Unit) {
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
            delay(150)
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .testTag("startup_splash_wallpaper_screen")
        ) {
            // 1. Full-screen Photo Wallpaper (Foto Lapangan Pemadaman Karhutla BPBD Mukomuko)
            Image(
                painter = painterResource(id = R.drawable.wallpaper_startup_bpbd),
                contentDescription = "Dokumentasi Operasi Lapangan BPBD Mukomuko",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("startup_wallpaper_photo")
            )

            // 2. High-contrast Cinematic Dark Vignette & Gradient Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.70f), // Top protection for status bar
                                Color.Black.copy(alpha = 0.40f), // Mid image visibility
                                Color.Black.copy(alpha = 0.75f), // Logo & text readability
                                Color(0xFF0F172A).copy(alpha = 0.94f) // Bottom foundation
                            )
                        )
                    )
            )

            // 3. Main Center Content: Logo, Titles, Badge & Mission Context
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .testTag("startup_header_tag")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PEMERINTAH KABUPATEN MUKOMUKO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Middle Section: Logo with Pulsing Halo and App Branding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    // Pulsing Glow & Logo Container
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(136.dp)
                    ) {
                        // Outer Pulsing Glow Circle
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(glowScale)
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(alpha = glowAlpha))
                        )

                        // Outer Border Ring
                        Box(
                            modifier = Modifier
                                .size(116.dp)
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
                                .size(102.dp)
                                .scale(logoScale.value)
                                .clip(CircleShape)
                                .border(2.5.dp, Color.White, CircleShape)
                                .testTag("startup_bpbd_logo")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title & Department Branding
                    Text(
                        text = "PUSDALOPS-PB BPBD",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "KABUPATEN MUKOMUKO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sistem Prakiraan Cuaca, Maritim Pesisir\n& Peringatan Dini Kebencanaan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Field Operations Context Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA580C).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFB923C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Dokumentasi Operasi Lapangan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB923C)
                                )
                                Text(
                                    text = "Tim Reaksi Cepat (TRC) Penanganan Karhutla & Siaga Cuaca Ekstrem Mukomuko",
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                // Bottom Section: Progress Bar, Status Text & Skip/Enter Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Status Loading Indicator Text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (progress.value < 0.95f) "Menyiapkan telemetri cuaca 15 kecamatan..." else "Sistem siap digunakan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = OrangePrimary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Enter App Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(46.dp)
                            .testTag("btn_enter_app"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = OrangeOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sentuh layar untuk langsung masuk • v1.2.0 PUSDALOPS",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
