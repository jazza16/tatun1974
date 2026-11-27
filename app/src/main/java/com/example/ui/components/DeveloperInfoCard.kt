package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    isAdministratorSession: Boolean? = null
) {
    val context = LocalContext.current
    val isAdminActive = isAdministratorSession ?: remember {
        com.example.data.local.OfficerAuthManager(context).isAdministratorSessionActive()
    }
    var isPlayProtectGuideExpanded by remember { mutableStateOf(false) }
    var isPresentationExpanded by remember { mutableStateOf(true) }
    // 0 = Naskah 1: Paparan Eksekutif (7 Slide Inti & Q&A Audiens), 1 = Naskah 2: Naskah Ringkas Presentasi Pimpinan
    var selectedScriptTab by remember { mutableStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_info_column_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                                    fontSize = 11.sp,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Developer Photo (replaces TA Monogram, retains exact 50.dp CircleShape)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8C8575))
                            .border(2.dp, OrangePrimary.copy(alpha = 0.65f), CircleShape)
                            .testTag("developer_avatar_circle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_developer_tathoenz),
                            contentDescription = "Foto Pengembang $developerName",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .testTag("developer_profile_photo")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = developerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF7C2D12),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = OrangePrimary,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.wrapContentWidth(unbounded = false)
                            ) {
                                Text(
                                    text = "Lead Dev",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = developerRole,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = organization,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF573E32),
                            modifier = Modifier.fillMaxWidth()
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
                                                fontSize = 10.sp,
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
                    icon = Icons.AutoMirrored.Filled.Send,
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
                    modifier = Modifier.padding(12.dp),
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
                        text = "Aplikasi ini bukan sekadar barisan kode dan desain. Ia adalah wujud ikhtiar kecil, berangkat dari sebuah keterbatasan, yang didedikasikan untuk memastikan bahwa di saat bencana datang, tak ada satu pun warga Mukomuko yang tertinggal tanpa informasi. Karena satu nyawa yang terselamatkan, adalah segalanya. Dengan harapan teknologi ini menjadi jembatan bagi masyarakat dengan pemerintah, agar alarm peringatan dini dapat berbunyi lebih cepat daripada guncangan dan gelombang.",
                        fontSize = 11.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Materi Presentasi & Naskah Paparan Pimpinan SIPAGAR KITA — HANYA MUNCUL SAAT SESI LOGIN ADMINISTRATOR AKTIF
            if (isAdminActive) {
                Surface(
                    color = if (isDark) Color(0xFF132238) else Color(0xFFF3F7FE),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isDark) Color(0xFF2B466B) else Color(0xFFC5D8F6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("developer_presentation_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPresentationExpanded = !isPresentationExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Materi Presentasi & Naskah Paparan Pimpinan (Khusus Admin)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0D47A1)
                                    )
                                    Text(
                                        text = "Selaras: Paparan Eksekutif (7 Slide & Q&A) + Naskah Ringkas Pimpinan",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OrangePrimary
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isPresentationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isPresentationExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Pendahuluan Fundamental Bersama (Wajib di Kedua Naskah)
                                Surface(
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF7ED),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        OrangePrimary.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "PENDAHULUAN UTAMA (BERLAKU PADA KEDUA NASKAH PAPARAN)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OrangePrimary
                                        )
                                        Text(
                                            text = "1. Proyek 100% Mandiri (\"Zero\" Dukungan APBD) & Serah Terima ke Pusdalops BPBD:\n" +
                                                "Hal paling mendasar adalah proyek SIPAGAR KITA ini dibangun dengan \"Zero\" (Nol) dukungan anggaran APBD Kabupaten Mukomuko dan dikerjakan secara mandiri penuh oleh Developer, yang nantinya akan diserahkan hak pengelolaan aplikasinya kepada BPBD Kabupaten Mukomuko dalam hal ini Pusdalops-PB BPBD.\n\n" +
                                                "2. Permohonan Maaf & Penegasan Bahwa Aplikasi Bukan Pengganti Manusia (ASN):\n" +
                                                "Developer menyampaikan permohonan maaf yang tulus apabila ada pekerjaan rekan ASN yang merasa \"dirampas\" pekerjaan manualnya akibat hadirnya otomatisasi SIPAGAR KITA. Aplikasi ini sama sekali BUKAN untuk menggantikan peran, harkat, pertimbangan, maupun kewenangan Manusia (ASN) itu sendiri.\n\n" +
                                                "3. Tujuan Developer & Menjawab Keterbatasan Sarpras BPBD Mukomuko:\n" +
                                                "Bertujuan memudahkan BPBD, petugas, dan masyarakat dalam melayani serta memperoleh pelayanan kebencanaan di tengah keterbatasan Sarana dan Prasarana (Sarpras) fisik BPBD Mukomuko (tercermin dari IKD 0,29 / Rendah), dengan menjadikan smartphone warga & petugas sebagai sensor peringatan dini 24 jam.",
                                            fontSize = 10.5.sp,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Pemilih Naskah 1 (Paparan Eksekutif) vs Naskah 2 (Naskah Ringkas Presentasi Pimpinan)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (selectedScriptTab == 0) OrangePrimary else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedScriptTab == 0) OrangePrimary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedScriptTab = 0 }
                                            .testTag("tab_script_paparan_eksekutif")
                                    ) {
                                        Text(
                                            text = "1. Paparan Eksekutif (7 Slide)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedScriptTab == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                        )
                                    }

                                    Surface(
                                        color = if (selectedScriptTab == 1) OrangePrimary else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedScriptTab == 1) OrangePrimary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedScriptTab = 1 }
                                            .testTag("tab_script_naskah_ringkas_pimpinan")
                                    ) {
                                        Text(
                                            text = "2. Naskah Ringkas Pimpinan",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedScriptTab == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                if (selectedScriptTab == 0) {
                                    // NASKAH 1: PAPARAN EKSEKUTIF: SIPAGAR KITA (7 SLIDE INTI + POIN KUNCI AUDIENS)
                                    Text(
                                        text = "A. STRUKTUR SLIDE PRESENTASI (7 SLIDE INTI — SELARAS KRB 2026–2030 & PERDA RTRW 12/2025)\n\n" +
                                            "• Slide 1: Latar Belakang, Urgensi Daerah & Keterbatasan Sarpras BPBD\n" +
                                            "  - Profil Risiko Tinggi (KRB 2026–2030 & IRBI BNPB): Skor IRBI Mukomuko 164,32 (Tinggi), IKD 0,29 (Rendah), IKM 0,348 (Sedang), 205.602 jiwa terpapar di 15 kecamatan, potensi kerugian multibahaya Rp 6,74 Triliun (zona megathrust Pantai Barat Sumatera, tsunami s/d 13 m, gelombang Samudra Hindia, banjir DAS Manjunto/Selagan, & karhutla 404.993,70 Ha).\n" +
                                            "  - Tantangan & Keterbatasan Sarpras: Keterbatasan sarpras fisik EWS/pos pantau BPBD Mukomuko, informasi tersebar di banyak kanal, notifikasi gempa sering bersifat nasional, dan pelaporan lapangan masih manual.\n" +
                                            "  - Solusi Mandiri (Zero APBD): SIPAGAR KITA hadir sebagai Single Window Command & Early Warning System untuk 15 Kecamatan yang dibangun mandiri tanpa APBD dan diserahkan ke Pusdalops-PB BPBD.\n\n" +
                                            "• Slide 2: Desain UI/UX Berorientasi Keselamatan Publik & Kecepatan Komando\n" +
                                            "  - Dual-Mode Architecture & Notifikasi 24 Jam Tanpa Login: Mode Warga (Tanpa Login) menyajikan akses cuaca 15 kecamatan, peta evakuasi, kompas titik kumpul, Call 117/112, & lapor bencana; Mode Komando Pusdalops (Login Petugas) untuk verifikasi laporan, posko/logistik, relawan, & laporan harian. Notifikasi ancaman & laporan warga masuk langsung muncul di HP pengguna 24 jam tanpa harus login dengan membedakan Porsi Mode Warga dan Porsi Mode Petugas/TRC.\n" +
                                            "  - Tab Khusus \"KRB & RTRW\": Ditempatkan tepat setelah Tab Laporan Warga agar kajian risiko 2026–2030 dan Perda RTRW No. 12 Tahun 2025 menjadi rujukan langsung tanpa bercampur fungsi lain.\n" +
                                            "  - Triase Peringatan Dini 4 Warna (Hijau, Kuning, Oranye, Merah), Filter Geografis Presisi (Khusus Provinsi Bengkulu & Mukomuko dengan Durasi Aktif 1–2 Jam), serta Tangguh Sinyal Lemah (Offline-First).\n\n" +
                                            "• Slide 3: Arsitektur Keamanan Siber & Tata Kelola Komando (Zero-Trust Security)\n" +
                                            "  - 8-Role RBAC (3 Tier Otoritas: Manajer/Kalaksa Tier 1, Supervisor/Petugas Jaga/TRC/Logistik Tier 2, Relawan & Warga Tier 3).\n" +
                                            "  - Otorisasi Berlapis (Four-Eyes Principle) Sirene Darurat (wajib persetujuan 2 pejabat berwenang).\n" +
                                            "  - Isolasi Data, Kriptografi PIN (Salted SHA-256/PBKDF2 di Cloud) & Jejak Audit (Audit Trail) berbasis NIP petugas untuk memperkuat akuntabilitas kerja ASN (bukan menggantikan kewenangan ASN).\n\n" +
                                            "• Slide 4: Integrasi Multi-Sensor, Standar BNPB–BMKG & Penyelarasan KRB + RTRW\n" +
                                            "  - Seismik & Tsunami: Real-time BMKG InaTEWS (WRS-NTWC) selaras KRB Tsunami Prioritas 2 (7.523,90 Ha) & Gempa PGA Tinggi serta Perda RTRW Pasal 92 ayat (4), (6), (8) & Jalur Evakuasi TES/TEA Pasal 42.\n" +
                                            "  - Hidrometeorologi & Maritim: Cuaca 15 kecamatan, gelombang Samudra Hindia & banjir rob selaras KRB Prioritas 1 Banjir (63.820,62 Ha), Banjir Bandang (8.554,32 Ha), Gelombang Ekstrem & Abrasi (2.892,69 Ha) serta Sempadan Pantai 100 m Perda RTRW Pasal 93.\n" +
                                            "  - Karhutla & Kualitas Udara: Titik panas satelit NASA FIRMS & BMKG + AQI/PM2.5 selaras KRB Prioritas 1 Karhutla (404.993,70 Ha, AUC 0,932) & larangan Zero Burning Perda RTRW Pasal 92 ayat (5) & Pasal 83 ayat (4).\n\n" +
                                            "• Slide 5: Akuntabilitas Logistik (FEFO) & Kaji Cepat Pascabencana (DALA)\n" +
                                            "  - Transparansi Gudang Logistik BPBD (FEFO First-Expired First-Out, alarm stok kritis <10 unit, & distribusi berbasis NIK 16 digit anti-data ganda).\n" +
                                            "  - Modul Pasca Bencana (DALA) & Laporan Resmi 1-Klik (PDF Kop 2 Logo & Excel) yang meringankan beban administrasi manual ASN.\n\n" +
                                            "• Slide 6: Mode Latihan (Sandbox Simulasi) untuk Kesiapsiagaan Berkelanjutan\n" +
                                            "  - Simulasi penanganan bencana & uji sirene oleh petugas Pusdalops dan TRC tanpa mencemari data laporan nyata maupun memicu kepanikan warga.\n\n" +
                                            "• Slide 7: Dampak Strategis & Rekomendasi Kebijakan\n" +
                                            "  - Peningkatan Indeks Kapasitas Daerah (IKD) Kabupaten Mukomuko dari baseline 0,29 (Rendah) menuju Tangguh Bencana sesuai 7 Fokus IKD KRB 2026–2030 & Indikasi Program Utama BPBD (Lampiran XII Perda RTRW No. 12/2025).\n" +
                                            "  - Efisiensi anggaran daerah (dibangun dengan Zero APBD) melalui digitalisasi pelaporan, pengawasan logistik anti-kedaluwarsa, dan percepatan respons darurat.\n\n" +
                                            "B. POIN KUNCI SAAT MENJAWAB PERTANYAAN (SESUAI AUDIENS)\n" +
                                            "1. Di Hadapan Bupati Kabupaten Mukomuko (Fokus: Perlindungan Rakyat, KRB–RTRW & Zero APBD):\n" +
                                            "\"Bapak Bupati, SIPAGAR KITA dibangun secara mandiri (Zero APBD) untuk diserahkan ke Pusdalops BPBD guna memastikan Pemkab Mukomuko hadir melindungi 205.602 warga terpapar selama 24 jam di tengah keterbatasan sarpras. Saat terjadi gempa lokal, gelombang tinggi, atau laporan warga, arahan evakuasi berbasis KRB 2026–2030 dan jalur TES/TEA Perda RTRW No. 12/2025 langsung diterima warga tanpa harus login, sementara Bapak Bupati dapat menerima Laporan Harian Pusdalops dan peta dampak dalam format PDF resmi kapan pun dibutuhkan.\"\n\n" +
                                            "2. Di Hadapan DPRD Kabupaten Mukomuko (Fokus: Akuntabilitas, Kemandirian Anggaran & Kepatuhan Perda RTRW):\n" +
                                            "\"Pimpinan dan Anggota DPRD yang terhormat, aplikasi ini hadir tanpa membebani APBD (Zero APBD) namun menjawab tuntutan transparansi tata kelola kebencanaan serta mengimplementasikan Perda RTRW No. 12 Tahun 2025 (Pasal 42, 92, 93 & pengendalian KKPR Pasal 95–108) secara digital. Seluruh stok logistik BPBD dipantau dengan sistem FEFO, memiliki jejak audit berbasis NIP ASN, serta rekapitulasi DALA yang terukur untuk mendukung fungsi pengawasan DPRD.\"\n\n" +
                                            "3. Di Hadapan Pihak BNPB RI (Fokus: Kepatuhan Standar Pusdalops Nasional, InaTEWS, & KRB 2026–2030):\n" +
                                            "\"Tim BNPB yang kami hormati, SIPAGAR KITA dirancang selaras dengan pedoman Pusdalops-PB BNPB, BMKG InaTEWS, dan Dokumen KRB Mukomuko 2026–2030 (IRBI 164,32; IKD 0,29). Kami menerapkan Triase 4 Warna 24 jam, Otoritas Berlapis (Four-Eyes Principle) sirene darurat, modul Kaji Cepat & DALA, serta Mode Sandbox Simulasi untuk mendongkrak 7 Fokus Indeks Ketahanan Daerah (IKD) Kabupaten Mukomuko.\"\n\n" +
                                            "4. Di Hadapan Rekan ASN & Internal BPBD (Fokus: Sinergi Manusia & Alat Bantu Kerja):\n" +
                                            "\"Rekan-rekan ASN yang kami hormati, kami memohon maaf apabila otomatisasi aplikasi ini sempat menimbulkan kesan mengambil alih pekerjaan manual. SIPAGAR KITA sama sekali tidak menggantikan peran, jabatan, maupun kewenangan manusia (ASN), melainkan menjadi asisten digital bagi rekan-rekan di Pusdalops dan TRC untuk mengatasi keterbatasan sarpras dan mempercepat pelayanan kepada masyarakat.\"",
                                        fontSize = 10.5.sp,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                        lineHeight = 15.5.sp
                                    )
                                } else {
                                    // NASKAH 2: NASKAH RINGKAS PRESENTASI PIMPINAN: APLIKASI SIPAGAR KITA
                                    Text(
                                        text = "NASKAH RINGKAS PRESENTASI PIMPINAN: APLIKASI SIPAGAR KITA\n" +
                                            "Unit Pengampu: Pusdalops-PB BPBD Kabupaten Mukomuko • Versi Rilis: v1.2.0 (Android Native • Real-Time Cloud & Offline-Ready)\n\n" +
                                            "• Ringkasan Eksekutif (Fokus Standar Pusdalops Nasional, InaTEWS, KRB 2026–2030 & Perda RTRW No. 12/2025):\n" +
                                            "\"SIPAGAR KITA dirancang secara mandiri (Zero APBD) untuk diserahkan pengelolaannya kepada Pusdalops-PB BPBD Kabupaten Mukomuko, selaras dengan pedoman Pusdalops BNPB, BMKG InaTEWS, Dokumen KRB 2026–2030, dan Perda RTRW No. 12 Tahun 2025. Kami menerapkan Triase 4 Warna 24 jam tanpa login, Otoritas Berlapis (Four-Eyes Principle) sirene peringatan dini, Tab Khusus KRB & RTRW, modul Kaji Cepat & DALA, serta Mode Sandbox Simulasi guna menjembatani keterbatasan sarpras dan meningkatkan Indeks Ketahanan Daerah (IKD) Kabupaten Mukomuko tanpa menggantikan peran dan kewenangan ASN.\"\n\n" +
                                            "1. Pembukaan, Kemandirian Proyek & Tujuan Strategis:\n" +
                                            "\"SIPAGAR KITA adalah platform digital komando dan layanan publik kebencanaan terpadu BPBD Kabupaten Mukomuko yang dibangun secara mandiri (Zero dukungan APBD) dan diserahkan hak pengelolaannya ke Pusdalops-PB BPBD. Kami memohon maaf apabila ada pekerjaan manual rekan ASN yang terasa tergantikan oleh otomatisasi sistem ini—sebab tujuan Developer bukanlah menggantikan peran dan kewenangan Manusia (ASN), melainkan menyediakan alat bantu di tengah keterbatasan sarpras BPBD untuk memangkas waktu respons darurat (Golden Time) serta memudahkan petugas dan masyarakat dalam melayani dan memperoleh pelayanan keselamatan.\"\n\n" +
                                            "2. Sisi UI/UX (Antarmuka, Notifikasi 24 Jam Tanpa Login & Tab Khusus KRB–RTRW):\n" +
                                            "• Gerbang Akses Ganda (Dual-Entry Portal) & Notifikasi Latar Belakang 24 Jam: Memisahkan Mode Warga/Publik (tanpa login) dengan Mode Petugas Jaga Pusdalops (Ruang Kerja Komando). Notifikasi laporan warga yang masuk, potensi ancaman, dan kejadian bencana kini langsung muncul di HP pengguna 24 jam tanpa harus login, dengan membedakan Porsi Mode Warga (evakuasi & keselamatan diri) dan Porsi Mode Petugas/TRC (koordinat GPS & komando respon).\n" +
                                            "• Tab Khusus \"KRB & RTRW\" (Tepat Setelah Tab Laporan Warga): Memisahkan Dokumen KRB 2026–2030 (12 matriks bahaya, IRBI 164,32, IKD 0,29) dan Perda RTRW No. 12 Tahun 2025 (Pasal 42, 83, 92, 93, 94 & Sanksi KKPR Pasal 95–108) ke dalam tab tersendiri yang tersinkronisasi dengan seluruh modul operasional.\n" +
                                            "• Sistem Notifikasi Triase 4 Warna (Hijau, Kuning, Oranye, Merah), Filter Lokal Provinsi Bengkulu/Mukomuko & Presisi Waktu (Durasi Aktif 1–2 Jam), serta Aksesibilitas Tanggap Darurat 1-Sentuhan (Call 117/112, SOP Evakuasi 20-20-20, Kompas & Peta Offline-First 15 Kecamatan).\n\n" +
                                            "3. Sisi Keamanan & Tata Kelola Komando (Zero-Trust Security):\n" +
                                            "• Kontrol Akses Berbasis Peran (8-Role RBAC & 3 Tier Otoritas): Hak akses terbagi ketat sesuai jabatan ASN/Petugas—mulai dari Manajer Pusdalops / Kalaksa (Tier 1 - Full Access), Supervisor & Petugas Jaga / TRC / Logistik (Tier 2), hingga Relawan & Warga (Tier 3).\n" +
                                            "• Otorisasi Berlapis (Four-Eyes Principle) Sirene Darurat, Isolasi Sesi & Kriptografi PIN Petugas (Salted SHA-256/PBKDF2 di Cloud Firestore), Isolasi Mode Warga 100%, serta Jejak Audit (Immutable Audit Trail) berbasis NIP petugas & pembatas pengiriman laporan (rate-limiter).\n\n" +
                                            "4. Keunggulan Operasional, Integrasi Data Multi-Sumber & Sinkronisasi KRB–RTRW:\n" +
                                            "• Integrasi Telemetri Real-Time & Kajian Daerah: Menggabungkan data BMKG InaTEWS (Gempa & Tsunami WRS), Cuaca & Gelombang Maritim Samudra Hindia, Pasang Surut & Banjir Rob Pesisir, serta Satelit NASA FIRMS (Titik Panas Karhutla) yang diselaraskan langsung dengan peta zonasi KRB 2026–2030 (potensi kerugian multibahaya Rp 6,74 Triliun) serta ketentuan jalur evakuasi TES/TEA (Pasal 42), kawasan rawan bencana (Pasal 92), dan sempadan pantai 100 meter (Pasal 93) Perda RTRW No. 12 Tahun 2025.\n" +
                                            "• Manajemen Posko, Logistik FEFO & Kaji Cepat (DALA), Pelaporan Resmi Siap Cetak (1-Klik PDF Kop 2 Logo & Excel/CSV), serta Mode Latihan (Sandbox Simulasi).\n\n" +
                                            "5. Penutup & Rekomendasi Tindak Lanjut:\n" +
                                            "\"Dengan SIPAGAR KITA yang dibangun dari semangat kemandirian (Zero APBD) untuk diserahterimakan kepada Pusdalops-PB BPBD Kabupaten Mukomuko, daerah kita kini memiliki sistem peringatan dini, rujukan spasial KRB–RTRW, dan komando lapangan yang cepat, aman, serta akuntabel untuk memperkuat tugas ASN di tengah keterbatasan sarpras. Kami mohon arahan dan persetujuan Pimpinan untuk penetapan SOP operasional petugas jaga serta sosialisasi bertahap kepada masyarakat di 15 kecamatan.\"",
                                        fontSize = 10.5.sp,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                        lineHeight = 15.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

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
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF78350F),
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Langkah pengujian sideload:\n" +
                                        "1. Pada dialog Play Protect saat instalasi: ketuk 'Detail selengkapnya' (More details) lalu pilih 'Tetap instal' (Install anyway).\n" +
                                        "2. Untuk menonaktifkan pemindaian Play Protect selama pengujian developer di perangkat uji: Buka Google Play Store > Profil > Play Protect > Ikon Roda Gigi (Pengaturan) > Matikan 'Pindai aplikasi dengan Play Protect'.",
                                fontSize = 11.sp,
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFFFEDD5) else Color(0xFF7C2D12),
                maxLines = 1
            )
        }
    }
}
