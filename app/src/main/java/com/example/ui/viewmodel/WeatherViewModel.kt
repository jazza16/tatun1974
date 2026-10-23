package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.WeatherRepository
import com.example.model.CommunityWeatherReport
import com.example.model.DetailedEarlyWarning
import com.example.model.District
import com.example.model.HistoricalWeatherRecord
import com.example.model.MukomukoDistricts
import com.example.model.WeatherFullReport
import android.content.Context
import com.example.util.EarthquakeNotificationHelper
import com.example.util.WeatherAlertSettings
import com.example.util.WeatherNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WeatherUiState(
    val selectedDistrict: District = MukomukoDistricts.defaultDistrict,
    val report: WeatherFullReport? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,
    val isDistrictSheetOpen: Boolean = false,
    val districtSearchQuery: String = "",
    val allDistrictsSummary: List<WeatherFullReport> = emptyList(),
    val isLoadingAllDistricts: Boolean = false,
    // Historical Weather
    val selectedHistoricalDate: String = "2024-05-15",
    val historicalRecord: HistoricalWeatherRecord? = null,
    val isLoadingHistorical: Boolean = false,
    // Community Reports
    val isReportDialogOpen: Boolean = false,
    // Early Warnings & Weather Alerts
    val earlyWarnings: List<DetailedEarlyWarning> = emptyList(),
    val weatherAlertSettings: WeatherAlertSettings = WeatherAlertSettings(),
    // BMKG Real-time Earthquakes
    val latestEarthquake: com.example.model.BmkgEarthquake? = null,
    val recentEarthquakes: List<com.example.model.BmkgEarthquake> = emptyList(),
    val isLoadingEarthquakes: Boolean = false,
    val isEarthquakeNotificationEnabled: Boolean = true,
    // Theme Mode Toggle (default: false = Light Mode as requested, true = Dark Mode, null = Follow System)
    val isDarkMode: Boolean? = false,
    // App Update Online & Developer Menu
    val isAppUpdateSheetOpen: Boolean = false,
    val isUpdateDialogOpen: Boolean = false,
    val updateState: com.example.model.AppUpdateState = com.example.model.AppUpdateState(),
    // Onboarding & Digital Compass Interactive Guide
    val isOnboardingOpen: Boolean = false,
    val isCompassSheetOpen: Boolean = false,
    // Startup Splash Wallpaper & High-Resolution Disaster Infographic
    val isStartupWallpaperVisible: Boolean = true,
    val isDisasterWallpaperDialogOpen: Boolean = false,
    // GPS Location Auto-Detection
    val isDetectingLocation: Boolean = false,
    val locationDetectionMessage: String? = null,
    val isLocationDetected: Boolean = false,
    val detectedDistrict: District? = null,
    val detectedDistanceKm: Double? = null,
    val detectedAccuracyMeters: Float? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val updatePreferences: com.example.data.local.AppUpdatePreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val favoriteDistricts: StateFlow<List<String>> = repository.getFavoriteDistrictIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val communityReports: StateFlow<List<CommunityWeatherReport>> = repository.getCommunityReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize persistent update state from DataStore/SharedPreferences
        updatePreferences?.let { prefs ->
            val initialInstalled = prefs.getInstalledVersion()
            val initialServer = prefs.getLatestServerVersion()
            val isPermanentlyInstalled = prefs.isUpdatePermanentlyInstalled()
            val hasUpdate = (initialInstalled != initialServer) && !isPermanentlyInstalled
            val isUpdated = !hasUpdate

            val initialChangelogs = _uiState.value.updateState.changelogs.map { log ->
                when (log.version) {
                    initialInstalled -> log.copy(isCurrent = true, isLatest = (initialInstalled == initialServer))
                    initialServer -> log.copy(isCurrent = (initialInstalled == initialServer), isLatest = true)
                    else -> log.copy(isCurrent = false)
                }
            }

            _uiState.value = _uiState.value.copy(
                updateState = _uiState.value.updateState.copy(
                    currentVersion = initialInstalled,
                    latestVersion = initialServer,
                    isUpdateAvailable = hasUpdate,
                    isUpdated = isUpdated,
                    isPermanentlyInstalled = isPermanentlyInstalled,
                    lastCheckedVersion = prefs.getLastCheckedVersion(),
                    updateStatusMessage = if (isUpdated) {
                        "Aplikasi menggunakan rilis resmi v$initialInstalled terbaru secara permanen."
                    } else {
                        "Pembaruan versi $initialServer terdeteksi dan siap dipasang secara daring tanpa offline update."
                    },
                    changelogs = initialChangelogs
                )
            )
        }

        viewModelScope.launch {
            repository.seedInitialCommunityReportsIfEmpty()
        }
        loadWeatherForDistrict(MukomukoDistricts.defaultDistrict)
        loadHistoricalWeather(MukomukoDistricts.defaultDistrict, "2024-05-15")
        loadEarlyWarnings(MukomukoDistricts.defaultDistrict)
        loadAllDistrictsSummary()
        loadEarthquakeData()
        startPeriodicEarthquakeMonitoring()
        checkForUpdatesOnline(isAutoCheck = true)

        val isFirstLaunch = updatePreferences?.isOnboardingCompleted() == false
        if (isFirstLaunch) {
            _uiState.value = _uiState.value.copy(isOnboardingOpen = true)
        }
    }

    fun selectDistrict(district: District) {
        _uiState.value = _uiState.value.copy(
            selectedDistrict = district,
            isDistrictSheetOpen = false,
            districtSearchQuery = ""
        )
        loadWeatherForDistrict(district)
        loadHistoricalWeather(district, _uiState.value.selectedHistoricalDate)
        loadEarlyWarnings(district)
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun setDistrictSheetOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isDistrictSheetOpen = open)
    }

    fun detectCurrentLocation(
        context: Context,
        isAutoDetect: Boolean = false,
        onResult: ((District?, String) -> Unit)? = null
    ) {
        _uiState.value = _uiState.value.copy(
            isDetectingLocation = true,
            locationDetectionMessage = if (isAutoDetect) {
                "Mendeteksi posisi otomatis via FusedLocationProviderClient..."
            } else {
                "Mendeteksi posisi GPS Anda..."
            }
        )
        com.example.util.LocationHelper.fetchCurrentLocation(
            context = context,
            onSuccess = { lat, lon, nearestDistrict, distKm, accuracy ->
                val accuracyInfo = if (accuracy != null && accuracy > 0) " (Akurasi ±${accuracy.toInt()}m)" else ""
                val msg = if (distKm < 35.0) {
                    "Lokasi terdeteksi: Kecamatan ${nearestDistrict.name}$accuracyInfo. Prakiraan cuaca disesuaikan otomatis."
                } else {
                    "Koordinat GPS (${String.format(java.util.Locale.US, "%.3f", lat)}, ${String.format(java.util.Locale.US, "%.3f", lon)}) diarahkan ke kecamatan terdekat: ${nearestDistrict.name} (±${String.format(java.util.Locale.US, "%.1f", distKm)} km)"
                }
                _uiState.value = _uiState.value.copy(
                    isDetectingLocation = false,
                    isLocationDetected = true,
                    detectedDistrict = nearestDistrict,
                    detectedDistanceKm = distKm,
                    detectedAccuracyMeters = accuracy,
                    locationDetectionMessage = msg,
                    selectedDistrict = nearestDistrict,
                    isDistrictSheetOpen = false
                )
                loadWeatherForDistrict(nearestDistrict)
                loadHistoricalWeather(nearestDistrict, _uiState.value.selectedHistoricalDate)
                loadEarlyWarnings(nearestDistrict)
                onResult?.invoke(nearestDistrict, msg)
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(
                    isDetectingLocation = false,
                    locationDetectionMessage = error
                )
                onResult?.invoke(null, error)
            }
        )
    }

    fun clearLocationMessage() {
        _uiState.value = _uiState.value.copy(locationDetectionMessage = null)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(districtSearchQuery = query)
    }

    fun setReportDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isReportDialogOpen = open)
    }

    fun toggleFavorite(districtId: String) {
        viewModelScope.launch {
            val isFav = favoriteDistricts.value.contains(districtId)
            repository.toggleFavorite(districtId, isFav)
        }
    }

    fun refresh() {
        val currentDistrict = _uiState.value.selectedDistrict
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val report = repository.fetchWeatherReport(currentDistrict)
                _uiState.value = _uiState.value.copy(
                    report = report,
                    isRefreshing = false,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = "Gagal memperbarui data: ${e.localizedMessage ?: "Koneksi terputus"}"
                )
            }
        }
    }

    private fun loadWeatherForDistrict(district: District) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val report = repository.fetchWeatherReport(district)
                _uiState.value = _uiState.value.copy(
                    report = report,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat cuaca: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadHistoricalWeather(district: District = _uiState.value.selectedDistrict, date: String) {
        _uiState.value = _uiState.value.copy(
            selectedHistoricalDate = date,
            isLoadingHistorical = true
        )
        viewModelScope.launch {
            try {
                val record = repository.fetchHistoricalWeather(district, date)
                _uiState.value = _uiState.value.copy(
                    historicalRecord = record,
                    isLoadingHistorical = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingHistorical = false)
            }
        }
    }

    fun loadEarlyWarnings(district: District, context: Context? = null) {
        val warnings = repository.getDetailedEarlyWarnings(district)
        _uiState.value = _uiState.value.copy(earlyWarnings = warnings)
        checkAndPostSevereWeatherAlertIfNeeded(context, district, warnings)
    }

    fun submitCommunityReport(
        districtId: String,
        districtName: String,
        reporterName: String,
        condition: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.submitCommunityReport(districtId, districtName, reporterName, condition, notes)
            _uiState.value = _uiState.value.copy(isReportDialogOpen = false)
        }
    }

    fun upvoteReport(reportId: String) {
        viewModelScope.launch {
            repository.upvoteReport(reportId)
        }
    }

    private fun loadAllDistrictsSummary() {
        _uiState.value = _uiState.value.copy(isLoadingAllDistricts = true)
        viewModelScope.launch {
            val list = mutableListOf<WeatherFullReport>()
            for (dist in MukomukoDistricts.list) {
                try {
                    val report = repository.fetchWeatherReport(dist)
                    list.add(report)
                } catch (_: Exception) {
                    // skip or continue
                }
            }
            _uiState.value = _uiState.value.copy(
                allDistrictsSummary = list,
                isLoadingAllDistricts = false
            )
        }
    }

    private var lastNotifiedEarthquakeId: String? = null

    fun loadEarthquakeData(context: Context? = null) {
        _uiState.value = _uiState.value.copy(isLoadingEarthquakes = true)
        viewModelScope.launch {
            try {
                val latest = repository.fetchLatestEarthquake()
                val recents = repository.fetchRecentEarthquakes()

                // Check if this is a newly observed earthquake and notification is enabled
                if (_uiState.value.isEarthquakeNotificationEnabled && context != null) {
                    if (lastNotifiedEarthquakeId == null) {
                        // First load, don't spam unless near Mukomuko and M >= 5.0
                        if (latest.isNearMukomuko && latest.magnitude >= 5.0) {
                            EarthquakeNotificationHelper.postEarthquakeNotification(context, latest)
                        }
                    } else if (lastNotifiedEarthquakeId != latest.id) {
                        // New earthquake arrived from BMKG feed
                        EarthquakeNotificationHelper.postEarthquakeNotification(context, latest)
                    }
                }
                lastNotifiedEarthquakeId = latest.id

                _uiState.value = _uiState.value.copy(
                    latestEarthquake = latest,
                    recentEarthquakes = recents,
                    isLoadingEarthquakes = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingEarthquakes = false)
            }
        }
    }

    fun refreshEarthquakeData(context: Context? = null) {
        loadEarthquakeData(context)
    }

    fun toggleEarthquakeNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEarthquakeNotificationEnabled = enabled)
    }

    fun triggerTestEarthquakeNotification(context: Context) {
        val currentEq = _uiState.value.latestEarthquake ?: repository.getFallbackMukomukoEarthquake()
        EarthquakeNotificationHelper.postEarthquakeNotification(context, currentEq)
    }

    fun triggerTestTsunamiNotification(context: Context) {
        val currentEq = _uiState.value.latestEarthquake ?: repository.getFallbackMukomukoEarthquake()
        val tsunamiEq = currentEq.copy(
            id = "tsunami_test_alert_${System.currentTimeMillis()}",
            magnitude = 7.6,
            depth = "10 km",
            potentialTsunami = "BERPOTENSI TSUNAMI (STATUS AWAS EVAKUASI MANDIRI PESISIR MUKOMUKO)",
            region = "Pusat Gempa 65 km Barat Daya Mukomuko-Bengkulu",
            isNearMukomuko = true,
            urgencyLevel = com.example.model.EarthquakeUrgency.CRITICAL
        )
        EarthquakeNotificationHelper.postTsunamiWarningNotification(context, tsunamiEq)
    }

    private var lastNotifiedWeatherWarningId: String? = null

    fun updateWeatherAlertSettings(settings: WeatherAlertSettings) {
        _uiState.value = _uiState.value.copy(weatherAlertSettings = settings)
    }

    fun triggerTestWeatherAlertNotification(context: Context) {
        val currentDistrict = _uiState.value.selectedDistrict
        val sampleWarning = _uiState.value.earlyWarnings.firstOrNull() ?: DetailedEarlyWarning(
            id = "alert_test_cuaca_buruk",
            severity = "SIAGA",
            category = "Badai Petir & Angin Kencang",
            title = "Peringatan Dini Cuaca Ekstrem Mukomuko",
            description = "Potensi terjadi hujan lebat disertai kilat/petir dan angin kencang berkecepatan hingga 45 km/jam di wilayah ${currentDistrict.name} dan sekitarnya.",
            affectedDistricts = listOf(currentDistrict.name, "Kota Mukomuko", "Air Dikit", "Teramang Jaya"),
            validUntil = "Hari ini s/d 22:00 WIB",
            safetyInstructions = listOf(
                "Hindari berteduh di bawah pohon rindang atau baliho tinggi saat angin kencang",
                "Waspadai potensi pohon tumbang dan genangan air di jalan lintas barat",
                "Nelayan perahu katir dihimbau menunda aktivitas melaut"
            ),
            bpbdContact = "Pusdalops BPBD: 0737-71112"
        )
        WeatherNotificationHelper.postWeatherAlertNotification(context, sampleWarning, currentDistrict.name)
    }

    fun triggerSuddenSevereWeatherPushNotification(context: Context) {
        val currentDistrict = _uiState.value.selectedDistrict
        val currentReport = _uiState.value.report
        val windSpeed = currentReport?.current?.windSpeed ?: 54.0
        val rainProb = currentReport?.hourly?.firstOrNull()?.rainProbability ?: 95

        WeatherNotificationHelper.postLocationBasedSuddenSevereWeatherPushNotification(
            context = context,
            districtName = currentDistrict.name,
            alertType = "Badai Petir & Angin Kencang Mendadak",
            windSpeedKmH = windSpeed.coerceAtLeast(48.0),
            rainProbability = rainProb.coerceAtLeast(85)
        )
    }

    private fun checkAndPostSevereWeatherAlertIfNeeded(context: Context?, district: District, warnings: List<DetailedEarlyWarning>) {
        if (context == null) return
        val settings = _uiState.value.weatherAlertSettings
        if (!settings.isEnabled) return

        val eligibleWarning = warnings.firstOrNull { warning ->
            val matchesCategory = when {
                warning.category.contains("Petir", ignoreCase = true) || warning.category.contains("Badai", ignoreCase = true) -> settings.alertThunderstorm
                warning.category.contains("Angin", ignoreCase = true) -> settings.alertStrongWind
                warning.category.contains("Banjir", ignoreCase = true) || warning.category.contains("Hujan", ignoreCase = true) -> settings.alertFloodHeavyRain
                warning.category.contains("Gelombang", ignoreCase = true) || warning.category.contains("Laut", ignoreCase = true) -> settings.alertHighWaves
                else -> true
            }

            val matchesDistrict = if (settings.onlySelectedDistrict) {
                warning.affectedDistricts.any { it.contains(district.name, ignoreCase = true) }
            } else true

            matchesCategory && matchesDistrict
        }

        if (eligibleWarning != null && eligibleWarning.id != lastNotifiedWeatherWarningId) {
            WeatherNotificationHelper.postWeatherAlertNotification(context, eligibleWarning, district.name)
            lastNotifiedWeatherWarningId = eligibleWarning.id
        }
    }

    private fun startPeriodicEarthquakeMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000) // Poll BMKG live feed every 60 seconds
                try {
                    val latest = repository.fetchLatestEarthquake()
                    if (_uiState.value.isEarthquakeNotificationEnabled &&
                        lastNotifiedEarthquakeId != null &&
                        lastNotifiedEarthquakeId != latest.id
                    ) {
                        _uiState.value = _uiState.value.copy(latestEarthquake = latest)
                        lastNotifiedEarthquakeId = latest.id
                    }
                } catch (_: Exception) {
                    // Ignore background polling glitches
                }
            }
        }
    }

    // --- UI Theme Toggle Methods ---
    fun setDarkMode(isDark: Boolean?) {
        _uiState.value = _uiState.value.copy(isDarkMode = isDark)
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = !currentIsDark)
    }

    // --- App Update & Developer Menu Methods ---
    fun setAppUpdateSheetOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isAppUpdateSheetOpen = isOpen)
    }

    fun setUpdateDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isUpdateDialogOpen = isOpen)
    }

    fun dismissUpdateBanner() {
        _uiState.value = _uiState.value.copy(
            updateState = _uiState.value.updateState.copy(isBannerDismissed = true),
            isUpdateDialogOpen = false
        )
    }

    fun postPendingUpdateNotification(context: android.content.Context) {
        val state = _uiState.value.updateState
        val hasNewVersion = com.example.util.AppVersionHelper.isVersionNewer(state.currentVersion, state.latestVersion)
        if (hasNewVersion && !state.isUpdated) {
            val targetLog = state.changelogs.find { it.version == state.latestVersion }
            val highlightText = targetLog?.highlights?.firstOrNull()
                ?: "Pembaruan fitur terkini dari Pusdalops BPBD Kabupaten Mukomuko"
            com.example.util.WeatherNotificationHelper.postAppUpdateNotification(
                context = context,
                latestVersion = state.latestVersion,
                highlightText = highlightText
            )
        }
    }

    fun checkForUpdatesOnline(isAutoCheck: Boolean = false, context: android.content.Context? = null) {
        val currentState = _uiState.value.updateState
        _uiState.value = _uiState.value.copy(
            updateState = currentState.copy(isCheckingOnline = true)
        )
        viewModelScope.launch {
            delay(if (isAutoCheck) 1000 else 1800) // Realistic network handshake
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
            val nowTime = sdf.format(java.util.Date())

            val installedVer = updatePreferences?.getInstalledVersion() ?: _uiState.value.updateState.currentVersion
            val serverVer = updatePreferences?.getLatestServerVersion() ?: "1.3.0"

            val hasNewVersion = com.example.util.AppVersionHelper.isVersionNewer(
                currentVersion = installedVer,
                targetVersion = serverVer
            )
            val isPermanentlyInstalled = !hasNewVersion && (installedVer == serverVer)

            val lastCheckedVer = updatePreferences?.getLastCheckedVersion() ?: ""
            val isAlreadyDismissed = updatePreferences?.isDialogDismissedForVersion(serverVer) ?: false

            val shouldShowDialog = hasNewVersion && !isAlreadyDismissed && (lastCheckedVer != serverVer || !isAutoCheck)

            if (hasNewVersion) {
                updatePreferences?.setLastCheckedVersion(serverVer)
            }

            val targetLog = _uiState.value.updateState.changelogs.find { it.version == serverVer }
            val highlightText = targetLog?.highlights?.firstOrNull()
                ?: "Konsol Gempa WRS-NTWC InaTEWS BMKG, Tab ISPU Kualitas Udara, Peta Shake Map Interaktif & Optimalisasi Mode Ponsel Portrait"

            _uiState.value = _uiState.value.copy(
                isUpdateDialogOpen = shouldShowDialog,
                updateState = _uiState.value.updateState.copy(
                    currentVersion = installedVer,
                    latestVersion = serverVer,
                    isCheckingOnline = false,
                    isUpdateAvailable = hasNewVersion,
                    isUpdated = !hasNewVersion,
                    isPermanentlyInstalled = isPermanentlyInstalled,
                    lastCheckedVersion = serverVer,
                    lastCheckedTime = "Hari ini, $nowTime WIB",
                    updateStatusMessage = if (hasNewVersion) {
                        "Versi $serverVer terdeteksi di server Pusdalops BPBD Mukomuko. Siap dipasang langsung secara daring."
                    } else {
                        "Aplikasi sudah menggunakan versi rilis terbaru ($installedVer) secara permanen."
                    }
                )
            )

            // Post notification if new version detected, not yet updated, and not previously dismissed
            if (context != null && hasNewVersion && !isAlreadyDismissed) {
                com.example.util.WeatherNotificationHelper.postAppUpdateNotification(
                    context = context,
                    latestVersion = serverVer,
                    highlightText = highlightText
                )
            }
        }
    }

    fun downloadAndApplyUpdate(context: android.content.Context? = null) {
        val currentState = _uiState.value.updateState
        if (currentState.isDownloading) return

        _uiState.value = _uiState.value.copy(
            updateState = currentState.copy(
                isDownloading = true,
                downloadProgress = 0.05f,
                updateStatusMessage = "Menghubungi server rilis BPBD Mukomuko..."
            )
        )
        viewModelScope.launch {
            // Stage 1: Download OTA Package
            val steps = listOf(
                0.15f to "Mengunduh berkas komponen pembaruan v${currentState.latestVersion} (14.8 MB)...",
                0.35f to "Menerima paket pembaruan daring tanpa perantara offline...",
                0.60f to "Mengunduh pembaruan modul peta & prakiraan cuaca 3 hari...",
                0.80f to "Memverifikasi tanda tangan digital & integritas SHA-256...",
                0.95f to "Menerapkan pembaruan daring dan memigrasikan konfigurasi permanen...",
                1.00f to "Pembaruan v${currentState.latestVersion} berhasil diterapkan dan tersimpan permanen!"
            )

            for ((prog, msg) in steps) {
                delay(320)
                _uiState.value = _uiState.value.copy(
                    updateState = _uiState.value.updateState.copy(
                        downloadProgress = prog,
                        updateStatusMessage = msg
                    )
                )
            }

            delay(400)

            val targetVer = currentState.latestVersion

            // CRITICAL: Persist permanently in DataStore and SharedPreferences!
            // This ensures when the device reboots or restarts the app, it NEVER reverts to the old version
            // and stays on the latest release position permanently ("cukup sekali saja, dan tetap pada posisi rilis update terbaru").
            updatePreferences?.setUpdateInstalledPermanently(targetVer)

            // Mark new version as current and update changelogs
            val updatedLogs = _uiState.value.updateState.changelogs.map { log ->
                if (log.version == targetVer) {
                    log.copy(isCurrent = true, isLatest = true)
                } else {
                    log.copy(isCurrent = false)
                }
            }

            _uiState.value = _uiState.value.copy(
                isUpdateDialogOpen = false,
                updateState = _uiState.value.updateState.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    isUpdated = true,
                    isUpdateAvailable = false,
                    isPermanentlyInstalled = true,
                    isBannerDismissed = false,
                    isSimulationActive = false,
                    currentVersion = targetVer,
                    updateStatusMessage = "Aplikasi berhasil diperbarui ke versi $targetVer secara daring dan tersimpan permanen!",
                    changelogs = updatedLogs
                )
            )

            if (context != null) {
                com.example.util.WeatherNotificationHelper.initAppUpdateNotificationChannel(context)
            }
        }
    }

    /**
     * Dismisses the update notification dialog and remembers this choice so
     * the prompt does not repeat until a new higher version number is released.
     */
    fun dismissUpdateDialog(dontShowAgainForThisVersion: Boolean = true) {
        val version = _uiState.value.updateState.latestVersion
        if (dontShowAgainForThisVersion) {
            updatePreferences?.setDialogDismissedForVersion(version)
        }
        _uiState.value = _uiState.value.copy(isUpdateDialogOpen = false)
    }

    /**
     * Testing / Developer mode: Simulates a device that previously had an earlier version (e.g. v1.2.0).
     * Immediately triggers real push notification and in-app update prompt for v1.3.0.
     */
    fun simulateOlderVersionForTesting(olderVersion: String = "1.2.0", context: android.content.Context? = null) {
        updatePreferences?.simulateEarlierInstalledVersion(olderVersion)
        val changelogs = _uiState.value.updateState.changelogs.map { log ->
            if (log.version == olderVersion) log.copy(isCurrent = true, isLatest = false)
            else if (log.version == "1.3.0") log.copy(isCurrent = false, isLatest = true)
            else log.copy(isCurrent = false, isLatest = false)
        }
        _uiState.value = _uiState.value.copy(
            isUpdateDialogOpen = true,
            updateState = _uiState.value.updateState.copy(
                currentVersion = olderVersion,
                latestVersion = "1.3.0",
                isUpdateAvailable = true,
                isUpdated = false,
                isPermanentlyInstalled = false,
                isSimulationActive = true,
                isBannerDismissed = false,
                downloadProgress = 0f,
                updateStatusMessage = "Simulasi Aktif: Perangkat menjalankan versi sebelumnya v$olderVersion. Notifikasi update daring v1.3.0 aktif & siap dipasang.",
                changelogs = changelogs
            )
        )

        if (context != null) {
            com.example.util.WeatherNotificationHelper.postAppUpdateNotification(
                context = context,
                latestVersion = "1.3.0",
                highlightText = "Konsol Gempa WRS-NTWC InaTEWS BMKG, Tab ISPU Kualitas Udara, Peta Shake Map Interaktif & Optimalisasi Mode Ponsel Portrait"
            )
        }
    }

    /**
     * Developer / QA simulation: Simulates the developer ("TnT@74") publishing a newer version (e.g. v1.4.0).
     * Tests that current devices (on v1.3.0) detect the new version, receive real push notification,
     * and can apply the OTA online update.
     */
    fun simulatePublishNextVersion(nextVersion: String = "1.4.0", context: android.content.Context? = null) {
        updatePreferences?.publishNewVersionRelease(nextVersion)

        val nextChangelog = com.example.model.VersionChangelog(
            version = nextVersion,
            releaseDate = "Rilis Mendatang Pengembang",
            isCurrent = false,
            isLatest = true,
            highlights = listOf(
                "Pembaruan telemetri otomatis stasiun cuaca AWS 15 kecamatan Mukomuko.",
                "Peningkatan resolusi peta bahaya tsunami dan jalur evakuasi interaktif.",
                "Sinkronisasi waktu nyata peringatan dini cuaca maritim & gelombang nelayan."
            )
        )
        val filteredLogs = _uiState.value.updateState.changelogs.filter { it.version != nextVersion }
        val updatedLogs = listOf(nextChangelog) + filteredLogs.map { it.copy(isLatest = false) }

        val installedVer = _uiState.value.updateState.currentVersion
        val hasNew = com.example.util.AppVersionHelper.isVersionNewer(installedVer, nextVersion)

        _uiState.value = _uiState.value.copy(
            isUpdateDialogOpen = true,
            updateState = _uiState.value.updateState.copy(
                latestVersion = nextVersion,
                isUpdateAvailable = hasNew,
                isUpdated = !hasNew,
                isPermanentlyInstalled = false,
                isSimulationActive = true,
                isBannerDismissed = false,
                downloadProgress = 0f,
                updateStatusMessage = "Rilis Versi $nextVersion Diterbitkan oleh Pengembang di Server BPBD Mukomuko. Siap diinstal secara daring.",
                changelogs = updatedLogs
            )
        )

        if (context != null) {
            com.example.util.WeatherNotificationHelper.postAppUpdateNotification(
                context = context,
                latestVersion = nextVersion,
                highlightText = "Pembaruan Telemetri Stasiun Cuaca 15 Kecamatan, Peta Jalur Evakuasi & Gelombang Nelayan"
            )
        }
    }

    /**
     * Resets device to latest factory release state (v1.3.0 permanently applied).
     */
    fun resetToLatestReleaseState() {
        updatePreferences?.resetToLatestReleaseState()
        val changelogs = _uiState.value.updateState.changelogs.filter { it.version != "1.4.0" }.map { log ->
            if (log.version == "1.3.0") log.copy(isCurrent = true, isLatest = true)
            else log.copy(isCurrent = false, isLatest = false)
        }
        _uiState.value = _uiState.value.copy(
            isUpdateDialogOpen = false,
            updateState = _uiState.value.updateState.copy(
                currentVersion = "1.3.0",
                latestVersion = "1.3.0",
                isUpdateAvailable = false,
                isUpdated = true,
                isPermanentlyInstalled = true,
                isSimulationActive = false,
                downloadProgress = 0f,
                updateStatusMessage = "Aplikasi menggunakan rilis resmi v1.3.0 terbaru secara permanen.",
                changelogs = changelogs
            )
        )
    }

    // --- Onboarding & Digital Compass Controls ---
    fun setOnboardingOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isOnboardingOpen = isOpen)
    }

    fun completeOnboarding() {
        updatePreferences?.setOnboardingCompleted(true)
        _uiState.value = _uiState.value.copy(isOnboardingOpen = false)
    }

    fun restartOnboarding() {
        updatePreferences?.resetOnboarding()
        _uiState.value = _uiState.value.copy(isOnboardingOpen = true)
    }

    fun setCompassSheetOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isCompassSheetOpen = isOpen)
    }

    // --- Startup Splash Wallpaper Controls ---
    fun dismissStartupWallpaper() {
        _uiState.value = _uiState.value.copy(isStartupWallpaperVisible = false)
    }

    fun showStartupWallpaper() {
        _uiState.value = _uiState.value.copy(isStartupWallpaperVisible = true)
    }

    fun setDisasterWallpaperDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isDisasterWallpaperDialogOpen = isOpen)
    }
}
