package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tsunami
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSunGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-Screen High-Resolution Wallpaper & Educational Disaster Mitigation Modal
 * for Mukomuko Regency, Bengkulu.
 *
 * Explains:
 * 1. Natural Geography & Landscape of Mukomuko (Coastline, Rivers, TNKS Hills)
 * 2. Disaster Potentials (Megathrust Mentawai-Sunda, Tsunami, Floods, Karhutla, Abrasion)
 * 3. Mitigation & Emergency Protocols (20-20-20 Rule, Call Center 117, BPBD Response)
 *
 * Provides direct one-tap "Set as Device Wallpaper" using Android WallpaperManager.
 */
@Composable
fun MukomukoDisasterWallpaperDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedSectionIndex by remember { mutableIntStateOf(0) }
    var isFullScreenZoomMode by remember { mutableStateOf(false) }
    var isApplyingWallpaper by remember { mutableStateOf(false) }

    // Pinch-to-zoom / Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF100B08) else Color(0xFFFFFDF9))
                .testTag("mukomuko_disaster_wallpaper_dialog")
        ) {
            if (isFullScreenZoomMode) {
                // --- FULL-SCREEN INTERACTIVE PINCH-ZOOM MODE ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .transformable(state = transformableState)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mukomuko_disaster_wallpaper),
                        contentDescription = "Wallpaper Resolusi Tinggi Alam & Mitigasi Bencana Mukomuko",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .testTag("wallpaper_zoom_image")
                    )

                    // Zoom Controls Overlay Top Bar
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isFullScreenZoomMode = false
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                    modifier = Modifier.testTag("btn_close_zoom_mode")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Penampil Wallpaper Layar Penuh",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cubit layar untuk zoom • Geser untuk jelajahi detail",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "Reset Zoom",
                                        tint = OrangePrimary
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Bar in Zoom Mode
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isApplyingWallpaper = true
                                        applyWallpaperToDevice(context)
                                        isApplyingWallpaper = false
                                    }
                                },
                                enabled = !isApplyingWallpaper,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_apply_wallpaper_zoom")
                            ) {
                                if (isApplyingWallpaper) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Memasang...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Terapkan Jadi Wallpaper HP")
                                }
                            }
                        }
                    }
                }
            } else {
                // --- STANDARD DETAILED INFORMATION & WALLPAPER HUB MODE ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar
                    Surface(
                        color = if (isDark) Color(0xFF1E140E) else OrangePrimary,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isDark) OrangePrimary else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Landscape,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Wallpaper & Infografis Bencana",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Geografi, Potensi & Penanganan Mukomuko",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("btn_close_wallpaper_dialog")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Wallpaper Preview Card with Quick Actions
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1F1610) else Color(0xFFFFF7ED)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isDark) OrangePrimary.copy(alpha = 0.5f) else Color(0xFFFDBA74)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Image Thumbnail Box with overlay badges
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(380.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black)
                                        .clickable { isFullScreenZoomMode = true }
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_mukomuko_disaster_wallpaper),
                                        contentDescription = "Wallpaper Alam dan Penanganan Bencana Kabupaten Mukomuko",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("wallpaper_preview_thumbnail")
                                    )

                                    // Top Badge: Resolusi Tinggi & Pusdalops BPBD
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                tint = OrangePrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Kearifan Budaya & Mitigasi Bencana",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Tap to zoom hint overlay
                                    Surface(
                                        color = OrangePrimary.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ketuk untuk Memperbesar & Detail",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Main Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isApplyingWallpaper = true
                                                applyWallpaperToDevice(context)
                                                isApplyingWallpaper = false
                                            }
                                        },
                                        enabled = !isApplyingWallpaper,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_set_wallpaper_device")
                                    ) {
                                        if (isApplyingWallpaper) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Memasang...", fontSize = 12.sp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pasang Wallpaper HP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            shareDisasterInfo(context)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("btn_share_disaster_wallpaper")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Bagikan",
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bagikan", fontSize = 12.sp, color = OrangePrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Section Tabs: Alam / Potensi Bencana / SOP Penanganan
                        val tabs = listOf(
                            "Kondisi Alam",
                            "Potensi Bencana",
                            "Penanganan & SOP"
                        )

                        TabRow(
                            selectedTabIndex = selectedSectionIndex,
                            containerColor = if (isDark) Color(0xFF1E140E) else Color(0xFFFFFBEB),
                            contentColor = OrangePrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSectionIndex]),
                                    height = 3.dp,
                                    color = OrangePrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedSectionIndex == index,
                                    onClick = { selectedSectionIndex = index },
                                    modifier = Modifier.testTag("tab_wallpaper_section_$index"),
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedSectionIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSectionIndex == index) OrangePrimary else if (isDark) Color(0xFFCBD5E1) else Color(0xFF78350F)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Content Based on Selected Tab
                        when (selectedSectionIndex) {
                            0 -> NaturalConditionsSection(isDark = isDark)
                            1 -> DisasterPotentialsSection(isDark = isDark)
                            2 -> DisasterMitigationSection(isDark = isDark)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Call Center 117 Alert Card
                        Surface(
                            color = if (isDark) Color(0xFF2E1515) else Color(0xFFFFF1F2),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFEF4444), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Siaga Bencana 24 Jam • BPBD Mukomuko",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Hubungi Call Center Pusdalops 117 jika terjadi keadaan darurat bencana gempa bumi, banjir luapan, atau tsunami.",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF7F1D1D),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Kondisi Alam & Geografis Mukomuko
 */
@Composable
private fun NaturalConditionsSection(isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoDetailCard(
            icon = Icons.Default.Home,
            title = "Kearifan Budaya Rumah Adat & Sirih Carano",
            description = "Kabupaten Mukomuko memiliki semboyan sakral 'Kapuang Sakti Ratau Batuah' dan kearifan Rumah Adat Mukomuko dengan arsitektur tiang tinggi berpasak kayu lentur. Tradisi ini telah terbukti secara turun-temurun meredam guncangan gempa tektonik pesisir, dipadukan dengan penghormatan tradisi Sirih Carano yang mempererat gotong royong antar warga saat tanggap darurat.",
            tag = "Kearifan Budaya",
            tagColor = OrangePrimary,
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Landscape,
            title = "Bentang Alam Pesisir Samudra Hindia",
            description = "Kabupaten Mukomuko memiliki bentang garis pantai sepanjang ~180 kilometer yang berhadapan langsung dengan samudra luas. Lanskap pesisir berpadu dengan vegetasi kelapa dan pohon cemara udang alami yang berfungsi sebagai sabuk hijau pemecah energi angin dan ombak laut.",
            tag = "Geografi Pesisir",
            tagColor = Color(0xFF0284C7),
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Public,
            title = "Gugusan Pegunungan Bukit Barisan & TNKS",
            description = "Wilayah timur Mukomuko dibentengi pegunungan hutan tropis Taman Nasional Kerinci Seblat (TNKS). Hutan primer ini menjadi kawasan tangkapan air dan penyangga ekologis utama yang menjaga keseimbangan tanah dari ancaman longsor di daerah perbukitan.",
            tag = "Topografi Hulu",
            tagColor = WeatherGoodGreen,
            isDark = isDark
        )
    }
}

/**
 * Tab 2: Potensi Ancaman Bencana di Mukomuko
 */
@Composable
private fun DisasterPotentialsSection(isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoDetailCard(
            icon = Icons.Default.Warning,
            title = "Zona Subduksi Megathrust Mentawai - Bengkulu",
            description = "Mukomuko berada di dekat zona tumbukan lempeng tektonik aktif (Megathrust Mentawai - Bengkulu). Potensi seismik ini menuntut kesiapsiagaan warga terhadap getaran gempa bumi dangkal serta kepatuhan evakuasi mandiri.",
            tag = "Ancaman Tinggi",
            tagColor = Color(0xFFDC2626),
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Tsunami,
            title = "Potensi Gelombang Tsunami Pesisir",
            description = "Karakteristik pesisir terbuka tanpa pulau penghalang memerlukan kewaspadaan tinggi. Jika gempa terasa kuat lebih dari 20 detik, seluruh warga pesisir diarahkan segera bergerak menuju tempat evakuasi sementara (TES) atau dataran tinggi terdekat.",
            tag = "Prioritas Evakuasi",
            tagColor = Color(0xFFE11D48),
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Flood,
            title = "Banjir Luapan Debit Aliran Sungai",
            description = "Saat musim hujan lebat berdurasi lama di hulu Bukit Barisan, peningkatan debit air pada daerah aliran sungai berpotensi menyebabkan genangan di area bantaran rendah dan pemukiman pinggir sungai.",
            tag = "Musiman",
            tagColor = Color(0xFF0284C7),
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "Karhutla (Kebakaran Hutan & Lahan Gambut)",
            description = "Pada musim kemarau dan fenomena El Nino, lahan perkebunan kelapa sawit dan semak belukar di atas tanah gambut rentan terbakar cepat sehingga memicu kabut asap dan penurunan kualitas udara ISPU.",
            tag = "Kemarau",
            tagColor = Color(0xFFD97706),
            isDark = isDark
        )
    }
}

/**
 * Tab 3: SOP Penanganan & Mitigasi Kebencanaan BPBD
 */
@Composable
private fun DisasterMitigationSection(isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoDetailCard(
            icon = Icons.Default.Security,
            title = "Protokol Rumus 20-20-20 Evakuasi Tsunami",
            description = "• Jika merasakan gempa berayun selama 20 DETIK atau lebih (atau sulit berdiri tegak):\n• Anda memiliki waktu 20 MENIT untuk segera evakuasi mandiri.\n• Menuju ke tempat aman dengan ketinggian minimal 20 METER dari permukaan laut atau Tempat Evakuasi Sementara (TES). Jangan menunggu sirine berbunyi!",
            tag = "SOP Utama",
            tagColor = WeatherGoodGreen,
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Tsunami,
            title = "Sistem Peringatan Dini WRS-NTWC BMKG",
            description = "Aplikasi SIPAGAR KITA terhubung langsung ke server InaTEWS BMKG secara daring untuk menampilkan peringatan dini gempa terkini, estimasi waktu gelombang tsunami, dan magnitudo secara seketika.",
            tag = "Telemetri Real-Time",
            tagColor = OrangePrimary,
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.Flood,
            title = "Mitigasi & Penanganan Banjir DAS",
            description = "PUSDALOPS BPBD menyiagakan posko pantau debit air sungai, normalisasi berkala tanggul penahan banjir, pengerahan perahu karet evakuasi cepat TRC, serta penyiapan logistik dapur umum bagi pengungsi.",
            tag = "Kesiapsiagaan Tim",
            tagColor = Color(0xFF0284C7),
            isDark = isDark
        )

        InfoDetailCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "Pencegahan & Pemadaman Karhutla",
            description = "Pembangunan sekat bakar dan sekat kanal di lahan gambut, sosialisasi larangan membakar lahan, serta pengerahan armada pompa air portable Tim Reaksi Cepat (TRC) BPBD Kabupaten Mukomuko.",
            tag = "Mitigasi Aktif",
            tagColor = Color(0xFFD97706),
            isDark = isDark
        )
    }
}

@Composable
private fun InfoDetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tag: String,
    tagColor: Color,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF241A13) else Color(0xFFFFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF452B1E) else Color(0xFFFED7AA).copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF7C2D12)
                    )
                }

                Surface(
                    color = tagColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563),
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Programmatically applies the high-resolution Mukomuko disaster wallpaper
 * as the device's home screen & lock screen wallpaper using Android WallpaperManager.
 */
private suspend fun applyWallpaperToDevice(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_mukomuko_disaster_wallpaper
            )
            if (bitmap != null) {
                wallpaperManager.setBitmap(bitmap)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Berhasil! Wallpaper Alam & Mitigasi Bencana Mukomuko telah dipasang di layar ponsel Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat gambar wallpaper.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Info: ${e.localizedMessage ?: "Tidak dapat memasang wallpaper otomatis"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * Shares the disaster mitigation guide summary to other apps (WhatsApp, Telegram, etc.)
 */
private fun shareDisasterInfo(context: Context) {
    try {
        val shareText = """
            *PANDUAN ALAM & MITIGASI BENCANA KABUPATEN MUKOMUKO*
            _PUSDALOPS-PB BPBD Kabupaten Mukomuko_
            
            📍 *Kondisi Wilayah & Kearifan Budaya*: Semboyan 'Kapuang Sakti Ratau Batuah', Rumah Adat panggung tahan gempa, garis pantai 180 km Samudra Hindia, dan kawasan penyangga TNKS.
            
            ⚠️ *Potensi Bencana & Mitigasi*:
            1. Zona Megathrust Mentawai - Bengkulu
            2. Potensi Gelombang Tsunami Pesisir (waktu evakuasi < 20 menit)
            3. Genangan Luapan Debit Sungai
            4. Kebakaran Hutan & Lahan (Karhutla) Gambut
            
            🛡️ *Rumus 20-20-20 Evakuasi Mandiri*:
            Jika gempa dirasakan 20 detik atau lebih, segera evakuasi dalam tempo 20 menit menuju tempat berketinggian minimal 20 meter / Titik Kumpul TES.
            
            ☎️ *Call Center BPBD Mukomuko*: 117 (Bebas Pulsa / 24 Jam)
            _SIPAGAR KITA BPBD Mukomuko_
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Panduan Mitigasi Bencana Mukomuko")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan panduan.", Toast.LENGTH_SHORT).show()
    }
}
