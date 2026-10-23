package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.District
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherSunGold

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTopBar(
    district: District,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    isDarkMode: Boolean,
    isUpdateAvailable: Boolean = false,
    isDetectingLocation: Boolean = false,
    onDistrictClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onRefreshClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onOpenAppMenu: () -> Unit,
    onCallCenter117Click: () -> Unit,
    onDetectLocationClick: () -> Unit = {},
    onWallpaperClick: () -> Unit = {}
) {
    val rotation by if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "refreshRotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val barContainerColor = if (isDarkMode) Color(0xFF1E1712) else OrangePrimary
    val barContentColor = if (isDarkMode) Color(0xFFFFEDD5) else OrangeOnPrimary

    Surface(
        color = barContainerColor,
        shadowElevation = if (isDarkMode) 2.dp else 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barContainerColor,
                    titleContentColor = barContentColor,
                    actionIconContentColor = barContentColor
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDistrictClick)
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("district_selector_header")
                    ) {
                        // Official BPBD Mukomuko Emblem
                        Image(
                            painter = painterResource(id = R.drawable.ic_bpbd_logo),
                            contentDescription = "Logo BPBD Mukomuko",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isDarkMode) OrangePrimary else Color.White, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = district.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barContentColor
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Pilih Kecamatan",
                                    tint = barContentColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Pusdalops-PB BPBD Mukomuko",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = barContentColor.copy(alpha = 0.95f)
                            )
                        }
                    }
                },
                actions = {
                    // Call Center 117 Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCallCenter117Click)
                            .padding(end = 2.dp)
                            .testTag("btn_topbar_call_center_117"),
                        color = Color(0xFFDC2626)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Call Center 117",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "117",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Wallpaper & Infografis Bencana Button
                    IconButton(
                        onClick = onWallpaperClick,
                        modifier = Modifier.size(34.dp).testTag("btn_topbar_wallpaper")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Landscape,
                            contentDescription = "Wallpaper Alam & Bencana",
                            tint = barContentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // UI Toggle Light / Dark Mode Button
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier.size(34.dp).testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Ganti ke Mode Terang" else "Ganti ke Mode Gelap",
                            tint = if (isDarkMode) WeatherSunGold else barContentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // App Menu & Online OTA Update Button
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = onOpenAppMenu,
                            modifier = Modifier.size(34.dp).testTag("app_menu_button")
                        ) {
                            Icon(
                                imageVector = if (isUpdateAvailable) Icons.Default.NewReleases else Icons.Default.Info,
                                contentDescription = "Pusat Pembaruan Aplikasi",
                                tint = if (isUpdateAvailable) Color(0xFFFDE047) else barContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isUpdateAvailable) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(8.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        }
                    }

                    // Favorite District Toggle Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(34.dp).testTag("btn_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Hapus Favorit" else "Tambah Favorit",
                            tint = if (isFavorite) WeatherSunGold else barContentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(34.dp).testTag("btn_refresh_weather")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Segarkan Cuaca",
                            tint = barContentColor,
                            modifier = Modifier.rotate(rotation).size(19.dp)
                        )
                    }
                }
            )

            // High contrast bottom accent line for dark mode
            if (isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(OrangePrimary.copy(alpha = 0.5f))
                )
            }
        }
    }
}
