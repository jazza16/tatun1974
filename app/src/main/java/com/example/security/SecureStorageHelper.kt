package com.example.security

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM encryption helper for sensitive local SharedPreferences and DataStore values.
 * Uses AndroidKeyStore on device; falls back cleanly to an ephemeral JVM AES-256 key under Robolectric tests.
 */
object SecureStorageHelper {
    private const val KEY_ALIAS = "sipagar_kita_aes_gcm_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc_v1:"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var jvmFallbackKey: SecretKey? = null

    private fun getOrCreateKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            return keyGen.generateKey()
        } catch (_: Exception) {
            return jvmFallbackKey ?: synchronized(this) {
                jvmFallbackKey ?: KeyGenerator.getInstance("AES").apply {
                    init(256)
                }.generateKey().also { jvmFallbackKey = it }
            }
        }
    }

    fun isEncryptedValue(value: String?): Boolean {
        return !value.isNullOrBlank() && value.startsWith(PREFIX)
    }

    fun encryptString(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptString(value: String?): String? {
        if (value == null) return null
        if (!value.startsWith(PREFIX)) return value // Legacy plaintext migration compatibility
        return try {
            val raw = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            if (raw.size <= GCM_IV_LENGTH) return null
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun putEncrypted(editor: SharedPreferences.Editor, key: String, value: String?) {
        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, encryptString(value))
        }
    }

    fun getDecrypted(prefs: SharedPreferences, key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return decryptString(raw)
    }
}
