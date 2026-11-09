package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.OrangePrimary

/**
 * Kartu Edukasi & Identitas Resmi SIPAGAR KITA.
 * Menampilkan logo resmi Kabupaten Mukomuko dan menjabarkan makna akronim
 * SIMAK, PAGAR, dan KITA menggunakan tipografi modern dan palet oranye khas aplikasi.
 */
@Composable
fun SipagarKitaAcronymCard(
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
    onDismissCard: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sipagar_kita_acronym_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1F1712) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) OrangePrimary.copy(alpha = 0.35f) else Color(0xFFFED7AA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Logo Kabupaten Mukomuko & BPBD + Nama Aplikasi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo BPBD
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFF97316), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bpbd_mukomuko_official),
                        contentDescription = "Logo BPBD Mukomuko",
                        modifier = Modifier
                            .size(38.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SIPAGAR KITA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OrangePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "KAB. MUKOMUKO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Sistem Informasi & Mitigasi Kebencanaan Terpadu",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFFED7AA) else Color(0xFF7C2D12)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    tint = OrangePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Educational Tagline Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF2C1B12) else Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF43281C) else Color(0xFFFFEDD5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mengenal Identitas & Filosofi Penamaan SIPAGAR KITA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
                            )
                        }
                    }

                    // 1. SIMAK Element
                    AcronymItemCard(
                        acronym = "SIMAK",
                        fullName = "Sistem Informasi Maritim, Atmosfer & Kebencanaan Mukomuko",
                        meaning = "Kata \"Simak\" mengajak masyarakat untuk selalu memperhatikan/memantau informasi cuaca dan kebencanaan.",
                        icon = Icons.Default.Surfing,
                        badgeColor = Color(0xFFEA580C),
                        isDark = isDark
                    )

                    // 2. PAGAR Element
                    AcronymItemCard(
                        acronym = "PAGAR",
                        fullName = "Peringatan Awal, Gelombang, Air & Resiko Mukomuko",
                        meaning = "Kata \"Pagar\" bermakna pelindung/benteng masyarakat Mukomuko dari ancaman bencana.",
                        icon = Icons.Default.Shield,
                        badgeColor = Color(0xFFD97706),
                        isDark = isDark
                    )

                    // 3. KITA Element
                    AcronymItemCard(
                        acronym = "KITA",
                        fullName = "Kesiapsiagaan, Iklim, dan Tanggap Bencana Mukomuko",
                        meaning = "Memberikan kesan kebersamaan (KITA) dalam menjaga keselamatan daerah.",
                        icon = Icons.Default.Diversity3,
                        badgeColor = Color(0xFFC2410C),
                        isDark = isDark
                    )

                    // Optional dismiss button to auto-hide and move card to info menu
                    if (onDismissCard != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onDismissCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_dismiss_sipagar_kita"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) OrangePrimary.copy(alpha = 0.5f) else Color(0xFFFDBA74))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sudah Paham (Sembunyikan & Simpan di Info)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcronymItemCard(
    acronym: String,
    fullName: String,
    meaning: String,
    icon: ImageVector,
    badgeColor: Color,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF261A13) else Color(0xFFFFFBF7),
        border = BorderStroke(
            1.dp,
            if (isDark) badgeColor.copy(alpha = 0.35f) else Color(0xFFFFEDD5)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = if (isDark) 0.25f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Akronim Chip + Judul Panjang
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = acronym,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fullName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF431407),
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Makna / Filosofi
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1D140F) else Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "Makna:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = meaning.removePrefix("Makna: ").trim(),
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF573E32),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
