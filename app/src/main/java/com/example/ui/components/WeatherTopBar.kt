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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTopBar(
    district: District,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    onDistrictClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onRefreshClick: () -> Unit,
    onCallCenter117Click: () -> Unit
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

    Surface(
        color = OrangePrimary,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary,
                    titleContentColor = OrangeOnPrimary,
                    actionIconContentColor = OrangeOnPrimary
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
                                .border(1.5.dp, Color.White, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = district.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeOnPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Pilih Kecamatan",
                                    tint = OrangeOnPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "BPBD Kab. Mukomuko",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = OrangeOnPrimary.copy(alpha = 0.9f)
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
                            .padding(end = 4.dp)
                            .testTag("btn_topbar_call_center_117"),
                        color = Color(0xFFDC2626)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Call Center 117",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "117",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(36.dp).testTag("btn_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Hapus Favorit" else "Tambah Favorit",
                            tint = if (isFavorite) WeatherSunGold else OrangeOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(36.dp).testTag("btn_refresh_weather")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Segarkan Cuaca",
                            tint = OrangeOnPrimary,
                            modifier = Modifier.rotate(rotation).size(20.dp)
                        )
                    }
                }
            )
        }
    }
}
