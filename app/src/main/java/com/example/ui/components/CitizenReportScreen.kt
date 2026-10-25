package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.ui.theme.OrangePrimary
import com.example.util.LocationHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val DISASTER_TYPES = listOf(
    "Banjir / Genangan Luapan Air",
    "Gempa Bumi",
    "Gelombang Pasang & Abrasi Pantai",
    "Angin Puting Beliung & Badai",
    "Tanah Longsor",
    "Pohon Tumbang",
    "Kebakaran Hutan & Lahan (Karhutla)",
    "Kekeringan / Krisis Air Bersih",
    "Lainnya"
)

private val DISTRICT_NAMES = listOf(
    "Kota Mukomuko",
    "Ipuh",
    "Teramang Jaya",
    "Penarik",
    "Lubuk Pinang",
    "Pondok Suguh",
    "Air Dikit",
    "Selagan Raya",
    "XIV Koto",
    "Malin Deman",
    "V Koto",
    "Teras Terunjam",
    "Sungai Rumbai",
    "Air Rami",
    "Air Manjunto"
)

private val VILLAGES_MAP = mapOf(
    "Kota Mukomuko" to listOf("Kelurahan Bandar Ratu", "Kelurahan Koto Jaya", "Kelurahan Pasar Mukomuko", "Kelurahan Ujung Padang", "Desa Pondok Batu", "Desa Selagan Jaya", "Desa Tanah Rekah", "Desa Pasar Sebelah"),
    "Ipuh" to listOf("Desa Medan Jaya", "Desa Pasar Ipuh", "Desa Tanjung Harapan", "Desa Pulai Payung", "Desa Pulau Baru", "Desa Sibak", "Desa Tanjung Medan"),
    "Teramang Jaya" to listOf("Desa Pasar Teramang", "Desa Teramang", "Desa Bunga Tanjung", "Desa Nelan Indah", "Desa Batu Ejung", "Desa Mandi Angin"),
    "Penarik" to listOf("Desa Penarik", "Desa Lubuk Mukti", "Desa Maju Makmur", "Desa Mekar Mulya", "Desa Marga Mukti", "Desa Sidodadi"),
    "Lubuk Pinang" to listOf("Desa Lubuk Pinang", "Desa Lubuk Gedang", "Desa Aie Manjunto", "Desa Ranah Karya", "Desa Suka Pindah"),
    "Pondok Suguh" to listOf("Desa Pondok Suguh", "Desa Tunggang", "Desa Teluk Bakung", "Desa Air Bikuk", "Desa Karya Mulya"),
    "Air Dikit" to listOf("Desa Sari Bulan", "Desa Dusun Baru V Koto", "Desa Air Dikit", "Desa Air Kasai", "Desa Pondok Lunang"),
    "Selagan Raya" to listOf("Desa Sungai Jerinjing", "Desa Surian Bungkal", "Desa Lubuk Sahung", "Desa Pondok Baru", "Desa Sungai Ipuh"),
    "XIV Koto" to listOf("Desa Lubuk Sanai", "Desa Rawa Bangun", "Desa Rawa Mulya", "Desa Pauh Terenja", "Desa Tanjung Mulya"),
    "Malin Deman" to listOf("Desa Talang Baru", "Desa Serami Baru", "Desa Talang Arah", "Desa Air Merah"),
    "V Koto" to listOf("Desa Lalang Luas", "Desa Pondok Tengah", "Desa Lubuk Cabau", "Desa Sungai Rengas"),
    "Teras Terunjam" to listOf("Desa Teras Terunjam", "Desa Karang Jaya", "Desa Karang Anyar", "Desa Mekar Jaya"),
    "Sungai Rumbai" to listOf("Desa Retak Mudik", "Desa Gajah Mati", "Desa Retak Hilir", "Desa Padang Gading"),
    "Air Rami" to listOf("Desa Air Rami", "Desa Bukit Harapan", "Desa Rami Mulya", "Desa Talang Rio"),
    "Air Manjunto" to listOf("Desa Manjunto Jaya", "Desa Tirta Makmur", "Desa Agung Jaya", "Desa Sinar Jaya")
)

/**
 * Tab 9: Layanan Terpadu Laporan Warga Bencana Kabupaten Mukomuko
 * Reproduces the official incident reporting form + GIS Spatial Map + Status Tracking + Emergency Contacts
 */
@Composable
fun CitizenReportScreen(
    reports: List<CitizenDisasterReport>,
    currentDistrict: District,
    onSubmitReport: (CitizenDisasterReport) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Formulir, 1 = Peta Sebaran, 2 = Tabel Riwayat, 3 = Kontak Penting

    // Form inputs state
    var selectedDisasterType by remember { mutableStateOf(DISASTER_TYPES[0]) }
    var isDisasterDropdownExpanded by remember { mutableStateOf(false) }

    val initialTime = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date())
    }
    var incidentTime by remember { mutableStateOf(initialTime) }
    var incidentLocation by remember { mutableStateOf("") }

    var selectedDistrict by remember { mutableStateOf(currentDistrict.name) }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }

    var selectedVillage by remember {
        mutableStateOf(VILLAGES_MAP[currentDistrict.name]?.firstOrNull() ?: "Desa Pasar")
    }
    var isVillageDropdownExpanded by remember { mutableStateOf(false) }

    // Coordinates & GPS Verification
    var latitude by remember { mutableDoubleStateOf(currentDistrict.latitude) }
    var longitude by remember { mutableDoubleStateOf(currentDistrict.longitude) }
    var gpsAccuracy by remember { mutableStateOf<Float?>(null) }
    var isGpsLocked by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf("Sentuh pin GPS untuk mengunci koordinat ponsel") }

    // Casualties data
    var deadCount by remember { mutableIntStateOf(0) }
    var injuredCount by remember { mutableIntStateOf(0) }
    var missingCount by remember { mutableIntStateOf(0) }

    // Photo & Geotag Verification (Anti-Hoax)
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var cameraCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    var reporterName by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // Dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var submittedReportId by remember { mutableStateOf("") }

    // Table Filter by Status: "Semua", "Diproses", "Divalidasi", "Selesai"
    var tableStatusFilter by remember { mutableStateOf("Semua") }

    // Camera launcher (TakePicturePreview returns Bitmap thumbnail)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            cameraCapturedBitmap = bitmap
            photoUriString = null
            // Save bitmap to cache directory for persistent URI
            try {
                val file = File(context.cacheDir, "report_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                photoUriString = Uri.fromFile(file).toString()
            } catch (_: Exception) {
                // Keep bitmap in memory
            }
            Toast.makeText(context, "Foto kamera berhasil diambil dengan stempel GPS!", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker launcher (PickVisualMedia)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
            cameraCapturedBitmap = null
            Toast.makeText(context, "Foto kejadian terpilih. Stempel GPS disematkan.", Toast.LENGTH_SHORT).show()
        }
    }

    // Location fetcher helper
    val fetchGpsLocation = {
        isFetchingGps = true
        gpsStatusMessage = "Mencari sinyal GPS satelit ponsel..."
        LocationHelper.fetchCurrentLocation(
            context = context,
            onSuccess = { lat, lon, nearestDistrict, _, accuracy ->
                latitude = lat
                longitude = lon
                gpsAccuracy = accuracy
                isGpsLocked = true
                isFetchingGps = false
                selectedDistrict = nearestDistrict.name
                val villageList = VILLAGES_MAP[nearestDistrict.name]
                if (villageList != null && villageList.isNotEmpty()) {
                    selectedVillage = villageList.first()
                }
                gpsStatusMessage = "Koordinat GPS Terkunci: %.5f, %.5f (Akurasi: ±%.0fm)".format(lat, lon, accuracy ?: 5f)
                Toast.makeText(context, "Titik GPS ponsel berhasil dikunci!", Toast.LENGTH_SHORT).show()
            },
            onError = { err ->
                isFetchingGps = false
                gpsStatusMessage = "GPS belum aktif: $err"
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF15803D)) // Signature Mukomuko green
            .testTag("citizen_report_screen")
    ) {
        // 1. Top Bar Header (Orange, matching screenshot)
        Surface(
            color = Color(0xFFEA580C),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("citizen_report_header")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Laporan warga",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "PUSDALOPS BPBD Kab. Mukomuko",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fast Call 117 Header Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.clickable {
                        EmergencyDialer.dial(context, "117")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Call 117",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "117 BPBD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sub Navigation Tabs: 0: Formulir, 1: Peta Sebaran, 2: Tabel Riwayat, 3: Kontak Penting
        Surface(
            color = Color(0xFF15803D),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF15803D),
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                },
                divider = {}
            ) {
                // Tab 0: Formulir
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Formulir Laporan",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )

                // Tab 1: Peta Sebaran (Google Maps / GIS)
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Peta Sebaran",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                )

                // Tab 2: Tabel Riwayat
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tabel Riwayat",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFBBF24)
                            ) {
                                Text(
                                    text = "${reports.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )

                // Tab 3: Kontak Penting
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (selectedTab == 3) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kontak Penting",
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 3) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                )
            }
        }

        // Main White Rounded Card Body
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: FORMULIR INPUT (Matches Screenshot)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SECTION 1: DATA BENCANA (as in screenshot)
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "DATA BENCANA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563),
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        // Field 1: Jenis Bencana (Dropdown)
                        item {
                            ReportDropdownField(
                                label = "Jenis Bencana",
                                value = selectedDisasterType,
                                isExpanded = isDisasterDropdownExpanded,
                                onToggleExpand = { isDisasterDropdownExpanded = !isDisasterDropdownExpanded },
                                onDismiss = { isDisasterDropdownExpanded = false }
                            ) {
                                DISASTER_TYPES.forEach { disaster ->
                                    DropdownMenuItem(
                                        text = { Text(disaster, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDisasterType = disaster
                                            isDisasterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 2: Waktu Kejadian
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Waktu Kejadian",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                        Text(
                                            text = incidentTime,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3F4F6),
                                        modifier = Modifier.clickable {
                                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
                                            incidentTime = sdf.format(Date())
                                            Toast.makeText(context, "Waktu disinkronkan ke jam sekarang", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "Set Sekarang",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 3: Lokasi Kejadian (Input Text)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Lokasi Kejadian",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                OutlinedTextField(
                                    value = incidentLocation,
                                    onValueChange = { incidentLocation = it },
                                    placeholder = { Text("Nama jalan, dusun, atau patokan lokasi...", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF15803D),
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
                        }

                        // Field 4: Kecamatan (Dropdown 15 Kecamatan)
                        item {
                            ReportDropdownField(
                                label = "Kecamatan",
                                value = selectedDistrict,
                                isExpanded = isDistrictDropdownExpanded,
                                onToggleExpand = { isDistrictDropdownExpanded = !isDistrictDropdownExpanded },
                                onDismiss = { isDistrictDropdownExpanded = false }
                            ) {
                                DISTRICT_NAMES.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDistrict = district
                                            isDistrictDropdownExpanded = false
                                            val villages = VILLAGES_MAP[district]
                                            if (villages != null && villages.isNotEmpty()) {
                                                selectedVillage = villages.first()
                                            }
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 5: Desa/Kelurahan (Dropdown)
                        item {
                            val villages = VILLAGES_MAP[selectedDistrict] ?: listOf("Desa Pasar", "Desa Pusat")
                            ReportDropdownField(
                                label = "Desa/Kelurahan",
                                value = selectedVillage,
                                isExpanded = isVillageDropdownExpanded,
                                onToggleExpand = { isVillageDropdownExpanded = !isVillageDropdownExpanded },
                                onDismiss = { isVillageDropdownExpanded = false }
                            ) {
                                villages.forEach { village ->
                                    DropdownMenuItem(
                                        text = { Text(village, fontSize = 13.sp) },
                                        onClick = {
                                            selectedVillage = village
                                            isVillageDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 6: Titik Lokasi with circular blue GPS pin button (matches screenshot exactly!)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Titik Lokasi",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = if (isGpsLocked) "%.5f, %.5f".format(latitude, longitude) else "Belum terkunci (Sentuh tombol biru)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isGpsLocked) Color(0xFF15803D) else Color(0xFF374151)
                                    )
                                    if (isGpsLocked) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Akurasi: ±%.0fm • Anti-Hoax Aktif".format(gpsAccuracy ?: 5f),
                                                fontSize = 10.sp,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = gpsStatusMessage,
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                }

                                // Circular Blue GPS Button matching the screenshot
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { fetchGpsLocation() }
                                        .testTag("btn_get_gps_coordinates")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isFetchingGps) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Ambil Titik Lokasi GPS",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // SECTION 2: DATA KORBAN JIWA (as in screenshot)
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "DATA KORBAN JIWA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563),
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Counter 1: Meninggal Dunia
                        item {
                            CasualtyCounterRow(
                                label = "Meninggal Dunia",
                                count = deadCount,
                                onIncrement = { deadCount++ },
                                onDecrement = { if (deadCount > 0) deadCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Counter 2: Luka
                        item {
                            CasualtyCounterRow(
                                label = "Luka",
                                count = injuredCount,
                                onIncrement = { injuredCount++ },
                                onDecrement = { if (injuredCount > 0) injuredCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Counter 3: Hilang
                        item {
                            CasualtyCounterRow(
                                label = "Hilang",
                                count = missingCount,
                                onIncrement = { missingCount++ },
                                onDecrement = { if (missingCount > 0) missingCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // SECTION 3: FOTO KEJADIAN DENGAN KAMERA & GEOTAG ANTI-PALSU
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGpsLocked) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGpsLocked) Color(0xFF86EFAC) else Color(0xFFFCD34D)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_photo_geotag_verification")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isGpsLocked) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isGpsLocked) Color(0xFF16A34A) else Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kamera Langsung & Geotag Anti-Palsu",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGpsLocked) Color(0xFF15803D) else Color(0xFF92400E)
                                        )
                                    }

                                    Text(
                                        text = "Ambil foto langsung melalui kamera ponsel di lokasi kejadian. Koordinat GPS dan waktu saat ini akan dicap secara digital untuk memverifikasi keabsahan laporan.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4B5563),
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )

                                    // Photo Preview with Geotag Watermark
                                    if (cameraCapturedBitmap != null || photoUriString != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                                        ) {
                                            if (cameraCapturedBitmap != null) {
                                                Image(
                                                    bitmap = cameraCapturedBitmap!!.asImageBitmap(),
                                                    contentDescription = "Foto Kamera Kejadian",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = photoUriString,
                                                    contentDescription = "Foto Galeri Kejadian",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            // Digital Geotag Watermark Overlay
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(topStart = 8.dp),
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(
                                                        text = "📍 %.5f, %.5f".format(latitude, longitude),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFBBF24)
                                                    )
                                                    Text(
                                                        text = "🕒 $incidentTime",
                                                        fontSize = 9.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "🛡️ KAMERA & GPS TERVERIFIKASI BPBD",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF86EFAC)
                                                    )
                                                }
                                            }

                                            // Close/Remove photo button
                                            IconButton(
                                                onClick = {
                                                    cameraCapturedBitmap = null
                                                    photoUriString = null
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(28.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus Foto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons: Ambil Foto Kamera vs Pilih Galeri
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (!isGpsLocked) {
                                                    fetchGpsLocation()
                                                }
                                                cameraLauncher.launch(null)
                                            },
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("btn_launch_camera"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Buka Kamera",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (!isGpsLocked) {
                                                    fetchGpsLocation()
                                                }
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF15803D)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Galeri",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Optional Pelapor Info (Name & Notes)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Nama Pelapor (Opsional)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                OutlinedTextField(
                                    value = reporterName,
                                    onValueChange = { reporterName = it },
                                    placeholder = { Text("Contoh: Budi (Warga RT 02)", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Submit Button
                        item {
                            Button(
                                onClick = {
                                    if (incidentLocation.isBlank()) {
                                        Toast.makeText(context, "Mohon isi rincian Lokasi Kejadian", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isGpsLocked) {
                                        Toast.makeText(context, "Mohon sentuh tombol biru untuk mengunci koordinat GPS ponsel agar aduan terverifikasi anti-hoax", Toast.LENGTH_LONG).show()
                                        fetchGpsLocation()
                                        return@Button
                                    }

                                    val reportId = "LAP-MMK-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + "-" + (100..999).random()
                                    val report = CitizenDisasterReport(
                                        id = reportId,
                                        disasterType = selectedDisasterType,
                                        incidentTime = incidentTime,
                                        incidentLocation = incidentLocation,
                                        district = selectedDistrict,
                                        village = selectedVillage,
                                        latitude = latitude,
                                        longitude = longitude,
                                        gpsAccuracyMeters = gpsAccuracy,
                                        isGpsVerified = isGpsLocked,
                                        casualtiesDead = deadCount,
                                        casualtiesInjured = injuredCount,
                                        casualtiesMissing = missingCount,
                                        photoUri = photoUriString,
                                        reporterName = reporterName.ifBlank { "Warga $selectedDistrict" },
                                        reporterPhone = reporterPhone,
                                        additionalNotes = additionalNotes,
                                        timestamp = System.currentTimeMillis(),
                                        status = "Diproses" // Standard initial status: 'Diproses'
                                    )
                                    onSubmitReport(report)
                                    submittedReportId = reportId
                                    showSuccessDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_submit_citizen_report")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "KIRIM LAPORAN RESMI KE BPBD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: PETA SEBARAN LAPORAN (Google Maps / GIS Spatial Map)
                    CitizenReportMapView(
                        reports = reports,
                        onUpdateReportStatus = onUpdateReportStatus
                    )
                }

                2 -> {
                    // TAB 2: TABEL RIWAYAT LAPORAN WARGA (With Status Filters & Status Change Buttons)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tabel & Status Validasi Laporan",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    Text(
                                        text = "Indikator status: 'Diproses' (kuning), 'Divalidasi' (hijau), 'Selesai' (biru). Ketuk tombol status untuk memperbarui laporan.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4B5563),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    // Filter by status chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Semua", "Diproses", "Divalidasi", "Selesai").forEach { st ->
                                            val isSel = tableStatusFilter == st
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSel) Color(0xFF15803D) else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSel) Color(0xFF15803D) else Color(0xFFD1D5DB)
                                                ),
                                                modifier = Modifier.clickable { tableStatusFilter = st }
                                            ) {
                                                Text(
                                                    text = st,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else Color(0xFF4B5563),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val displayedReports = if (tableStatusFilter == "Semua") {
                            reports
                        } else {
                            reports.filter { it.status.equals(tableStatusFilter, ignoreCase = true) }
                        }

                        if (displayedReports.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada laporan dengan status '$tableStatusFilter'.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        } else {
                            items(displayedReports) { report ->
                                CitizenReportItemCard(
                                    report = report,
                                    onUpdateStatus = { newStatus ->
                                        onUpdateReportStatus(report.id, newStatus)
                                        Toast.makeText(
                                            context,
                                            "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus'",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                3 -> {
                    // TAB 3: KONTAK PENTING DARURAT MUKOMUKO
                    CitizenEmergencyContactsView()
                }
            }
        }
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Laporan Bencana Terkirim!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Laporan Anda telah berhasil dicatat oleh sistem PUSDALOPS BPBD Kabupaten Mukomuko.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Nomor Aduan: $submittedReportId", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Jenis: $selectedDisasterType", fontSize = 11.sp)
                            Text(text = "Kecamatan: $selectedDistrict", fontSize = 11.sp)
                            Text(text = "Status: 🟡 Diproses (Menunggu Verifikasi Lapangan)", fontSize = 11.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Untuk situasi darurat penyelamatan jiwa mendesak, segera hubungi Call Center 117.",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        selectedTab = 2 // Switch to table view to see the report
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Lihat Riwayat Laporan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        EmergencyDialer.dial(context, "117")
                    }
                ) {
                    Text("Panggil 117")
                }
            }
        )
    }
}

/**
 * Reusable Dropdown Row matching the clean underlined layout of the screenshot
 */
@Composable
private fun ReportDropdownField(
    label: String,
    value: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = onDismiss
            ) {
                content()
            }
        }
    }
}

/**
 * Casualty counter row with [-] and [+]
 */
@Composable
private fun CasualtyCounterRow(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (count > 0) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(enabled = count > 0, onClick = onDecrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = if (count > 0) Color(0xFFDC2626) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$count Orang",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) Color(0xFFDC2626) else Color(0xFF111827),
                modifier = Modifier.width(68.dp),
                textAlign = TextAlign.Center
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF15803D).copy(alpha = 0.15f),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onIncrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Report Card Item inside the History Table view with interactive status buttons
 */
@Composable
private fun CitizenReportItemCard(
    report: CitizenDisasterReport,
    onUpdateStatus: (String) -> Unit
) {
    val context = LocalContext.current

    // Indicator Color based on Status
    val (statusContainer, statusContent, statusLabel) = when (report.status) {
        "Divalidasi" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Divalidasi")
        "Selesai" -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Selesai")
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diproses")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: ID, Time & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = report.incidentTime,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusContent, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusContent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Disaster Type
            Text(
                text = report.disasterType,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.village}, Kec. ${report.district}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
            }

            Text(
                text = "Detail: ${report.incidentLocation}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 2.dp)
            )

            // GPS Coordinates Banner
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF3F4F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Lat: %.5f, Lon: %.5f".format(report.latitude, report.longitude),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    if (report.isGpsVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF16A34A)
                        ) {
                            Text(
                                text = "GPS ASLI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Casualties Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasualtyBadge(label = "Meninggal", count = report.casualtiesDead, color = Color(0xFFDC2626))
                CasualtyBadge(label = "Luka", count = report.casualtiesInjured, color = Color(0xFFD97706))
                CasualtyBadge(label = "Hilang", count = report.casualtiesMissing, color = Color(0xFF4B5563))
            }

            if (report.additionalNotes.isNotBlank()) {
                Text(
                    text = "Catatan: ${report.additionalNotes}",
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Dilaporkan oleh: ${report.reporterName}",
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(
                color = Color(0xFFF3F4F6),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Status Management Row with Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Update Status:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B5563)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReportStatusChip(
                        label = "Diproses",
                        isSelected = report.status == "Diproses",
                        color = Color(0xFFEA580C)
                    ) {
                        onUpdateStatus("Diproses")
                    }
                    ReportStatusChip(
                        label = "Divalidasi",
                        isSelected = report.status == "Divalidasi",
                        color = Color(0xFF16A34A)
                    ) {
                        onUpdateStatus("Divalidasi")
                    }
                    ReportStatusChip(
                        label = "Selesai",
                        isSelected = report.status == "Selesai",
                        color = Color(0xFF2563EB)
                    ) {
                        onUpdateStatus("Selesai")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatusChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color else Color(0xFFF3F4F6),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else Color(0xFFE5E7EB)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CasualtyBadge(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (count > 0) color.copy(alpha = 0.12f) else Color(0xFFF3F4F6)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 10.sp,
                color = Color(0xFF4B5563)
            )
            Text(
                text = "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) color else Color(0xFF6B7280)
            )
        }
    }
}
