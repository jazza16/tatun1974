package com.example.data.auth

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central Authentication Manager for BPBD Kabupaten Mukomuko (SIPAGAR KITA).
 * Manages Firebase Authentication sessions and links authenticated sessions
 * with the Pusdalops & TRC Personnel database to secure the 'Ruang Kerja' module.
 */
class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val officerAuthManager = OfficerAuthManager(context)
    private val database = AppDatabase.getInstance(context)

    private val _isAuthenticated = MutableStateFlow(checkInitialAuthState())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(auth.currentUser?.email)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private fun checkInitialAuthState(): Boolean {
        // User is considered authenticated for Ruang Kerja if Firebase user is logged in
        // OR an official duty officer session is active in OfficerAuthManager
        return auth.currentUser != null || officerAuthManager.isOfficerLoggedIn()
    }

    /**
     * Returns true if either Firebase Auth user or Officer local session is active.
     */
    fun isSessionActive(): Boolean {
        val active = auth.currentUser != null || officerAuthManager.isOfficerLoggedIn()
        _isAuthenticated.value = active
        return active
    }

    /**
     * Returns current FirebaseUser if available.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Returns the active officer profile name or fallback authenticated email.
     */
    fun getDisplayName(): String {
        val officerName = officerAuthManager.getOfficerName()
        if (officerName.isNotBlank()) return officerName
        return auth.currentUser?.displayName
            ?: auth.currentUser?.email
            ?: "Petugas Terverifikasi"
    }

    /**
     * Returns the active officer badge ID / NIP or UID.
     */
    fun getBadgeOrUid(): String {
        val badge = officerAuthManager.getOfficerBadgeId()
        if (badge.isNotBlank()) return badge
        return auth.currentUser?.uid?.take(8) ?: "PUSDALOPS-MMK"
    }

    /**
     * Authenticate staff member using official NIP/PIN credential against BPBD Personnel database.
     */
    suspend fun signInOfficer(name: String, nip: String, pin: String, role: String, shift: String): Boolean {
        if (!officerAuthManager.verifyPin(pin)) {
            return false
        }

        val success = officerAuthManager.loginOfficer(
            name = name,
            badgeId = nip,
            shift = shift,
            pin = pin,
            role = role
        )

        if (success) {
            _isAuthenticated.value = true
        }
        return success
    }

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUserEmail.value = user?.email
            if (user != null || officerAuthManager.isOfficerLoggedIn()) {
                _isAuthenticated.value = true
            } else {
                _isAuthenticated.value = false
            }
        }
    }

    /**
     * Sign in to Firebase using Google ID Token from Credential Manager.
     */
    fun signInWithGoogleToken(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                _isAuthenticated.value = true
                _currentUserEmail.value = user?.email
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage ?: "Gagal autentikasi Google Firebase.")
            }
    }

    /**
     * Terminate both Firebase Auth and duty officer sessions.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        officerAuthManager.logoutOfficer()
        _isAuthenticated.value = false
        _currentUserEmail.value = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
