package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.model.DailyItem
import com.example.model.WeatherFullReport
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.AirQualityAndUvCard
import com.example.ui.components.AllDistrictsView
import com.example.ui.components.AppUpdateBottomSheet
import com.example.ui.components.CallCenter117DetailBottomSheet
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.DistrictSelectorBottomSheet
import com.example.ui.components.EarlyWarningSection
import com.example.ui.components.EarthquakeEvacuationPreparednessSection
import com.example.ui.components.EmergencyDialer
import com.example.ui.components.GeminiWeatherSection
import com.example.ui.components.HeroWeatherCard
import com.example.ui.components.HistoricalWeatherSection
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.InaRiskBnpbSection
import com.example.ui.components.MarinePesisirCard
import com.example.ui.components.MarineWaveHistoryChart
import com.example.ui.components.WeeklyRainfallBarChartCard
import com.example.ui.components.WeatherTabs
import com.example.ui.components.WeatherTopBar
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.viewmodel.AiWeatherViewModel
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BMKG Earthquake and Severe Weather Notification Channels
        com.example.util.EarthquakeNotificationHelper.initNotificationChannel(this)
        com.example.util.WeatherNotificationHelper.initNotificationChannel(this)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = AppDatabase.getInstance(context)
            val repository = WeatherRepository(database.weatherDao())
            val weatherViewModel: WeatherViewModel = viewModel {
                WeatherViewModel(repository)
            }
            val aiViewModel: AiWeatherViewModel = viewModel()
            val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDark = uiState.isDarkMode ?: systemInDark

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

    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val aiChatMessages by aiViewModel.chatMessages.collectAsStateWithLifecycle()

    var isCallCenter117SheetOpen by remember { mutableStateOf(false) }


    val currentDistrict = uiState.selectedDistrict
    val isFavorite = favorites.contains(currentDistrict.id)

    // Handle incoming navigation from BMKG earthquake notifications
    val initialTab = remember {
        (context as? android.app.Activity)?.intent?.getIntExtra("EXTRA_NAVIGATE_TAB", -1) ?: -1
    }
    LaunchedEffect(initialTab) {
        if (initialTab >= 0) {
            viewModel.selectTab(initialTab)
        }
    }

    val systemInDark = isSystemInDarkTheme()
    val isActuallyDark = uiState.isDarkMode ?: systemInDark

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WeatherTopBar(
                district = currentDistrict,
                isFavorite = isFavorite,
                isRefreshing = uiState.isRefreshing,
                isDarkMode = isActuallyDark,
                onDistrictClick = { viewModel.setDistrictSheetOpen(true) },
                onFavoriteToggle = { viewModel.toggleFavorite(currentDistrict.id) },
                onRefreshClick = { viewModel.refresh() },
                onThemeToggle = { viewModel.toggleDarkMode(isActuallyDark) },
                onOpenAppMenu = { viewModel.setAppUpdateSheetOpen(true) },
                onCallCenter117Click = { isCallCenter117SheetOpen = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Tab Navigation
            WeatherTabs(
                selectedTabIndex = uiState.selectedTab,
                warningCount = uiState.earlyWarnings.size,
                reportCount = communityReports.size,
                onTabSelected = { viewModel.selectTab(it) }
            )

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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("weather_content_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (uiState.selectedTab) {
                        0 -> {
                            // Tab 0: Hari Ini (Summary + Hourly + Astro Highlights)
                            val report = uiState.report
                            if (report != null) {
                                item {
                                    HeroWeatherCard(report = report)
                                }

                                // Live BMKG Earthquake Banner
                                val latestEq = uiState.latestEarthquake
                                if (latestEq != null) {
                                    item {
                                        LiveEarthquakeQuickCard(
                                            earthquake = latestEq,
                                            onClick = { viewModel.selectTab(3) }
                                        )
                                    }
                                }

                                // Quick Kesiapsiagaan Evakuasi Mandiri Gempa Bumi Banner
                                item {
                                    QuickEvacuationPreparednessBanner(
                                        isDark = isActuallyDark,
                                        onOpenGuide = { viewModel.selectTab(4) }
                                    )
                                }

                                // Quick Gemini AI Prompt Teaser
                                item {
                                    QuickGeminiAdvisorBanner(
                                        isDark = isActuallyDark,
                                        onAskAi = { viewModel.selectTab(1) }
                                    )
                                }

                                // Quick Call Center 117 Alert Strip
                                item {
                                    QuickEmergency117Strip(
                                        isDark = isActuallyDark,
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onViewDetails = { isCallCenter117SheetOpen = true }
                                    )
                                }

                                // Active Alert Card if available
                                if (report.alert != null) {
                                    item {
                                        ActiveAlertCard(
                                            alert = report.alert,
                                            onViewDetail = { viewModel.selectTab(3) }
                                        )
                                    }
                                }

                                item {
                                    HourlyForecastCard(hourlyList = report.hourly)
                                }

                                item {
                                    SunAndAtmosphereCard(daily = report.daily.firstOrNull())
                                }

                                // Weekly Rainfall Precipitation Bar Chart Card (Recharts-inspired Data Visualization)
                                item {
                                    WeeklyRainfallBarChartCard(
                                        currentDistrict = currentDistrict,
                                        dailyList = report.daily
                                    )
                                }

                                item {
                                    MarinePesisirCard(
                                        district = currentDistrict,
                                        marine = report.marine
                                    )
                                }

                                // Historical Marine Wave Height Chart (Recharts-inspired Data Visualization)
                                item {
                                    MarineWaveHistoryChart(district = currentDistrict)
                                }
                            }
                        }

                        1 -> {
                            // Tab 1: Tanya AI (Gemini)
                            item {
                                GeminiWeatherSection(
                                    currentDistrict = currentDistrict,
                                    weatherReport = uiState.report,
                                    chatMessages = aiChatMessages,
                                    uiState = aiUiState,
                                    onSendMessage = { prompt ->
                                        aiViewModel.askGemini(
                                            prompt = prompt,
                                            currentDistrict = currentDistrict,
                                            weatherReport = uiState.report
                                        )
                                    }
                                )
                            }
                        }

                        2 -> {
                            // Tab 2: 7 Hari
                            val report = uiState.report
                            if (report != null) {
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

                        3 -> {
                            // Tab 3: Peringatan Dini Cuaca & Bencana + BMKG Gempa Real-time + Call Center 117
                            item {
                                EarlyWarningSection(
                                    warnings = uiState.earlyWarnings,
                                    currentDistrict = currentDistrict,
                                    weatherAlertSettings = uiState.weatherAlertSettings,
                                    onUpdateWeatherAlertSettings = { viewModel.updateWeatherAlertSettings(it) },
                                    onTestWeatherAlertNotification = { viewModel.triggerTestWeatherAlertNotification(it) },
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

                        4 -> {
                            // Tab 4: Kesiapsiagaan & SOP Evakuasi Mandiri Gempa Bumi
                            item {
                                EarthquakeEvacuationPreparednessSection(
                                    currentDistrict = currentDistrict,
                                    onCall117 = { EmergencyDialer.dial(context, "117") },
                                    onTestTsunamiAlert = { viewModel.triggerTestTsunamiNotification(it) },
                                    onTestSevereWeatherAlert = { viewModel.triggerTestWeatherAlertNotification(it) }
                                )
                            }
                        }

                        5 -> {
                            // Tab 5: inaRISK BNPB - Kajian Risiko Bencana Indonesia Kabupaten Mukomuko
                            item {
                                InaRiskBnpbSection(
                                    currentDistrict = currentDistrict,
                                    onOpenEvacuationGuide = { viewModel.selectTab(4) },
                                    onCall117 = { EmergencyDialer.dial(context, "117") }
                                )
                            }
                        }

                        6 -> {
                            // Tab 6: Cuaca Historis
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

                        7 -> {
                            // Tab 7: Maritim & Pesisir Pantai
                            val report = uiState.report
                            if (report != null) {
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

                        8 -> {
                            // Tab 8: Kualitas Udara & Indeks UV
                            val report = uiState.report
                            if (report != null) {
                                val maxUv = report.daily.firstOrNull()?.uvIndexMax ?: 8.0
                                item {
                                    AirQualityAndUvCard(
                                        airQuality = report.airQuality,
                                        uvIndex = maxUv
                                    )
                                }
                            }
                        }

                        9 -> {
                            // Tab 9: 15 Kecamatan
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

    // Call Center 117 Detail Bottom Sheet
    if (isCallCenter117SheetOpen) {
        CallCenter117DetailBottomSheet(
            onDismiss = { isCallCenter117SheetOpen = false },
            onCall117 = { EmergencyDialer.dial(context, "117") },
            onCallPoskoMukomuko = { EmergencyDialer.dial(context, "073771112") }
        )
    }

    // District Selector Bottom Sheet
    if (uiState.isDistrictSheetOpen) {
        DistrictSelectorBottomSheet(
            selectedDistrict = currentDistrict,
            searchQuery = uiState.districtSearchQuery,
            favoriteDistricts = favorites,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSelectDistrict = { viewModel.selectDistrict(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
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
            onCheckUpdateOnline = { viewModel.checkForUpdatesOnline() },
            onDownloadUpdate = { viewModel.downloadAndApplyUpdate() }
        )
    }
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
    daily: DailyItem?
) {
    val sunrise = daily?.sunrise ?: "06:05"
    val sunset = daily?.sunset ?: "18:15"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sun_astro_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Matahari Terbit",
                    tint = OrangeSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Terbit (WIB)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Text(
                        text = sunrise,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbTwilight,
                    contentDescription = "Matahari Terbenam",
                    tint = OrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Terbenam (WIB)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Text(
                        text = sunset,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveEarthquakeQuickCard(
    earthquake: com.example.model.BmkgEarthquake,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("live_earthquake_quick_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFDC2626), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GEMPA BMKG M ${earthquake.magnitude}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${earthquake.region} (${earthquake.timeWib})",
                    fontSize = 11.sp,
                    color = Color(0xFF7F1D1D),
                    maxLines = 1
                )
            }

            Text(
                text = "Detail >",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
            )
        }
    }
}

