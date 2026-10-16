package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.District
import com.example.model.WeatherFullReport
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiWeatherViewModel"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class AiChatMessage(
    val sender: String, // "user" or "gemini"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AiWeatherViewModel : ViewModel() {
    // Lazy initialization of Gemini model to prevent startup crashes
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "gemini",
                content = "Halo! Saya Asisten AI Cuaca & Kebencanaan Kabupaten Mukomuko didukung Gemini. Tanyakan prakiraan cuaca, kondisi laut Samudra Hindia bagi nelayan, jadwal pertanian sawit/padi, atau tips mitigasi bencana BPBD."
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    fun askGemini(prompt: String, currentDistrict: District, weatherReport: WeatherFullReport?) {
        if (prompt.isBlank()) return

        val userMsg = AiChatMessage(sender = "user", content = prompt.trim())
        _chatMessages.value = _chatMessages.value + userMsg
        _uiState.value = AiUiState.Loading

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                // Build rich geographical & weather context
                val weatherContext = if (weatherReport != null) {
                    """
                    Konteks Data Cuaca Terkini Kabupaten Mukomuko:
                    - Wilayah: Kecamatan ${currentDistrict.name} (${currentDistrict.category}, Landmark: ${currentDistrict.landmark})
                    - Suhu: ${weatherReport.current.temperature}°C (Terasa: ${weatherReport.current.feelsLike}°C)
                    - Cuaca: ${weatherReport.current.weatherInfo.conditionText}
                    - Kelembaban: ${weatherReport.current.humidity}%
                    - Angin: ${weatherReport.current.windSpeed} km/j dari ${weatherReport.current.windDirectionCompass}
                    - Kondisi Maritim Samudra Hindia: Tinggi gelombang ${weatherReport.marine.waveHeight}m (${weatherReport.marine.seaCondition}), Status: ${weatherReport.marine.fishermanSafety}
                    - Peringatan Dini Aktif: ${weatherReport.alert?.title ?: "Tidak ada peringatan cuaca ekstrem"}
                    """.trimIndent()
                } else {
                    "Wilayah: Kecamatan ${currentDistrict.name}, Kabupaten Mukomuko, Bengkulu."
                }

                val fullPrompt = """
                    Anda adalah Asisten Cerdas Cuaca dan Kesiapsiagaan Bencana BPBD Kabupaten Mukomuko, Bengkulu, didukung oleh Gemini.
                    Berikan jawaban yang ramah, informatif, tepat secara geografis (Mukomuko pesisir barat Sumatera, perkebunan sawit, irigasi Manjunto, Samudra Hindia), dan memprioritaskan keselamatan warga.
                    
                    $weatherContext
                    
                    Pertanyaan Pengguna: $prompt
                """.trimIndent()

                // Mandated: realistic 30-60s timeout for model inference
                withTimeout(60_000L) {
                    model.generateContentStream(fullPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _uiState.value = AiUiState.Streaming(accumulated.toString())
                    }
                }

                val finalText = accumulated.toString()
                if (finalText.isNotBlank()) {
                    _uiState.value = AiUiState.Success(finalText)
                    _chatMessages.value = _chatMessages.value + AiChatMessage(
                        sender = "gemini",
                        content = finalText
                    )
                } else {
                    val errMsg = "Model Gemini mengembalikan respons kosong."
                    _uiState.value = AiUiState.Error(errMsg)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini generation timed out", e)
                val timeoutMsg = "Permintaan ke Gemini melebihi batas waktu (60 detik). Silakan coba lagi."
                _uiState.value = AiUiState.Error(timeoutMsg)
                _chatMessages.value = _chatMessages.value + AiChatMessage(
                    sender = "gemini",
                    content = "⚠️ $timeoutMsg"
                )
            } catch (e: CancellationException) {
                // ALWAYS re-throw CancellationException to preserve cooperative cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                val errText = e.localizedMessage ?: "Terjadi kesalahan saat memproses jawaban dengan Gemini."
                _uiState.value = AiUiState.Error(errText)
                _chatMessages.value = _chatMessages.value + AiChatMessage(
                    sender = "gemini",
                    content = "⚠️ Gagal memuat respons Gemini: $errText"
                )
            }
        }
    }
}
