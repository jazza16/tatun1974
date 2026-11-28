package com.example
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.model.AirQuality
import com.example.model.BmkgEarthquake
import com.example.model.DetailedEarlyWarning
import com.example.model.EarthquakeUrgency
import com.example.model.HourlyItem
import com.example.model.MarineWeather
import com.example.model.MukomukoDistricts
import com.example.util.WeatherAlertSettings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.ui.components.WeatherNavigationRail
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.InaRiskBnpbSection
import com.example.ui.components.FirmsHotspotOsmMapSection
import com.example.ui.components.InteractiveDisasterMapSection
import com.example.ui.components.InteractiveTemperatureMapCard
import com.example.ui.components.MarinePesisirCard
import com.example.ui.components.MarineWaveHistoryChart
import com.example.ui.components.TidalInformationCard
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
import com.example.ui.theme.geminiGradientBorder
import com.example.ui.viewmodel.AiWeatherViewModel
import com.example.ui.viewmodel.WeatherViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val APP_CHECK_TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        // CRITICAL: Firebase App Check provider MUST be installed BEFORE any FirebaseFirestore.getInstance(...)
        // call, otherwise Firestore caches an unauthenticated token provider and Cloud PIN lookups fail/timeout.
        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit() is required before DebugAppCheckProviderFactory reads the secret.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(APP_CHECK_TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(APP_CHECK_TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(APP_CHECK_TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val staticSiteKey = runCatching { context.getString(R.string.recaptcha_site_key).trim() }.getOrNull()
            val dynamicSiteKey = if (staticSiteKey.isNullOrBlank()) {
                val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
                if (resId != 0) runCatching { context.getString(resId).trim() }.getOrNull() else null
            } else {
                null
            }
            val siteKey = staticSiteKey?.takeIf { it.isNotBlank() } ?: dynamicSiteKey.orEmpty()
            if (siteKey.isNotBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(APP_CHECK_TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.e(APP_CHECK_TAG, "recaptcha_site_key string resource is missing or blank.")
            }
        }

        // Aktifkan Firestore Offline Cache (persistenceEnabled = true / PersistentCacheSettings) SETELAH App Check siap
        com.example.data.local.FirestoreHelper.ensureOfflinePersistenceEnabled(context)
    } catch (e: Exception) {
        Log.e(APP_CHECK_TAG, "Failed to initialize Firebase App Check", e)
        try {
            com.example.data.local.FirestoreHelper.ensureOfflinePersistenceEnabled(context)
        } catch (_: Exception) {
        }
    }
}

class MainActivity : FragmentActivity() {
    private val notificationIntentFlow = kotlinx.coroutines.flow.MutableStateFlow<Intent?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 0: Automatic Post-Update Sanitizer (Auto Clear Cache & Safe Data Migration)
        // Eliminates post-update Force Close without requiring manual "Hapus Cache & Hapus Data"
        com.example.util.AppUpgradeSanitizer.runOnStartupIfUpdated(this, intent)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        notificationIntentFlow.value = intent

        // STEP 1b: Sync Supabase Storage configuration with Firestore & SharedPreferences cache
        // Ensures both AI Studio builds and local Android Studio builds share the configured SUPABASE_URL & SUPABASE_KEY
        try {
            com.example.data.api.SupabaseClient.syncConfigWithFirestore(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "Supabase config sync skipped: ${e.message}")
        }

        // Seed 34 data ASN BPBD Kabupaten Mukomuko (hanya jika NIP belum terdaftar)
        try {
            com.example.data.local.seedDataAsnMukomuko2026(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "Seed ASN skipped: ${e.message}")
        }

        // Jadwalkan WorkManager sinkronisasi periodik titik panas FIRMS (1-3 jam), BMKG (3-6 jam),
        // serta Pemantauan Latar Belakang 24 Jam SIPAGAR KITA (Laporan Warga, Potensi Ancaman & Kejadian Bencana tanpa perlu login)
        try {
            com.example.service.KarhutlaSyncScheduler.schedulePeriodicSync(this)
            com.example.service.SipagarBackgroundAlertScheduler.scheduleBackgroundMonitoring(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "Background Scheduler skipped: ${e.message}")
        }

        // Initialize BMKG Earthquake, Severe Weather, and Real-Time Citizen/Officer Push Notification Channels
        try {
            com.example.util.DisasterTriageNotificationManager.initNotificationChannels(this)
            com.example.data.sync.OfficerPushNotificationHelper.ensureNotificationChannels(this)
            com.example.util.EarthquakeNotificationHelper.initNotificationChannel(this)
            com.example.util.WeatherNotificationHelper.initNotificationChannel(this)
            com.example.util.WeatherNotificationHelper.initAppUpdateNotificationChannel(this)
            com.example.data.sync.OfficerPushNotificationHelper.registerAppContext(applicationContext)
            com.example.data.sync.OfficerPushNotificationHelper.syncOfficerTopicSubscription(applicationContext)
            com.example.data.sync.SyncController.getInstance(
                applicationContext,
                AppDatabase.getInstance(applicationContext)
            )
        } catch (e: Throwable) {
            Log.w("MainActivity", "NotificationChannel / SyncController init skipped: ${e.message}")
        }

        // Pemicu pemeriksaan Auto-Update GitHub saat aplikasi dibuka
        checkForAppUpdates(this)

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
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
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density = currentDensity.density,
                    fontScale = 1.0f
                )
            ) {
                MyApplicationTheme(darkTheme = isDark) {
                    WeatherAppScreen(
                        viewModel = weatherViewModel,
                        aiViewModel = aiViewModel,
                        windowSizeClass = windowSizeClass,
                        notificationIntentFlow = notificationIntentFlow
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pemicu pemeriksaan Auto-Update saat aplikasi kembali dari background
        checkForAppUpdates(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationIntentFlow.value = intent
    }
}

@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel,
    aiViewModel: AiWeatherViewModel,
    windowSizeClass: WindowSizeClass,
    notificationIntentFlow: kotlinx.coroutines.flow.StateFlow<Intent?>? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var savedMainTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(uiState.selectedTab) }
    var savedWallpaperDismissed by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(!uiState.isStartupWallpaperVisible) }

    LaunchedEffect(Unit) {
        if (savedWallpaperDismissed && uiState.isStartupWallpaperVisible) {
            viewModel.dismissStartupWallpaper()
        }
        if (savedMainTab != uiState.selectedTab) {
            viewModel.selectTab(savedMainTab)
        }
    }
    LaunchedEffect(uiState.selectedTab) {
        savedMainTab = uiState.selectedTab
    }
    LaunchedEffect(uiState.isStartupWallpaperVisible) {
        savedWallpaperDismissed = !uiState.isStartupWallpaperVisible
    }
    val favorites by viewModel.favoriteDistricts.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val citizenDisasterReports by viewModel.citizenDisasterReports.collectAsStateWithLifecycle()

    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val aiChatMessages by aiViewModel.chatMessages.collectAsStateWithLifecycle()
    val pendingNotificationIntent = notificationIntentFlow?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(pendingNotificationIntent) {
        val notifIntent = pendingNotificationIntent ?: return@LaunchedEffect
        val explicitNavTab = notifIntent.getIntExtra("EXTRA_NAVIGATE_TAB", -1)
        val openReportId = notifIntent.getStringExtra("extra_open_report_id")
        val openOfficerReports = notifIntent.getBooleanExtra("extra_open_officer_reports", false)
        val navTab = when {
            explicitNavTab >= 0 -> explicitNavTab
            !openReportId.isNullOrBlank() || openOfficerReports -> 1
            else -> -1
        }
        val openUpdate = notifIntent.getBooleanExtra("EXTRA_OPEN_UPDATE_SHEET", false)
        val autoStartUpdate = notifIntent.getBooleanExtra("EXTRA_AUTO_START_UPDATE", false)
        val hasValidUpdate = uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated
        if (navTab >= 0 || (openUpdate && hasValidUpdate) || (autoStartUpdate && hasValidUpdate)) {
            viewModel.dismissStartupWallpaper()
        }
        if (navTab >= 0) {
            viewModel.selectTab(navTab)
            notifIntent.removeExtra("EXTRA_NAVIGATE_TAB")
            notifIntent.removeExtra("extra_open_report_id")
            notifIntent.removeExtra("extra_open_officer_reports")
        }
        if (openUpdate) {
            notifIntent.removeExtra("EXTRA_OPEN_UPDATE_SHEET")
            if (hasValidUpdate) {
                viewModel.setAppUpdateSheetOpen(true)
            }
        }
        if (autoStartUpdate) {
            notifIntent.removeExtra("EXTRA_AUTO_START_UPDATE")
            if (hasValidUpdate) {
                viewModel.downloadAndApplyUpdate(context)
            }
        }
    }

    var isCallCenter117SheetOpen by remember { mutableStateOf(false) }
    var isQuickActionsFabExpanded by remember { mutableStateOf(false) }
    var isEmergencyPopupDismissed by remember { mutableStateOf(false) }
    var isSipagarKitaDismissed by remember { mutableStateOf(false) }
    var isPanduanEvakuasiOpen by remember { mutableStateOf(false) }
    var isVolunteerModuleDialogOpen by remember { mutableStateOf(false) }
    var officerLoginRequestId by remember { mutableStateOf(0) }
    var openRuangKerjaRequestId by remember { mutableStateOf(0) }
    val disclaimerConsentManager = remember { com.example.data.local.DisclaimerConsentManager(context) }
    var isDisclaimerAccepted by remember {
        mutableStateOf(disclaimerConsentManager.hasAcceptedCurrentDisclaimer())
    }
    var legalDocumentsModalTab by remember { mutableStateOf<Int?>(null) }

    val currentDistrict = uiState.selectedDistrict
    val isFavorite = favorites.contains(currentDistrict.id)

    val isExpandedWindow = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

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
        if (shouldOpenUpdate && uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated) {
            viewModel.setAppUpdateSheetOpen(true)
        }
        // Seamlessly check for newest version online on startup and notify
        viewModel.checkForUpdatesOnline(isAutoCheck = true, context = context)
    }

    val database = remember { AppDatabase.getInstance(context) }
    val syncController = remember { com.example.data.sync.SyncController.getInstance(context, database) }
    val syncState by syncController.syncState.collectAsStateWithLifecycle()
    val isRetrying by syncController.isRetrying.collectAsStateWithLifecycle()
    val retryProgress by syncController.retryProgress.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        try {
            viewModel.runAutomatedCleanup90Days()
            com.example.data.sync.IntegrityCheckWorker.performCheck(context, database)
            com.example.service.WeatherReportAggregator.schedulePeriodic(context)
            com.example.data.sync.DatabaseMaintenanceWorker.schedule(context)
            com.example.data.local.DatabaseIndexManager.ensureIndexes(database)
        } catch (e: Throwable) {
            Log.w("WeatherAppScreen", "Startup DB maintenance warning: ${e.message}")
        }

        // Request notification permission for Android 13+ so simultaneous disaster alerts are delivered
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } catch (e: Throwable) {
            Log.w("WeatherAppScreen", "Notification permission check warning: ${e.message}")
        }
    }

    val isActuallyDark = uiState.isDarkMode ?: false

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectCurrentLocation(context, isAutoDetect = true)
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

    // Auto-Select Kecamatan via GPS saat pertama kali dibuka & Auto-Refresh setiap 30 menit
    LaunchedEffect(Unit) {
        viewModel.attachApplicationContext(context.applicationContext)
        viewModel.startAutoRefresh30Minutes(context)
        if (com.example.util.LocationHelper.hasLocationPermission(context)) {
            viewModel.autoSelectDistrictViaGpsOnStartup(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-Refresh kondisional saat aplikasi dibuka kembali dari background (ON_RESUME)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    viewModel.onAppResumedFromBackground(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Column {
                WeatherTopBar(
                    district = currentDistrict,
                    isDarkMode = isActuallyDark,
                    isUpdateAvailable = uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated,
                    isDetectingLocation = uiState.isDetectingLocation,
                    onDistrictClick = { viewModel.setDistrictSheetOpen(true) },
                    onThemeToggle = { viewModel.toggleDarkMode(isActuallyDark) },
                    onOpenAppMenu = { viewModel.setAppUpdateSheetOpen(true) },
                    onEvacuationGuideClick = { isPanduanEvakuasiOpen = true },
                    onCallCenter117Click = { isCallCenter117SheetOpen = true },
                    onDetectLocationClick = triggerLocationDetection
                )
                if (!isExpandedWindow) {
                    WeatherTabs(
                        selectedTabIndex = uiState.selectedTab,
                        warningCount = uiState.earlyWarnings.size,
                        reportCount = citizenDisasterReports.size,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpandedWindow) {
                    WeatherNavigationRail(
                        selectedTabIndex = uiState.selectedTab,
                        warningCount = uiState.earlyWarnings.size,
                        reportCount = citizenDisasterReports.size,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
            // AUTOMATED RETRY PROGRESS BAR IN STATUS STRIP
            if (isRetrying) {
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth().testTag("sync_retry_progress_strip")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = OrangePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Memulihkan koneksi & sinkronisasi multi-perangkat...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${(retryProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { retryProgress },
                            color = OrangePrimary,
                            trackColor = Color(0xFF334155),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Banner status jaringan lambat dan notifikasi deteksi lokasi disembunyikan dari layar utama;
            // detectCurrentLocation & syncAllDataOnline tetap berjalan otomatis di background.

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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxHeight()
                    ) {
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
                                    // Tab 0: Hari Ini (Adaptive Grid Dashboard)
                                    val report = uiState.report
                                    if (report != null) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 360.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("weather_content_list"),
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // Active Severe Weather Alert if available
                                            if (report.alert != null) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    ActiveAlertCard(
                                                        alert = report.alert,
                                                        onViewDetail = { viewModel.selectTab(5) }
                                                    )
                                                }
                                            }

                                            // In-App OTA Update Available Banner
                                            if (uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated && !uiState.updateState.isBannerDismissed) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    InAppUpdateBannerCard(
                                                        updateState = uiState.updateState,
                                                        onUpdateNow = { viewModel.downloadAndApplyUpdate(context) },
                                                        onViewDetails = { viewModel.setAppUpdateSheetOpen(true) },
                                                        onDismiss = { viewModel.dismissUpdateBanner() }
                                                    )
                                                }
                                            }

                                            val latestEq = uiState.latestEarthquake

                                            // 4 Aksi Darurat 1-Ketuk untuk Kondisi Panik / Aksesibilitas Warga
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                com.example.ui.components.CitizenEmergencyQuickActionPanel(
                                                    activeAlertCount = uiState.earlyWarnings.size + if (report.alert != null) 1 else 0,
                                                    hasNearEarthquake = latestEq?.isNearMukomuko == true,
                                                    onCall117 = { EmergencyDialer.dial(context, "117") },
                                                    onOpenQuickReport = { viewModel.selectTab(1) },
                                                    onOpenEarlyWarning = { viewModel.selectTab(5) },
                                                    onOpenEvacuationGuide = { isPanduanEvakuasiOpen = true }
                                                )
                                            }

                                            // Hero Weather Overview Card with Dynamic Animated Weather Icon (Diposisikan tepat di bawah Card Aksi Cepat Tanggap Darurat)
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                HeroWeatherCard(report = report)
                                            }

                                            // Banner Sinkronisasi Cepat Profil KRB 2026-2030 & Perda RTRW No. 12/2025 Kecamatan Aktif
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                com.example.ui.components.KrbRtrwModuleContextBanner(
                                                    currentDistrict = currentDistrict,
                                                    moduleTag = "hari_ini",
                                                    moduleTitle = "Beranda Hari Ini",
                                                    krbAlignmentNote = "Pemantauan cuaca harian diselaraskan dengan 6 bahaya Prioritas 1 KRB 2026–2030 (Banjir, Banjir Bandang, Cuaca Ekstrem, Gelombang Ekstrem & Abrasi, Karhutla, Tanah Longsor).",
                                                    rpbAlignmentNote = "Mendukung Strategi 1.2 & 1.4 RPB 2025–2029 (Modernisasi EWS, Pusdalops-PB & Sistem Informasi Kebencanaan Kode 1.05.03.2.04.0004) serta target penurunan IRB menuju 136,87 di 2029.",
                                                    rtrwRegulationNote = "Mengacu pada ketentuan khusus Kawasan Rawan Bencana (Pasal 92) & Sempadan Pantai/Sungai (Pasal 93) Perda RTRW No. 12 Tahun 2025.",
                                                    isDark = isActuallyDark,
                                                    onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                                )
                                            }

                                            // Feature: Edukasi & Makna Akronim SIPAGAR KITA (Otomatis disembunyikan bila sudah dibaca/ditutup, tersimpan di Menu Info)
                                            if (!isSipagarKitaDismissed) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    SipagarKitaAcronymCard(
                                                        isDark = isActuallyDark,
                                                        onDismissCard = { isSipagarKitaDismissed = true }
                                                    )
                                                }
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
                                            item(span = { GridItemSpan(maxLineSpan) }) {
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

                                            // Parameter Meteorologi Harian: Native Compose Canvas Temperature & Rainfall Trend Chart
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                com.example.ui.components.MeteorologySummaryCard(
                                                    report = report,
                                                    isDark = isActuallyDark
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

                                            // Info Pasang Surut & Pasang Naik Dinamis Samudra Hindia Mukomuko
                                            item {
                                                TidalInformationCard(district = currentDistrict)
                                            }
                                        }
                                    }
                                }

                        1 -> {
                            // Tab 1: Laporan Warga Bencana (Formulir, Peta GIS Spasial, Tabel Riwayat, Kontak Penting) - Moved directly next to Hari Ini!
                            CitizenReportScreen(
                                reports = citizenDisasterReports,
                                currentDistrict = currentDistrict,
                                onSubmitReport = { report ->
                                    viewModel.submitCitizenDisasterReport(report, context)
                                },
                                onSubmitReportWithResult = { report, onComplete ->
                                    viewModel.submitCitizenDisasterReport(report, context, onComplete)
                                },
                                onUpdateReportStatus = { id, status ->
                                    val officerAuth = com.example.data.local.OfficerAuthManager(context)
                                    val authMgr = runCatching { com.example.data.auth.AuthManager.getInstance(context) }.getOrNull()
                                    viewModel.updateCitizenReportStatus(
                                        reportId = id,
                                        newStatus = status,
                                        officerName = officerAuth.getOfficerName().ifBlank { authMgr?.getDisplayName().orEmpty() },
                                        officerNip = officerAuth.getOfficerBadgeId().ifBlank { authMgr?.getBadgeOrUid().orEmpty() },
                                        officerRole = officerAuth.getOfficerRole()
                                    )
                                },
                                onUpdateReportStatusAndNotes = { id, status, notes ->
                                    val officerAuth = com.example.data.local.OfficerAuthManager(context)
                                    val authMgr = runCatching { com.example.data.auth.AuthManager.getInstance(context) }.getOrNull()
                                    viewModel.updateCitizenReportStatusAndNotes(
                                        reportId = id,
                                        newStatus = status,
                                        notes = notes,
                                        officerName = officerAuth.getOfficerName().ifBlank { authMgr?.getDisplayName().orEmpty() },
                                        officerNip = officerAuth.getOfficerBadgeId().ifBlank { authMgr?.getBadgeOrUid().orEmpty() },
                                        officerRole = officerAuth.getOfficerRole()
                                    )
                                },
                                onDeleteReport = { id ->
                                    val officerAuth = com.example.data.local.OfficerAuthManager(context)
                                    viewModel.deleteCitizenReport(
                                        reportId = id,
                                        officerRole = officerAuth.getOfficerRole()
                                    )
                                },
                                onSyncReports = { onComplete ->
                                    viewModel.syncCitizenDisasterReportsOnline(onComplete)
                                },
                                initialShowOfficerLogin = officerLoginRequestId > 0,
                                initialOpenRuangKerja = openRuangKerjaRequestId > 0,
                                officerLoginRequestId = officerLoginRequestId,
                                openRuangKerjaRequestId = openRuangKerjaRequestId,
                                onOfficerLoginDismissed = {
                                    officerLoginRequestId = 0
                                    openRuangKerjaRequestId = 0
                                },
                                onOfficerLogoutToLanding = {
                                    officerLoginRequestId = 0
                                    openRuangKerjaRequestId = 0
                                    viewModel.showStartupWallpaper()
                                },
                                onOpenKrbRtrwTab = {
                                    viewModel.selectTab(2)
                                },
                                onBackClick = {
                                    viewModel.selectTab(0)
                                }
                            )
                        }

                        2 -> {
                            // Tab 2 (TAB KHUSUS): Dokumen KRB 2026–2030 & Perda RTRW No. 12 Tahun 2025 (2025–2045)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_krb_rtrw_dedicated_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbMukomuko2026Section(
                                        currentDistrict = currentDistrict,
                                        isDark = isActuallyDark,
                                        onNavigateToTab = { targetTab -> viewModel.selectTab(targetTab) }
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Tab 3: Statistik & Tren Bencana Bulanan (Selaras Baseline KRB 2026-2030 & Perda RTRW)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_monthly_disaster_analytics_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "statistik_bencana",
                                        moduleTitle = "Statistik & Tren Bencana Bulanan",
                                        krbAlignmentNote = "Distribusi kejadian bulanan dievaluasi terhadap baseline 12 ancaman KRB 2026–2030 (Total potensi kerugian multibahaya Rp 6,74 Triliun; 205.602 jiwa terpapar).",
                                        rpbAlignmentNote = "Disandingkan dengan rekap sejarah 184 kejadian bencana 2014–2024 (82 Gempa, 49 Banjir, 20 Karhutla, 17 Longsor; 8.057 jiwa terdampak) & evaluasi Monev Tahunan RPB 2025–2029 (Tabel 6.1).",
                                        rtrwRegulationNote = "Menjadi dasar evaluasi pengendalian pemanfaatan ruang & prioritas program mitigasi BPBD (Lampiran XII Program A.2.5.5).",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                item {
                                    com.example.ui.components.MonthlyDisasterAnalyticsSection(
                                        reports = citizenDisasterReports,
                                        currentDistrict = currentDistrict,
                                        isDark = isActuallyDark,
                                        onOpenCitizenReportTab = { viewModel.selectTab(1) }
                                    )
                                }
                            }
                        }

                        4 -> {
                            // Tab 4: Prakiraan Cuaca
                            val report = uiState.report
                            if (report != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tab_forecast_list"),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        com.example.ui.components.KrbRtrwModuleContextBanner(
                                            currentDistrict = currentDistrict,
                                            moduleTag = "prakiraan_cuaca",
                                            moduleTitle = "Prakiraan Cuaca & Curah Hujan 15 Kecamatan",
                                            krbAlignmentNote = "Pemantauan curah hujan & cuaca ekstrem mendukung kesiapsiagaan 3 ancaman hidrometeorologi Prioritas 1 KRB 2026–2030: Banjir (72.244,44 Ha), Banjir Bandang (11.279,30 Ha), dan Cuaca Ekstrem (143.621,15 Ha).",
                                            rpbAlignmentNote = "Selaras Strategi 1.1 & 1.2 RPB 2025–2029: Penguatan Flood Forecasting & Warning System (FFWS Kode 1.03.02.2.01.0090), polder/kolam retensi & adaptasi perubahan iklim sektor pertanian (42,44% PDRB).",
                                            rtrwRegulationNote = "Pasal 92 ayat (1)–(2) & Pasal 93 ayat (3) Perda RTRW No. 12/2025: Pengendalian sempadan sungai, drainase primer-sekunder, dan perlindungan kawasan resapan air DAS.",
                                            isDark = isActuallyDark,
                                            onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                        )
                                    }

                                    // Tabel Prakiraan Cuaca 15 Kecamatan Bergaya BMKG + Unduh PDF
                                    item {
                                        com.example.ui.components.BmkgStyleForecastTable(
                                            allDistrictsSummary = uiState.allDistrictsSummary,
                                            currentDistrict = currentDistrict,
                                            isDark = isActuallyDark
                                        )
                                    }

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

                        5 -> {
                            // Tab 5: Peringatan Dini Cuaca & Bencana + BMKG Gempa Real-time + Push Notification
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_early_warning_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "peringatan_wrs",
                                        moduleTitle = "Peringatan Dini & Gempa WRS-BMKG",
                                        krbAlignmentNote = "Selaras KRB 2026–2030: Tsunami Prioritas 2 (7.523,90 Ha; tinggi rendaman s/d 13 m di 6 kecamatan pesisir) & Gempa Bumi Prioritas 3 (bahaya PGA tinggi di 100% wilayah, risiko sedang karena struktur bangunan mayoritas 1 lantai).",
                                        rpbAlignmentNote = "Strategi 1.2 & 2.8 RPB 2025–2029: Antisipasi Sesar Aktif Sumatera (slip rate 5 mm/thn) & Megathrust Mentawai–Pagai (31.122 jiwa zona tsunami; 82 gempa tercatat 2014–2024) serta aktivasi SKPDB & respon cepat ≤1×24 jam.",
                                        rtrwRegulationNote = "Pasal 92 ayat (4) & (6) Perda RTRW No. 12/2025: Wajib EWS, struktur bangunan tahan gempa SNI, dan larangan fasilitas kritis baru di zona bahaya tinggi tsunami.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                item {
                                    EarlyWarningSection(
                                        warnings = uiState.earlyWarnings,
                                        currentDistrict = currentDistrict,
                                        triageNotificationSettings = uiState.triageNotificationSettings,
                                        activeTriageSignals = uiState.activeTriageSignals,
                                        onUpdateTriageNotificationSettings = { viewModel.updateTriageNotificationSettings(context, it) },
                                        onTestTriageNotification = { ctx, lvl -> viewModel.triggerTestTriageNotification(ctx, lvl) },
                                        onNavigateToTab = { tabIdx -> viewModel.selectTab(tabIdx) },
                                        latestEarthquake = uiState.latestEarthquake,
                                        recentEarthquakes = uiState.recentEarthquakes,
                                        isLoadingEarthquakes = uiState.isLoadingEarthquakes,
                                        isNotificationEnabled = uiState.isEarthquakeNotificationEnabled,
                                        onToggleNotification = { viewModel.toggleEarthquakeNotifications(it) },
                                        onTestNotification = { viewModel.triggerTestEarthquakeNotification(it) },
                                        onRefreshEarthquake = { viewModel.refreshEarthquakeData(context) },
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onOpenCallCenterInfo = { isCallCenter117SheetOpen = true },
                                        onOpenEvacuationGuide = { viewModel.selectTab(7) }
                                    )
                                }
                            }
                        }

                        6 -> {
                            // Tab 6: Monitoring Karhutla (VIIRS FIRMS & BMKG Hotspot Sync + OpenStreetMap + Rilis Resmi BMKG 2025-Kini)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_monitoring_karhutla_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "monitoring_karhutla",
                                        moduleTitle = "Monitoring Titik Panas & Karhutla",
                                        krbAlignmentNote = "Prioritas 1 KRB 2026–2030: Luas risiko Karhutla tertinggi mencapai 404.993,70 Ha (validasi ROC/AUC = 0,932) dengan potensi kerugian ekonomi Rp 699,61 Miliar di lahan gambut & perkebunan.",
                                        rpbAlignmentNote = "Strategi 3.7 RPB 2025–2029: Sinergi sistem informasi kebakaran real-time SKIKP (Kode 1.05.04.2.01.0008), RISPKP & Relawan Damkar SKKL Disdamkarmat untuk menekan 20 kejadian historis Karhutla & potensi rugi Rp 834,44 M.",
                                        rtrwRegulationNote = "Pasal 92 ayat (5) & Pasal 83 ayat (4) Perda RTRW No. 12/2025: Larangan pembukaan lahan dengan cara membakar (Zero Burning) & wajib sekat bakar/menara pantau api.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                item {
                                    FirmsHotspotOsmMapSection(
                                        currentDistrict = currentDistrict,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        isDark = isActuallyDark
                                    )
                                }

                                item {
                                    com.example.ui.components.BmkgOfficialHotspotReleaseCard(
                                        currentDistrict = currentDistrict,
                                        isDark = isActuallyDark
                                    )
                                }
                            }
                        }

                        7 -> {
                            // Tab 7: Modul Peta Interaktif Bencana & SOP Evakuasi Mandiri
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_disaster_map_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "peta_sop_evakuasi",
                                        moduleTitle = "Peta Risiko & Sistem Jaringan Evakuasi",
                                        krbAlignmentNote = "Peta zonasi bahaya dan titik kumpul mengacu pada peta risiko multibahaya KRB 2026–2030 (Skala 1:50.000) di 15 Kecamatan dan 151 Desa/Kelurahan.",
                                        rpbAlignmentNote = "Strategi 1.3 & 2.9 RPB 2025–2029: Terhubung ke 11 Lokasi Resmi Penampungan Pengungsi BPBD (Kode 1.06.06.2.01.0007), jaringan jalan evakuasi 681,37 Km & rambu Dishub (20–25 unit/thn) guna menutup gap SPM SAR 25%.",
                                        rtrwRegulationNote = "Pasal 42 & Lampiran XII Perda RTRW No. 12/2025: Penetapan Jaringan Jalur Evakuasi Bencana, Tempat Evakuasi Sementara (TES), dan Tempat Evakuasi Akhir (TEA) di seluruh kecamatan.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                // Interactive Map Module with Disaster Prone Points & Evacuation Routes
                                item {
                                    InteractiveDisasterMapSection(
                                        currentDistrict = currentDistrict,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onOpenSopEvacuation = { isPanduanEvakuasiOpen = true },
                                        isDark = isActuallyDark
                                    )
                                }

                                // Kesiapsiagaan & SOP Evakuasi Mandiri Gempa Bumi & Tsunami
                                item {
                                    EarthquakeEvacuationPreparednessSection(
                                        currentDistrict = currentDistrict,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onTestTsunamiAlert = { viewModel.triggerTestTsunamiNotification(it) },
                                        onTestSevereWeatherAlert = { viewModel.triggerSuddenSevereWeatherPushNotification(it) },
                                        onOpenEvacuationGuide = { isPanduanEvakuasiOpen = true }
                                    )
                                }
                            }
                        }

                        8 -> {
                            // Tab 8: inaRISK BNPB (Tersinkronisasi dengan KRB 2026-2030 & terhubung ke Tab Khusus KRB & RTRW)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_inarisk_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "inarisk_bnpb",
                                        moduleTitle = "Portal Risiko inaRISK BNPB",
                                        krbAlignmentNote = "Indikator inaRISK telah disinkronkan 100% dengan KRB 2026–2030: Skor IRBI 2023–2024 = 164,32 (Tinggi), IKD 2025 = 0,29 (Rendah), IKM = 0,348 (Sedang), dan 12 matriks bahaya.",
                                        rpbAlignmentNote = "Dokumen RPB 2025–2029 (Gambar 3.15 & Tabel 4.1) mencatat pembaruan metodologi IRBI 2025 sebesar 228,28 dengan target penurunan IRB menjadi 136,87 & lonjakan IKD dari 0,29 menjadi 0,57 pada 2029.",
                                        rtrwRegulationNote = "Rincian lengkap pasal demi pasal Perda RTRW No. 12 Tahun 2025 (Pasal 42, 83, 92, 93, 94 & Sanksi KKPR Pasal 95–108) tersedia di Tab Khusus KRB, RPB & RTRW.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                item {
                                    InaRiskBnpbSection(
                                        currentDistrict = currentDistrict,
                                        onOpenEvacuationGuide = { viewModel.selectTab(7) },
                                        onCall117 = { EmergencyDialer.dial(context, "117") }
                                    )
                                }
                            }
                        }

                        9 -> {
                            // Tab 9: Cuaca Historis
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_historical_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "cuaca_historis",
                                        moduleTitle = "Rekam Jejak & Tren Cuaca Historis",
                                        krbAlignmentNote = "Arsip cuaca historis digunakan untuk kalibrasi kerentanan Kekeringan (Prioritas 1: 410.422,60 Ha; Rp 887,93 Miliar) dan anomali hidrometeorologi dalam Dokumen KRB 2026–2030.",
                                        rpbAlignmentNote = "Strategi 3.8 & 3.11 RPB 2025–2029: Mendukung perlindungan LP2B/KP2B & Pengendalian Dampak Perubahan Iklim (DPI Distan Kode 3.27.05.2.01.0002) terhadap 414.320,37 Ha potensi kekeringan.",
                                        rtrwRegulationNote = "Mendukung evaluasi berkala daya dukung & daya tampung lingkungan hidup pada Kawasan Pertanian Pangan Berkelanjutan (KP2B) Perda RTRW No. 12/2025.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

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

                        10 -> {
                            // Tab 10: Maritim & Pesisir Pantai
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
                                        com.example.ui.components.KrbRtrwModuleContextBanner(
                                            currentDistrict = currentDistrict,
                                            moduleTag = "maritim_pesisir",
                                            moduleTitle = "Maritim, Gelombang & Pesisir Samudra Hindia",
                                            krbAlignmentNote = "Prioritas 1 KRB 2026–2030: Gelombang Ekstrem dan Abrasi mengancam 2.892,69 Ha (tinggi gelombang signifikan s/d 2,7 m; 25.008 jiwa terpapar) di 7 kecamatan pesisir.",
                                            rpbAlignmentNote = "Strategi 1.1 & 3.6 RPB 2025–2029: Pengaman pantai/abrasi & rehabilitasi vegetasi pesisir/mangrove untuk melindungi 7 kecamatan pesisir dari ancaman GEA (tren meningkat; potensi rugi Rp 131,69 M).",
                                            rtrwRegulationNote = "Pasal 92 ayat (9) & Pasal 93 ayat (2) Perda RTRW No. 12/2025: Garis Sempadan Pantai minimal 100 meter dari titik pasang tertinggi (KDB 0%, KDH min 80%) & sabuk hijau mangrove/cemara laut.",
                                            isDark = isActuallyDark,
                                            onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                        )
                                    }

                                    item {
                                        MarinePesisirCard(
                                            district = currentDistrict,
                                            marine = report.marine
                                        )
                                    }
                                    // Info Pasang Surut & Pasang Naik Dinamis Samudra Hindia Mukomuko
                                    item {
                                        TidalInformationCard(district = currentDistrict)
                                    }

                                    // Historical Marine Wave Height Chart
                                    item {
                                        MarineWaveHistoryChart(district = currentDistrict)
                                    }
                                }
                            }
                        }

                        11 -> {
                            // Tab 11: Kualitas Udara Lengkap BMKG & Himbauan Kesehatan Masyarakat + Grafik Harian UV
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
                                        com.example.ui.components.KrbRtrwModuleContextBanner(
                                            currentDistrict = currentDistrict,
                                            moduleTag = "kualitas_udara",
                                            moduleTitle = "Kualitas Udara, Polutan PM2.5 & Indeks UV",
                                            krbAlignmentNote = "Pemantauan PM2.5/PM10 berkaitan langsung dengan dampak asap Karhutla (Prioritas 1: 404.993,70 Ha) serta risiko Epidemi dan Wabah Penyakit ISPA (Prioritas 3: 6.135 jiwa terpapar).",
                                            rpbAlignmentNote = "Strategi 3.10 & 3.11 RPB 2025–2029 (DLH Kode 2.11.03.2.01.0009): Pemantauan kualitas lingkungan & perluasan RTH untuk mendongkrak IKLH 66,20 → 69,58 & penurunan GRK 28,10% → 41,29% pada 2029.",
                                            rtrwRegulationNote = "Pasal 92 ayat (5) & ketentuan Ruang Terbuka Hijau (RTH) Perda RTRW No. 12/2025: Pengendalian emisi pembakaran lahan & penyediaan sabuk hijau penyangga.",
                                            isDark = isActuallyDark,
                                            onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                        )
                                    }

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

                        12 -> {
                            // Tab 12: 15 Kecamatan (Lengkap dengan Profil KRB 2026-2030 & Hierarki RTRW)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_all_districts_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    com.example.ui.components.KrbRtrwModuleContextBanner(
                                        currentDistrict = currentDistrict,
                                        moduleTag = "lima_belas_kecamatan",
                                        moduleTitle = "Direktori Cuaca & Risiko 15 Kecamatan",
                                        krbAlignmentNote = "Memuat profil risiko multibahaya KRB 2026–2030 dan Indeks Kapasitas Daerah (IKD) di seluruh 15 Kecamatan (148 Desa & 3 Kelurahan, luas 414.320,37 Ha).",
                                        rpbAlignmentNote = "Memetakan 11 Lokasi Resmi Tempat Penampungan Pengungsi RPB 2025–2029 (Kode 1.06.06.2.01.0007) & perlindungan kelompok rentan GEDSI (1.460 miskin P3KE & 269 disabilitas) di tiap kecamatan.",
                                        rtrwRegulationNote = "Setiap kartu kecamatan dilengkapi rincian Sistem Perkotaan Perda RTRW No. 12/2025 (1 PKL Kota Mukomuko, 4 PPK, dan 10 PPL) yang dapat dibuka/ditutup sesuai kebutuhan.",
                                        isDark = isActuallyDark,
                                        onOpenKrbRtrwTab = { viewModel.selectTab(2) }
                                    )
                                }

                                item {
                                    AllDistrictsView(
                                        districtsSummary = uiState.allDistrictsSummary,
                                        selectedDistrict = currentDistrict,
                                        favoriteDistricts = favorites,
                                        onSelectDistrict = { dist ->
                                            viewModel.selectDistrict(dist)
                                            viewModel.selectTab(0)
                                        },
                                        onOpenKrbRtrwDistrict = { dist ->
                                            viewModel.selectDistrict(dist)
                                            viewModel.selectTab(2)
                                        }
                                    )
                                }
                            }
                        }
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
            onSelectFormLaporan = {
                isQuickActionsFabExpanded = false
                val officerAuth = com.example.data.local.OfficerAuthManager(context)
                if (officerAuth.isOfficerLoggedIn()) {
                    context.startActivity(android.content.Intent(context, com.example.FormLaporanActivity::class.java))
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Login Petugas Pusdalops aktif diperlukan untuk membuka Form Laporan Harian.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    openRuangKerjaRequestId = 0
                    officerLoginRequestId += 1
                    viewModel.selectTab(1)
                }
            },
            onSelectCitizenReport = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(1)
            },
            onSelectForecast = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(4)
            },
            onSelectMaritimeGuide = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(10)
            },
            onSelectEvacuationProcedures = {
                isQuickActionsFabExpanded = false
                isPanduanEvakuasiOpen = true
            },
            onSelectDisasterMap = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(7)
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
            onOpenVolunteerModule = {
                isQuickActionsFabExpanded = false
                isVolunteerModuleDialogOpen = true
            },
            parentMaxWidth = this.maxWidth,
            parentMaxHeight = this.maxHeight,
            isDark = isActuallyDark
        )
        }
    }

    // Layar Penuh Modul Koordinasi Relawan Kebencanaan (Accessible directly from QuickActionsFab)
    if (isVolunteerModuleDialogOpen) {
        val officerAuthManager = remember { com.example.data.local.OfficerAuthManager(context) }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isVolunteerModuleDialogOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            com.example.ui.components.VolunteerCoordinationScreen(
                isOfficerLoggedIn = officerAuthManager.isOfficerLoggedIn(),
                officerName = officerAuthManager.getOfficerName(),
                officerNip = officerAuthManager.getOfficerBadgeId(),
                officerRole = officerAuthManager.getOfficerRole(),
                onClose = { isVolunteerModuleDialogOpen = false },
                onRequestOfficerLogin = {
                    isVolunteerModuleDialogOpen = false
                    officerLoginRequestId++
                    viewModel.selectTab(1)
                }
            )
        }
    }

    // Layar Penuh SOP Evakuasi Mandiri (Offline) — Fase Pra-Bencana
    if (isPanduanEvakuasiOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isPanduanEvakuasiOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            com.example.ui.components.PanduanEvakuasiScreen(
                onBackClick = { isPanduanEvakuasiOpen = false },
                onCall117 = { EmergencyDialer.dial(context, "117") }
            )
        }
    }

    // Call Center 117 Detail Bottom Sheet
    if (isCallCenter117SheetOpen) {
        CallCenter117DetailBottomSheet(
            onDismiss = { isCallCenter117SheetOpen = false },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onCallPoskoMukomuko = { EmergencyDialer.dial(context, "112") }
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
            onSimulateOlderVersion = {
                viewModel.simulateOlderVersionForTesting(
                    com.example.data.local.AppUpdatePreferences.PREVIOUS_VERSION_MOCK,
                    context
                )
            },
            onSimulatePublishNextVersion = { viewModel.simulatePublishNextVersion("1.3.1", context) },
            onResetLatestRelease = { viewModel.resetToLatestReleaseState() },
            onOpenOnboarding = { viewModel.setOnboardingOpen(true) },
            onOpenCompass = { viewModel.setCompassSheetOpen(true) },
            onOpenEvacuationGuide = { isPanduanEvakuasiOpen = true },
            onOpenDisasterWallpaper = { viewModel.setDisasterWallpaperDialogOpen(true) },
            onOpenDisclaimer = { legalDocumentsModalTab = 0 },
            onOpenPrivacyPolicy = { legalDocumentsModalTab = 1 }
        )
    }

    // Dialog Info Lanskap & Wallpaper HD (diakses dari menu Info Aplikasi)
    if (uiState.isDisasterWallpaperDialogOpen) {
        com.example.ui.components.MukomukoDisasterWallpaperDialog(
            onDismiss = { viewModel.setDisasterWallpaperDialogOpen(false) }
        )
    }

    // In-App OTA Update Dialog (Prompts only once per release, persistent in DataStore)
    if (!uiState.isStartupWallpaperVisible && uiState.isUpdateDialogOpen && uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated) {
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
                                ?: com.example.model.LATEST_RELEASE_CHANGELOG.highlights
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
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
            onOpenEvacuationMap = { viewModel.selectTab(7) },
            onCall117 = { EmergencyDialer.dial(context, "117") }
        )
    }

    // Emergency Warning Popup Notification Dialog (Active severe weather, high waves, or live Bengkulu 1-2h earthquake)
    val currentReport = uiState.report
    val activeEq = uiState.latestEarthquake
    val activePopupSignature = com.example.ui.components.EmergencyWarningPopupPolicy.buildActivePopupSignature(
        weatherAlert = currentReport?.alert,
        marineWeather = currentReport?.marine,
        latestEarthquake = activeEq
    )
    val hasEmergencyConditions = activePopupSignature.isNotBlank() &&
        !com.example.ui.components.EmergencyWarningPopupPolicy.isPopupSignatureAlreadyDismissed(
            context = context,
            signature = activePopupSignature
        )

    if (!uiState.isStartupWallpaperVisible && !isEmergencyPopupDismissed && hasEmergencyConditions && currentReport != null) {
        EmergencyWarningPopupDialog(
            weatherAlert = currentReport.alert,
            marineWeather = currentReport.marine,
            latestEarthquake = activeEq,
            earlyWarnings = uiState.earlyWarnings,
            isDark = isActuallyDark,
            onDismiss = {
                com.example.ui.components.EmergencyWarningPopupPolicy.markPopupSignatureDismissed(
                    context = context,
                    signature = activePopupSignature
                )
                isEmergencyPopupDismissed = true
            },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onOpenEvacuationMap = {
                viewModel.selectTab(7)
            }
        )
    }

    // High-Resolution Wallpaper & Mukomuko Disaster Mitigation Screen (Dual Entry Portal)
    val enterCitizenGuestMode: () -> Unit = {
        // Strict Role Isolation: Masuk Sebagai Tamu / Warga MUST terminate any lingering officer session,
        // detach officer-only Firestore listeners, clear cached security/audit artifacts, and open Citizen Home (Tab 0: Hari Ini).
        com.example.security.SecurityManager.getInstance(context).clearLocalSession()
        com.example.data.auth.AuthManager.getInstance(context).signOut()
        com.example.data.local.OfficerAuthManager(context).logoutOfficer()
        runCatching {
            com.example.data.sync.SyncController.getInstance(
                context,
                com.example.data.local.AppDatabase.getInstance(context)
            ).syncModeWithOfficerSession()
        }
        runCatching {
            com.example.data.repository.VolunteerRepository.getInstance(context).syncModeWithOfficerSession()
        }
        officerLoginRequestId = 0
        openRuangKerjaRequestId = 0
        viewModel.selectTab(0)
        viewModel.dismissStartupWallpaper()
    }
    StartupSplashWallpaperScreen(
        isVisible = uiState.isStartupWallpaperVisible,
        onDismiss = enterCitizenGuestMode,
        onEnterAsGuest = enterCitizenGuestMode,
        onOpenEvacuationGuide = {
            enterCitizenGuestMode()
            isPanduanEvakuasiOpen = true
        },
        onLoginAsOfficer = {
            openRuangKerjaRequestId = 0
            officerLoginRequestId += 1
            viewModel.selectTab(1) // Jump directly to Laporan Warga & Portal Konsol Petugas Pusdalops
            viewModel.dismissStartupWallpaper()
        },
        onBiometricLoginSuccess = {
            val officerAuth = com.example.data.local.OfficerAuthManager(context)
            if (officerAuth.isOfficerLoggedIn()) {
                officerLoginRequestId = 0
                openRuangKerjaRequestId += 1
            } else {
                openRuangKerjaRequestId = 0
                officerLoginRequestId += 1
            }
            viewModel.selectTab(1)
            viewModel.dismissStartupWallpaper()
        }
    )

    // Pop-up Disklaimer Singkat (Bagian B) sebelum memulai aplikasi (muncul sekali per versi)
    if (!isDisclaimerAccepted) {
        com.example.ui.components.SipagarFirstLaunchDisclaimerDialog(
            onAcceptAndContinue = {
                disclaimerConsentManager.recordConsentAccepted()
                isDisclaimerAccepted = true
            },
            onCallEmergency = { number -> EmergencyDialer.dial(context, number) }
        )
    }

    // Modal Baca Disklaimer Lengkap (Bagian A) & Kebijakan Privasi UU PDP (Bagian C) dari Menu
    val activeLegalTab = legalDocumentsModalTab
    if (activeLegalTab != null) {
        com.example.ui.components.SipagarLegalDocumentsDialog(
            initialTab = activeLegalTab,
            consentDateLabel = disclaimerConsentManager.getFormattedAcceptedDate(),
            onDismiss = { legalDocumentsModalTab = null },
            onCallEmergency = { number -> EmergencyDialer.dial(context, number) }
        )
    }
}
