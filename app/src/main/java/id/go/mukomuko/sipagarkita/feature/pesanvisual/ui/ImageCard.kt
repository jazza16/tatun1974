// FILE: feature/pesanvisual/ui/ImageCard.kt
package id.go.mukomuko.sipagarkita.feature.pesanvisual.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import id.go.mukomuko.sipagarkita.feature.pesanvisual.config.PesanVisualConfig

@Composable
fun ImageCard(
    bitmap: Bitmap?,
    shortText: String?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Poster Kesiapsiagaan Bencana",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // OVERLAY LOGO TOP-END
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_bpbd),
                    contentDescription = "Logo BPBD Mukomuko",
                    modifier = Modifier.size(PesanVisualConfig.LOGO_SIZE_DP.dp)
                )
                if (PesanVisualConfig.SHOW_LAMBANG_MUKOMUKO) {
                    Image(
                        painter = painterResource(id = R.drawable.lambang_mukomuko),
                        contentDescription = "Lambang Kabupaten Mukomuko",
                        modifier = Modifier.size(PesanVisualConfig.LOGO_SIZE_DP.dp)
                    )
                }
            }

            // OVERLAY FOOTER BOTTOM-CENTER
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                shortText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = PesanVisualConfig.FOOTER_IMAGE_LINE1,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = PesanVisualConfig.FOOTER_IMAGE_LINE2,
                    color = Color.White,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = PesanVisualConfig.APP_SOURCE,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = PesanVisualConfig.APP_SOURCE_FONT_SP.sp,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Text(
                text = "Tekan tombol untuk membuat poster",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
