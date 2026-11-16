package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerRoles
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FirestoreRulesTest : FirestoreEmulatorTestBase() {

    @Before
    override fun setUpFirebase() {
        super.setUpFirebase()
        FirestoreHelper.overrideFirestoreForTesting = firestore
    }

    @After
    override fun tearDownFirebase() {
        FirestoreHelper.overrideFirestoreForTesting = null
        super.tearDownFirebase()
    }

    @Test
    fun syncAndQueryCitizenReport_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val reportId = "LAP_TEST_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val payload = mapOf(
            "id" to reportId,
            "authorUid" to aliceUid,
            "disasterType" to "Banjir",
            "incidentTime" to "14 Nov 2026, 10:00 WIB",
            "incidentLocation" to "Jl. Lintas Koto Jaya",
            "district" to "Kota Mukomuko",
            "village" to "Koto Jaya",
            "latitude" to -2.58,
            "longitude" to 101.12,
            "isGpsVerified" to true,
            "casualtiesDead" to 0,
            "casualtiesInjured" to 0,
            "casualtiesMissing" to 0,
            "reporterName" to "Warga Test",
            "reporterPhone" to "08123456789",
            "additionalNotes" to "Luapan sungai",
            "timestamp" to System.currentTimeMillis(),
            "status" to "Menunggu Verifikasi"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("citizen_disaster_reports")
                .document(reportId)
                .set(payload)
                .await()
        }

        val querySnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("citizen_disaster_reports")
                .whereEqualTo("authorUid", aliceUid)
                .get()
                .await()
        }

        assertTrue(querySnap.documents.any { it.id == reportId })
    }

    @Test
    fun getCitizenReport_crossDeviceOfficerReadSucceeds_andDeleteFailsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val reportId = "LAP_CROSS_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val payload = mapOf(
            "id" to reportId,
            "authorUid" to aliceUid,
            "disasterType" to "Longsor",
            "incidentTime" to "14 Nov 2026",
            "incidentLocation" to "Teramang Jaya",
            "district" to "Teramang Jaya",
            "village" to "Brangan Mulya",
            "latitude" to -2.65,
            "longitude" to 101.20,
            "isGpsVerified" to true,
            "casualtiesDead" to 0,
            "casualtiesInjured" to 0,
            "casualtiesMissing" to 0,
            "reporterName" to "Alice",
            "reporterPhone" to "0811111111",
            "additionalNotes" to "",
            "timestamp" to System.currentTimeMillis(),
            "status" to "Diproses"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("citizen_disaster_reports").document(reportId).set(payload).await()
        }

        signInTestUser(BOB_EMAIL)
        val doc = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("citizen_disaster_reports").document(reportId).get().await()
        }
        assertTrue("Officer on another device must be able to read citizen report", doc.exists())

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("citizen_disaster_reports").document(reportId).delete().await()
            }
            fail("Expected cross-user delete to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun syncCredentialToCloud_stripsInitialSecurePinAndEnforcesOwnerRead() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val nip = "TEST_NIP_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val cred = OfficerCredentialEntity(
            nip = nip,
            fullName = "Petugas Uji Firestore",
            role = OfficerRoles.OPERATOR,
            salt = salt,
            pinHash = OfficerCredentialEntity.hashWithSalt("998877", salt),
            isCustomPinSet = true,
            initialSecurePin = "123456"
        )

        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            FirestoreHelper.syncCredentialToCloudSuspend(cred)
        }
        assertTrue("Sync credential failed: ${syncRes.exceptionOrNull()}", syncRes.isSuccess)

        val fetchedDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("officer_credentials").document(nip).get().await()
        }
        assertEquals("", fetchedDoc.getString("initialSecurePin"))
        assertEquals(aliceUid, fetchedDoc.getString("ownerUid"))

        val helperFetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            FirestoreHelper.fetchCredentialByNipFromCloud(nip)
        }
        assertTrue(helperFetched.isSuccess)
        assertNotNull(helperFetched.getOrNull())
        assertEquals("", helperFetched.getOrNull()?.initialSecurePin)
    }

    @Test
    fun updateOfficerCredential_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val nip = "TEST_TAMPER_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val cred = OfficerCredentialEntity(
            nip = nip,
            fullName = "Alice Officer",
            role = OfficerRoles.OPERATOR,
            salt = salt,
            pinHash = OfficerCredentialEntity.hashWithSalt("778899", salt),
            isCustomPinSet = true,
            initialSecurePin = ""
        )
        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) { FirestoreHelper.syncCredentialToCloudSuspend(cred) }
        assertTrue(syncRes.isSuccess)

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("officer_credentials")
                    .document(nip)
                    .update("pinHash", "tampered_hash")
                    .await()
            }
            fail("Expected cross-user update to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun unauthenticatedAccess_rejectedByHelperAndFirestoreRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()

        val helperCreds = withTimeout(DEFAULT_TIMEOUT_MS) {
            FirestoreHelper.fetchAllCredentialsFromCloudSuspend()
        }
        assertTrue(helperCreds.isFailure)
        assertTrue(helperCreds.exceptionOrNull() is SecurityException)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("device_security_sessions")
                    .whereEqualTo("ownerUid", aliceUid)
                    .get()
                    .await()
            }
            fail("Expected unauthenticated query on protected collection to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_bpbd@test.com"
        const val BOB_EMAIL = "bob_bpbd@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
