// FILE: feature/pesanvisual/ui/PesanVisualScreen.kt
package id.go.mukomuko.sipagarkita.feature.pesanvisual.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.ContextDataProvider
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.RealContextDataProvider
import id.go.mukomuko.sipagarkita.feature.pesanvisual.data.ImageRepository
import id.go.mukomuko.sipagarkita.feature.pesanvisual.util.ShareUtil
import id.go.mukomuko.sipagarkita.feature.pesanvisual.viewmodel.PesanVisualViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesanVisualScreen(
    imageRepository: ImageRepository,
    contextDataProvider: ContextDataProvider = remember { RealContextDataProvider() },
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: PesanVisualViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PesanVisualViewModel(
                    imageRepository = imageRepository,
                    contextDataProvider = contextDataProvider
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesan Visual Kesiapsiagaan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Khusus Petugas Piket BPBD Mukomuko",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ImageCard(
                bitmap = uiState.image,
                shortText = uiState.disasterLabel,
                isLoading = uiState.isLoading
            )

            uiState.disasterLabel?.let { label ->
                Text(
                    text = "Kategori: $label",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Tombol Aksi: Buat Poster & Bagikan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::generateRandom,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Buat Poster")
                }

                Button(
                    onClick = {
                        val img = uiState.image
                        val cap = uiState.caption
                        if (img != null && cap != null) {
                            val success = ShareUtil.sharePoster(
                                context,
                                img,
                                cap,
                                "Pesan Kesiapsiagaan",
                                "siaga_${System.currentTimeMillis()}.jpg"
                            )
                            if (!success) {
                                ShareUtil.shareTextOnly(context, cap, "Pesan Kesiapsiagaan")
                            }
                        }
                    },
                    enabled = uiState.image != null && uiState.caption != null && !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan")
                }
            }

            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
