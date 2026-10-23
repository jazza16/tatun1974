package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper to play simulated WRS-NTWC (Warning Receiver System) emergency alarm tones
 * and haptic vibrations matching BMKG InaTEWS warning consoles.
 */
object WrsAlertSoundHelper {

    fun playWrsAlarmTone(context: Context, isTsunamiAlert: Boolean = false) {
        CoroutineScope(Dispatchers.Default).launch {
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

                // Dual-tone emergency beep matching InaTEWS receiver consoles
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
                if (isTsunamiAlert) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 400)
                    delay(450)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 700)
                } else {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    delay(350)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 400)
                }
                delay(800)
                toneGen.release()
            } catch (_: Exception) {
                // Graceful fallback if device audio is restricted
            }
        }
    }
}
