package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricManager
import androidx.biometric.compose.rememberAuthenticationLauncher
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.SmartOfficerAuthRepository
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showExplanationSheet by remember { mutableStateOf(false) }
    var isPureWallpaperMode by remember { mutableStateOf(false) }
    var isSettingWallpaper by remember { mutableStateOf(false) }

    val officerAuthManager = remember { OfficerAuthManager(context) }
    val savedNip = officerAuthManager.getOfficerBadgeId()
    val canAuthenticateBiometric = remember(context) {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val showBiometricButton = canAuthenticateBiometric && savedNip.isNotBlank()

    val authLauncher = rememberAuthenticationLauncher { authResult ->
        when (authResult) {
            is AuthenticationResult.Success -> {
                coroutineScope.launch {
                    val smartAuthRepo = SmartOfficerAuthRepository(context)
                    val bioResult = smartAuthRepo.authenticateWithBiometric(savedNip)
                    if (bioResult.isSuccess) {
                        val cred = bioResult.getOrThrow()
                        Toast.makeText(
                            context,
                            "Selamat bertugas, ${cred.fullName}! (Biometrik Terverifikasi)",
                            Toast.LENGTH_SHORT
                        ).show()
                        onBiometricLoginSuccess()
                    } else {
                        val errMsg = bioResult.exceptionOrNull()?.message ?: "Gagal memverifikasi kredensial petugas."
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        // JANGAN panggil onBiometricLoginSuccess()
                    }
                }
            }
            is AuthenticationResult.Error -> {
                Toast.makeText(
                    context,
                    "Biometrik gagal: ${authResult.errString}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is AuthenticationResult.CustomFallbackSelected -> {
                onLoginAsOfficer()
            }
        }
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
                    animationSpec = tween(1200, easing = LinearEasing)
                )
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
            // 1. High-Resolution Wallpaper Photo Background
            Image(
                painter = painterResource(id = R.drawable.img_mukomuko_wallpaper),
                contentDescription = "Foto Lanskap Alam & Mitigasi Bencana Mukomuko",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim Overlay to maintain perfect contrast and readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (isPureWallpaperMode) {
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            } else {
                                listOf(
                                    Color.Black.copy(alpha = 0.52f),
                                    Color(0xFF0F172A).copy(alpha = 0.78f),
                                    Color(0xFF0F172A).copy(alpha = 0.94f)
                                )
                            }
                        )
                    )
            )

            // 2. Pure Wallpaper Mode Controls (when user views full photo without distractions)
            if (isPureWallpaperMode) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.70f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isPureWallpaperMode = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kembali ke Menu",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = OrangePrimary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable(enabled = !isSettingWallpaper) {
                                    coroutineScope.launch {
                                        isSettingWallpaper = true
                                        applyWallpaperDirectly(context)
                                        isSettingWallpaper = false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSettingWallpaper) {
                                CircularProgressIndicator(color = OrangeOnPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memasang...", fontSize = 12.sp, color = OrangeOnPrimary, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = OrangeOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pasang di HP", fontSize = 12.sp, color = OrangeOnPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom hint overlay
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "Ketuk layar di mana saja untuk kembali ke menu login",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                // 3. Main Center Content: Symmetrical Proportional Spacing, Logo, Titles, Slogan & Login Menu
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 16.dp,
                                bottom = 72.dp
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_enter_guest"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    contentColor = OrangeOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = OrangeOnPrimary,
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
                                    tint = OrangeOnPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Button(
                                onClick = onLoginAsOfficer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_enter_officer"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF15803D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenEvacuationGuide,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("btn_startup_sop_evakuasi"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.45f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "SOP Evakuasi",
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SOP Evakuasi (100% Offline)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (showBiometricButton) {
                                OutlinedButton(
                                    onClick = {
                                        val fallbackOption = AuthenticationRequest.Biometric.Fallback.CustomOption(
                                            "Gunakan PIN",
                                            AuthenticationRequest.Biometric.Fallback.ICON_TYPE_PASSWORD
                                        )
                                        val biometricRequest = AuthenticationRequest.Biometric.Builder(
                                            "Login Biometrik Petugas Pusdalops",
                                            fallbackOption
                                        )
                                            .setSubtitle("${officerAuthManager.getOfficerName()} ($savedNip)")
                                            .build()
                                        authLauncher.launch(biometricRequest)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .height(48.dp)
                                        .testTag("btn_biometric_login"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        contentColor = Color.White
                                    )
                                ) {
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
                            .padding(bottom = 32.dp)
                            .navigationBarsPadding()
                    )
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
                        fontSize = 11.sp,
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
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

private suspend fun applyWallpaperDirectly(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_mukomuko_wallpaper
            )
            if (bitmap != null) {
                wallpaperManager.setBitmap(bitmap)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Wallpaper Resmi BPBD Mukomuko berhasil dipasang di layar HP!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat file wallpaper.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Info: ${e.localizedMessage ?: "Tidak dapat memasang wallpaper"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
