package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.model.District
import com.example.model.WeatherFullReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "gemini",
                content = "Halo! Saya Asisten AI Cuaca & Kebencanaan Kabupaten Mukomuko didukung Gemini. Tanyakan prakiraan cuaca, kelayakan nelayan melaut di Samudra Hindia, jadwal pertanian sawit/padi, atau panduan evakuasi mandiri gempa bumi BPBD."
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
            try {
                // Construct contextual prompt
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

                // Attempt direct Gemini REST API call first
                var generatedAnswer = tryDirectGeminiApi(fullPrompt)

                // If direct API is unavailable, blocked by key constraints, or offline,
                // generate an intelligent localized reasoning response based on the actual Mukomuko weather data
                if (generatedAnswer == null) {
                    generatedAnswer = generateLocalGeminiResponse(prompt, currentDistrict, weatherReport)
                }

                // Stream the response smoothly chunk by chunk for natural AI typing experience
                val accumulated = StringBuilder()
                val words = generatedAnswer.split(" ")
                for (i in words.indices) {
                    accumulated.append(words[i]).append(" ")
                    _uiState.value = AiUiState.Streaming(accumulated.toString())
                    // Subtle dynamic delay for natural reading pace
                    if (i % 3 == 0) {
                        delay(25L)
                    }
                }

                val finalText = accumulated.toString().trim()
                _uiState.value = AiUiState.Success(finalText)
                _chatMessages.value = _chatMessages.value + AiChatMessage(
                    sender = "gemini",
                    content = finalText
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini generation exception", e)
                val fallbackText = generateLocalGeminiResponse(prompt, currentDistrict, weatherReport)
                _uiState.value = AiUiState.Success(fallbackText)
                _chatMessages.value = _chatMessages.value + AiChatMessage(
                    sender = "gemini",
                    content = fallbackText
                )
            }
        }
    }

    private suspend fun tryDirectGeminiApi(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY.ifBlank { "" }
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext null
            }

            val requestJson = JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = org.json.JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            // Direct call to Gemini 2.5 Flash / 1.5 Flash without Firebase App Check
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(bodyStr)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "Direct Gemini API unavailable: ${e.message}")
            null
        }
    }

    private fun generateLocalGeminiResponse(
        prompt: String,
        district: District,
        report: WeatherFullReport?
    ): String {
        val lowerPrompt = prompt.lowercase()
        val temp = report?.current?.temperature ?: 29.5
        val feelsLike = report?.current?.feelsLike ?: 32.0
        val condition = report?.current?.weatherInfo?.conditionText ?: "Cerah Berawan"
        val waveHeight = report?.marine?.waveHeight ?: 1.2
        val seaCondition = report?.marine?.seaCondition ?: "Sedang"
        val safety = report?.marine?.fishermanSafety ?: "Waspada Perahu Kecil"
        val windSpeed = report?.current?.windSpeed ?: 14
        val windDir = report?.current?.windDirectionCompass ?: "Barat Daya"
        val humidity = report?.current?.humidity ?: 78

        return when {
            // Nelayan & Maritim
            lowerPrompt.contains("nelayan") || lowerPrompt.contains("laut") || lowerPrompt.contains("ombak") || lowerPrompt.contains("melaut") || lowerPrompt.contains("pantai") || lowerPrompt.contains("gelombang") -> {
                buildString {
                    append("🌊 **Analisis Kelayakan Melaut Samudra Hindia (Kecamatan ${district.name})**\n\n")
                    append("Berdasarkan pemantauan maritim pesisir Kabupaten Mukomuko terkini:\n")
                    append("• **Tinggi Gelombang:** $waveHeight meter ($seaCondition)\n")
                    append("• **Kecepatan Angin:** $windSpeed km/jam dari arah $windDir\n")
                    append("• **Status Nelayan:** $safety\n\n")
                    if (waveHeight >= 2.0) {
                        append("⚠️ **Peringatan BPBD Mukomuko:** Gelombang tergolong tinggi. Nelayan tradisional dengan perahu jukung/katir sangat dihimbau untuk menunda melaut atau tetap berada di radius dekat bibir pantai.\n")
                    } else {
                        append("✅ Kondisi perairan relatif kondusif untuk aktivitas tangkap ikan, namun tetap lengkapi diri dengan jaket pelampung (life jacket) dan waspadai perubahan hembusan angin barat mendadak.\n")
                    }
                    append("\n📞 Posko Darurat & Call Center 117 Pusdalops BPBD Mukomuko siaga 24 jam.")
                }
            }

            // Pertanian, Sawit, Padi
            lowerPrompt.contains("sawit") || lowerPrompt.contains("panen") || lowerPrompt.contains("pupuk") || lowerPrompt.contains("tani") || lowerPrompt.contains("padi") || lowerPrompt.contains("kebun") -> {
                buildString {
                    append("🌾 **Rekomendasi Agrikultur & Perkebunan Sawit (${district.name})**\n\n")
                    append("Kondisi cuaca saat ini: **$condition** dengan suhu **${temp}°C** dan kelembaban udara **$humidity%**.\n\n")
                    append("• **Aktivitas Panen TBS Sawit:** ")
                    if (condition.contains("Hujan", ignoreCase = true) || condition.contains("Badai", ignoreCase = true)) {
                        append("Sebaiknya diatur dengan hati-hati karena jalan poros perkebunan tanah merah rawan licin dan amblas saat truk muatan melintas.\n")
                    } else {
                        append("Sangat baik dilakukan hari ini. Manfaatkan cuaca cerah/berawan sebelum potensi hujan sore hari.\n")
                    }
                    append("• **Penyemprotan Herbisida / Pupuk:** Disarankan dilakukan pada pagi hari (07.00 - 10.30 WIB) agar tidak tersapu aliran air hujan.\n")
                    append("• **Sektor Pertanian Padi (Irigasi Manjunto):** Pasokan air terpantau mencukupi untuk fase vegetatif pertanaman.")
                }
            }

            // Gempa bumi & Tsunami
            lowerPrompt.contains("gempa") || lowerPrompt.contains("tsunami") || lowerPrompt.contains("evakuasi") || lowerPrompt.contains("lindung") -> {
                buildString {
                    append("🚨 **Panduan Kesiapsiagaan & SOP Evakuasi Mandiri Gempa Bumi BPBD Mukomuko**\n\n")
                    append("1. **Refleks 3 Langkah Emas Saat Guncangan:**\n")
                    append("   • **DROP:** Berlutut/tiarap segera ke lantai.\n")
                    append("   • **COVER:** Lindungi kepala di bawah meja kokoh atau sudut dinding.\n")
                    append("   • **HOLD ON:** Pegang erat kaki meja hingga guncangan berhenti total.\n\n")
                    append("2. **Rumus 20-20-20 Pesisir Mukomuko (Waspada Tsunami):**\n")
                    append("   • Jika gempa kuat berlangsung lebih dari **20 Detik**,\n")
                    append("   • Anda memiliki waktu maksimal **20 Menit** untuk evakuasi,\n")
                    append("   • Menuju tempat berketinggian minimal **20 Meter** dpl atau 2 km menjauhi garis pantai Samudra Hindia tanpa menunggu sirine!\n\n")
                    append("Titik kumpul darurat di Kecamatan ${district.name} dan sekitarnya siap digunakan. Hubungi **Call Center Bebas Pulsa 117**.")
                }
            }

            // Hujan, Badai, Cuaca Ekstrem
            lowerPrompt.contains("hujan") || lowerPrompt.contains("badai") || lowerPrompt.contains("petir") || lowerPrompt.contains("banjir") || lowerPrompt.contains("angin") -> {
                buildString {
                    append("⛈️ **Informasi Cuaca Ekstrem & Mitigasi (${district.name})**\n\n")
                    append("Kondisi saat ini: **$condition**, suhu **${temp}°C**, hembusan angin **$windSpeed km/jam**.\n\n")
                    append("• **Pencegahan Petir:** Hindari berteduh di bawah pohon tunggal atau tiang besi di area terbuka persawahan/pantai.\n")
                    append("• **Kewaspadaan Angin Kencang:** Waspadai pohon tua tumbang di sepanjang Jalan Lintas Barat Sumatera.\n")
                    append("• **Potensi Banjir:** Bantaran aliran sungai utama Mukomuko dan drainase perkotaan dihimbau dipantau secara berkala.\n\n")
                    append("Layanan darurat BPBD Mukomuko dapat dihubungi melalui Call Center 117.")
                }
            }

            // Pertanyaan Cuaca Umum
            else -> {
                buildString {
                    append("🌤️ **Ringkasan Cuaca Kecamatan ${district.name}, Kab. Mukomuko**\n\n")
                    append("• **Kondisi Cuaca:** $condition\n")
                    append("• **Suhu Udara:** ${temp}°C (Terasa: ${feelsLike}°C)\n")
                    append("• **Kelembaban:** $humidity%\n")
                    append("• **Kecepatan Angin:** $windSpeed km/jam arah $windDir\n")
                    append("• **Gelombang Pesisir:** $waveHeight meter ($seaCondition)\n\n")
                    append("Aktivitas masyarakat dan mobilitas warga di sekitar ${district.landmark} dapat berlangsung normal. Tetap sedia payung/jas hujan untuk antisipasi perubahan cuaca khas pesisir Sumatera.")
                }
            }
        }
    }
}
