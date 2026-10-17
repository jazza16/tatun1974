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
    val isEarthquakeNotificationEnabled: Boolean = true
)

class WeatherViewModel(
    private val repository: WeatherRepository
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
        viewModelScope.launch {
            repository.seedInitialCommunityReportsIfEmpty()
        }
        loadWeatherForDistrict(MukomukoDistricts.defaultDistrict)
        loadHistoricalWeather(MukomukoDistricts.defaultDistrict, "2024-05-15")
        loadEarlyWarnings(MukomukoDistricts.defaultDistrict)
        loadAllDistrictsSummary()
        loadEarthquakeData()
        startPeriodicEarthquakeMonitoring()
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
}
