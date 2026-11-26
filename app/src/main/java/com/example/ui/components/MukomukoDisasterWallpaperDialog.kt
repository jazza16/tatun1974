package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen dialog displaying the Mukomuko Landscape & Disaster Mitigation HD Poster,
 * complete with pinch-to-zoom preview, educational mitigation highlights, and zero-permission
 * "Simpan Poster" & "Bagikan" actions without requiring SET_WALLPAPER.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MukomukoDisasterWallpaperDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isFullScreenZoom by remember { mutableStateOf(false) }
    var isSavingPoster by remember { mutableStateOf(false) }

    val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val activityNavBottom = remember(context, density) {
        var c: Context? = context
        while (c is android.content.ContextWrapper && c !is android.app.Activity) {
            c = c.baseContext
        }
        val act = c as? android.app.Activity
        val bottomPx = act?.window?.decorView?.let {
            androidx.core.view.ViewCompat.getRootWindowInsets(it)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom
        } ?: 0
        with(density) { bottomPx.toDp() }
    }
    val safeBottomInset = maxOf(composeNavBottom, activityNavBottom, 28.dp)

    Dialog(
        onDismissRequest = {
            if (isFullScreenZoom) {
                isFullScreenZoom = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        if (isFullScreenZoom) {
            FullScreenZoomableWallpaperView(
                onExitZoom = { isFullScreenZoom = false },
                isSavingPoster = isSavingPoster,
                safeBottomInset = safeBottomInset,
                onSavePoster = {
                    coroutineScope.launch {
                        isSavingPoster = true
                        saveEducationalPosterToGallery(context)
                        isSavingPoster = false
                    }
                }
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mukomuko_disaster_wallpaper_dialog"),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Header Bar
                    Surface(
                        color = OrangePrimary,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Landscape,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Poster Edukasi & Lanskap HD",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Kondisi Alam & Mitigasi Kab. Mukomuko",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_close_disaster_wallpaper_dialog")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = safeBottomInset + 24.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // High-Resolution Wallpaper Preview Card
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.2.dp, OrangePrimary.copy(alpha = 0.45f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { isFullScreenZoom = true }
                                .testTag("wallpaper_preview_card")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_mukomuko_disaster_wallpaper),
                                    contentDescription = "Wallpaper Lanskap & Mitigasi Bencana Kabupaten Mukomuko",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = 0.25f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.82f)
                                                )
                                            )
                                        )
                                )

                                // Fullscreen zoom badge
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Perbesar",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Lihat Penuh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Caption Overlay
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(14.dp)
                                ) {
                                    Surface(
                                        color = OrangePrimary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "POSTER EDUKASI HD • BPBD MUKOMUKO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Pesisir Samudra Hindia, Kearifan Lokal & Siaga Mitigasi",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Ketuk gambar untuk memperbesar (pinch-to-zoom)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }

                        // Action Buttons Row (FlowRow for large font scale resilience)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSavingPoster = true
                                        saveEducationalPosterToGallery(context)
                                        isSavingPoster = false
                                    }
                                },
                                enabled = !isSavingPoster,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    contentColor = OrangeOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 140.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_save_disaster_poster")
                            ) {
                                if (isSavingPoster) {
                                    CircularProgressIndicator(
                                        color = OrangeOnPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menyimpan...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan Poster", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        shareEducationalPoster(context)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 140.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_share_landscape_info")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bagikan Poster",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangePrimary
                                )
                            }
                        }

                        // Featured Cultural Heritage Card: Semboyan Daerah & Kearifan Lokal Mukomuko
                        MukomukoCulturalHeritageCard()

                        // Educational Sections
                        LandscapeInfoSectionCard(
                            icon = Icons.Default.Terrain,
                            title = "2. Profil Geografis & Lanskap Mukomuko",
                            accentColor = Color(0xFF0284C7),
                            items = listOf(
                                "Garis Pantai ±150–180 km: Membentang di pesisir barat Sumatra dan berhadapan langsung dengan Samudra Hindia.",
                                "15 Kecamatan Beragam: Mencakup kawasan pesisir pantai, muara Sungai Selagan & Manjuto, dataran rendah pertanian, hingga perbukitan Bukit Barisan.",
                                "Iklim Tropis Basah: Curah hujan tinggi sepanjang tahun dengan dinamika angin muson dan gelombang laut signifikan."
                            )
                        )

                        LandscapeInfoSectionCard(
                            icon = Icons.Default.Warning,
                            title = "3. Potensi Ancaman Bencana Wilayah",
                            accentColor = Color(0xFFDC2626),
                            items = listOf(
                                "Gempa & Megathrust Mentawai: Zona subduksi aktif Indo-Australia dan Eurasia yang menuntut kesiapsiagaan gempa-tsunami 24 jam.",
                                "Abrasi Pantai Kritis: Gelombang Samudra Hindia mengikis bibir pantai, mengancam permukiman nelayan dan Jalan Lintas Barat (Jalinbar).",
                                "Banjir & Longsor: Luapan DAS Selagan, Manjuto, dan Air Dikit serta kerentanan lereng perbukitan saat hujan ekstrem.",
                                "Cuaca Maritim & Karhutla: Gelombang tinggi 2,5–4,0 m pada musim barat serta kerawanan kebakaran lahan gambut pada musim kemarau."
                            )
                        )

                        LandscapeInfoSectionCard(
                            icon = Icons.Default.Emergency,
                            title = "4. Kesiapsiagaan & Mitigasi BPBD Mukomuko",
                            accentColor = Color(0xFF16A34A),
                            items = listOf(
                                "PUSDALOPS-PB 24 Jam & Call Center 117: Pemantauan real-time BMKG WRS-NTWC, radar cuaca, hotspot Karhutla, dan respon cepat TRC.",
                                "Infrastruktur & Rambu Evakuasi: Penguatan tanggul abrasi (revetment/tetrapod), rambu jalur evakuasi tsunami, dan Tempat Evakuasi Sementara (TES).",
                                "Kearifan Lokal & Destana: Penguatan Desa Tangguh Bencana bersama masyarakat pesisir, nelayan, petani, dan relawan kebencanaan."
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenZoomableWallpaperView(
    onExitZoom: () -> Unit,
    isSavingPoster: Boolean,
    safeBottomInset: androidx.compose.ui.unit.Dp,
    onSavePoster: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale
                    offset = if (newScale > 1f) {
                        Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        Offset.Zero
                    }
                }
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_mukomuko_disaster_wallpaper),
            contentDescription = "Pratinjau Penuh Poster Edukasi Mukomuko",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Top Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.70f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onExitZoom)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kembali",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onSavePoster,
                enabled = !isSavingPoster,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = OrangeOnPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                if (isSavingPoster) {
                    CircularProgressIndicator(
                        color = OrangeOnPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Menyimpan...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Poster", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom Hint
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = safeBottomInset + 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cubit dengan dua jari untuk zoom & geser gambar",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
private fun LandscapeInfoSectionCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    items: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MukomukoCulturalHeritageCard() {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val heritageGold = if (isDark) Color(0xFFF59E0B) else Color(0xFFB45309)
    val heritageCardBg = if (isDark) Color(0xFF1F1A12) else Color(0xFFFFFBEB)
    val heritageBorder = Color(0xFFF59E0B)
    val titleColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
    val bodyColor = if (isDark) Color(0xFFF5EFE6).copy(alpha = 0.90f) else Color(0xFF451A03).copy(alpha = 0.90f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = heritageCardBg),
        border = BorderStroke(1.2.dp, heritageBorder.copy(alpha = if (isDark) 0.50f else 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_semboyan_kapuang_sakti_sirih_carano")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, heritageBorder.copy(alpha = 0.45f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mukomuko_logo),
                        contentDescription = "Lambang Daerah Kabupaten Mukomuko Kapuang Sakti Ratau Batuah",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFFB45309),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "1. SEMBOYAN & KEARIFAN LOKAL MUKOMUKO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"Kapuang Sakti Ratau Batuah\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    Text(
                        text = "Filosofi Adat Sirih Carano • Bumi Kabupaten Mukomuko",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = heritageGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val culturalItems = listOf(
                "Kapuang Sakti: Bermakna kampung atau negeri asal yang memiliki kekuatan adat, keteguhan moral, persatuan jiwa, serta daya juang masyarakat dalam menjaga marwah dan keselamatan tanah kelahiran.",
                "Ratau Batuah: Bermakna tanah rantau yang bertuah — wilayah yang ramah, terbuka, membawa keberkahan, kemakmuran, dan kedamaian bagi seluruh penduduk maupun perantau yang hidup dan mengabdi di Kabupaten Mukomuko.",
                "Filosofi Sirih Carano: Carano berisi sirih pinang lengkap merupakan wadah adat luhur masyarakat Mukomuko yang melambangkan ketulusan hati, penghormatan tertinggi, musyawarah mufakat, keramahtamahan, serta eratnya tali silaturahmi antarwarga.",
                "Kearifan Lokal dalam Mitigasi Bencana: Semangat Kapuang Sakti Ratau Batuah dan budaya Sirih Carano menjiwai gotong royong Desa Tangguh Bencana (Destana), solidaritas antar-15 kecamatan, serta prinsip \"Bencana Urusan Bersama\" dari pesisir Samudra Hindia hingga perbukitan Bukit Barisan."
            )

            culturalItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(heritageGold, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        fontSize = 12.sp,
                        lineHeight = 17.5.sp,
                        color = bodyColor
                    )
                }
            }
        }
    }
}

private suspend fun saveEducationalPosterToGallery(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_mukomuko_disaster_wallpaper
            )
            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat gambar poster edukasi.", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fileName = "Poster_Edukasi_BPBD_Mukomuko_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SIPAGAR KITA")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Poster Edukasi HD berhasil disimpan ke Galeri (Pictures/SIPAGAR KITA).",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@withContext
                }
            }

            // Fallback tanpa izin penyimpanan pada perangkat pre-Android 10: buka lembar Bagikan/Simpan
            withContext(Dispatchers.Main) {
                shareEducationalPoster(context)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Tidak dapat menyimpan poster: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

private suspend fun shareEducationalPoster(context: Context) {
    val settings = com.example.data.repository.DocumentSettingsRepository.getCachedOrDefaultSettings(context)
    val shareText = buildString {
        append(com.example.util.ReportExporter.buildStandardKopPlainText(settings))
        appendLine("POSTER EDUKASI LANSKAP, KEARIFAN LOKAL & KESIAPSIAGAAN BENCANA KABUPATEN MUKOMUKO")
        appendLine()
        appendLine("• Semboyan Daerah: \"Kapuang Sakti Ratau Batuah\" & Filosofi Adat Sirih Carano — simbol persatuan adat, ketulusan hati, musyawarah mufakat, dan gotong royong kebencanaan.")
        appendLine("• Garis Pantai ±150–180 km menghadap Samudra Hindia (15 Kecamatan).")
        appendLine("• Ancaman utama: Gempa Megathrust Mentawai, Tsunami, Abrasi Pesisir, Banjir, Longsor & Cuaca Maritim.")
        appendLine("• Posko Siaga 24 Jam & Call Center Darurat BPBD: 117 / 112.")
    }

    val posterUri = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_mukomuko_disaster_wallpaper
            ) ?: return@runCatching null
            val exportDir = File(context.cacheDir, "exported_reports").apply { mkdirs() }
            val posterFile = File(exportDir, "Poster_Edukasi_BPBD_Mukomuko.png")
            FileOutputStream(posterFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", posterFile)
        }.getOrNull()
    }

    val intent = if (posterUri != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, posterUri)
            putExtra(Intent.EXTRA_SUBJECT, "Poster Edukasi & Mitigasi Bencana Kab. Mukomuko")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Info Lanskap & Mitigasi Bencana Kab. Mukomuko")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Poster Edukasi Mukomuko"))
}
