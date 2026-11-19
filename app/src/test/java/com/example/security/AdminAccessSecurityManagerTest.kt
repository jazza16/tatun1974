package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.AdminAuthApi
import com.example.data.local.AppDatabase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAccessSecurityManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        AdminAccessSecurityManager.resetForTesting()
        AdminAccessSecurityManager.disableFirestoreForTesting = true
        AppDatabase.getInstance(context).auditLogDao().clearAuditLogs()
    }

    @After
    fun tearDown() {
        AdminAccessSecurityManager.resetForTesting()
    }

    @Test
    fun verifyMasterPin_firestorePlaintextMatchIssuesTemporaryTokenAndWritesAuditLog() = runBlocking {
        var fakeNow = 1_750_000_000_000L
        AdminAccessSecurityManager.clockProvider = { fakeNow }
        AdminAccessSecurityManager.overrideBaseUrlForTesting = ""
        AdminAccessSecurityManager.overrideFirestorePinFetcherForTesting = {
            FirestoreMasterPinLookup.Found(masterPin = "@helloworld26@", sourceLabel = "Source.SERVER")
        }

        val result = AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = "@helloworld26@",
            officerNip = "198501012010012001",
            officerName = "AINUN MARDIATI CANIAGO SPd",
            officerRole = "Operator Pusdalops",
            actionTitle = "Kelola Direktori Personil BPBD"
        )

        assertTrue(result is AdminVerificationResult.Authorized)
        val authorized = result as AdminVerificationResult.Authorized
        assertEquals(600L, authorized.accessToken.remainingSeconds(fakeNow))
        assertNotNull(AdminAccessSecurityManager.getActiveTokenForOfficer("198501012010012001"))

        val logs = AppDatabase.getInstance(context).auditLogDao().getAllAuditLogsList()
        assertEquals(1, logs.size)
        assertEquals(AdminAccessSecurityManager.ACTION_GRANTED, logs.first().action)
        assertEquals("198501012010012001", logs.first().nip)
    }

    @Test
    fun verifyMasterPin_firestoreWrongPinReturnsSpecificErrorWithoutRetryFlag() = runBlocking {
        AdminAccessSecurityManager.overrideBaseUrlForTesting = ""
        AdminAccessSecurityManager.overrideFirestorePinFetcherForTesting = {
            FirestoreMasterPinLookup.Found(masterPin = "@helloworld26@", sourceLabel = "Source.SERVER")
        }

        val result = AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = "pin-salah",
            officerNip = "198802022015011002",
            officerName = "Petugas Uji",
            officerRole = "Petugas Jaga",
            actionTitle = "Kelola Direktori Personil BPBD"
        )

        assertTrue(result is AdminVerificationResult.Denied)
        val denied = result as AdminVerificationResult.Denied
        assertTrue(denied.message.contains("PIN salah", ignoreCase = true))
        assertFalse(denied.canRetry)
        assertNull(AdminAccessSecurityManager.getActiveTokenForOfficer("198802022015011002"))
    }

    @Test
    fun mapFirestoreException_unavailableReturnsSpecificNetworkMessageAndCanRetryTrue() = runBlocking {
        val unavailableEx = FirebaseFirestoreException(
            "Unable to resolve host firestore.googleapis.com",
            FirebaseFirestoreException.Code.UNAVAILABLE
        )
        val mapped = AdminAccessSecurityManager.mapFirestoreException(unavailableEx)

        AdminAccessSecurityManager.overrideBaseUrlForTesting = ""
        AdminAccessSecurityManager.overrideFirestorePinFetcherForTesting = { mapped }

        val result = AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = "@helloworld26@",
            officerNip = "199001012020011003",
            officerName = "Petugas Offline",
            officerRole = "Operator",
            actionTitle = "Kelola Direktori Personil BPBD"
        )

        assertTrue(result is AdminVerificationResult.Denied)
        val denied = result as AdminVerificationResult.Denied
        assertTrue(denied.message.contains("Koneksi internet bermasalah", ignoreCase = true))
        assertTrue(denied.canRetry)
        assertEquals("UNAVAILABLE", denied.errorCode)
    }

    @Test
    fun verifyMasterPassword_remoteRestApiSuccessIssuesTokenAndExpiresProperly() = runBlocking {
        var fakeNow = 1_750_000_000_000L
        AdminAccessSecurityManager.clockProvider = { fakeNow }
        AdminAccessSecurityManager.overrideApiForTesting = object : AdminAuthApi {
            override suspend fun verifyAdminPassword(payload: RequestBody): Response<ResponseBody> {
                val json = """
                    {
                      "authorized": true,
                      "accessToken": "tmp_adm_9988776655",
                      "expiresInSeconds": 600,
                      "scope": "KELOLA_PERSONIL_ADMIN",
                      "message": "Otorisasi berhasil"
                    }
                """.trimIndent()
                return Response.success(json.toResponseBody("application/json".toMediaType()))
            }

            override suspend fun recordAdminAuditLog(payload: RequestBody): Response<ResponseBody> {
                return Response.success("{}".toResponseBody("application/json".toMediaType()))
            }
        }

        val result = AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = "StrongServerVerifiedPass!99",
            officerNip = "198501012010012001",
            officerName = "AINUN MARDIATI CANIAGO SPd",
            officerRole = "Operator Pusdalops",
            actionTitle = "Kelola Direktori Personil BPBD"
        )

        assertTrue(result is AdminVerificationResult.Authorized)
        assertNotNull(AdminAccessSecurityManager.getActiveTokenForOfficer("198501012010012001"))

        fakeNow += 601_000L
        assertNull(AdminAccessSecurityManager.getActiveTokenForOfficer("198501012010012001"))
    }
}
