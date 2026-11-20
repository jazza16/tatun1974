package com.example.data.api

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseStorageDiagnosticsTest {

    private val testBaseUrl = "https://demo-project.supabase.co"
    private val testAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test_anon_key_payload"

    @Before
    fun setUp() {
        SupabaseStorageDiagnostics.resetTestHooks()
    }

    @After
    fun tearDown() {
        SupabaseStorageDiagnostics.resetTestHooks()
    }

    private fun createMockAdapter(
        configured: Boolean = true,
        onUpload: (String, String, ByteArray) -> Result<String> = { bucket, path, _ ->
            Result.success("$testBaseUrl/storage/v1/object/public/$bucket/$path")
        },
        onDelete: (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) }
    ): SupabaseStorageDiagnostics.StorageProbeAdapter {
        return object : SupabaseStorageDiagnostics.StorageProbeAdapter {
            override fun isConfigured(): Boolean = configured
            override fun supabaseUrl(): String = if (configured) testBaseUrl else "YOUR_SUPABASE_URL"
            override fun supabaseKey(): String = if (configured) testAnonKey else "YOUR_SUPABASE_KEY"
            override suspend fun uploadBytes(bucket: String, path: String, bytes: ByteArray): Result<String> =
                onUpload(bucket, path, bytes)
            override suspend fun deleteObject(bucket: String, path: String): Result<Unit> =
                onDelete(bucket, path)
        }
    }

    @Test
    fun `public bucket policy returns PUBLIC_READ_ALLOWED and verifies exact JPEG bytes and SHA256`() = runBlocking {
        var uploadedBytes = ByteArray(0)
        var uploadedPath = ""
        var deletedPath = ""

        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter(
            onUpload = { bucket, path, bytes ->
                uploadedPath = path
                uploadedBytes = bytes
                Result.success("$testBaseUrl/storage/v1/object/public/$bucket/$path")
            },
            onDelete = { _, path ->
                deletedPath = path
                Result.success(Unit)
            }
        )
        SupabaseStorageDiagnostics.overrideHttpProbeExecutorForTesting =
            SupabaseStorageDiagnostics.HttpProbeExecutor { url, headers ->
                val isPublicEndpoint = url.contains("/storage/v1/object/public/reports/")
                if (isPublicEndpoint) {
                    assertTrue("Public probe must NOT send auth headers", headers.isEmpty())
                } else {
                    assertTrue("Auth probe must include apikey header", headers.containsKey("apikey"))
                    assertTrue("Auth probe must include Authorization header", headers.containsKey("Authorization"))
                }
                HttpProbeResult(
                    url = url,
                    statusCode = 200,
                    contentType = "image/jpeg",
                    bodyBytes = uploadedBytes,
                    durationMs = 18L
                )
            }

        val report = SupabaseClient.diagnoseImageUploadPath(
            reportId = "REP-2026-001",
            cleanupProbeObject = true
        )

        assertTrue("Upload must succeed", report.uploadSucceeded)
        assertTrue("JPEG magic header must be valid", report.hasValidJpegMagic)
        assertTrue("Transmitted byte size must be > 0", report.expectedByteSize > 0)
        assertTrue("Byte transmission SHA-256 must verify intact", report.bytesTransmittedVerified)
        assertTrue("Public access must be allowed", report.publicAccessAllowed)
        assertTrue("Authenticated access must also succeed", report.authenticatedAccessAllowed)
        assertEquals(200, report.publicHttpStatus)
        assertEquals(200, report.authenticatedHttpStatus)
        assertEquals(BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED, report.bucketPolicyStatus)
        assertTrue("Probe object should be cleaned up", report.cleanupDeleted)
        assertEquals(uploadedPath, deletedPath)
        assertTrue("Object path must follow diagnostics/REP-2026-001_*.jpg format", report.objectPath.startsWith("diagnostics/REP-2026-001_"))
        assertTrue("Formatted log must contain policy verdict", report.formattedLog.contains("PUBLIC_READ_ALLOWED"))
        assertFalse("Formatted log must NEVER leak raw Supabase API key", report.formattedLog.contains(testAnonKey))
    }

    @Test
    fun `private bucket with authenticated SELECT returns AUTHENTICATED_READ_ONLY while verifying bytes intact`() = runBlocking {
        var uploadedBytes = ByteArray(0)

        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter(
            onUpload = { bucket, path, bytes ->
                uploadedBytes = bytes
                Result.success("$testBaseUrl/storage/v1/object/public/$bucket/$path")
            }
        )
        SupabaseStorageDiagnostics.overrideHttpProbeExecutorForTesting =
            SupabaseStorageDiagnostics.HttpProbeExecutor { url, headers ->
                if (url.contains("/storage/v1/object/public/")) {
                    HttpProbeResult(
                        url = url,
                        statusCode = 403,
                        contentType = "application/json",
                        bodyBytes = """{"statusCode":"403","error":"Unauthorized","message":"Bucket is not public"}""".toByteArray(),
                        errorSnippet = """{"statusCode":"403","error":"Unauthorized","message":"Bucket is not public"}"""
                    )
                } else {
                    assertTrue(headers["Authorization"]?.startsWith("Bearer ") == true)
                    HttpProbeResult(
                        url = url,
                        statusCode = 200,
                        contentType = "image/jpeg",
                        bodyBytes = uploadedBytes
                    )
                }
            }

        val report = SupabaseClient.diagnoseImageUploadPath(reportId = "REP-PRIVATE")

        assertTrue(report.uploadSucceeded)
        assertTrue("Bytes must still be verified via authenticated GET", report.bytesTransmittedVerified)
        assertFalse("Public read must be false", report.publicAccessAllowed)
        assertTrue("Authenticated read must be true", report.authenticatedAccessAllowed)
        assertEquals(403, report.publicHttpStatus)
        assertEquals(200, report.authenticatedHttpStatus)
        assertEquals(BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY, report.bucketPolicyStatus)
        assertTrue(report.recommendation.contains("PRIVATE/AUTHENTICATED-ONLY"))
    }

    @Test
    fun `corrupted or truncated bytes on retrieval yields BYTE_MISMATCH_CORRUPTED`() = runBlocking {
        var uploadedBytes = ByteArray(0)

        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter(
            onUpload = { bucket, path, bytes ->
                uploadedBytes = bytes
                Result.success("$testBaseUrl/storage/v1/object/public/$bucket/$path")
            }
        )
        SupabaseStorageDiagnostics.overrideHttpProbeExecutorForTesting =
            SupabaseStorageDiagnostics.HttpProbeExecutor { url, _ ->
                // Return truncated/altered bytes despite HTTP 200
                val corrupted = uploadedBytes.copyOf(maxOf(1, uploadedBytes.size / 2))
                HttpProbeResult(
                    url = url,
                    statusCode = 200,
                    contentType = "image/jpeg",
                    bodyBytes = corrupted
                )
            }

        val report = SupabaseClient.diagnoseImageUploadPath(reportId = "REP-CORRUPT")

        assertTrue(report.uploadSucceeded)
        assertFalse("Corrupted bytes must fail SHA-256 verification", report.bytesTransmittedVerified)
        assertFalse(report.publicSha256Matches)
        assertFalse(report.authenticatedSha256Matches)
        assertEquals(BucketAccessPolicyStatus.BYTE_MISMATCH_CORRUPTED, report.bucketPolicyStatus)
    }

    @Test
    fun `RLS INSERT failure on upload returns UPLOAD_FAILED with actionable RLS recommendation`() = runBlocking {
        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter(
            onUpload = { _, _, _ ->
                Result.failure(IllegalStateException("403 Unauthorized: new row violates row-level security policy"))
            }
        )

        val report = SupabaseClient.diagnoseImageUploadPath(reportId = "REP-RLS-BLOCK")

        assertFalse(report.uploadSucceeded)
        assertFalse(report.bytesTransmittedVerified)
        assertEquals(BucketAccessPolicyStatus.UPLOAD_FAILED, report.bucketPolicyStatus)
        assertTrue(report.recommendation.contains("RLS policy"))
    }

    @Test
    fun `both public and authenticated read denied returns ACCESS_DENIED_OR_MISCONFIGURED`() = runBlocking {
        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter()
        SupabaseStorageDiagnostics.overrideHttpProbeExecutorForTesting =
            SupabaseStorageDiagnostics.HttpProbeExecutor { url, _ ->
                HttpProbeResult(
                    url = url,
                    statusCode = 403,
                    contentType = "application/json",
                    bodyBytes = """{"error":"Forbidden"}""".toByteArray(),
                    errorSnippet = """{"error":"Forbidden"}"""
                )
            }

        val report = SupabaseClient.diagnoseImageUploadPath(reportId = "REP-DENIED")

        assertTrue(report.uploadSucceeded)
        assertFalse(report.bytesTransmittedVerified)
        assertFalse(report.publicAccessAllowed)
        assertFalse(report.authenticatedAccessAllowed)
        assertEquals(BucketAccessPolicyStatus.ACCESS_DENIED_OR_MISCONFIGURED, report.bucketPolicyStatus)
    }

    @Test
    fun `unconfigured Supabase secrets returns NOT_CONFIGURED cleanly`() = runBlocking {
        SupabaseStorageDiagnostics.overrideStorageAdapterForTesting = createMockAdapter(configured = false)

        val report = SupabaseClient.diagnoseImageUploadPath(reportId = "REP-UNCONF")

        assertFalse(report.uploadSucceeded)
        assertFalse(report.bytesTransmittedVerified)
        assertEquals(BucketAccessPolicyStatus.NOT_CONFIGURED, report.bucketPolicyStatus)
        assertTrue(report.recommendation.contains("SUPABASE_URL"))
    }
}
