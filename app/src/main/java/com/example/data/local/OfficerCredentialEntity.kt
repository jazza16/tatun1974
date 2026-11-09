package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

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
     * Helper to verify if an input PIN matches this credential.
     */
    fun verifyPin(inputPin: String): Boolean {
        val trimmed = inputPin.trim()
        if (trimmed.isEmpty()) return false

        // Compute hash of input pin with this user's unique salt
        val calculatedHash = hashWithSalt(trimmed, salt)
        if (calculatedHash.equals(pinHash, ignoreCase = true)) {
            return true
        }

        // If officer has not yet customized their PIN, check against their initial secure PIN
        if (!isCustomPinSet && initialSecurePin.isNotBlank() && trimmed == initialSecurePin) {
            return true
        }

        return false
    }

    companion object {
        /**
         * Computes salted SHA-256 hash.
         */
        fun hashWithSalt(pin: String, salt: String): String {
            val combined = "$salt:$pin:mukomuko_bpbd_2026"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
            return digest.fold("") { str, it -> str + "%02x".format(it) }
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
            val salt = java.util.UUID.randomUUID().toString().take(12)
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
