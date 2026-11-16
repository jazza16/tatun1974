package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Room Database Entity for Officer Login Credentials in Pusdalops BPBD Kabupaten Mukomuko.
 * Stores individual login credentials per-personnel with secure SHA-256 password hashing.
 *
 * Replaces generic shared default PINs (117 / TRC2026) with individual officer credentials
 * that are automatically synchronized with Cloud Firestore across all devices.
 */
@Entity(
    tableName = "officer_credentials",
    indices = [
        Index(value = ["nip"], unique = true),
        Index(value = ["fullName"]),
        Index(value = ["role"]),
        Index(value = ["isCustomPinSet"]),
        Index(value = ["isSyncedWithCloud"])
    ]
)
data class OfficerCredentialEntity(
    @PrimaryKey
    val nip: String,                     // Official NIP (18 digits) or TRC Registration Number
    val fullName: String,                // Full name and title of the officer
    val role: String,                    // Hierarchy role (Pengawas, Manajer, Operator, TRC, etc.)
    val unit: String = "Pusdalops BPBD Kab. Mukomuko",
    val shift: String = "Shift Siaga 24 Jam",
    val phone: String = "",
    val pinHash: String,                 // Salted SHA-256 hash of the personal PIN
    val salt: String,                    // Unique per-user cryptographic salt
    val isCustomPinSet: Boolean = false, // True if the officer has created their personal PIN
    val initialSecurePin: String = "",   // Initial secure PIN derived uniquely from NIP (e.g. last 6 digits of NIP)
    val lastLoginAt: Long = 0L,          // Timestamp of the latest successful login
    val lastLoginDevice: String = "",    // Device model and ID that logged in
    val isSyncedWithCloud: Boolean = false, // Cloud Firestore sync status
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Alias property matching Firestore 'isPinChanged' field.
     */
    val isPinChanged: Boolean
        get() = isCustomPinSet

    /**
     * Helper to verify if an input PIN matches this credential.
     * - Strictly rejects generic default PINs ('117', 'BPBD117', 'TRC2026', etc.).
     * - Before PIN change (isCustomPinSet == false), accepts the last 6 digits of NIP as the valid initial PIN.
     * - After PIN change (isCustomPinSet == true), strictly blocks the 6-digit NIP and ONLY accepts the custom pinHash.
     */
    fun verifyPin(inputPin: String): Boolean {
        val trimmed = inputPin.trim()
        if (trimmed.isEmpty()) return false

        // 1. Reject forbidden generic default PINs unconditionally (does NOT block 6-digit NIP)
        val forbiddenDefaults = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117", "0000", "1234")
        if (forbiddenDefaults.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }

        val sixDigitNip = generateInitialPinForNip(nip)
        val rawLastSixNip = nip.trim().takeLast(6)
        val isMatchingInitialNip = trimmed.equals(sixDigitNip, ignoreCase = true) ||
            (rawLastSixNip.length == 6 && trimmed.equals(rawLastSixNip, ignoreCase = true)) ||
            (initialSecurePin.isNotBlank() && trimmed.equals(initialSecurePin, ignoreCase = true))

        // 2. If officer has ALREADY changed their PIN (isCustomPinSet == true),
        // 6-digit NIP is strictly forbidden for login
        if (isCustomPinSet) {
            if (isMatchingInitialNip) {
                return false
            }
            return verifyHash(trimmed, salt, pinHash)
        }

        // 3. Before officer changes PIN (isCustomPinSet == false), accept 6-digit NIP as valid initial PIN
        if (isMatchingInitialNip) {
            return true
        }

        return verifyHash(trimmed, salt, pinHash)
    }

    companion object {
        private val secureRandom = SecureRandom()
        private const val PBKDF2_PREFIX = "pbkdf2"
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_BITS = 256

        fun generateCryptographicSalt(): String {
            val bytes = ByteArray(16)
            secureRandom.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun constantTimeEqualsHex(a: String, b: String): Boolean {
            if (a.isBlank() || b.isBlank()) return false
            return MessageDigest.isEqual(
                a.lowercase().toByteArray(Charsets.UTF_8),
                b.lowercase().toByteArray(Charsets.UTF_8)
            )
        }

        /**
         * Computes salted PBKDF2WithHmacSHA256 hash (10,000 iterations).
         * Format: pbkdf2$10000$<hexDigest>
         */
        fun hashWithSalt(pin: String, salt: String): String {
            return hashPbkdf2(pin, salt, PBKDF2_ITERATIONS)
        }

        private fun hashPbkdf2(pin: String, salt: String, iterations: Int): String {
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val saltBytes = "$salt:mukomuko_bpbd_2026".toByteArray(Charsets.UTF_8)
            val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), saltBytes, iterations, PBKDF2_KEY_BITS)
            val encoded = factory.generateSecret(spec).encoded
            spec.clearPassword()
            val hex = encoded.joinToString("") { "%02x".format(it) }
            return "$PBKDF2_PREFIX\$$iterations\$$hex"
        }

        private fun hashLegacySha256(pin: String, salt: String): String {
            val combined = "$salt:$pin:mukomuko_bpbd_2026"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        /**
         * Verifies a candidate PIN against a stored PBKDF2 hash or legacy 64-char SHA-256 hex hash.
         */
        fun verifyHash(pin: String, salt: String, storedHash: String): Boolean {
            if (pin.isBlank() || salt.isBlank() || storedHash.isBlank()) return false
            return if (storedHash.startsWith("$PBKDF2_PREFIX\$")) {
                val parts = storedHash.split("$")
                if (parts.size != 3) return false
                val iterations = parts[1].toIntOrNull()?.coerceIn(1_000, 100_000) ?: PBKDF2_ITERATIONS
                val expectedHex = parts[2]
                val candidateFull = hashPbkdf2(pin, salt, iterations)
                val candidateHex = candidateFull.substringAfterLast("$")
                constantTimeEqualsHex(candidateHex, expectedHex)
            } else {
                val legacyCandidate = hashLegacySha256(pin, salt)
                constantTimeEqualsHex(legacyCandidate, storedHash)
            }
        }

        /**
         * Generates an initial secure deterministic PIN for an official officer based on their NIP.
         * For 18-digit ASN NIP: uses the last 6 digits (e.g. NIP 199807202025061002 -> 061002).
         * For TRC badge IDs: extracts digits or fallback 6-digit hash.
         */
        fun generateInitialPinForNip(nip: String): String {
            val digitsOnly = nip.filter { it.isDigit() }
            return when {
                digitsOnly.length >= 6 -> digitsOnly.takeLast(6)
                digitsOnly.isNotEmpty() -> digitsOnly.padStart(6, '0')
                else -> {
                    val hash = nip.hashCode()
                    val abs = kotlin.math.abs(hash)
                    (abs % 900000 + 100000).toString()
                }
            }
        }

        /**
         * Creates a new credential entry for an official officer.
         */
        fun createDefault(
            nip: String,
            fullName: String,
            role: String,
            unit: String = "Pusdalops BPBD Kab. Mukomuko",
            shift: String = "Shift Siaga 24 Jam",
            phone: String = "",
            customPin: String? = null
        ): OfficerCredentialEntity {
            val salt = generateCryptographicSalt()
            val initialPin = generateInitialPinForNip(nip)
            val effectivePin = customPin?.trim()?.ifBlank { null } ?: initialPin
            val isCustom = customPin != null && customPin.isNotBlank()

            return OfficerCredentialEntity(
                nip = nip.trim(),
                fullName = fullName.trim(),
                role = role.trim(),
                unit = unit.trim(),
                shift = shift.trim(),
                phone = phone.trim(),
                pinHash = hashWithSalt(effectivePin, salt),
                salt = salt,
                isCustomPinSet = isCustom,
                initialSecurePin = if (isCustom) "" else initialPin,
                lastLoginAt = 0L,
                lastLoginDevice = "",
                isSyncedWithCloud = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
