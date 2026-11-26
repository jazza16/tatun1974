package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Helper Text-to-Speech (TTS) Bahasa Indonesia 100% Offline untuk membacakan
 * langkah SOP Evakuasi Mandiri maupun pengumuman Sirene Peringatan Dini.
 */
class IndonesianTtsHelper(context: Context) {
    private val appContext = context.applicationContext ?: context
    private var tts: TextToSpeech? = null

    @Volatile
    private var isReady: Boolean = false

    @Volatile
    private var pendingUtterance: Pair<String, String>? = null

    private val _activeSpeakingKey = MutableStateFlow<String?>(null)
    val activeSpeakingKey: StateFlow<String?> = _activeSpeakingKey.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val indonesianLocale = Locale.forLanguageTag("id-ID")
                    val langResult = tts?.setLanguage(indonesianLocale)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        runCatching { tts?.setLanguage(Locale.getDefault()) }
                    }
                    tts?.setSpeechRate(0.95f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _activeSpeakingKey.value = utteranceId
                        }

                        override fun onDone(utteranceId: String?) {
                            if (_activeSpeakingKey.value == utteranceId) {
                                _activeSpeakingKey.value = null
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (_activeSpeakingKey.value == utteranceId) {
                                _activeSpeakingKey.value = null
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            if (_activeSpeakingKey.value == utteranceId) {
                                _activeSpeakingKey.value = null
                            }
                        }
                    })
                    isReady = true
                    pendingUtterance?.let { (key, text) ->
                        pendingUtterance = null
                        speakNow(key, text)
                    }
                } else {
                    Log.w(TAG, "TextToSpeech engine gagal diinisialisasi (status=$status)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech tidak tersedia pada perangkat ini: ${e.message}")
        }
    }

    /**
     * Memutar teks Bahasa Indonesia untuk [key]. Jika [key] sedang dibacakan, menghentikan pemutaran (toggle).
     */
    fun toggleSpeak(key: String, text: String): Boolean {
        if (_activeSpeakingKey.value == key) {
            stop()
            return false
        }
        speak(key, text)
        return true
    }

    fun speak(key: String, text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        if (!isReady) {
            pendingUtterance = key to cleanText
            _activeSpeakingKey.value = key
            return
        }
        speakNow(key, cleanText)
    }

    private fun speakNow(key: String, text: String) {
        try {
            _activeSpeakingKey.value = key
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memutar TTS: ${e.message}")
            _activeSpeakingKey.value = null
        }
    }

    fun stop() {
        pendingUtterance = null
        _activeSpeakingKey.value = null
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        isReady = false
    }

    companion object {
        private const val TAG = "IndonesianTtsHelper"
    }
}
