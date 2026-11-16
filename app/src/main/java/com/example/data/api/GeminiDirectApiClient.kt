package com.example.data.api

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

object GeminiDirectApiClient {

    private const val TAG = "GeminiAiClient"

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    /**
     * Generates AI content via Firebase AI Logic (App Check attested) without client-side API keys.
     */
    suspend fun generateContent(prompt: String): Result<String> {
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val resultText = response.text?.trim().orEmpty()
            if (resultText.isNotBlank()) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("Jawaban teks kosong dari Gemini."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            Result.failure(Exception("Permintaan Gemini melebihi batas waktu 60 detik."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase AI call", e)
            Result.failure(e)
        }
    }
}
