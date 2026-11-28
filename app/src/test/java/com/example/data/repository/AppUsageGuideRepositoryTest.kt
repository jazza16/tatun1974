package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppUsageGuide
import com.example.model.DefaultAppUsageGuideCatalog
import com.example.model.UsageGuideStep
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
class AppUsageGuideRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppUsageGuideRepository.resetForTesting()
        AppUsageGuideRepository.clearCache(context)
    }

    @After
    fun tearDown() {
        AppUsageGuideRepository.resetForTesting()
        AppUsageGuideRepository.clearCache(context)
    }

    @Test
    fun bundledDefaultGuideContainsConciseCitizenAndOfficerSteps() {
        val guide = AppUsageGuideRepository.getCachedOrDefaultGuide(context)

        assertEquals(AppUsageGuide.SOURCE_BUNDLED, guide.source)
        assertTrue(guide.citizenSummary.isNotBlank())
        assertTrue(guide.officerSummary.isNotBlank())
        assertEquals(5, guide.citizenSteps.size)
        assertEquals(6, guide.officerSteps.size)
        assertTrue(guide.citizenSteps.all { it.title.isNotBlank() && it.description.isNotBlank() && it.moduleTag.isNotBlank() })
        assertTrue(guide.officerSteps.all { it.title.isNotBlank() && it.description.isNotBlank() && it.moduleTag.isNotBlank() })
    }

    @Test
    fun cloudFetchPersistsToOfflineCacheWithoutApkUpdate() = runBlocking {
        val remotePayload: Map<String, Any> = mapOf(
            "guideVersion" to "v1.3.0-live.9",
            "releaseTitle" to "Panduan Penggunaan SIPAGAR KITA",
            "updatedAtLabel" to "28 November 2026",
            "updatedBy" to "Admin Pusdalops BPBD",
            "updatedAt" to 1795800000000L,
            "citizenSummary" to "Panduan terbaru Warga langsung dari Cloud.",
            "officerSummary" to "Panduan terbaru Petugas langsung dari Cloud.",
            "citizenSteps" to listOf(
                mapOf(
                    "stepNumber" to 1,
                    "title" to "Cek Cuaca Live",
                    "description" to "Pantau cuaca kecamatan terbaru dari Cloud.",
                    "moduleTag" to "Beranda"
                )
            ),
            "officerSteps" to listOf(
                mapOf(
                    "stepNumber" to 1,
                    "title" to "Verifikasi Cepat",
                    "description" to "Validasi laporan masuk sesuai SOP baru.",
                    "moduleTag" to "Pusdalops"
                )
            )
        )

        AppUsageGuideRepository.overrideCloudGuideFetcherForTesting = {
            Result.success(remotePayload)
        }

        val onlineSync = AppUsageGuideRepository.fetchLatestGuide(context)
        assertTrue(onlineSync.syncedFromCloud)
        assertEquals("v1.3.0-live.9", onlineSync.guide.guideVersion)
        assertEquals("Cek Cuaca Live", onlineSync.guide.citizenSteps.first().title)

        // Simulate subsequent offline launch: reset memory and disable Firestore
        AppUsageGuideRepository.resetForTesting()
        AppUsageGuideRepository.disableFirestoreForTesting = true

        val offlineResult = AppUsageGuideRepository.fetchLatestGuide(context)
        assertFalse(offlineResult.syncedFromCloud)
        assertEquals(AppUsageGuide.SOURCE_CACHE, offlineResult.guide.source)
        assertEquals("v1.3.0-live.9", offlineResult.guide.guideVersion)
        assertEquals("Verifikasi Cepat", offlineResult.guide.officerSteps.first().title)
    }

    @Test
    fun publishGuideToCloudSanitizesPayloadAndUpdatesCache() = runBlocking {
        var capturedPayload: Map<String, Any>? = null
        AppUsageGuideRepository.overrideCloudGuidePublisherForTesting = { payload ->
            capturedPayload = payload
            Result.success(Unit)
        }

        val edited = DefaultAppUsageGuideCatalog.defaultGuide().copy(
            guideVersion = "v1.3.0-live.3",
            updatedBy = "Kalaksa BPBD",
            citizenSteps = listOf(
                UsageGuideStep(
                    stepNumber = 99, // will be normalized to 1
                    title = "Langkah Warga Baru",
                    description = "Penjelasan ringkas langkah warga.",
                    moduleTag = "Laporan Warga"
                )
            )
        )

        val result = AppUsageGuideRepository.publishGuideToCloud(context, edited)
        assertTrue(result.isSuccess)
        assertEquals("v1.3.0-live.3", capturedPayload?.get("guideVersion"))

        val cachedAfterPublish = AppUsageGuideRepository.loadCachedGuide(context)
        assertEquals("v1.3.0-live.3", cachedAfterPublish?.guideVersion)
        assertEquals(1, cachedAfterPublish?.citizenSteps?.first()?.stepNumber)
        assertEquals("Langkah Warga Baru", cachedAfterPublish?.citizenSteps?.first()?.title)
    }
}
