package id.go.mukomuko.sipagarkita.feature.pesanvisual.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.ContextDataProvider
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.PlaceholderContextDataProvider
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.SituationalContext
import id.go.mukomuko.sipagarkita.feature.pesanvisual.data.ImageRepository
import id.go.mukomuko.sipagarkita.feature.pesanvisual.data.PromptLibrary
import id.go.mukomuko.sipagarkita.feature.pesanvisual.util.ContextualCaptionBuilder
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.RealContextDataProvider

data class PesanVisualUiState(
    val isLoading: Boolean = false,
    val image: Bitmap? = null,
    val disasterLabel: String? = null,
    val caption: String? = null,
    val error: String? = null
)

class PesanVisualViewModel(
    private val imageRepository: ImageRepository,
    private val contextDataProvider: ContextDataProvider = RealContextDataProvider()
) : ViewModel() {
    private val _uiState = MutableStateFlow(PesanVisualUiState())
    val uiState = _uiState.asStateFlow()

    fun generateRandom() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val entry = PromptLibrary.entries.random()
            imageRepository.generateImage(entry.prompt)
                .onSuccess { bitmap ->
                    val ctx = runCatching { contextDataProvider.fetchContext() }
                        .getOrElse { SituationalContext(fetchedAt = Instant.now()) }
                    val caption = ContextualCaptionBuilder.build(entry.disaster.label, ctx)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            image = bitmap,
                            disasterLabel = entry.disaster.label,
                            caption = caption
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.message) }
                }
        }
    }
}
