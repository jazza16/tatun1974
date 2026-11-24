package com.example.ui.components

import android.widget.Toast
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.SmartOfficerAuthRepository
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

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
    onLoginAsOfficer: () -> Unit = {},
    onEnterAsGuest: () -> Unit = onDismiss,
    onOpenEvacuationGuide: () -> Unit = {},
    onBiometricLoginSuccess: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val officerAuthManager = remember { OfficerAuthManager(context) }
    val savedNip = remember(isVisible) {
        officerAuthManager.getSavedNip().ifBlank { officerAuthManager.getOfficerBadgeId() }.trim()
    }
    val savedOfficerName = remember(isVisible) {
        officerAuthManager.getOfficerName().trim()
    }
    val canAuthenticateBiometric = remember(context, isVisible) {
        com.example.security.checkBiometricAvailability(context) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val showBiometricButton = canAuthenticateBiometric && savedNip.isNotBlank()
    var isBiometricAuthenticating by remember(isVisible) { mutableStateOf(false) }

    val handleSplashBiometricClick: () -> Unit = triggerSplashBio@{
        if (isBiometricAuthenticating) return@triggerSplashBio
        val currentNip = officerAuthManager.getSavedNip().ifBlank { officerAuthManager.getOfficerBadgeId() }.trim()
        val currentName = officerAuthManager.getOfficerName().trim()
        if (currentNip.isBlank()) {
            Toast.makeText(
                context,
                "Login dengan PIN terlebih dahulu sekali untuk mengaktifkan Login Biometrik.",
                Toast.LENGTH_LONG
            ).show()
            onLoginAsOfficer()
            return@triggerSplashBio
        }

        val subtitle = if (currentName.isNotBlank()) "$currentName ($currentNip)" else currentNip
        com.example.security.launchOfficerBiometricPrompt(
            context = context,
            title = "Login Biometrik Petugas Pusdalops",
            subtitle = subtitle,
            negativeButtonText = "Gunakan PIN",
            onSuccess = {
                coroutineScope.launch {
                    isBiometricAuthenticating = true
                    try {
                        val smartAuthRepo = SmartOfficerAuthRepository.getInstance(context)
                        val securityManager = com.example.security.SecurityManager.getInstance(context)
                        val bioResult = kotlinx.coroutines.withTimeout(22_000L) {
                            smartAuthRepo.authenticateWithBiometric(currentNip)
                        }
                        if (bioResult.isSuccess) {
                            val cred = bioResult.getOrThrow()
                            val hasSession = securityManager.hasActiveVerifiedLocalSession(cred.nip) || run {
                                val sessionRes = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                                    securityManager.verifyDeviceAuthenticityDuringLogin(
                                        officerNip = cred.nip,
                                        officerName = cred.fullName,
                                        officerRole = cred.role,
                                        loginMethod = "BIOMETRIK",
                                        loginAction = "LOGIN_SUCCESSFUL_BIOMETRIC_AUTH"
                                    )
                                }
                                sessionRes?.isSuccess == true && sessionRes.getOrNull()?.accessGranted == true
                            }
                            if (hasSession && officerAuthManager.isOfficerLoggedIn()) {
                                Toast.makeText(
                                    context,
                                    "Selamat bertugas, ${cred.fullName}! (Biometrik Terverifikasi)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onBiometricLoginSuccess()
                            } else {
                                officerAuthManager.logoutOfficer()
                                Toast.makeText(
                                    context,
                                    "Integritas perangkat tidak memenuhi syarat keamanan untuk masuk ke Ruang Kerja.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            val errMsg = bioResult.exceptionOrNull()?.message
                                ?: "Gagal memverifikasi kredensial petugas."
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Toast.makeText(
                            context,
                            "Waktu verifikasi biometrik ke server habis. Periksa sinyal internet lalu coba lagi.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            e.localizedMessage ?: "Terjadi kendala saat login biometrik.",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isBiometricAuthenticating = false
                    }
                }
            },
            onUsePinFallback = {
                isBiometricAuthenticating = false
                onLoginAsOfficer()
            },
            onCancelled = {
                isBiometricAuthenticating = false
            },
            onError = { errMsg ->
                isBiometricAuthenticating = false
                Toast.makeText(context, "Biometrik gagal: $errMsg", Toast.LENGTH_SHORT).show()
            }
        )
    }

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

        // Drive entry animation
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
                animationSpec = tween(1200, easing = LinearEasing)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("startup_splash_wallpaper_screen")
        ) {
            // 1. High-Resolution Wallpaper Photo Background (BoxFit.cover / ContentScale.Crop + Alignment.TopCenter)
            Image(
                painter = painterResource(id = R.drawable.img_mukomuko_wallpaper),
                contentDescription = "Foto Lanskap Alam & Mitigasi Bencana Mukomuko",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim Overlay to maintain perfect contrast and readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.52f),
                                Color(0xFF0F172A).copy(alpha = 0.78f),
                                Color(0xFF0F172A).copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            // Main Center Content: Safe Area + Top-Aligned Logo & Responsive Scroll
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 28.dp,
                                bottom = 88.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                            modifier = Modifier.size(116.dp)
                        ) {
                            // Outer Pulsing Glow Circle
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(glowScale)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(alpha = glowAlpha))
                            )

                            // Outer Border Ring
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(2.dp, OrangePrimary, CircleShape)
                            )

                            // Official BPBD Logo Image
                            Image(
                                painter = painterResource(id = R.drawable.ic_bpbd_mukomuko_circle),
                                contentDescription = "Logo BPBD Mukomuko",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .size(86.dp)
                                    .aspectRatio(1f)
                                    .scale(logoScale.value)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .testTag("startup_bpbd_logo")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Name Title SIPAGAR KITA
                        Text(
                            text = "SIPAGAR KITA",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Title & Department Branding
                        Text(
                            text = "PUSDALOPS-PB BPBD",
                            fontSize = 19.sp,
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Sistem Informasi Penanggulangan Bencana\n& Kesiapsiagaan Mitigasi Daerah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Semboyan Penanganan Bencana Indonesia
                    Text(
                        text = "BENCANA URUSAN BERSAMA",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                            .testTag("text_motto_bencana_urusan_bersama")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Telemetry Status & Clean Login Options
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
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
                                fontSize = 11.sp,
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dual Entry Portal Options: Locked & Unified GUI
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onEnterAsGuest,
                                enabled = !isBiometricAuthenticating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_enter_guest"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    contentColor = OrangeOnPrimary,
                                    disabledContainerColor = OrangePrimary.copy(alpha = 0.80f),
                                    disabledContentColor = OrangeOnPrimary.copy(alpha = 0.90f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    disabledElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = OrangeOnPrimary.copy(alpha = if (isBiometricAuthenticating) 0.90f else 1f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Masuk Sebagai Tamu / Warga",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = OrangeOnPrimary.copy(alpha = if (isBiometricAuthenticating) 0.90f else 1f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Button(
                                onClick = onLoginAsOfficer,
                                enabled = !isBiometricAuthenticating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_enter_officer"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF15803D),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF15803D).copy(alpha = 0.80f),
                                    disabledContentColor = Color.White.copy(alpha = 0.90f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    disabledElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = if (isBiometricAuthenticating) 0.90f else 1f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Login Petugas Pusdalops",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = if (isBiometricAuthenticating) 0.90f else 1f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (showBiometricButton) {
                                OutlinedButton(
                                    onClick = handleSplashBiometricClick,
                                    enabled = !isBiometricAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_biometric_login"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.14f),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.White.copy(alpha = 0.16f),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    if (isBiometricAuthenticating) {
                                        CircularProgressIndicator(
                                            color = OrangePrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Memverifikasi Biometrik...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Login Biometrik",
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Login Biometrik",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }

                // Tagline di bagian paling bawah layar (seperti Copyright)
                Text(
                    text = "Satu Data, Satu Langkah, Siaga Mukomuko",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}
