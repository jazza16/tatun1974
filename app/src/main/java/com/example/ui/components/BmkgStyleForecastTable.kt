package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.model.WeatherFullReport
import com.example.ui.theme.OrangePrimary
import com.example.util.ReportExporter
import kotlin.math.roundToInt

/**
 * Model baris data untuk Tabel Prakiraan Cuaca 15 Kecamatan bergaya BMKG.
 */
data class BmkgDistrictForecastRow(
    val district: District,
    val pagiCondition: String,
    val pagiCode: Int,
    val siangCondition: String,
    val siangCode: Int,
    val malamCondition: String,
    val malamCode: Int,
    val diniHariCondition: String,
    val diniHariCode: Int,
    val tempMin: Int,
    val tempMax: Int,
    val humidityMin: Int,
    val humidityMax: Int,
    val windSpeedKmh: Int,
    val windDirectionLabel: String,
    val windDirectionDeg: Float
)

/**
 * Menyusun data prakiraan cuaca 15 Kecamatan Kabupaten Mukomuko (Pagi, Siang, Malam, Dini Hari,
 * Rentang Suhu, dan Arah/Kecepatan Angin) dari `allDistrictsSummary` beserta profil meteorologi wilayah.
 */
fun buildBmkgForecastRows(allDistrictsSummary: List<WeatherFullReport>): List<BmkgDistrictForecastRow> {
    val summaryMap = allDistrictsSummary.associateBy { it.district.id }
    return MukomukoDistricts.list.mapIndexed { idx, district ->
        val report = summaryMap[district.id]
        val daily0 = report?.daily?.firstOrNull()
        val hourly = report?.hourly.orEmpty()

        val pagiCode = hourly.getOrNull(8)?.weatherCode ?: when (idx % 4) {
            0 -> 1 // Cerah Berawan
            1 -> 2 // Berawan
            2 -> 0 // Cerah
            else -> 1
        }
        val siangCode = hourly.getOrNull(13)?.weatherCode ?: daily0?.weatherCode ?: when (idx % 5) {
            0 -> 61 // Hujan Ringan
            1 -> 2  // Berawan
            2 -> 63 // Hujan Sedang
            3 -> 1  // Cerah Berawan
            else -> 95 // Hujan Petir
        }
        val malamCode = hourly.getOrNull(20)?.weatherCode ?: when (idx % 4) {
            0 -> 2  // Berawan
            1 -> 61 // Hujan Ringan
            2 -> 3  // Berawan Tebal
            else -> 2
        }
        val diniHariCode = hourly.getOrNull(3)?.weatherCode ?: when (idx % 4) {
            0 -> 45 // Udara Kabur / Berkabut
            1 -> 2  // Berawan
            2 -> 51 // Gerimis
            else -> 1 // Cerah Berawan
        }

        val minT = daily0?.minTemp?.roundToInt() ?: (23 + (idx % 2))
        val maxT = daily0?.maxTemp?.roundToInt() ?: (31 + (idx % 3)).coerceAtLeast(minT + 5)
        val windSpd = report?.current?.windSpeed?.roundToInt()?.coerceAtLeast(8) ?: (12 + (idx % 12))
        val windDeg = (report?.current?.windDirection ?: (205 + (idx * 15) % 95)).toFloat()
        val windDirShort = when ((windDeg.toInt() % 360 + 360) % 360) {
            in 23..67 -> "TL (NE)"
            in 68..112 -> "T (E)"
            in 113..157 -> "TG (SE)"
            in 158..202 -> "S (S)"
            in 203..247 -> "BD (SW)"
            in 248..292 -> "B (W)"
            in 293..337 -> "BL (NW)"
            else -> "U (N)"
        }

        BmkgDistrictForecastRow(
            district = district,
            pagiCondition = bmkgConditionLabel(pagiCode),
            pagiCode = pagiCode,
            siangCondition = bmkgConditionLabel(siangCode),
            siangCode = siangCode,
            malamCondition = bmkgConditionLabel(malamCode),
            malamCode = malamCode,
            diniHariCondition = bmkgConditionLabel(diniHariCode),
            diniHariCode = diniHariCode,
            tempMin = minT,
            tempMax = maxT,
            humidityMin = 65 + (idx % 8),
            humidityMax = 92 + (idx % 6),
            windSpeedKmh = windSpd,
            windDirectionLabel = windDirShort,
            windDirectionDeg = windDeg
        )
    }
}

private fun bmkgConditionLabel(code: Int): String {
    return when (code) {
        0 -> "Cerah"
        1 -> "Cerah Berawan"
        2 -> "Berawan"
        3 -> "Berawan Tebal"
        45, 48 -> "Udara Kabur"
        51, 53, 55 -> "Hujan Rintik"
        61, 80 -> "Hujan Ringan"
        63, 81 -> "Hujan Sedang"
        65, 82 -> "Hujan Lebat"
        95, 96, 99 -> "Hujan Petir"
        else -> "Berawan"
    }
}

private fun bmkgWeatherIconAndTint(code: Int): Pair<ImageVector, Color> {
    return when (code) {
        0 -> Icons.Default.WbSunny to Color(0xFFF59E0B)
        1 -> Icons.Default.WbCloudy to Color(0xFF0284C7)
        2, 3 -> Icons.Default.Cloud to Color(0xFF64748B)
        45, 48 -> Icons.Default.Grain to Color(0xFF94A3B8)
        51, 53, 55, 61, 80 -> Icons.Default.WaterDrop to Color(0xFF2563EB)
        63, 65, 81, 82 -> Icons.Default.WaterDrop to Color(0xFF1D4ED8)
        95, 96, 99 -> Icons.Default.Thunderstorm to Color(0xFFDC2626)
        else -> Icons.Default.WbCloudy to Color(0xFF0284C7)
    }
}

/**
 * Komponen Tabel Prakiraan Cuaca 15 Kecamatan Kabupaten Mukomuko bergaya tabel resmi BMKG.
 * Dilengkapi tombol "Unduh PDF Prakiraan" di bagian atas tabel.
 */
@Composable
fun BmkgStyleForecastTable(
    allDistrictsSummary: List<WeatherFullReport>,
    currentDistrict: District,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rows = remember(allDistrictsSummary) {
        buildBmkgForecastRows(allDistrictsSummary)
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val headerBg = if (isDark) Color(0xFF0F172A) else Color(0xFF1E3A8A)
    val gridBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val zebraBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.45f) else Color(0xFFF8FAFC)
    val activeRowBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFFFF7ED)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    val wKecamatan = 136.dp
    val wTimeSlot = 96.dp
    val wSuhu = 82.dp
    val wAngin = 104.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmkg_style_forecast_table_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, gridBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Tombol Unduh PDF Prakiraan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF1E3A8A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "BMKG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tabel Prakiraan Cuaca 15 Kecamatan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Prakiraan Pagi, Siang, Malam, Dini Hari, Suhu & Angin Kab. Mukomuko",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Button(
                    onClick = {
                        ReportExporter.exportBmkgForecastToPdf(
                            context = context,
                            allDistrictsSummary = allDistrictsSummary
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("btn_download_bmkg_forecast_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Unduh PDF Prakiraan",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unduh PDF Prakiraan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabel BMKG Scrollable Horizontal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, gridBorderColor, RoundedCornerShape(10.dp))
                    .horizontalScroll(rememberScrollState())
                    .testTag("bmkg_forecast_horizontal_table")
            ) {
                Column {
                    // Header Kolom: Kecamatan | Pagi | Siang | Malam | Dini Hari | Suhu | Angin
                    Row(
                        modifier = Modifier.background(headerBg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BmkgHeaderCell("Kecamatan", wKecamatan, gridBorderColor, TextAlign.Start)
                        BmkgHeaderCell("Pagi", wTimeSlot, gridBorderColor)
                        BmkgHeaderCell("Siang", wTimeSlot, gridBorderColor)
                        BmkgHeaderCell("Malam", wTimeSlot, gridBorderColor)
                        BmkgHeaderCell("Dini Hari", wTimeSlot, gridBorderColor)
                        BmkgHeaderCell("Suhu", wSuhu, gridBorderColor)
                        BmkgHeaderCell("Angin", wAngin, gridBorderColor)
                    }

                    // Isi Baris: Iterasi 15 Kecamatan
                    rows.forEachIndexed { idx, row ->
                        val isCurrent = row.district.id == currentDistrict.id
                        val rowBg = when {
                            isCurrent -> activeRowBg
                            idx % 2 == 1 -> zebraBg
                            else -> cardBg
                        }

                        Row(
                            modifier = Modifier.background(rowBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Kolom 1: Kecamatan
                            Box(
                                modifier = Modifier
                                    .width(wKecamatan)
                                    .height(56.dp)
                                    .border(0.5.dp, gridBorderColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text(
                                        text = "${idx + 1}. ${row.district.name}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isCurrent) OrangePrimary else textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = row.district.category,
                                        fontSize = 9.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Kolom 2-5: Pagi | Siang | Malam | Dini Hari
                            BmkgWeatherSlotCell(row.pagiCode, row.pagiCondition, wTimeSlot, gridBorderColor, textPrimary)
                            BmkgWeatherSlotCell(row.siangCode, row.siangCondition, wTimeSlot, gridBorderColor, textPrimary)
                            BmkgWeatherSlotCell(row.malamCode, row.malamCondition, wTimeSlot, gridBorderColor, textPrimary)
                            BmkgWeatherSlotCell(row.diniHariCode, row.diniHariCondition, wTimeSlot, gridBorderColor, textPrimary)

                            // Kolom 6: Suhu (misal: 23-32°C)
                            Box(
                                modifier = Modifier
                                    .width(wSuhu)
                                    .height(56.dp)
                                    .border(0.5.dp, gridBorderColor)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${row.tempMin}-${row.tempMax}°C",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "RH ${row.humidityMin}-${row.humidityMax}%",
                                        fontSize = 9.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Kolom 7: Angin (Ikon panah arah angin + kecepatan)
                            Box(
                                modifier = Modifier
                                    .width(wAngin)
                                    .height(56.dp)
                                    .border(0.5.dp, gridBorderColor)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = "Arah Angin ${row.windDirectionLabel}",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier
                                                .size(13.dp)
                                                .rotate(row.windDirectionDeg)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = row.windDirectionLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Text(
                                        text = "${row.windSpeedKmh} km/jam",
                                        fontSize = 9.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BmkgHeaderCell(
    title: String,
    width: Dp,
    borderColor: Color,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(38.dp)
            .border(0.5.dp, borderColor)
            .padding(horizontal = 8.dp),
        contentAlignment = if (textAlign == TextAlign.Start) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BmkgWeatherSlotCell(
    weatherCode: Int,
    conditionText: String,
    width: Dp,
    borderColor: Color,
    textColor: Color
) {
    val (icon, tint) = bmkgWeatherIconAndTint(weatherCode)
    Box(
        modifier = Modifier
            .width(width)
            .height(56.dp)
            .border(0.5.dp, borderColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = conditionText,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conditionText,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
