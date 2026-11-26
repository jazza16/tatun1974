package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AirQuality
import com.example.model.BmkgEarthquake
import com.example.model.CurrentWeather
import com.example.model.DailyItem
import com.example.model.DetailedEarlyWarning
import com.example.model.EarthquakeUrgency
import com.example.model.HourlyItem
import com.example.model.MarineWeather
import com.example.model.MukomukoDistricts
import com.example.model.WeatherFullReport
import com.example.ui.components.EarlyWarningSection
import com.example.ui.components.HeroWeatherCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.geminiGradientBorder
import com.example.util.WeatherAlertSettings
import java.util.Locale

@Composable
internal fun QuickEvacuationPreparednessBanner(
    isDark: Boolean,
    onOpenGuide: () -> Unit
) {
    val containerBg = MaterialTheme.colorScheme.surface
    val titleCol = if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    val descCol = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
    val borderStroke = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGuide)
            .testTag("quick_evacuation_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFDC2626), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kesiapsiagaan Evakuasi Gempa",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleCol,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = if (isDark) 0.3f else 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SOP BPBD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFECACA) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "Refleks Drop-Cover-Hold On & Rumus 20-20-20 Tsunami Pesisir",
                    fontSize = 11.sp,
                    color = descCol,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Panduan >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun QuickGeminiAdvisorBanner(
    isDark: Boolean,
    onAskAi: () -> Unit
) {
    val containerBg = MaterialTheme.colorScheme.surface
    val titleCol = if (isDark) Color(0xFFF3E8FF) else Color(0xFF581C87)
    val descCol = if (isDark) Color(0xFFE9D5FF) else Color(0xFF7E22CE)
    val borderStroke = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAskAi)
            .testTag("quick_gemini_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF9333EA), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tanya AI Gemini",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleCol,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF9333EA).copy(alpha = if (isDark) 0.3f else 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "AI Advisor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF9333EA),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "Konsultasi kelayakan melaut nelayan & panen sawit",
                    fontSize = 11.sp,
                    color = descCol,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Tanya >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
            )
        }
    }
}

@Composable
internal fun QuickEmergency117Strip(
    isDark: Boolean,
    onCall117: () -> Unit,
    onViewDetails: () -> Unit
) {
    val stripBg = if (isDark) Color(0xFF331414) else Color(0xFFFEF2F2)
    val stripBorder = if (isDark) Color(0xFFF87171) else Color(0xFFFCA5A5)
    val titleCol = if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    val subtitleCol = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)

    Surface(
        color = stripBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, stripBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_emergency_117_strip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewDetails)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDC2626), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "117",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Call Center 117",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleCol,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Bebas Pulsa 24 Jam",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.6f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Pusdalops-PB BPBD Mukomuko • Tanggap Darurat Bencana",
                        fontSize = 11.sp,
                        color = subtitleCol,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCall117,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "117",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
internal fun ActiveAlertCard(
    alert: com.example.model.WeatherAlert,
    onViewDetail: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentRed = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
    val titleRed = if (isDarkTheme) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    val bodyRed = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF7F1D1D)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .geminiGradientBorder(borderWidth = 2.dp, shape = cardShape)
            .clickable(onClick = onViewDetail)
            .testTag("card_active_alert"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Peringatan",
                tint = accentRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleRed
                )
                Text(
                    text = alert.message,
                    fontSize = 11.sp,
                    color = bodyRed,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lihat >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentRed,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun SunAndAtmosphereCard(
    daily: DailyItem?,
    current: CurrentWeather? = null
) {
    val sunrise = daily?.sunrise ?: "06:05"
    val sunset = daily?.sunset ?: "18:15"
    val uvIndex = daily?.uvIndexMax ?: 6.5
    val pressure = current?.surfacePressure ?: 1012.0
    val humidity = current?.humidity ?: 78

    val uvCategory = when {
        uvIndex >= 11.0 -> "Ekstrem"
        uvIndex >= 8.0 -> "Sgt Tinggi"
        uvIndex >= 6.0 -> "Tinggi"
        uvIndex >= 3.0 -> "Sedang"
        else -> "Rendah"
    }

    val uvBadgeColor = when {
        uvIndex >= 8.0 -> Color(0xFFDC2626)
        uvIndex >= 6.0 -> Color(0xFFEA580C)
        uvIndex >= 3.0 -> Color(0xFFD97706)
        else -> Color(0xFF16A34A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sun_astro_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Matahari & Atmosfer",
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Matahari & Atmosfer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Radiasi UV & Siklus Surya Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Surface(
                    color = OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "BMKG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(OrangeSecondary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Terbit (WIB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = sunrise,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7C2D12)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(OrangePrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Terbenam",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = sunset,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Indeks UV Maks",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(uvBadgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", uvIndex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "($uvCategory)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = uvBadgeColor,
                            modifier = Modifier.weight(0.6f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column {
                    Text(
                        text = "Tekanan Udara",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${pressure.toInt()} hPa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column {
                    Text(
                        text = "Kelembaban",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$humidity%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WrsLiveEarthquakeQuickCard(
    earthquake: BmkgEarthquake,
    onClick: () -> Unit
) {
    val isTsunami = earthquake.potentialTsunami.contains("berpotensi tsunami", ignoreCase = true) &&
        !earthquake.potentialTsunami.contains("tidak", ignoreCase = true)
    val cardShape = RoundedCornerShape(18.dp)
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .geminiGradientBorder(borderWidth = 2.dp, shape = cardShape)
            .clickable(onClick = onClick)
            .testTag("live_earthquake_quick_banner"),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isTsunami) Color(0xFFDC2626) else Color(0xFFEA580C), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WRS-NTWC BMKG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) {
                                    if (isTsunami) Color(0xFFFCA5A5) else Color(0xFFFDBA74)
                                } else {
                                    if (isTsunami) Color(0xFF991B1B) else Color(0xFF9A3412)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isTsunami) Color(0xFFDC2626) else Color(0xFF16A34A),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "${earthquake.date} • ${earthquake.timeWib}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (isTsunami) Color(0xFFDC2626).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isTsunami) "⚠️ POTENSI TSUNAMI" else "✓ AMAN TSUNAMI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) {
                            if (isTsunami) Color(0xFFF87171) else Color(0xFF4ADE80)
                        } else {
                            if (isTsunami) Color(0xFFDC2626) else Color(0xFF16A34A)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "M ${String.format(Locale.US, "%.1f", earthquake.magnitude)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = com.example.ui.components.formatEarthquakeRegion(earthquake.region),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kedalaman: ${earthquake.depth} • ~${earthquake.distanceKmToMukomuko} km dari Mukomuko",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Peta WRS >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }
        }
    }
}

private fun createPreviewReport(): WeatherFullReport {
    val district = MukomukoDistricts.defaultDistrict
    return WeatherFullReport(
        district = district,
        current = CurrentWeather(
            temperature = 31.0,
            feelsLike = 34.5,
            humidity = 78,
            precipitation = 2.4,
            weatherCode = 61,
            surfacePressure = 1010.5,
            windSpeed = 14.5,
            windDirection = 210,
            time = "2026-11-08T14:00"
        ),
        hourly = listOf(
            HourlyItem("2026-11-08T12:00", "12:00", 32.0, 1, 10, 70, 12.0),
            HourlyItem("2026-11-08T13:00", "13:00", 31.5, 2, 20, 72, 13.0),
            HourlyItem("2026-11-08T14:00", "14:00", 31.0, 61, 65, 78, 14.5),
            HourlyItem("2026-11-08T15:00", "15:00", 29.0, 63, 80, 85, 16.0),
            HourlyItem("2026-11-08T16:00", "16:00", 28.0, 80, 90, 88, 18.0)
        ),
        daily = listOf(
            DailyItem("2026-11-08", "Hari Ini", 61, 32.0, 24.0, 75, 12.5, 7.5, 14.0, "05:58", "18:12"),
            DailyItem("2026-11-09", "Senin", 63, 31.0, 23.0, 85, 24.0, 6.0, 16.0, "05:58", "18:12"),
            DailyItem("2026-11-10", "Selasa", 2, 33.0, 24.0, 30, 1.2, 8.5, 12.0, "05:58", "18:12")
        ),
        marine = MarineWeather(
            waveHeight = 1.75,
            seaCondition = "Sedang (1.25 - 2.50 m)",
            windSpeedKnots = 12.0,
            tideStatus = "Pasang Tinggi",
            fishermanSafety = "Waspada Perahu Kecil",
            fishermanAdvice = "Waspada potensi gelombang sedang di perairan Mukomuko",
            waterTemp = 29.0
        ),
        airQuality = AirQuality(
            aqi = 42,
            pm25 = 18.4,
            pm10 = 28.0,
            status = "Baik",
            recommendation = "Kualitas udara sangat baik untuk beraktivitas di luar."
        ),
        alert = null
    )
}

private fun createPreviewEarthquake(): BmkgEarthquake {
    return BmkgEarthquake(
        id = "gempa_preview_1",
        date = "08 Nov 2026",
        timeWib = "14:22:10 WIB",
        dateTimeIso = "2026-11-08T14:22:10+07:00",
        latitude = -2.85,
        longitude = 100.95,
        magnitude = 5.2,
        depth = "18 km",
        region = "62 km BaratDaya MUKOMUKO-BENGKULU",
        potentialTsunami = "Tidak berpotensi tsunami",
        feltDescription = "Dirasakan (MMI): III Mukomuko, II-III Kerinci",
        shakemapUrl = null,
        distanceKmToMukomuko = 62,
        isNearMukomuko = true,
        urgencyLevel = EarthquakeUrgency.WARNING
    )
}

private fun createPreviewEarlyWarnings(): List<DetailedEarlyWarning> {
    return listOf(
        DetailedEarlyWarning(
            id = "warning_1",
            title = "Peringatan Dini Cuaca Ekstrem Pesisir",
            severity = "SIAGA",
            category = "Hujan Lebat & Angin Kencang",
            affectedDistricts = listOf("Kota Mukomuko", "Air Dikit", "Teramang Jaya"),
            validUntil = "Hari Ini, 20:00 WIB",
            description = "Berpotensi terjadi hujan sedang hingga lebat disertai kilat/petir dan angin kencang berdurasi singkat.",
            safetyInstructions = listOf(
                "Hindari berada di bawah pohon rindang atau baliho besar",
                "Nelayan diimbau menunda melaut sementara waktu",
                "Waspada genangan air di dataran rendah pesisir"
            )
        )
    )
}

@Composable
private fun WeatherDenseComponentsPreviewContent() {
    val report = remember<WeatherFullReport> { createPreviewReport() }
    val earthquake = remember<BmkgEarthquake> { createPreviewEarthquake() }
    val warnings = remember<List<DetailedEarlyWarning>> { createPreviewEarlyWarnings() }
    val alertSettings = remember<WeatherAlertSettings> { WeatherAlertSettings() }

    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickEmergency117Strip(
                    isDark = isSystemInDarkTheme(),
                    onCall117 = {},
                    onViewDetails = {}
                )

                WrsLiveEarthquakeQuickCard(
                    earthquake = earthquake,
                    onClick = {}
                )

                EarlyWarningSection(
                    warnings = warnings,
                    currentDistrict = report.district,
                    weatherAlertSettings = alertSettings,
                    onUpdateWeatherAlertSettings = {},
                    onTestWeatherAlertNotification = {},
                    onCall117 = {},
                    onOpenCallCenterInfo = {}
                )

                HeroWeatherCard(report = report)
            }
        }
    }
}

@Preview(
    name = "Preview 1: Baseline (1.0x) - Phone (360dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 360,
    fontScale = 1.0f,
    showBackground = true
)
@Preview(
    name = "Preview 1: Baseline (1.0x) - Tablet (800dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 800,
    fontScale = 1.0f,
    showBackground = true
)
@Composable
fun WeatherAppScreenPreviewBaseline() {
    WeatherDenseComponentsPreviewContent()
}

@Preview(
    name = "Preview 2: Samsung Default (1.3x) - Phone (360dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 360,
    fontScale = 1.3f,
    showBackground = true
)
@Preview(
    name = "Preview 2: Samsung Default (1.3x) - Tablet (800dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 800,
    fontScale = 1.3f,
    showBackground = true
)
@Composable
fun WeatherAppScreenPreviewSamsung() {
    WeatherDenseComponentsPreviewContent()
}

@Preview(
    name = "Preview 3: Accessibility Max (2.0x) - Phone (360dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 360,
    fontScale = 2.0f,
    showBackground = true
)
@Preview(
    name = "Preview 3: Accessibility Max (2.0x) - Tablet (800dp)",
    group = "WeatherAppScreen Dense Previews",
    widthDp = 800,
    fontScale = 2.0f,
    showBackground = true
)
@Composable
fun WeatherAppScreenPreviewAccessibilityMax() {
    WeatherDenseComponentsPreviewContent()
}
