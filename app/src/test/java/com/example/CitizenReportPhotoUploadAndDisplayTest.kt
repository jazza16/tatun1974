package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.FirebaseStoragePhotoUploader
import com.example.data.api.PhotoUploadOutcome
import com.example.data.local.AppDatabase
import com.example.data.local.FirestoreHelper
import com.example.data.repository.WeatherRepository
import com.example.model.CitizenDisasterReport
import com.example.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CitizenReportPhotoUploadAndDisplayTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WeatherRepository(db.weatherDao())
        FirebaseStoragePhotoUploader.overrideUploaderForTesting = null
        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = null
        FirestoreHelper.overrideCitizenReportUploaderForTesting = null
        FirestoreHelper.overrideCitizenReportFetcherForTesting = null
    }

    @After
    fun tearDown() {
        FirebaseStoragePhotoUploader.overrideUploaderForTesting = null
        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = null
        FirestoreHelper.overrideCitizenReportUploaderForTesting = null
        FirestoreHelper.overrideCitizenReportFetcherForTesting = null
        db.close()
    }

    private fun createSolidTestPhotoFile(color: Int = Color.rgb(40, 120, 210)): String {
        val raw = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        val stamped = ImageUtils.renderGpsWatermarkOnBitmap(
            source = raw,
            latitude = -2.58123,
            longitude = 101.12345,
            incidentTime = "2026-11-19 16:00 WIB",
            locationLabel = "Desa Kates, Kec. Kota Mukomuko",
            accuracyMeters = 8f
        )
        val uri = ImageUtils.saveBitmapToPersistentReportFile(context, stamped)
        assertNotNull("Test watermarked photo file must be saved", uri)
        return uri!!
    }

    @Test
    fun renderGpsWatermarkOnBitmap_burnsWatermarkIntoActualBitmapPixels() {
        val raw = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val watermarked = ImageUtils.renderGpsWatermarkOnBitmap(
            source = raw,
            latitude = -2.58123,
            longitude = 101.12345,
            incidentTime = "19 Nov 2026 16:15 WIB",
            locationLabel = "Desa Kates, Kec. Kota Mukomuko",
            accuracyMeters = 6f
        )

        assertEquals(480, watermarked.width)
        assertEquals(360, watermarked.height)

        // Top region remains white (original image content), bottom bar has burned dark watermark stamp
        assertEquals("Top pixels must preserve original image", Color.WHITE, watermarked.getPixel(20, 20))
        val bottomPixel = watermarked.getPixel(20, watermarked.height - 10)
        assertNotEquals("Bottom watermark bar must be burned directly into bitmap pixels", Color.WHITE, bottomPixel)
    }

    @Test
    fun submitCitizenReport_waits100PercentForFirebaseStorageAndSavesFotoUrlToFirestore() = runBlocking {
        val localPhotoUri = createSolidTestPhotoFile()
        val storageUploadCompleted = AtomicBoolean(false)
        val expectedStorageUrl = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2FLAP-PHOTO-100.jpg?alt=media&token=abc"

        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = { true }
        FirebaseStoragePhotoUploader.overrideUploaderForTesting = { _, _ ->
            delay(50) // Simulate real async Firebase Storage upload
            storageUploadCompleted.set(true)
            PhotoUploadOutcome.Success(expectedStorageUrl)
        }

        var capturedFirestoreFields: Map<String, Any>? = null
        FirestoreHelper.overrideCitizenReportUploaderForTesting = { pushedReport ->
            assertTrue(
                "Firestore write MUST ONLY happen AFTER Firebase Storage upload completes 100%",
                storageUploadCompleted.get()
            )
            capturedFirestoreFields = FirestoreHelper.buildMutableCitizenReportFields(pushedReport)
            Result.success(Unit)
        }

        val report = CitizenDisasterReport(
            id = "LAP-PHOTO-100",
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 16:20 WIB",
            incidentLocation = "Kates",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.58123,
            longitude = 101.12345,
            gpsAccuracyMeters = 7f,
            isGpsVerified = true,
            casualtiesDead = 0,
            casualtiesInjured = 0,
            casualtiesMissing = 0,
            photoUri = localPhotoUri,
            reporterName = "Warga Uji",
            reporterPhone = "08123456789",
            additionalNotes = "Air setinggi lutut di jalan Kates",
            timestamp = System.currentTimeMillis(),
            status = "Diproses"
        )

        val result = repository.submitCitizenDisasterReportWithResult(report, context)

        assertTrue("Report must be saved locally", result.savedLocally)
        assertTrue("Report must be synced to cloud", result.syncedToCloud)
        assertFalse("Must not be deferred offline", result.deferredOffline)
        assertEquals(expectedStorageUrl, result.cloudPhotoUrl)

        assertNotNull("Firestore fields must be pushed", capturedFirestoreFields)
        assertEquals(expectedStorageUrl, capturedFirestoreFields!!["fotoUrl"])
        assertEquals(expectedStorageUrl, capturedFirestoreFields!!["fotoLampiran"])
        assertEquals(expectedStorageUrl, capturedFirestoreFields!!["photoUri"])

        val resolvedBack = FirestoreHelper.resolvePhotoUriFromDocumentFields(capturedFirestoreFields!!)
        assertEquals(expectedStorageUrl, resolvedBack)
    }

    @Test
    fun submitCitizenReport_whenOffline_neverSendsTextOnly_andSyncsAtomicallyWhenSignalReturns() = runBlocking {
        val localPhotoUri = createSolidTestPhotoFile()
        val firestorePushCalls = AtomicInteger(0)
        val cloudDocs = mutableMapOf<String, CitizenDisasterReport>()

        // Step 1: Device is OFFLINE
        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = { false }
        FirestoreHelper.overrideCitizenReportUploaderForTesting = { pushed ->
            firestorePushCalls.incrementAndGet()
            cloudDocs[pushed.id] = pushed
            Result.success(Unit)
        }
        FirestoreHelper.overrideCitizenReportFetcherForTesting = {
            Result.success(cloudDocs.values.toList())
        }

        val report = CitizenDisasterReport(
            id = "LAP-OFFLINE-ATOMIC-1",
            disasterType = "Longsor",
            incidentTime = "19 Nov 2026 16:30 WIB",
            incidentLocation = "Kates",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            gpsAccuracyMeters = 9f,
            isGpsVerified = true,
            casualtiesDead = 0,
            casualtiesInjured = 1,
            casualtiesMissing = 0,
            photoUri = localPhotoUri,
            reporterName = "Warga Offline",
            reporterPhone = "0812000000",
            additionalNotes = "Tebing longsor menutup akses",
            timestamp = System.currentTimeMillis(),
            status = "Diproses"
        )

        val offlineResult = repository.submitCitizenDisasterReportWithResult(report, context)

        // MUST NOT push text-only report to Firestore while offline!
        assertEquals("Firestore MUST NOT receive a text-only report while offline", 0, firestorePushCalls.get())
        assertTrue("Report must be stored in Room SQLite locally", offlineResult.savedLocally)
        assertFalse("Report must NOT be marked syncedToCloud while offline", offlineResult.syncedToCloud)
        assertTrue("Report must be marked deferredOffline", offlineResult.deferredOffline)
        assertNull("No cloudPhotoUrl while offline", offlineResult.cloudPhotoUrl)

        val localEntity = db.weatherDao().getCitizenReportById(report.id)
        assertNotNull("Local SQLite record must exist", localEntity)
        assertFalse("Local SQLite record must have isSyncedToCloud = false", localEntity!!.isSyncedToCloud)
        assertEquals("Local SQLite record must preserve local photo file path", localPhotoUri, localEntity.photoUri)

        // Step 2: Signal returns! Firebase Storage uploads the local photo 100% and Firestore receives text + fotoUrl together
        val uploadedUrl = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2FLAP-OFFLINE-ATOMIC-1.jpg?alt=media"
        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = { true }
        FirebaseStoragePhotoUploader.overrideUploaderForTesting = { _, _ ->
            PhotoUploadOutcome.Success(uploadedUrl)
        }

        val syncedCount = repository.syncCitizenDisasterReportsOnline()
        assertTrue("Pending offline report must sync once signal returns", syncedCount >= 1)
        assertEquals("Firestore must now be called once with both text + photo", 1, firestorePushCalls.get())

        val pushedDoc = cloudDocs[report.id]
        assertNotNull("Cloud must now contain the report", pushedDoc)
        assertEquals("Cloud report MUST carry the uploaded Firebase Storage URL", uploadedUrl, pushedDoc!!.photoUri)

        val syncedLocalEntity = db.weatherDao().getCitizenReportById(report.id)
        assertTrue("Local Room record must now be marked isSyncedToCloud = true", syncedLocalEntity!!.isSyncedToCloud)
        assertEquals(uploadedUrl, syncedLocalEntity.photoUri)
    }

    @Test
    fun firestoreFieldResolver_prioritizesFotoUrlThenFotoLampiranThenPhotoUri() {
        val withFotoUrl = mapOf(
            "fotoUrl" to "https://firebasestorage.googleapis.com/foto-utama.jpg",
            "photoUri" to "https://legacy.example/old.jpg"
        )
        assertEquals(
            "https://firebasestorage.googleapis.com/foto-utama.jpg",
            FirestoreHelper.resolvePhotoUriFromDocumentFields(withFotoUrl)
        )

        val withFotoLampiranOnly = mapOf(
            "fotoUrl" to "",
            "fotoLampiran" to "https://firebasestorage.googleapis.com/lampiran.jpg"
        )
        assertEquals(
            "https://firebasestorage.googleapis.com/lampiran.jpg",
            FirestoreHelper.resolvePhotoUriFromDocumentFields(withFotoLampiranOnly)
        )

        val withNoPhoto = mapOf<String, Any?>(
            "fotoUrl" to null,
            "fotoLampiran" to "",
            "photoUri" to null
        )
        assertNull(FirestoreHelper.resolvePhotoUriFromDocumentFields(withNoPhoto))
    }

    @Test
    fun reportsRetrievedFromFirestore_alwaysShowSyncedGreen_neverMenungguSync() = runBlocking {
        val now = System.currentTimeMillis()
        // Simulate a stale local row marked isSyncedToCloud = false with slightly skewed clock
        val staleLocal = com.example.data.local.CitizenDisasterReportEntity(
            id = "LAP-SERVER-SYNCED-1",
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 16:45 WIB",
            incidentLocation = "Desa Kates",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            gpsAccuracyMeters = 5f,
            isGpsVerified = true,
            casualtiesDead = 0,
            casualtiesInjured = 0,
            casualtiesMissing = 0,
            photoUri = null,
            reporterName = "Warga A",
            reporterPhone = "08123456",
            additionalNotes = "Air naik",
            timestamp = now,
            status = "Diproses",
            updatedAt = now + 5_000L, // slight local clock skew ahead of server
            isSyncedToCloud = false
        )
        db.weatherDao().insertCitizenDisasterReport(staleLocal)

        val serverReport = CitizenDisasterReport(
            id = "LAP-SERVER-SYNCED-1",
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 16:45 WIB",
            incidentLocation = "Desa Kates",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            gpsAccuracyMeters = 5f,
            isGpsVerified = true,
            photoUri = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2Fok.jpg?alt=media",
            reporterName = "Warga A",
            reporterPhone = "08123456",
            additionalNotes = "Air naik",
            timestamp = now,
            status = "Diproses",
            updatedAt = now,
            isSyncedToCloud = true
        )

        // Realtime Firestore arrival reconciles the report -> MUST become Synced (isSyncedToCloud = true)
        repository.upsertRealtimeCitizenReportsFromCloud(listOf(serverReport))
        val reconciled = db.weatherDao().getCitizenReportById(serverReport.id)
        assertNotNull(reconciled)
        assertTrue("Report already present on Firestore MUST be marked isSyncedToCloud = true", reconciled!!.isSyncedToCloud)

        // UI helper also guarantees reports with cloud Storage URL never display "Menunggu Sync"
        assertTrue(
            "UI sync badge must show Synced for server report",
            com.example.ui.components.isReportSyncedWithServer(serverReport.copy(isSyncedToCloud = false))
        )
    }

    @Test
    fun truncatedOrCorruptedJpeg_returnsNullInsteadOfRenderingGlitchedBitmap() {
        val raw = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(30, 90, 180))
        }
        val validDataUri = ImageUtils.encodeBitmapToCloudDataUri(raw)
        assertNotNull("Valid JPEG Data URI must encode", validDataUri)

        // Valid image decodes cleanly
        val decodedValid = com.example.ui.components.decodeCitizenPhotoModel(validDataUri)
        assertTrue("Valid JPEG Data URI must decode to Bitmap", decodedValid is Bitmap)

        // Truncated JPEG (missing EOI FF D9 marker, which causes half-rendered glitch images on Android)
        val base64Body = validDataUri!!.substringAfter("base64,")
        val truncatedBase64 = base64Body.take((base64Body.length / 2).coerceAtLeast(64))
        val truncatedDataUri = "data:image/jpeg;base64,$truncatedBase64"

        val decodedTruncated = com.example.ui.components.decodeCitizenPhotoModel(truncatedDataUri)
        assertNull(
            "Truncated/corrupted JPEG MUST return null so UI displays clean gray 'Foto tidak dapat dimuat' placeholder",
            decodedTruncated
        )
    }

    @Test
    fun officerPushNotification_onlyFiresOnOfficerSession_andDeduplicatesSameReportId() {
        com.example.data.sync.OfficerPushNotificationHelper.clearNotificationHistoryForTesting(context)
        val newReport = CitizenDisasterReport(
            id = "LAP-MMK-20261119-991",
            disasterType = "Banjir Bandang",
            incidentTime = "19 Nov 2026 17:10 WIB",
            incidentLocation = "Jembatan Sungai Selagan",
            district = "Kota Mukomuko",
            village = "Desa Selagan Jaya",
            latitude = -2.581,
            longitude = 101.123,
            gpsAccuracyMeters = 5f,
            isGpsVerified = true,
            photoUri = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2FLAP-MMK-20261119-991.jpg?alt=media",
            reporterName = "Warga Siaga",
            reporterPhone = "081299998888",
            additionalNotes = "Debit air naik cepat",
            timestamp = System.currentTimeMillis(),
            status = "Diproses",
            isSyncedToCloud = true
        )

        // 1. Citizen-only device (not logged in as Officer/Pusdalops) -> MUST NOT receive officer push notification
        com.example.data.sync.OfficerPushNotificationHelper.overrideOfficerSessionForTesting = false
        val notifiedOnCitizenDevice = com.example.data.sync.OfficerPushNotificationHelper.notifyOfficerForNewReport(
            context = context,
            report = newReport
        )
        assertFalse("Citizen-only device must not receive officer push alert", notifiedOnCitizenDevice)

        // 2. Officer/Pusdalops device -> MUST receive high-priority push notification for new report
        com.example.data.sync.OfficerPushNotificationHelper.overrideOfficerSessionForTesting = true
        val notifiedOnOfficerDevice = com.example.data.sync.OfficerPushNotificationHelper.notifyOfficerForNewReport(
            context = context,
            report = newReport
        )
        assertTrue("Officer/Pusdalops device MUST receive push notification for new citizen report", notifiedOnOfficerDevice)

        // 3. Repeated snapshot for the same report ID -> MUST be deduplicated
        val duplicateNotify = com.example.data.sync.OfficerPushNotificationHelper.notifyOfficerForNewReport(
            context = context,
            report = newReport
        )
        assertFalse("Same report ID must not re-trigger duplicate push notification", duplicateNotify)

        com.example.data.sync.OfficerPushNotificationHelper.overrideOfficerSessionForTesting = null
    }

    @Test
    fun statusTracking_resolvesReportById_andMapsPendingProsesSelesaiStages() {
        val now = System.currentTimeMillis()
        val pendingOfflineReport = CitizenDisasterReport(
            id = "LAP-MMK-20261119-101",
            disasterType = "Pohon Tumbang",
            incidentTime = "19 Nov 2026 17:00 WIB",
            incidentLocation = "Jl. Lintas Barat",
            district = "Ipuh",
            village = "Desa Pasar Ipuh",
            latitude = -3.008,
            longitude = 101.491,
            photoUri = "/data/user/0/com.example/files/citizen_reports/local_101.jpg",
            reporterName = "Warga 1",
            reporterPhone = "0811",
            additionalNotes = "Menunggu sinyal",
            timestamp = now,
            status = "Diproses",
            isSyncedToCloud = false
        )
        val inProgressValidatedReport = pendingOfflineReport.copy(
            id = "LAP-MMK-20261119-202",
            photoUri = "https://firebasestorage.googleapis.com/v0/b/jazzakallah.firebasestorage.app/o/citizen_reports%2F202.jpg?alt=media",
            status = "Divalidasi",
            handledByOfficerName = "Petugas Contoh A",
            handledByOfficerRole = "Operator Pusdalops",
            isSyncedToCloud = true
        )
        val completedReport = inProgressValidatedReport.copy(
            id = "LAP-MMK-20261119-303",
            status = "Selesai"
        )
        val reports = listOf(pendingOfflineReport, inProgressValidatedReport, completedReport)

        // Exact and suffix ID lookup
        assertEquals(
            pendingOfflineReport.id,
            com.example.ui.components.resolveTrackedCitizenReport("LAP-MMK-20261119-101", reports)?.id
        )
        assertEquals(
            inProgressValidatedReport.id,
            com.example.ui.components.resolveTrackedCitizenReport("202", reports)?.id
        )

        // Stage mapping: Pending (0) -> Proses (1) -> Selesai (2)
        val pendingSummary = com.example.ui.components.mapReportToTrackingProgress(pendingOfflineReport)
        assertEquals(0, pendingSummary.stageIndex)
        assertEquals("PENDING", pendingSummary.stageBadgeLabel)

        val prosesSummary = com.example.ui.components.mapReportToTrackingProgress(inProgressValidatedReport)
        assertEquals(1, prosesSummary.stageIndex)
        assertEquals("PROSES", prosesSummary.stageBadgeLabel)

        val selesaiSummary = com.example.ui.components.mapReportToTrackingProgress(completedReport)
        assertEquals(2, selesaiSummary.stageIndex)
        assertEquals("SELESAI", selesaiSummary.stageBadgeLabel)
    }

    @Test
    fun zeroFrictionOfflinePersistence_catchesStorageUploadException_savesLocalPath_andReturnsExactCalmFeedbackMessage() = runBlocking {
        // 1. Verify Firestore Offline Persistence setting is enabled
        FirestoreHelper.ensureOfflinePersistenceEnabled(context)
        assertTrue("Firestore offline persistence must be configured", FirestoreHelper.isOfflinePersistenceConfigured)

        // 2. Simulate photo upload throwing a network IOException mid-upload (must be caught by try-catch!)
        val localPhotoUri = createSolidTestPhotoFile()
        FirebaseStoragePhotoUploader.overrideOnlineCheckForTesting = { true }
        FirebaseStoragePhotoUploader.overrideUploaderForTesting = { _, _ ->
            throw java.io.IOException("Network unreachable during Firebase Storage upload")
        }

        val report = CitizenDisasterReport(
            id = "LAP-ZERO-FRICTION-01",
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 18:05 WIB",
            incidentLocation = "Desa Kates",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            gpsAccuracyMeters = 6f,
            isGpsVerified = true,
            photoUri = localPhotoUri,
            reporterName = "Warga Tenang",
            reporterPhone = "0812001122",
            additionalNotes = "Laporan saat sinyal terputus tiba-tiba",
            timestamp = System.currentTimeMillis(),
            status = "Diproses"
        )

        val submitResult = repository.submitCitizenDisasterReportWithResult(report, context)

        assertTrue("Report must be saved locally on device", submitResult.savedLocally)
        assertTrue("Report must be flagged deferredOffline", submitResult.deferredOffline)
        assertEquals(
            "UI feedback message MUST match exact non-panicking offline string",
            "Laporan tersimpan di perangkat. Akan dikirim otomatis saat sinyal tersedia.",
            submitResult.message
        )

        val savedLocal = db.weatherDao().getCitizenReportById(report.id)
        assertNotNull("Report must exist in local database", savedLocal)
        assertEquals("Local photo path MUST be preserved on device for retry", localPhotoUri, savedLocal!!.photoUri)
    }

    @Test
    fun officerRealtimeAlert_detectsNewPendingReportId_triggersSirenAndVibration_andUpdatesLastReadId() {
        val detector = com.example.ui.components.OfficerRealtimeNewReportDetector()
        com.example.ui.components.OfficerAlarmPlayer.resetCountersForTesting()

        assertEquals(
            "Red banner headline MUST match required emergency text",
            "🚨 LAPORAN WARGA BARU! Segera Cek!",
            com.example.ui.components.OfficerAlarmPlayer.RED_BANNER_HEADLINE
        )

        val baseTime = 1_763_500_000_000L
        val existingPendingReport = CitizenDisasterReport(
            id = "LAP-MMK-EXISTING-001",
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 18:00 WIB",
            incidentLocation = "Desa Pasar",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            reporterName = "Warga Lama",
            reporterPhone = "081100001",
            additionalNotes = "Laporan awal",
            timestamp = baseTime,
            status = "Pending"
        )

        // 1. Initial baseline snapshot from pending stream: records lastReadReportId without firing false alarm
        val firstPass = detector.onIncomingPendingReports(listOf(existingPendingReport))
        assertNull("Initial baseline snapshot must not trigger false alarm", firstPass)
        assertEquals("lastReadReportId must store initial latest pending ID", "LAP-MMK-EXISTING-001", detector.lastReadReportId)

        // 2. New pending report arrives in stream with newer timestamp (ordered descending)
        val newlyArrivedPendingReport = existingPendingReport.copy(
            id = "LAP-MMK-NEW-ALERT-002",
            disasterType = "Tanah Longsor",
            reporterName = "Warga Darurat",
            timestamp = baseTime + 60_000L,
            status = "Pending"
        )
        val detectedNew = detector.onIncomingPendingReports(
            listOf(newlyArrivedPendingReport, existingPendingReport)
        )
        assertNotNull("Detector MUST detect newly arrived pending report ID", detectedNew)
        assertEquals("LAP-MMK-NEW-ALERT-002", detectedNew!!.id)
        assertEquals(
            "lastReadReportId MUST update to the newly arrived report ID",
            "LAP-MMK-NEW-ALERT-002",
            detector.lastReadReportId
        )

        // 3. Trigger alarm & vibration for the newly detected report
        com.example.ui.components.OfficerAlarmPlayer.triggerSirenAndVibration(
            context = context,
            report = detectedNew,
            dispatchSystemNotification = false
        )
        assertEquals("LAP-MMK-NEW-ALERT-002", com.example.ui.components.OfficerAlarmPlayer.lastAlarmTriggeredReportId)
        assertTrue("Alarm trigger count must increment", com.example.ui.components.OfficerAlarmPlayer.alarmTriggerCount >= 1)
        assertTrue("Strong vibration trigger count must increment", com.example.ui.components.OfficerAlarmPlayer.vibrationTriggerCount >= 1)

        // 4. Subsequent emission with the same pending reports must not re-detect as new
        val duplicateCheck = detector.onIncomingPendingReports(
            listOf(newlyArrivedPendingReport, existingPendingReport)
        )
        assertNull("Already seen report ID must not re-trigger new report detection", duplicateCheck)
    }

    @Test
    fun feedbackLoopVerticalStepper_streamsSpecificReportDocument_synchronizesColorsAndFirestoreTimestamp() {
        val reportId = "LAP-MMK-FEEDBACK-20261119-001"
        val createdTime = 1_763_550_000_000L
        var latestStreamedReport: CitizenDisasterReport? = null

        // 1. Attach StreamBuilder Pelacakan on specific document ID (LAP-MMK-...)
        val registration = FirestoreHelper.listenCitizenReportByIdRealtime(
            reportId = reportId,
            onUpdate = { doc ->
                latestStreamedReport = doc
            }
        )
        assertNotNull("Real-time document stream listener must register", registration)
        assertEquals(1, FirestoreHelper.getActiveDocumentStreamListenerCountForTesting(reportId))

        // 2. Stage 1: [1] Laporan Diterima (Status: Pending)
        val stage1Report = CitizenDisasterReport(
            id = reportId,
            disasterType = "Banjir",
            incidentTime = "19 Nov 2026 19:00 WIB",
            incidentLocation = "Desa Pasar",
            district = "Kota Mukomuko",
            village = "Desa Pasar",
            latitude = -2.581,
            longitude = 101.123,
            reporterName = "Warga Pelapor",
            reporterPhone = "081200001",
            timestamp = createdTime,
            updatedAt = createdTime,
            status = "Pending",
            isSyncedToCloud = true
        )
        FirestoreHelper.emitTestRealtimeReportDocumentUpdate(stage1Report)
        assertNotNull("Stream listener must receive Pending document update", latestStreamedReport)

        val stepsAtPending = com.example.ui.components.buildCitizenReportVerticalStepperSteps(latestStreamedReport!!)
        assertEquals(4, stepsAtPending.size)
        assertEquals("[1] Laporan Diterima", stepsAtPending[0].stepTitle)
        assertEquals("Status: Pending", stepsAtPending[0].statusSubtitle)
        assertEquals("[2] Tim Menuju Lokasi", stepsAtPending[1].stepTitle)
        assertEquals("Status: Diproses", stepsAtPending[1].statusSubtitle)
        assertEquals("[3] Penanganan", stepsAtPending[2].stepTitle)
        assertEquals("Status: Divalidasi", stepsAtPending[2].statusSubtitle)
        assertEquals("[4] Selesai", stepsAtPending[3].stepTitle)
        assertEquals("Status: Selesai", stepsAtPending[3].statusSubtitle)

        // Verify Color Synchronization at Stage 1 (Active = Oranye, Unreached = Abu-abu)
        assertEquals(com.example.ui.components.StepperVisualState.ACTIVE, stepsAtPending[0].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_ORANGE_ACTIVE_HEX, stepsAtPending[0].iconColorHex)
        assertEquals(com.example.ui.components.StepperVisualState.UNREACHED, stepsAtPending[1].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GRAY_UNREACHED_HEX, stepsAtPending[1].iconColorHex)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GRAY_UNREACHED_HEX, stepsAtPending[2].iconColorHex)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GRAY_UNREACHED_HEX, stepsAtPending[3].iconColorHex)

        // 3. Officer updates status to "Diproses" ([2] Tim Menuju Lokasi) 5 minutes later
        val diprosesTime = createdTime + (5 * 60 * 1000L)
        val stage2Report = stage1Report.copy(
            status = "Diproses",
            updatedAt = diprosesTime,
            handledByOfficerName = "Budi Santoso, S.Kom"
        )
        FirestoreHelper.emitTestRealtimeReportDocumentUpdate(stage2Report)
        val stepsAtDiproses = com.example.ui.components.buildCitizenReportVerticalStepperSteps(latestStreamedReport!!)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtDiproses[0].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX, stepsAtDiproses[0].iconColorHex)
        assertEquals(com.example.ui.components.StepperVisualState.ACTIVE, stepsAtDiproses[1].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_ORANGE_ACTIVE_HEX, stepsAtDiproses[1].iconColorHex)
        assertEquals(com.example.ui.components.StepperVisualState.UNREACHED, stepsAtDiproses[2].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GRAY_UNREACHED_HEX, stepsAtDiproses[2].iconColorHex)
        assertTrue(
            "Active step MUST display officer update timestamp from Firestore",
            stepsAtDiproses[1].handlingTimeDisplay?.contains("Diubah Petugas:") == true
        )
        assertEquals(
            "Kecepatan Respons: 5 menit sejak laporan diterima",
            stepsAtDiproses[1].responseSpeedDisplay
        )

        // 4. Officer updates status to "Divalidasi" ([3] Penanganan) 12 minutes after creation
        val divalidasiTime = createdTime + (12 * 60 * 1000L)
        val stage3Report = stage2Report.copy(
            status = "Divalidasi",
            updatedAt = divalidasiTime
        )
        FirestoreHelper.emitTestRealtimeReportDocumentUpdate(stage3Report)
        val stepsAtDivalidasi = com.example.ui.components.buildCitizenReportVerticalStepperSteps(latestStreamedReport!!)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtDivalidasi[0].visualState)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtDivalidasi[1].visualState)
        assertEquals(com.example.ui.components.StepperVisualState.ACTIVE, stepsAtDivalidasi[2].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_ORANGE_ACTIVE_HEX, stepsAtDivalidasi[2].iconColorHex)
        assertEquals(com.example.ui.components.StepperVisualState.UNREACHED, stepsAtDivalidasi[3].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GRAY_UNREACHED_HEX, stepsAtDivalidasi[3].iconColorHex)
        assertEquals(
            "Kecepatan Respons: 12 menit sejak laporan diterima",
            stepsAtDivalidasi[2].responseSpeedDisplay
        )

        // 5. Officer updates status to "Selesai" ([4] Selesai) 35 minutes after creation
        val selesaiTime = createdTime + (35 * 60 * 1000L)
        val stage4Report = stage3Report.copy(
            status = "Selesai",
            updatedAt = selesaiTime
        )
        FirestoreHelper.emitTestRealtimeReportDocumentUpdate(stage4Report)
        val stepsAtSelesai = com.example.ui.components.buildCitizenReportVerticalStepperSteps(latestStreamedReport!!)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtSelesai[0].visualState)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtSelesai[1].visualState)
        assertEquals(com.example.ui.components.StepperVisualState.COMPLETED, stepsAtSelesai[2].visualState)
        assertEquals(com.example.ui.components.StepperVisualState.ACTIVE, stepsAtSelesai[3].visualState)
        assertEquals(com.example.ui.components.STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX, stepsAtSelesai[3].iconColorHex)
        assertTrue(
            "Completed step 4 must show Firestore handling timestamp",
            stepsAtSelesai[3].handlingTimeDisplay?.contains("Diubah Petugas:") == true
        )

        // Cleanup listener registration
        registration?.remove()
        assertEquals(0, FirestoreHelper.getActiveDocumentStreamListenerCountForTesting(reportId))
    }
}
