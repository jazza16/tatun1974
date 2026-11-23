package com.example.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Resolves the hosting [FragmentActivity] from any Android [Context] (including Compose ContextWrappers).
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Checks biometric authentication availability (Strong or Weak fingerprint/face hardware).
 */
fun checkBiometricAvailability(context: Context): Int {
    return try {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    } catch (_: Throwable) {
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }
}

/**
 * Launches the native AndroidX [BiometricPrompt] bound directly to the hosting [FragmentActivity].
 * Reliable across both full-screen splash portals and modal Compose Dialogs without alpha launcher quirks.
 */
fun launchOfficerBiometricPrompt(
    context: Context,
    title: String = "Login Biometrik Petugas Pusdalops",
    subtitle: String = "",
    negativeButtonText: String = "Gunakan PIN",
    onSuccess: () -> Unit,
    onUsePinFallback: () -> Unit,
    onCancelled: () -> Unit = {},
    onError: (String) -> Unit
) {
    val activity = context.findFragmentActivity()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        onError("Layar biometrik tidak tersedia saat ini. Silakan gunakan PIN.")
        return
    }

    val availability = checkBiometricAvailability(context)
    if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
        val message = when (availability) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Sidik jari / pengenalan wajah belum didaftarkan di Pengaturan perangkat Anda."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Sensor biometrik sedang sibuk atau tidak tersedia sementara."
            else ->
                "Perangkat ini tidak mendukung autentikasi biometrik."
        }
        onError(message)
        return
    }

    try {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onUsePinFallback()
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancelled()
                    else -> onError(errString.toString().ifBlank { "Autentikasi biometrik gagal." })
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // BiometricPrompt automatically keeps listening for another scan attempt
            }
        }

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setConfirmationRequired(false)

        if (subtitle.isNotBlank()) {
            promptBuilder.setSubtitle(subtitle)
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(promptBuilder.build())
    } catch (e: Throwable) {
        onError(e.localizedMessage ?: "Gagal membuka dialog biometrik. Silakan gunakan PIN.")
    }
}
