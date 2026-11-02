package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen

/**
 * Dedicated Developer Information Card displaying profile, contributions,
 * contacts (email jazzakallah@gmail.com & Telegram @takajuekgue),
 * and sideload testing / Google Play Protect guidance.
 */
@Composable
fun DeveloperInfoCard(
    modifier: Modifier = Modifier,
    developerName: String = "tathoenz_arazak",
    developerRole: String = "Lead Software Engineer & System Architect",
    organization: String = "PUSDALOPS-PB BPBD Kabupaten Mukomuko",
    developerEmail: String = "jazzakallah@gmail.com",
    developerTelegram: String = "@takajuikgue",
    isDark: Boolean = false,
    onOpenWallpaper: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlayProtectGuideExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_info_column_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E140E) else Color(0xFFFFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isDark) OrangePrimary.copy(alpha = 0.4f) else Color(0xFFFED7AA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Section Category & Verified Badge directly underneath title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(OrangePrimary, Color(0xFFC2410C))
                            ),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Informasi Pengembang Aplikasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = WeatherGoodGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = WeatherGoodGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Terverifikasi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WeatherGoodGreen
                                )
                            }
                        }
                        Text(
                            text = "Profil Kreator & Arsitek Sistem",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Developer Profile Card Box
            Surface(
                color = if (isDark) Color(0xFF2B1C14) else Color(0xFFFFF7ED),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF452B1E) else Color(0xFFFFEDD5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Programmer Monogram
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFEA580C), Color(0xFF9A3412))
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (developerName.contains("_")) "TA" else developerName.take(2).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = developerName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF7C2D12)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = OrangePrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Lead Dev",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = developerRole,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412)
                        )

                        Text(
                            text = organization,
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF573E32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tech Stack & Contributions Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TechPill(icon = Icons.Default.Code, label = "Kotlin 2.0 & Compose", isDark = isDark, modifier = Modifier.weight(1f))
                TechPill(icon = Icons.Default.Security, label = "WRS-NTWC BMKG", isDark = isDark, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Official Contributors Section
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kontributor Aplikasi:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val contributors = listOf(
                        "M@mi",
                        "Afid_Reborn",
                        "Ran_NAM",
                        "Erhy",
                        "Ande_@inun",
                        "maz_Mei",
                        "Erick P",
                        "Bg-Fir",
                        "Manda_apak",
                        "Rigan",
                        "David",
                        "Noky D Luffy"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        contributors.chunked(3).forEach { rowGroup ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowGroup.forEach { name ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF334155) else Color(0xFFFFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.8.dp,
                                            if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.5.dp)
                                                    .clip(CircleShape)
                                                    .background(OrangePrimary)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = name,
                                                fontSize = 7.8.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowGroup.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Contact Pills: Email and Telegram
            Text(
                text = "Kontak Resmi Pengembang:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TechPill(
                    icon = Icons.Default.Email,
                    label = developerEmail,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$developerEmail")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Email: $developerEmail", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                TechPill(
                    icon = Icons.Default.Send,
                    label = "TG: $developerTelegram",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        try {
                            val tgUser = developerTelegram.removePrefix("@")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$tgUser"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Telegram: $developerTelegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Wallpaper HD Feature Trigger Card
            if (onOpenWallpaper != null) {
                Surface(
                    color = OrangePrimary.copy(alpha = if (isDark) 0.18f else 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenWallpaper() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(OrangePrimary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lihat Wallpaper & Infografis Bencana HD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12)
                            )
                            Text(
                                text = "Kondisi Alam & Mitigasi Bencana Kab. Mukomuko",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Buka",
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Dedication & Message Box
            Surface(
                color = if (isDark) Color(0xFF1A1F2C) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aplikasi ini didedikasikan oleh pengembang \"$developerName\" untuk kemudahan akses informasi prakiraan cuaca, kualitas udara ISPU, dan peringatan dini bencana gempa/tsunami bagi masyarakat Kabupaten Mukomuko.",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sideload Testing & Google Play Protect Guide (Expandable)
            Surface(
                color = if (isDark) Color(0xFF231C13) else Color(0xFFFEF3C7).copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF452B1E) else Color(0xFFFDE68A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isPlayProtectGuideExpanded = !isPlayProtectGuideExpanded }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Panduan Sideload & Pengujian APK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF92400E)
                            )
                        }
                        Icon(
                            imageVector = if (isPlayProtectGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isPlayProtectGuideExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Saat menguji APK sideload di perangkat Android di luar Google Play Store, Google Play Protect memunculkan dialog 'Aplikasi tidak dikenal' karena belum melalui Google Play Console.",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF78350F),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Langkah pengujian sideload:\n" +
                                        "1. Pada dialog Play Protect saat instalasi: ketuk 'Detail selengkapnya' (More details) lalu pilih 'Tetap instal' (Install anyway).\n" +
                                        "2. Untuk menonaktifkan pemindaian Play Protect selama pengujian developer di perangkat uji: Buka Google Play Store > Profil > Play Protect > Ikon Roda Gigi (Pengaturan) > Matikan 'Pindai aplikasi dengan Play Protect'.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF451A03),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() } else Modifier
        ),
        color = if (isDark) Color(0xFF261911) else Color(0xFFFFFBEB),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (isDark) Color(0xFF452B1E) else Color(0xFFFDE68A)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12),
                maxLines = 1
            )
        }
    }
}
