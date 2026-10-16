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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.WeatherRepository
import com.example.model.DailyItem
import com.example.model.WeatherFullReport
import com.example.ui.components.AirQualityAndUvCard
import com.example.ui.components.AllDistrictsView
import com.example.ui.components.CallCenter117DetailBottomSheet
import com.example.ui.components.CommunityReportsSection
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.DistrictSelectorBottomSheet
import com.example.ui.components.EarlyWarningSection
import com.example.ui.components.EmergencyDialer
import com.example.ui.components.HeroWeatherCard
import com.example.ui.components.HistoricalWeatherSection
import com.example.ui.components.HourlyForecastCard
import com.example.ui.components.MarinePesisirCard
import com.example.ui.components.WeatherTabs
import com.example.ui.components.WeatherTopBar
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val database = AppDatabase.getInstance(context)
                val repository = WeatherRepository(database.weatherDao())
                val viewModel: WeatherViewModel = viewModel {
                    WeatherViewModel(repository)
                }

                WeatherAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteDistricts.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()

    var isCallCenter117SheetOpen by remember { mutableStateOf(false) }

    val currentDistrict = uiState.selectedDistrict
    val isFavorite = favorites.contains(currentDistrict.id)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WeatherTopBar(
                district = currentDistrict,
                isFavorite = isFavorite,
                isRefreshing = uiState.isRefreshing,
                onDistrictClick = { viewModel.setDistrictSheetOpen(true) },
                onFavoriteToggle = { viewModel.toggleFavorite(currentDistrict.id) },
                onRefreshClick = { viewModel.refresh() },
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

                                // Quick Call Center 117 Alert Strip
                                item {
                                    QuickEmergency117Strip(
                                        onCall117 = { EmergencyDialer.dial(context, "117") },
                                        onViewDetails = { isCallCenter117SheetOpen = true }
                                    )
                                }

                                // Active Alert Card if available
                                if (report.alert != null) {
                                    item {
                                        ActiveAlertCard(
                                            alert = report.alert,
                                            onViewDetail = { viewModel.selectTab(2) }
                                        )
                                    }
                                }

                                item {
                                    HourlyForecastCard(hourlyList = report.hourly)
                                }

                                item {
                                    SunAndAtmosphereCard(daily = report.daily.firstOrNull())
                                }

                                item {
                                    MarinePesisirCard(
                                        district = currentDistrict,
                                        marine = report.marine
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Tab 1: 7 Hari
                            val report = uiState.report
                            if (report != null) {
                                item {
                                    DailyForecastCard(dailyList = report.daily)
                                }
                            }
                        }

                        2 -> {
                            // Tab 2: Peringatan Dini (BMKG & BPBD Warnings + Call Center 117)
                            item {
                                EarlyWarningSection(
                                    warnings = uiState.earlyWarnings,
                                    onCall117 = { EmergencyDialer.dial(context, "117") },
                                    onOpenCallCenterInfo = { isCallCenter117SheetOpen = true }
                                )
                            }
                        }

                        3 -> {
                            // Tab 3: Laporan Langsung Masyarakat
                            item {
                                CommunityReportsSection(
                                    reports = communityReports,
                                    currentDistrict = currentDistrict,
                                    isDialogOpen = uiState.isReportDialogOpen,
                                    onOpenDialog = { viewModel.setReportDialogOpen(true) },
                                    onDismissDialog = { viewModel.setReportDialogOpen(false) },
                                    onSubmitReport = { distId, distName, reporter, cond, notes ->
                                        viewModel.submitCommunityReport(distId, distName, reporter, cond, notes)
                                    },
                                    onUpvote = { viewModel.upvoteReport(it) }
                                )
                            }
                        }

                        4 -> {
                            // Tab 4: Cuaca Historis
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

                        5 -> {
                            // Tab 5: Maritim & Pesisir Pantai
                            val report = uiState.report
                            if (report != null) {
                                item {
                                    MarinePesisirCard(
                                        district = currentDistrict,
                                        marine = report.marine
                                    )
                                }
                            }
                        }

                        6 -> {
                            // Tab 6: Kualitas Udara & Indeks UV
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

                        7 -> {
                            // Tab 7: 15 Kecamatan
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
}

@Composable
private fun QuickEmergency117Strip(
    onCall117: () -> Unit,
    onViewDetails: () -> Unit
) {
    Surface(
        color = Color(0xFFFEF2F2),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
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
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Bebas Pulsa 24 Jam",
                            fontSize = 10.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Layanan tanggap darurat bencana BPBD Mukomuko",
                        fontSize = 10.sp,
                        color = Color(0xFF7F1D1D),
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
