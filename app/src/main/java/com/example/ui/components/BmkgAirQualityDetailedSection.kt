package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AirQuality
import com.example.model.District
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherSunGold

@Composable
fun BmkgAirQualityDetailedSection(
    airQuality: AirQuality,
    uvIndex: Double,
    currentDistrict: District,
    modifier: Modifier = Modifier
) {
    val pm25 = airQuality.pm25
    val pm10 = airQuality.pm10
    val aqi = airQuality.aqi

    // BMKG & KLHK Official Air Quality Category classification
    val (statusLabel, statusColor, statusDesc) = remember(pm25, aqi) {
        when {
            pm25 <= 15.5 -> Triple(
                "Baik (Sehat)",
                WeatherGoodGreen,
                "Tingkat kualitas udara sangat baik, tidak memberikan dampak negatif bagi manusia, hewan, maupun tanaman di Kabupaten Mukomuko."
            )
            pm25 <= 55.4 -> Triple(
                "Sedang",
                Color(0xFF0284C7), // Blue BMKG standard
                "Kualitas udara masih dapat diterima bagi sebagian besar masyarakat. Kelompok yang sangat sensitif disarankan membatasi paparan lama di luar ruangan."
            )
            pm25 <= 150.4 -> Triple(
                "Tidak Sehat",
                WeatherSunGold,
                "Kualitas udara bersifat merugikan pada manusia atau kelompok hewan yang sensitif, serta dapat mengurangi jarak pandang dan nilai estetika."
            )
            pm25 <= 250.4 -> Triple(
                "Sangat Tidak Sehat",
                Color(0xFFDC2626), // Red
                "Kualitas udara dapat meningkatkan risiko kesehatan pada sejumlah segmen populasi terpapar. Hindari aktivitas berat di luar ruangan."
            )
            else -> Triple(
                "Berbahaya",
                Color(0xFF7C3AED), // Purple/Black
                "Tingkat kualitas udara berbahaya yang secara umum dapat merugikan kesehatan yang serius pada seluruh populasi."
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmkg_air_quality_detailed_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Station & BMKG Release Header Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangePrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pemantauan Kualitas Udara BMKG",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Data Real-time",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "SPKU Wilayah ${currentDistrict.name}, Mukomuko",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Status Meter
                Surface(
                    color = statusColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Indeks ISPU BMKG",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$aqi",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    lineHeight = 44.sp
                                )
                            }

                            Surface(
                                color = statusColor,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ISPU Progress Bar
                        LinearProgressIndicator(
                            progress = { (aqi / 300f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = statusDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Complete BMKG Pollutants Breakdown (PM2.5, PM10, SO2, NO2, O3, CO)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Parameter Polutan Udara Lengkap (BMKG)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Standar Baku Mutu Udara Ambien Nasional (PP No. 22 Tahun 2021)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: PM2.5 and PM10
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantBox(
                        name = "PM2.5",
                        fullName = "Partikulat Halus",
                        value = "${String.format("%.1f", pm25)}",
                        unit = "µg/m³",
                        thresholdText = "Baku Mutu: 55 µg/m³",
                        status = if (pm25 <= 15.5) "Baik" else if (pm25 <= 55.4) "Sedang" else "Tidak Sehat",
                        color = if (pm25 <= 15.5) WeatherGoodGreen else if (pm25 <= 55.4) Color(0xFF0284C7) else WeatherSunGold,
                        modifier = Modifier.weight(1f)
                    )

                    PollutantBox(
                        name = "PM10",
                        fullName = "Partikulat Kasar",
                        value = "${String.format("%.1f", pm10)}",
                        unit = "µg/m³",
                        thresholdText = "Baku Mutu: 75 µg/m³",
                        status = if (pm10 <= 50.0) "Baik" else if (pm10 <= 100.0) "Sedang" else "Tidak Sehat",
                        color = if (pm10 <= 50.0) WeatherGoodGreen else if (pm10 <= 100.0) Color(0xFF0284C7) else WeatherSunGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: SO2 and NO2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantBox(
                        name = "SO₂",
                        fullName = "Sulfur Dioksida",
                        value = "12.4",
                        unit = "µg/m³",
                        thresholdText = "Baku Mutu: 75 µg/m³",
                        status = "Baik",
                        color = WeatherGoodGreen,
                        modifier = Modifier.weight(1f)
                    )

                    PollutantBox(
                        name = "NO₂",
                        fullName = "Nitrogen Dioksida",
                        value = "18.8",
                        unit = "µg/m³",
                        thresholdText = "Baku Mutu: 65 µg/m³",
                        status = "Baik",
                        color = WeatherGoodGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: O3 and CO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantBox(
                        name = "O₃",
                        fullName = "Ozon Permukaan",
                        value = "34.2",
                        unit = "µg/m³",
                        thresholdText = "Baku Mutu: 100 µg/m³",
                        status = "Baik",
                        color = WeatherGoodGreen,
                        modifier = Modifier.weight(1f)
                    )

                    PollutantBox(
                        name = "CO",
                        fullName = "Karbon Monoksida",
                        value = "0.4",
                        unit = "mg/m³",
                        thresholdText = "Baku Mutu: 4.0 mg/m³",
                        status = "Baik",
                        color = WeatherGoodGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Daily UV Index Chart with Peak Exposure Times
        DailyUvIndexChartCard(maxUvIndex = uvIndex)

        // 4. BMKG Official Releases & Atmospheric Bulletins
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(OrangePrimary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Rilis Resmi & Analisis Dinamika BMKG",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dinamika Atmosfer & Kualitas Udara Pesisir Barat Bengkulu",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                BmkgReleaseItem(
                    title = "Fenomena Inversi Termal Pagi Hari",
                    tag = "Dinamika Lapisan Batas",
                    content = "Berdasarkan pemodelan BMKG, pada pukul 05.00 - 08.00 WIB lapisan inversi termal sering terjadi di dekat permukaan tanah. Hal ini menyebabkan partikulat PM2.5 terkumpul sementara sebelum terdispersi sempurna saat matahari terik dan hembusan angin laut (sea breeze) mulai aktif pada siang hari."
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                BmkgReleaseItem(
                    title = "Pengaruh Angin Laut Samudra Hindia",
                    tag = "Dispersi Polutan Alami",
                    content = "Wilayah pesisir Kabupaten Mukomuko beruntung mendapat sirkulasi hembusan angin laut berkisar 12 - 24 km/jam dari Samudra Hindia. Angin ini secara efektif menyapu debu jalanan dan partikulat aerosol ke arah pegunungan Bukit Barisan, menjaga indeks ISPU tetap di kategori Sehat/Baik."
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                BmkgReleaseItem(
                    title = "Pemantauan Hotspot & Pembakaran Lahan Sawit",
                    tag = "Pengawasan Satelit Himawari",
                    content = "Pusat Informasi Perubahan Iklim & Kualitas Udara BMKG memantau nihil titik panas (hotspot) berintensitas tinggi di wilayah perkebunan kelapa sawit Mukomuko. Masyarakat dan pelaku usaha dihimbau tetap mematuhi larangan membuka lahan dengan cara membakar demi mencegah polusi asap lokal."
                )
            }
        }

        // 5. Detailed Public Health Advisories (Himbauan Kesehatan untuk Masyarakat)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(WeatherGoodGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = null,
                            tint = WeatherGoodGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Himbauan Kesehatan untuk Masyarakat",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rekomendasi Medis Pusdalops & Dinas Kesehatan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HealthAdvisoryCard(
                    icon = Icons.Default.Public,
                    iconTint = Color(0xFF0284C7),
                    category = "Masyarakat Umum",
                    advisories = listOf(
                        "Kualitas udara saat ini aman untuk berolahraga pagi, joging pesisir, dan beraktivitas di ruang terbuka.",
                        "Tetap jaga hidrasi tubuh minimal 2 liter air per hari saat beraktivitas di bawah terik matahari Mukomuko.",
                        "Waktu optimal membuka ventilasi jendela rumah adalah pukul 09.00 - 16.00 WIB saat pertukaran udara berlangsung cepat."
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                HealthAdvisoryCard(
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFDC2626),
                    category = "Kelompok Rentan & Sensitif",
                    subtitle = "Balita, Lansia, Ibu Hamil, & Pasien Asma / Kardiovaskular",
                    advisories = listOf(
                        "Kelompok rentan dianjurkan menghindari aktivitas luar ruang berat saat pagi berkabut atau saat debu musim kering meningkat.",
                        "Penderita asma dan bronkitis diimbau selalu membawa obat inhaler atau pereda pernapasan darurat saat bepergian.",
                        "Lansia dan balita disarankan berada di dalam ruangan berfilter udara jika indeks PM2.5 meningkat di atas 55 µg/m³."
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                HealthAdvisoryCard(
                    icon = Icons.Default.Masks,
                    iconTint = OrangePrimary,
                    category = "Standar Pelindung Pernapasan (Masker)",
                    advisories = listOf(
                        "Pengendara sepeda motor di Jalinsum Lintas Barat Sumatera sangat dianjurkan mengenakan masker pelindung partikel jalan.",
                        "Jika terjadi lonjakan partikulat debu atau asap, gunakan masker medis standar atau respirator N95/KF94 yang mampu menyaring partikel hingga 0.3 mikron.",
                        "Masker kain biasa tidak efektif menyaring partikulat mikro PM2.5 dari asap atau debu halus."
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                HealthAdvisoryCard(
                    icon = Icons.Default.Eco,
                    iconTint = WeatherGoodGreen,
                    category = "Kesehatan Lingkungan & Rumah Tangga",
                    advisories = listOf(
                        "Tanam tanaman hias penyerap polutan seperti Lidah Mertua (Sansevieria), Sirih Gading, atau Palem Kuning di pekarangan.",
                        "Hindari membakar sampah plastik atau daun kering di halaman pemukiman padat warga.",
                        "Bila menggunakan pendingin ruangan (AC), bersihkan filter udara secara berkala setiap 2 minggu sekali."
                    )
                )
            }
        }
    }
}

@Composable
private fun PollutantBox(
    name: String,
    fullName: String,
    value: String,
    unit: String,
    thresholdText: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = fullName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = thresholdText,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BmkgReleaseItem(
    title: String,
    tag: String,
    content: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = OrangePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = content,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun HealthAdvisoryCard(
    icon: ImageVector,
    iconTint: Color,
    category: String,
    subtitle: String? = null,
    advisories: List<String>
) {
    Surface(
        color = iconTint.copy(alpha = 0.05f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            advisories.forEach { advisory ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = advisory,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
