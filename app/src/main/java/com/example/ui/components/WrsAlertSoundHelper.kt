package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.util.EmergencyTonePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper to play simulated WRS-NTWC (Warning Receiver System) emergency alarm tones
 * and haptic vibrations matching BMKG InaTEWS warning consoles.
 */
object WrsAlertSoundHelper {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playWrsAlarmTone(context: Context, isTsunamiAlert: Boolean = false) {
        scope.launch {
            try {
                // Haptic vibration
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = if (isTsunamiAlert) {
                            longArrayOf(0, 300, 150, 300, 150, 600)
                        } else {
                            longArrayOf(0, 200, 100, 200, 100, 400)
                        }
                        val amplitudes = if (isTsunamiAlert) {
                            intArrayOf(0, 255, 0, 255, 0, 255)
                        } else {
                            intArrayOf(0, 200, 0, 200, 0, 220)
                        }
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 250, 150, 250), -1)
                    }
                }

                // Dual-tone emergency beep matching InaTEWS receiver consoles (PCM AudioTrack, zero ToneGenerator timeout)
                val usage = if (isTsunamiAlert) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION
                if (isTsunamiAlert) {
                    EmergencyTonePlayer.playToneSuspend(freqHz1 = 960.0, freqHz2 = 1280.0, durationMs = 400, usage = usage)
                    delay(120)
                    EmergencyTonePlayer.playToneSuspend(freqHz1 = 880.0, freqHz2 = 1100.0, durationMs = 650, usage = usage)
                } else {
                    EmergencyTonePlayer.playToneSuspend(freqHz1 = 1000.0, freqHz2 = 0.0, durationMs = 300, usage = usage)
                    delay(120)
                    EmergencyTonePlayer.playToneSuspend(freqHz1 = 1320.0, freqHz2 = 0.0, durationMs = 400, usage = usage)
                }
            } catch (_: Throwable) {
                // Graceful fallback if device audio is restricted
            }
        }
    }
}
