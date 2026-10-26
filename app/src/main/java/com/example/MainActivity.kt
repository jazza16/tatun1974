package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.components.AppUpdatePromptDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import com.example.data.local.AppDatabase
import com.example.data.repository.WeatherRepository
import com.example.model.CurrentWeather
import com.example.model.DailyItem
import com.example.model.WeatherFullReport
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.example.ui.components.AirQualityAndUvCard
import com.example.ui.components.AllDistrictsView
import com.example.ui.components.AppUpdateBottomSheet
import com.example.ui.components.CallCenter117DetailBottomSheet
import com.example.ui.components.CitizenReportScreen
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.DeveloperInfoCard
import com.example.ui.components.DistrictSelectorBottomSheet
import com.example.ui.components.EarlyWarningSection
import com.example.ui.components.EarthquakeEvacuationPreparednessSection
import com.example.ui.components.EmergencyDialer
import com.example.ui.components.EmergencyWarningPopupDialog
import com.example.ui.components.HeroWeatherCard
import com.example.ui.components.HistoricalWeatherSection
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.SipagarKitaAcronymCard
import com.example.ui.components.InAppUpdateBannerCard
import com.example.ui.components.InAppUpdateSuccessCard
import com.example.ui.components.InaRiskBnpbSection
import com.example.ui.components.InteractiveDisasterMapSection
import com.example.ui.components.InteractiveTemperatureMapCard
import com.example.ui.components.MarinePesisirCard
import com.example.ui.components.MarineWaveHistoryChart
import com.example.ui.components.QuickActionsFab
import com.example.ui.components.StartupSplashWallpaperScreen
import com.example.ui.components.ThreeDayForecastCard
import com.example.ui.components.WeatherTabs
import com.example.ui.components.WeatherTopBar
import com.example.ui.components.WeeklyRainfallBarChartCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.viewmodel.AiWeatherViewModel
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BMKG Earthquake and Severe Weather Notification Channels
        com.example.util.EarthquakeNotificationHelper.initNotificationChannel(this)
        com.example.util.WeatherNotificationHelper.initNotificationChannel(this)
        com.example.util.WeatherNotificationHelper.initAppUpdateNotificationChannel(this)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = AppDatabase.getInstance(context)
            val repository = WeatherRepository(database.weatherDao())
            val updatePreferences = remember { com.example.data.local.AppUpdatePreferences(context.applicationContext) }
            val weatherViewModel: WeatherViewModel = viewModel {
                WeatherViewModel(repository, updatePreferences)
            }
            val aiViewModel: AiWeatherViewModel = viewModel()
            val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.isDarkMode ?: false

            MyApplicationTheme(darkTheme = isDark) {
                WeatherAppScreen(
                    viewModel = weatherViewModel,
                    aiViewModel = aiViewModel
                )
            }
        }
    }
}

@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel,
    aiViewModel: AiWeatherViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDistricts.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val citizenDisasterReports by viewModel.citizenDisasterReports.collectAsStateWithLifecycle()

    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val aiChatMessages by aiViewModel.chatMessages.collectAsStateWithLifecycle()

    var isCallCenter117SheetOpen by remember { mutableStateOf(false) }
    var isQuickActionsFabExpanded by remember { mutableStateOf(false) }
    var isEmergencyPopupDismissed by remember { mutableStateOf(false) }

    val currentDistrict = uiState.selectedDistrict
    val isFavorite = favorites.contains(currentDistrict.id)

    // Handle incoming navigation from BMKG earthquake or App Update notifications
    val initialTab = remember {
        (context as? android.app.Activity)?.intent?.getIntExtra("EXTRA_NAVIGATE_TAB", -1) ?: -1
    }
    val shouldOpenUpdate = remember {
        (context as? android.app.Activity)?.intent?.getBooleanExtra("EXTRA_OPEN_UPDATE_SHEET", false) ?: false
    }
    LaunchedEffect(initialTab, shouldOpenUpdate) {
        if (initialTab >= 0) {
            viewModel.selectTab(initialTab)
        }
        if (shouldOpenUpdate) {
            viewModel.setAppUpdateSheetOpen(true)
        }
        // Seamlessly check for newest version online on startup and notify
        viewModel.checkForUpdatesOnline(isAutoCheck = true, context = context)
    }

    val isActuallyDark = uiState.isDarkMode ?: false

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectCurrentLocation(context)
        }
    }

    val triggerLocationDetection: () -> Unit = {
        if (com.example.util.LocationHelper.hasLocationPermission(context)) {
            viewModel.detectCurrentLocation(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WeatherTopBar(
                district = currentDistrict,
                isFavorite = isFavorite,
                isRefreshing = uiState.isRefreshing,
                isDarkMode = isActuallyDark,
                isUpdateAvailable = uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated,
                isDetectingLocation = uiState.isDetectingLocation,
                onDistrictClick = { viewModel.setDistrictSheetOpen(true) },
                onFavoriteToggle = { viewModel.toggleFavorite(currentDistrict.id) },
                onRefreshClick = { viewModel.refresh() },
                onThemeToggle = { viewModel.toggleDarkMode(isActuallyDark) },
                onOpenAppMenu = { viewModel.setAppUpdateSheetOpen(true) },
                onCallCenter117Click = { isCallCenter117SheetOpen = true },
                onDetectLocationClick = triggerLocationDetection
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // Horizontal Tab Navigation
            WeatherTabs(
                selectedTabIndex = uiState.selectedTab,
                warningCount = uiState.earlyWarnings.size,
                reportCount = citizenDisasterReports.size,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Location detection notification banner if any
            if (uiState.locationDetectionMessage != null) {
                Surface(
                    color = if (uiState.isLocationDetected) WeatherGoodGreen.copy(alpha = 0.12f) else OrangePrimary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (uiState.isLocationDetected) WeatherGoodGreen else OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.locationDetectionMessage ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearLocationMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Error notice banner if any
            if (uiState.errorMessage != null && uiState.report == null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Coba Lagi", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Main Tab Content
            if (uiState.isLoading && uiState.report == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OrangePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Memuat data cuaca Kabupaten Mukomuko...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        val duration = 320
                        val slideDistanceFraction = 0.22f
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) { width -> (width * slideDistanceFraction).toInt() } +
                             fadeIn(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(duration - 60, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { width -> (-width * slideDistanceFraction).toInt() } +
                                    fadeOut(animationSpec = tween(duration - 60))
                                )
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) { width -> (-width * slideDistanceFraction).toInt() } +
                             fadeIn(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(duration - 60, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { width -> (width * slideDistanceFraction).toInt() } +
                                    fadeOut(animationSpec = tween(duration - 60))
                                )
                        }
                    },
                    label = "tabModuleTransition",
                    modifier = Modifier.fillMaxSize()
                ) { currentTabIndex ->
                    when (currentTabIndex) {
                        0 -> {
                            // Tab 0: Hari Ini (Maximized Mobile Phone Portrait Single-Column Dashboard)
                            val report = uiState.report
                            if (report != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("weather_content_list"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Active Severe Weather Alert if available
                                    if (report.alert != null) {
                                        item {
                                            ActiveAlertCard(
                                                alert = report.alert,
                                                onViewDetail = { viewModel.selectTab(3) }
                                            )
                                        }
                                    }

                                    // In-App OTA Update Available Banner
                                    if (uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated && !uiState.updateState.isBannerDismissed) {
                                        item {
                                            InAppUpdateBannerCard(
                                                updateState = uiState.updateState,
                                                onUpdateNow = { viewModel.downloadAndApplyUpdate(context) },
                                                onViewDetails = { viewModel.setAppUpdateSheetOpen(true) },
                                                onDismiss = { viewModel.dismissUpdateBanner() }
                                            )
                                        }
                                    } else if (uiState.updateState.isUpdated && !uiState.updateState.isBannerDismissed) {
                                        item {
                                            InAppUpdateSuccessCard(
                                                version = uiState.updateState.currentVersion,
                                                onDismiss = { viewModel.dismissUpdateBanner() }
                                            )
                                        }
                                    }

                                    // Real-Time WRS-NTWC BMKG Earthquake Quick Banner
                                    val latestEq = uiState.latestEarthquake
                                    if (latestEq != null) {
                                        item {
                                            WrsLiveEarthquakeQuickCard(
                                                earthquake = latestEq,
                                                onClick = { viewModel.selectTab(3) }
                                            )
                                        }
                                    }

                                    // Hero Weather Overview Card with Dynamic Animated Weather Icon
                                    item {
                                        HeroWeatherCard(report = report)
                                    }

                                    // Feature: Edukasi & Makna Akronim SIPAGAR KITA (SIMAK, PAGAR, KITA)
                                    item {
                                        SipagarKitaAcronymCard(isDark = isActuallyDark)
                                    }

                                    // Feature: Prakiraan Cuaca 3 Hari (Mukomuko)
                                    item {
                                        ThreeDayForecastCard(
                                            district = currentDistrict,
                                            dailyList = report.daily,
                                            isDark = isActuallyDark
                                        )
                                    }

                                    // Sun, UV Index & Atmospheric Metrics Card
                                    item {
                                        SunAndAtmosphereCard(
                                            daily = report.daily.firstOrNull(),
                                            current = report.current
                                        )
                                    }

                                    // 24-Hour Forecast Timeline
                                    item {
                                        HourlyForecastCard(hourlyList = report.hourly)
                                    }

                                    // Feature: Overlay Peta Suhu Interaktif BMKG (Distribusi Panas 15 Kecamatan)
                                    item {
                                        InteractiveTemperatureMapCard(
                                            currentDistrict = currentDistrict,
                                            districtsSummary = uiState.allDistrictsSummary,
                                            onSelectDistrict = { dist ->
                                                viewModel.selectDistrict(dist)
                                            }
                                        )
                                    }

                                    // Data Visualization: Weekly Rainfall Precipitation Bar Chart
                                    item {
                                        WeeklyRainfallBarChartCard(
                                            currentDistrict = currentDistrict,
                                            dailyList = report.daily
                                        )
                                    }

                                    // Data Visualization: Historical Marine Wave Height Chart
                                    item {
                                        MarineWaveHistoryChart(district = currentDistrict)
                                    }

                                    // Marine Pesisir Conditions Card
                                    item {
                                        MarinePesisirCard(
                                            district = currentDistrict,
                                            marine = report.marine
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Tab 1: Prakiraan Cuaca
                            val report = uiState.report
                            if (report != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tab_forecast_list"),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Feature: Prakiraan Cuaca 3 Hari Mukomuko
                                    item {
                                        ThreeDayForecastCard(
                                            district = currentDistrict,
                                            dailyList = report.daily,
                                            isDark = isActuallyDark
                                        )
                                    }

                                    // Weekly Rainfall Precipitation Bar Chart Card (Recharts-inspired Data Visualization)
                                    item {
                                        WeeklyRainfallBarChartCard(
                                            currentDistrict = currentDistrict,
                                            dailyList = report.daily
                                        )
                                    }

                                    item {
                                        DailyForecastCard(dailyList = report.daily)
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Tab 2: Laporan Warga Bencana (Formulir, Peta GIS Spasial, Tabel Riwayat, Kontak Penting) - Moved next to Prakiraan Cuaca!
                            CitizenReportScreen(
                                reports = citizenDisasterReports,
                                currentDistrict = currentDistrict,
                                onSubmitReport = { report ->
                                    viewModel.submitCitizenDisasterReport(report)
                                },
                                onUpdateReportStatus = { id, status ->
                                    viewModel.updateCitizenReportStatus(id, status)
                                },
                                onUpdateReportStatusAndNotes = { id, status, notes ->
                                    viewModel.updateCitizenReportStatusAndNotes(id, status, notes)
                                },
                                onSyncReports = { onComplete ->
                                    viewModel.syncCitizenDisasterReportsOnline(onComplete)
                                },
                                onBackClick = {
                                    viewModel.selectTab(0)
                                }
                            )
                        }

                        3 -> {
                            // Tab 3: Peringatan Dini Cuaca & Bencana + BMKG Gempa Real-time + Push Notification
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_early_warning_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    EarlyWarningSection(
                                        warnings = uiState.earlyWarnings,
                                        currentDistrict = currentDistrict,
                                        weatherAlertSettings = uiState.weatherAlertSettings,
                                        onUpdateWeatherAlertSettings = { viewModel.updateWeatherAlertSettings(it) },
                                        onTestWeatherAlertNotification = { viewModel.triggerTestWeatherAlertNotification(it) },
                                        onTestSuddenSevereWeatherNotification = { viewModel.triggerSuddenSevereWeatherPushNotification(it) },
                                        latestEarthquake = uiState.latestEarthquake,
                                        recentEarthquakes = uiState.recentEarthquakes,
                                        isLoadingEarthquakes = uiState.isLoadingEarthquakes,
                                        isNotificationEnabled = uiState.isEarthquakeNotificationEnabled,
                                        onToggleNotification = { viewModel.toggleEarthquakeNotifications(it) },
                                        onTestNotification = { viewModel.triggerTestEarthquakeNotification(it) },
                                        onRefreshEarthquake = { viewModel.refreshEarthquakeData(context) },
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onOpenCallCenterInfo = { isCallCenter117SheetOpen = true },
                                        onOpenEvacuationGuide = { viewModel.selectTab(4) }
                                    )
                                }
                            }
                        }

                        4 -> {
                            // Tab 4: Modul Peta Interaktif Bencana & SOP Evakuasi Mandiri
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_disaster_map_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Interactive Map Module with Disaster Prone Points & Evacuation Routes
                                item {
                                    InteractiveDisasterMapSection(
                                        currentDistrict = currentDistrict,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onOpenSopEvacuation = { /* View SOP below */ },
                                        isDark = isActuallyDark
                                    )
                                }

                                // Kesiapsiagaan & SOP Evakuasi Mandiri Gempa Bumi & Tsunami
                                item {
                                    EarthquakeEvacuationPreparednessSection(
                                        currentDistrict = currentDistrict,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onTestTsunamiAlert = { viewModel.triggerTestTsunamiNotification(it) },
                                        onTestSevereWeatherAlert = { viewModel.triggerSuddenSevereWeatherPushNotification(it) }
                                    )
                                }
                            }
                        }

                        5 -> {
                            // Tab 5: inaRISK BNPB - Kajian Risiko Bencana Indonesia Kabupaten Mukomuko
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_inarisk_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    InaRiskBnpbSection(
                                        currentDistrict = currentDistrict,
                                        onOpenEvacuationGuide = { viewModel.selectTab(4) },
                                        onCall117 = { EmergencyDialer.dial(context, "117") }
                                    )
                                }
                            }
                        }

                        6 -> {
                            // Tab 6: Cuaca Historis
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_historical_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    HistoricalWeatherSection(
                                        district = currentDistrict,
                                        record = uiState.historicalRecord,
                                        selectedDate = uiState.selectedHistoricalDate,
                                        isLoading = uiState.isLoadingHistorical,
                                        onDateSelected = { newDate ->
                                            viewModel.loadHistoricalWeather(currentDistrict, newDate)
                                        }
                                    )
                                }
                            }
                        }

                        7 -> {
                            // Tab 7: Maritim & Pesisir Pantai
                            val report = uiState.report
                            if (report != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tab_marine_list"),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        MarinePesisirCard(
                                            district = currentDistrict,
                                            marine = report.marine
                                        )
                                    }
                                    // Historical Marine Wave Height Chart
                                    item {
                                        MarineWaveHistoryChart(district = currentDistrict)
                                    }
                                }
                            }
                        }

                        8 -> {
                            // Tab 8: Kualitas Udara Lengkap BMKG & Himbauan Kesehatan Masyarakat + Grafik Harian UV
                            val report = uiState.report
                            if (report != null) {
                                val maxUv = report.daily.firstOrNull()?.uvIndexMax ?: 8.0
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tab_air_quality_list"),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        com.example.ui.components.BmkgAirQualityDetailedSection(
                                            airQuality = report.airQuality,
                                            uvIndex = maxUv,
                                            currentDistrict = currentDistrict
                                        )
                                    }
                                }
                            }
                        }

                        9 -> {
                            // Tab 9: 15 Kecamatan
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_all_districts_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    AllDistrictsView(
                                        districtsSummary = uiState.allDistrictsSummary,
                                        selectedDistrict = currentDistrict,
                                        favoriteDistricts = favorites,
                                        onSelectDistrict = { dist ->
                                            viewModel.selectDistrict(dist)
                                            viewModel.selectTab(0)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Speed Dial Quick Actions FAB Overlay (100% Reliable Tap & Direct Links)
        QuickActionsFab(
            isExpanded = isQuickActionsFabExpanded,
            onToggleExpand = { isQuickActionsFabExpanded = !isQuickActionsFabExpanded },
            onSelectCitizenReport = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(2)
            },
            onSelectForecast = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(1)
            },
            onSelectMaritimeGuide = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(7)
            },
            onSelectEvacuationProcedures = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(4)
            },
            onSelectDisasterMap = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(4)
            },
            onCall117 = {
                isQuickActionsFabExpanded = false
                EmergencyDialer.dial(context, "117")
            },
            onOpenCompass = {
                isQuickActionsFabExpanded = false
                viewModel.setCompassSheetOpen(true)
            },
            onOpenOnboarding = {
                isQuickActionsFabExpanded = false
                viewModel.setOnboardingOpen(true)
            },
            parentMaxWidth = this.maxWidth,
            parentMaxHeight = this.maxHeight,
            isDark = isActuallyDark
        )
        }
    }

    // Call Center 117 Detail Bottom Sheet
    if (isCallCenter117SheetOpen) {
        CallCenter117DetailBottomSheet(
            onDismiss = { isCallCenter117SheetOpen = false },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onCallPoskoMukomuko = { EmergencyDialer.dial(context, "073771112") }
        )
    }

    // District Selector Bottom Sheet with GPS Auto-Detection
    if (uiState.isDistrictSheetOpen) {
        DistrictSelectorBottomSheet(
            selectedDistrict = currentDistrict,
            searchQuery = uiState.districtSearchQuery,
            favoriteDistricts = favorites,
            isDetectingLocation = uiState.isDetectingLocation,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSelectDistrict = { viewModel.selectDistrict(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDetectLocationClick = triggerLocationDetection,
            onDismiss = { viewModel.setDistrictSheetOpen(false) }
        )
    }

    // App Update & Developer Info Bottom Sheet
    if (uiState.isAppUpdateSheetOpen) {
        AppUpdateBottomSheet(
            updateState = uiState.updateState,
            isDarkMode = uiState.isDarkMode,
            onDismiss = { viewModel.setAppUpdateSheetOpen(false) },
            onSetThemeMode = { viewModel.setDarkMode(it) },
            onCheckUpdateOnline = { viewModel.checkForUpdatesOnline(context = context) },
            onDownloadUpdate = { viewModel.downloadAndApplyUpdate(context = context) },
            onSimulateOlderVersion = { viewModel.simulateOlderVersionForTesting("1.4.0", context) },
            onSimulatePublishNextVersion = { viewModel.simulatePublishNextVersion("1.6.0", context) },
            onResetLatestRelease = { viewModel.resetToLatestReleaseState() },
            onOpenOnboarding = { viewModel.setOnboardingOpen(true) },
            onOpenCompass = { viewModel.setCompassSheetOpen(true) },
            onShowStartupWallpaper = { viewModel.showStartupWallpaper() }
        )
    }

    // In-App OTA Update Dialog (Prompts only once per release, persistent in DataStore)
    if (uiState.isUpdateDialogOpen && uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog(dontShowAgainForThisVersion = true) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Pembaruan Versi ${uiState.updateState.latestVersion} Tersedia",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Perangkat Anda menjalankan versi ${uiState.updateState.currentVersion}. Rilis pembaruan v${uiState.updateState.latestVersion} resmi dari Pusdalops BPBD Mukomuko dapat langsung dipasang secara daring tanpa offline update.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Sorotan Pembaruan v${uiState.updateState.latestVersion}:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val activeHighlights = uiState.updateState.changelogs
                                .find { it.version == uiState.updateState.latestVersion }
                                ?.highlights
                                ?: listOf(
                                    "Tab Laporan Warga Bersebelahan dengan Prakiraan Cuaca",
                                    "Peluncur Kamera Langsung (Live Camera Launcher) & Geotag",
                                    "Peta Spasial GIS Laporan Warga & Filter Kategori Kejadian",
                                    "Layer Overlay Titik Rawan Bencana BPBD Mukomuko",
                                    "Pelacakan Status Laporan & Notifikasi Toast Konfirmasi",
                                    "Direktori Kontak Darurat Mukomuko & Dial Cepat 117 BPBD",
                                    "Optimalisasi Tombol Aksi Cepat Speed Dial FAB Material 3"
                                )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                activeHighlights.take(4).forEach { hl ->
                                    Text(
                                        text = "• $hl",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ukuran paket: ${uiState.updateState.packageSizeMb} MB • Tanda Tangan Digital Terverifikasi",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadAndApplyUpdate(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.testTag("btn_dialog_install_update")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OrangeOnPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Perbarui Daring", color = OrangeOnPrimary, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissUpdateDialog(dontShowAgainForThisVersion = true) },
                    modifier = Modifier.testTag("btn_dialog_dismiss_update")
                ) {
                    Text("Nanti Saja (Cukup Sekali)", fontSize = 12.sp)
                }
            }
        )
    }

    // Interactive Onboarding Guide (First launch or opened via menu)
    if (uiState.isOnboardingOpen) {
        com.example.ui.components.InteractiveOnboardingDialog(
            onComplete = { viewModel.completeOnboarding() },
            onOpenCompassDetailed = {
                viewModel.setOnboardingOpen(false)
                viewModel.setCompassSheetOpen(true)
            }
        )
    }

    // Digital Compass & Tsunami Evacuation Direction Bottom Sheet
    if (uiState.isCompassSheetOpen) {
        com.example.ui.components.DigitalCompassBottomSheet(
            onDismiss = { viewModel.setCompassSheetOpen(false) },
            onOpenEvacuationMap = { viewModel.selectTab(4) },
            onCall117 = { EmergencyDialer.dial(context, "117") }
        )
    }

    // Emergency Warning Popup Notification Dialog (Active severe weather, high waves, or BMKG earthquake)
    val currentReport = uiState.report
    val activeEq = uiState.latestEarthquake
    val hasEmergencyConditions = (currentReport?.alert != null) ||
            (currentReport?.marine != null && (currentReport.marine.waveHeight >= 2.0 || currentReport.marine.seaCondition.contains("Tinggi", ignoreCase = true))) ||
            (activeEq != null && (activeEq.magnitude >= 5.0 || activeEq.isNearMukomuko || activeEq.potentialTsunami.contains("tsunami", ignoreCase = true)))

    if (!isEmergencyPopupDismissed && hasEmergencyConditions && currentReport != null) {
        EmergencyWarningPopupDialog(
            weatherAlert = currentReport.alert,
            marineWeather = currentReport.marine,
            latestEarthquake = activeEq,
            earlyWarnings = uiState.earlyWarnings,
            isDark = isActuallyDark,
            onDismiss = { isEmergencyPopupDismissed = true },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onOpenEvacuationMap = {
                viewModel.selectTab(4)
            }
        )
    }

    // High-Resolution Wallpaper & Mukomuko Disaster Mitigation Screen
    StartupSplashWallpaperScreen(
        isVisible = uiState.isStartupWallpaperVisible,
        onDismiss = { viewModel.dismissStartupWallpaper() }
    )
}

@Composable
private fun QuickEvacuationPreparednessBanner(
    isDark: Boolean,
    onOpenGuide: () -> Unit
) {
    val containerBg = if (isDark) Color(0xFF2E1515) else Color(0xFFFFF1F2)
    val titleCol = if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    val descCol = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
    val borderStroke = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isDark) Color(0xFFDC2626).copy(alpha = 0.5f) else Color(0xFFFECDD3)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGuide)
            .testTag("quick_evacuation_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFDC2626), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kesiapsiagaan Evakuasi Gempa",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleCol
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = if (isDark) 0.3f else 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SOP BPBD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFECACA) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Refleks Drop-Cover-Hold On & Rumus 20-20-20 Tsunami Pesisir",
                    fontSize = 10.sp,
                    color = descCol,
                    maxLines = 1
                )
            }

            Text(
                text = "Panduan >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            )
        }
    }
}

@Composable
private fun QuickGeminiAdvisorBanner(
    isDark: Boolean,
    onAskAi: () -> Unit
) {
    val containerBg = if (isDark) Color(0xFF2B1834) else Color(0xFFFDF4FF)
    val titleCol = if (isDark) Color(0xFFF3E8FF) else Color(0xFF581C87)
    val descCol = if (isDark) Color(0xFFE9D5FF) else Color(0xFF7E22CE)
    val borderStroke = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9333EA).copy(alpha = 0.5f)) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAskAi)
            .testTag("quick_gemini_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF9333EA), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tanya AI Gemini",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleCol
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF9333EA).copy(alpha = if (isDark) 0.3f else 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "AI Advisor",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF9333EA),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Konsultasi kelayakan melaut nelayan & panen sawit",
                    fontSize = 10.sp,
                    color = descCol,
                    maxLines = 1
                )
            }

            Text(
                text = "Tanya >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
            )
        }
    }
}

@Composable
private fun QuickEmergency117Strip(
    isDark: Boolean,
    onCall117: () -> Unit,
    onViewDetails: () -> Unit
) {
    val stripBg = if (isDark) Color(0xFF331414) else Color(0xFFFEF2F2)
    val stripBorder = if (isDark) Color(0xFFF87171) else Color(0xFFFCA5A5)
    val titleCol = if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
    val subtitleCol = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)

    Surface(
        color = stripBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, stripBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_emergency_117_strip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewDetails)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDC2626), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "117",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Call Center 117",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleCol
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Bebas Pulsa 24 Jam",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Pusdalops-PB BPBD Mukomuko • Tanggap Darurat Bencana",
                        fontSize = 10.sp,
                        color = subtitleCol,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCall117,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("117", fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ActiveAlertCard(
    alert: com.example.model.WeatherAlert,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetail)
            .testTag("card_active_alert"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Peringatan",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
                Text(
                    text = alert.message,
                    fontSize = 11.sp,
                    color = Color(0xFF7F1D1D),
                    maxLines = 2
                )
            }
            Text(
                text = "Lihat >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
            )
        }
    }
}

@Composable
private fun SunAndAtmosphereCard(
    daily: DailyItem?,
    current: CurrentWeather? = null
) {
    val sunrise = daily?.sunrise ?: "06:05"
    val sunset = daily?.sunset ?: "18:15"
    val uvIndex = daily?.uvIndexMax ?: 6.5
    val pressure = current?.surfacePressure ?: 1012.0
    val humidity = current?.humidity ?: 78

    val uvCategory = when {
        uvIndex >= 11.0 -> "Ekstrem"
        uvIndex >= 8.0 -> "Sgt Tinggi"
        uvIndex >= 6.0 -> "Tinggi"
        uvIndex >= 3.0 -> "Sedang"
        else -> "Rendah"
    }

    val uvBadgeColor = when {
        uvIndex >= 8.0 -> Color(0xFFDC2626)
        uvIndex >= 6.0 -> Color(0xFFEA580C)
        uvIndex >= 3.0 -> Color(0xFFD97706)
        else -> Color(0xFF16A34A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sun_astro_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Matahari & Atmosfer",
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Matahari & Atmosfer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Radiasi UV & Siklus Surya Mukomuko",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Surface(
                    color = OrangePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "BMKG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sunrise & Sunset Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sunrise
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(OrangeSecondary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = OrangeSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Terbit (WIB)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = sunrise,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7C2D12)
                            )
                        }
                    }
                }

                // Sunset
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(OrangePrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Terbenam",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = sunset,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Atmospheric Metrics Strip (UV, Pressure, Humidity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UV Index
                Column {
                    Text(
                        text = "Indeks UV Maks",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(uvBadgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", uvIndex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "($uvCategory)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = uvBadgeColor
                        )
                    }
                }

                // Air Pressure
                Column {
                    Text(
                        text = "Tekanan Udara",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${pressure.toInt()} hPa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Humidity
                Column {
                    Text(
                        text = "Kelembaban",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$humidity%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WrsLiveEarthquakeQuickCard(
    earthquake: com.example.model.BmkgEarthquake,
    onClick: () -> Unit
) {
    val isTsunami = earthquake.potentialTsunami.contains("berpotensi tsunami", ignoreCase = true) &&
            !earthquake.potentialTsunami.contains("tidak", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("live_earthquake_quick_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTsunami) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isTsunami) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isTsunami) Color(0xFFDC2626) else Color(0xFFEA580C), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WRS-NTWC BMKG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTsunami) Color(0xFF991B1B) else Color(0xFF9A3412)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isTsunami) Color(0xFFDC2626) else Color(0xFF16A34A),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${earthquake.date} • ${earthquake.timeWib}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Surface(
                    color = if (isTsunami) Color(0xFFDC2626).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isTsunami) "⚠️ POTENSI TSUNAMI" else "✓ AMAN TSUNAMI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTsunami) Color(0xFFDC2626) else Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Magnitude Badge
                Surface(
                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "M ${String.format(java.util.Locale.US, "%.1f", earthquake.magnitude)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = earthquake.region,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Kedalaman: ${earthquake.depth} • ~${earthquake.distanceKmToMukomuko} km dari Mukomuko",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Peta WRS >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }
        }
    }
}

