package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Pemutar nada darurat / sirene akustik ringan berbasis PCM [AudioTrack.MODE_STATIC].
 *
 * Menggantikan [android.media.ToneGenerator] yang sering mengalami native deadlock
 * (`E/ToneGenerator: --- Stop timed out`) atau kebocoran handle audio saat dipanggil
 * berulang kali pada siklus alarm maupun saat dilepas dari main thread.
 */
object EmergencyTonePlayer {
    private const val TAG = "EmergencyTonePlayer"
    private const val SAMPLE_RATE = 22_050

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var activeJob: Job? = null

    @Volatile
    private var activeTrack: AudioTrack? = null

    /**
     * Memutar satu pulsa nada darurat (single/dual-tone) secara non-blocking pada background thread.
     * Otomatis membatalkan dan melepas nada sebelumnya agar tidak pernah terjadi kebocoran AudioTrack.
     */
    fun playToneAsync(
        freqHz1: Double = 880.0,
        freqHz2: Double = 1100.0,
        durationMs: Int = 750,
        usage: Int = AudioAttributes.USAGE_ALARM,
        volume: Float = 0.85f
    ) {
        val job = scope.launch {
            playToneSuspend(
                freqHz1 = freqHz1,
                freqHz2 = freqHz2,
                durationMs = durationMs,
                usage = usage,
                volume = volume
            )
        }
        synchronized(lock) {
            activeJob?.cancel()
            activeJob = job
        }
    }

    /**
     * Memutar satu pulsa nada darurat berbasis PCM [AudioTrack.MODE_STATIC] secara suspend pada [Dispatchers.IO].
     */
    suspend fun playToneSuspend(
        freqHz1: Double = 880.0,
        freqHz2: Double = 0.0,
        durationMs: Int = 400,
        usage: Int = AudioAttributes.USAGE_ALARM,
        volume: Float = 0.85f
    ) = withContext(Dispatchers.IO) {
        val clampedDurationMs = durationMs.coerceIn(80, 2_500)
        val numSamples = ((SAMPLE_RATE.toLong() * clampedDurationMs) / 1_000L).toInt().coerceAtLeast(1)
        val pcm = ShortArray(numSamples)
        val fadeSamples = min(numSamples / 10, (SAMPLE_RATE * 0.012).toInt()).coerceAtLeast(1)
        val amp = (Short.MAX_VALUE * volume.coerceIn(0.05f, 0.95f)).toDouble()
        val hasSecondFreq = freqHz2 > 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val wave = if (hasSecondFreq) {
                0.5 * (sin(2.0 * PI * freqHz1 * t) + sin(2.0 * PI * freqHz2 * t))
            } else {
                sin(2.0 * PI * freqHz1 * t)
            }
            val env = when {
                i < fadeSamples -> i.toDouble() / fadeSamples.toDouble()
                i >= numSamples - fadeSamples -> (numSamples - 1 - i).toDouble() / fadeSamples.toDouble()
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            pcm[i] = (wave * env * amp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        var track: AudioTrack? = null
        try {
            releaseCurrentTrackLocked()
            val bufferBytes = pcm.size * 2
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val builtTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build()

            if (builtTrack.state != AudioTrack.STATE_INITIALIZED) {
                runCatching { builtTrack.release() }
                return@withContext
            }

            track = builtTrack
            synchronized(lock) {
                activeTrack = builtTrack
            }

            builtTrack.write(pcm, 0, pcm.size)
            builtTrack.play()
            delay(clampedDurationMs.toLong() + 40L)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.d(TAG, "AudioTrack darurat tidak tersedia pada lingkungan ini: ${t.message}")
        } finally {
            releaseTrackSafely(track)
        }
    }

    /**
     * Menghentikan nada darurat yang sedang diputar dan melepas resource AudioTrack segera.
     */
    fun stop() {
        synchronized(lock) {
            activeJob?.cancel()
            activeJob = null
        }
        releaseCurrentTrackLocked()
    }

    private fun releaseCurrentTrackLocked() {
        val old = synchronized(lock) {
            val current = activeTrack
            activeTrack = null
            current
        }
        releaseTrackSafely(old)
    }

    private fun releaseTrackSafely(track: AudioTrack?) {
        if (track == null) return
        synchronized(lock) {
            if (activeTrack === track) {
                activeTrack = null
            }
        }
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
        } catch (_: Throwable) {
        }
        try {
            track.flush()
        } catch (_: Throwable) {
        }
        try {
            track.release()
        } catch (_: Throwable) {
        }
    }
}
