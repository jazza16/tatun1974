package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.DocumentSettings
import com.example.util.ReportExporter

/**
 * Komponen Jetpack Compose **Kop Surat Standar Resmi Aplikasi SIPAGAR KITA**
 * (seragam dengan Kop Surat Tugas Relawan & `ReportExporter.drawKopSuratResmi`):
 * - Logo Kiri  : Lambang Kabupaten Mukomuko (transparan tanpa background)
 * - Teks Tengah: 4 baris seragam hitam resmi (Serif Bold instansi + Sans-Serif alamat)
 * - Logo Kanan : Logo BPBD Kabupaten Mukomuko (transparan tanpa background)
 * - Garis Bawah: Garis ganda hitam resmi (tebal 2.dp + tipis 0.8.dp)
 */
@Composable
fun StandardKopSuratHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    documentSettings: DocumentSettings? = null
) {
    val context = LocalContext.current
    val settings = remember(documentSettings) {
        (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(context)).clamped()
    }
    val leftLogoBitmap = remember(context) {
        try {
            ReportExporter.getCleanTransparentLogo(context, isMukomukoShield = true)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val rightLogoBitmap = remember(context) {
        try {
            ReportExporter.getCleanTransparentLogo(context, isMukomukoShield = false)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    val logoHeight = if (compact) 36.dp else 44.dp
    val line1Size = if (compact) 8.5.sp else 10.sp
    val line2Size = if (compact) 9.2.sp else 10.8.sp
    val line3Size = if (compact) 6.8.sp else 7.8.sp
    val addressSize = if (compact) 6.4.sp else 7.2.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("standard_kop_surat_header"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo Kiri: Kabupaten Mukomuko (Perisai proporsional sesuai Laporan Harian Pusdalops)
            if (leftLogoBitmap != null) {
                Image(
                    bitmap = leftLogoBitmap,
                    contentDescription = "Lambang Kabupaten Mukomuko",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_mukomuko_logo),
                    contentDescription = "Lambang Kabupaten Mukomuko",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )
            }

            // Teks Kop Surat Resmi (4 Baris Seragam Hitam)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = settings.kopHeaderLine1,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = line1Size,
                    lineHeight = line1Size * 1.2f,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = settings.kopHeaderLine2,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = line2Size,
                    lineHeight = line2Size * 1.2f,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = settings.kopHeaderLine3,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = line3Size,
                    lineHeight = line3Size * 1.25f,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = settings.kopAddressLine,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = addressSize,
                    lineHeight = addressSize * 1.25f,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Logo Kanan: BPBD Kabupaten Mukomuko (Lingkaran proporsional sejajar tinggi)
            if (rightLogoBitmap != null) {
                Image(
                    bitmap = rightLogoBitmap,
                    contentDescription = "Logo BPBD Kabupaten Mukomuko",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_bpbd_mukomuko_circle),
                    contentDescription = "Logo BPBD Kabupaten Mukomuko",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        // Garis ganda hitam resmi Kop Surat
        HorizontalDivider(thickness = 2.dp, color = Color.Black)
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(thickness = 0.8.dp, color = Color.Black)
    }
}
