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

        // Jadwalkan WorkManager sinkronisasi periodik titik panas FIRMS (1-3 jam) & BMKG (3-6 jam)
        try {
            com.example.service.KarhutlaSyncScheduler.schedulePeriodicSync(this)
        } catch (e: Throwable) {
            Log.w("MainActivity", "KarhutlaSyncScheduler skipped: ${e.message}")
        }

        // Initialize BMKG Earthquake, Severe Weather, and Real-Time Officer Push Notification Channels
        try {
            com.example.util.DisasterTriageNotificationManager.initNotificationChannels(this)
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
        val navTab = notifIntent.getIntExtra("EXTRA_NAVIGATE_TAB", -1)
        val openUpdate = notifIntent.getBooleanExtra("EXTRA_OPEN_UPDATE_SHEET", false)
        val autoStartUpdate = notifIntent.getBooleanExtra("EXTRA_AUTO_START_UPDATE", false)
        val hasValidUpdate = uiState.updateState.isUpdateAvailable && !uiState.updateState.isUpdated
        if (navTab >= 0 || (openUpdate && hasValidUpdate) || (autoStartUpdate && hasValidUpdate)) {
            viewModel.dismissStartupWallpaper()
        }
        if (navTab >= 0) {
            viewModel.selectTab(navTab)
            notifIntent.removeExtra("EXTRA_NAVIGATE_TAB")
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
                                                        onViewDetail = { viewModel.selectTab(4) }
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

                                            // Real-Time WRS-NTWC BMKG Earthquake Quick Banner
                                            val latestEq = uiState.latestEarthquake
                                            if (latestEq != null) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    WrsLiveEarthquakeQuickCard(
                                                        earthquake = latestEq,
                                                        onClick = { viewModel.selectTab(4) }
                                                    )
                                                }
                                            }

                                            // 4 Aksi Darurat 1-Ketuk untuk Kondisi Panik / Aksesibilitas Warga
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                com.example.ui.components.CitizenEmergencyQuickActionPanel(
                                                    activeAlertCount = uiState.earlyWarnings.size + if (report.alert != null) 1 else 0,
                                                    hasNearEarthquake = latestEq?.isNearMukomuko == true,
                                                    onCall117 = { EmergencyDialer.dial(context, "117") },
                                                    onOpenQuickReport = { viewModel.selectTab(1) },
                                                    onOpenEarlyWarning = { viewModel.selectTab(4) },
                                                    onOpenEvacuationGuide = { isPanduanEvakuasiOpen = true }
                                                )
                                            }

                                            // Hero Weather Overview Card with Dynamic Animated Weather Icon
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                HeroWeatherCard(report = report)
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
                                onBackClick = {
                                    viewModel.selectTab(0)
                                }
                            )
                        }

                        2 -> {
                            // Tab 2: Statistik & Tren Bencana Bulanan (Grafik Batang & Pie Chart Interaktif)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_monthly_disaster_analytics_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
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

                        3 -> {
                            // Tab 3: Prakiraan Cuaca
                            val report = uiState.report
                            if (report != null) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tab_forecast_list"),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
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

                        4 -> {
                            // Tab 4: Peringatan Dini Cuaca & Bencana + BMKG Gempa Real-time + Push Notification
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
                                        onOpenEvacuationGuide = { viewModel.selectTab(6) }
                                    )
                                }
                            }
                        }

                        5 -> {
                            // Tab 5: Monitoring Karhutla (VIIRS FIRMS & BMKG Hotspot Sync + OpenStreetMap + Rilis Resmi BMKG 2025-Kini)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("tab_monitoring_karhutla_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
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

                        6 -> {
                            // Tab 6: Modul Peta Interaktif Bencana & SOP Evakuasi Mandiri
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
                                        onTestSevereWeatherAlert = { viewModel.triggerSuddenSevereWeatherPushNotification(it) },
                                        onOpenEvacuationGuide = { isPanduanEvakuasiOpen = true }
                                    )
                                }
                            }
                        }

                        7 -> {
                            // Tab 7: inaRISK BNPB - Kajian Risiko Bencana Indonesia Kabupaten Mukomuko
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
                                        onOpenEvacuationGuide = { viewModel.selectTab(6) },
                                        onCall117 = { EmergencyDialer.dial(context, "117") }
                                    )
                                }
                            }
                        }

                        8 -> {
                            // Tab 8: Cuaca Historis
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

                        9 -> {
                            // Tab 9: Maritim & Pesisir Pantai
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

                        10 -> {
                            // Tab 10: Kualitas Udara Lengkap BMKG & Himbauan Kesehatan Masyarakat + Grafik Harian UV
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

                        11 -> {
                            // Tab 11: 15 Kecamatan
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
                viewModel.selectTab(3)
            },
            onSelectMaritimeGuide = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(9)
            },
            onSelectEvacuationProcedures = {
                isQuickActionsFabExpanded = false
                isPanduanEvakuasiOpen = true
            },
            onSelectDisasterMap = {
                isQuickActionsFabExpanded = false
                viewModel.selectTab(6)
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
            onSimulatePublishNextVersion = { viewModel.simulatePublishNextVersion("1.2.0", context) },
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
            onOpenEvacuationMap = { viewModel.selectTab(6) },
            onCall117 = { EmergencyDialer.dial(context, "117") }
        )
    }

    // Emergency Warning Popup Notification Dialog (Active severe weather, high waves, or BMKG earthquake)
    val currentReport = uiState.report
    val activeEq = uiState.latestEarthquake
    val hasEmergencyConditions = (currentReport?.alert != null) ||
            (currentReport?.marine != null && (currentReport.marine.waveHeight >= 2.0 || currentReport.marine.seaCondition.contains("Tinggi", ignoreCase = true))) ||
            (activeEq != null && (activeEq.magnitude >= 5.0 || activeEq.isNearMukomuko || activeEq.potentialTsunami.contains("tsunami", ignoreCase = true)))

    if (!uiState.isStartupWallpaperVisible && !isEmergencyPopupDismissed && hasEmergencyConditions && currentReport != null) {
        EmergencyWarningPopupDialog(
            weatherAlert = currentReport.alert,
            marineWeather = currentReport.marine,
            latestEarthquake = activeEq,
            earlyWarnings = uiState.earlyWarnings,
            isDark = isActuallyDark,
            onDismiss = { isEmergencyPopupDismissed = true },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onOpenEvacuationMap = {
                viewModel.selectTab(6)
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
