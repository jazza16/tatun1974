package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.EmergencyContactsRepository
import com.example.data.repository.EmergencyContactsSource
import com.example.data.repository.PublicEmergencyContact
import kotlinx.coroutines.launch

/**
 * Tab "Kontak Penting" pada halaman Laporan Warga.
 *
 * Menampilkan daftar dinamis instansi layanan darurat publik (BPBD/117, Kepolisian/110,
 * Damkar/113, Ambulans/RSUD, Basarnas/115, dll.) yang diambil dari server API
 * (`GET /emergency-contacts`) tanpa menampilkan data personil ASN/NIP.
 */
@Composable
fun CitizenEmergencyContactsView(
    modifier: Modifier = Modifier,
    onOpenVolunteerRegistration: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loadResult by remember { mutableStateOf(EmergencyContactsRepository.getCachedOrDefault(context)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Semua") }

    // Sinkronisasi otomatis dari API server saat tab dibuka
    LaunchedEffect(Unit) {
        isRefreshing = true
        try {
            loadResult = EmergencyContactsRepository.fetchPublicContacts(context)
        } finally {
            isRefreshing = false
        }
    }

    val categories = remember(loadResult.contacts) {
        listOf("Semua") + loadResult.contacts.map { it.category }.distinct()
    }

    val filteredContacts = remember(selectedCategory, loadResult.contacts) {
        if (selectedCategory == "Semua") {
            loadResult.contacts
        } else {
            loadResult.contacts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("citizen_emergency_contacts_view")
    ) {
        val columnCount = if (maxWidth >= 600.dp) 2 else 1
        val chunkedContacts = remember(filteredContacts, columnCount) {
            filteredContacts.chunked(columnCount)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("public_emergency_contacts_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header Banner Darurat Publik + Tombol Sinkronisasi API
            item(key = "header_emergency_banner") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFEA580C), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneInTalk,
                                        contentDescription = "Kontak Darurat Publik",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Kontak Darurat Publik 24 Jam",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412)
                                    )
                                    Text(
                                        text = "Tekan tombol Panggil untuk langsung membuka dialer ponsel.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF7C2D12),
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = if (loadResult.source == EmergencyContactsSource.REMOTE_API) {
                                                Color(0xFF15803D)
                                            } else {
                                                Color(0xFFEA580C)
                                            },
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = loadResult.source.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (!isRefreshing) {
                                        isRefreshing = true
                                        coroutineScope.launch {
                                            try {
                                                loadResult = EmergencyContactsRepository.fetchPublicContacts(context)
                                                Toast.makeText(
                                                    context,
                                                    "Daftar kontak darurat diperbarui (${loadResult.source.label})",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                isRefreshing = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_refresh_emergency_contacts")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Perbarui daftar kontak dari server",
                                    tint = Color(0xFFEA580C)
                                )
                            }
                        }
                    }

                    if (isRefreshing) {
                        LinearProgressIndicator(
                            color = Color(0xFFEA580C),
                            trackColor = Color(0xFFFED7AA),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Filter Kategori Instansi (Dinamis sesuai respons server)
            item(key = "header_category_filter") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) Color(0xFFEA580C) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFEA580C) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .clickable { selectedCategory = category }
                                .testTag("chip_contact_category_$category")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Daftar / Grid Bersih Instansi Darurat Publik
            items(chunkedContacts, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (columnCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { contact ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PublicEmergencyContactCard(
                                        contact = contact,
                                        onCallClick = {
                                            EmergencyDialer.dial(context, contact.phoneNumber)
                                            Toast.makeText(
                                                context,
                                                "Membuka dialer ${contact.agencyName} (${contact.displayPhone})",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                            if (rowItems.size < columnCount) {
                                repeat(columnCount - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        val contact = rowItems.first()
                        PublicEmergencyContactCard(
                            contact = contact,
                            onCallClick = {
                                EmergencyDialer.dial(context, contact.phoneNumber)
                                Toast.makeText(
                                    context,
                                    "Membuka dialer ${contact.agencyName} (${contact.displayPhone})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            if (onOpenVolunteerRegistration != null) {
                item(key = "volunteer_registration_portal_card") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("card_open_volunteer_portal_from_contacts")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Pendataan & Registrasi Relawan BPBD",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pendaftaran relawan kebencanaan (Individu & Organisasi/Komunitas terdaftar di BPBD Mukomuko).",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 15.sp
                                )
                            }
                            Button(
                                onClick = onOpenVolunteerRegistration,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("btn_header_open_volunteer_module")
                            ) {
                                Text(
                                    text = "Portal Relawan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicEmergencyContactCard(
    contact: PublicEmergencyContact,
    onCallClick: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = remember(contact.accentHex) { parseSafeHexColor(contact.accentHex) }
    val agencyIcon = remember(contact.iconKey) { resolveAgencyIcon(contact.iconKey) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_emergency_contact_${contact.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Kiri: Ikon Instansi + Nama Instansi + Nomor Telepon Besar & Jelas
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = agencyIcon,
                            contentDescription = contact.agencyName,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.agencyName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = contact.displayPhone,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (contact.isTollFree) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        text = "Bebas Pulsa",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Kanan: Tombol "Panggil" dengan ikon telepon langsung membuka Dial Pad
                Button(
                    onClick = onCallClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_call_${contact.phoneNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Panggil ${contact.agencyName}",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Panggil",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (contact.subtitle.isNotBlank()) {
                Text(
                    text = contact.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 17.sp
                )
            }

            if (contact.address.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            val query = Uri.encode("${contact.agencyName} ${contact.address}")
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Lihat lokasi ${contact.agencyName}",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contact.address,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun resolveAgencyIcon(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "bpbd", "shield" -> Icons.Default.Shield
        "police", "polisi" -> Icons.Default.LocalPolice
        "fire", "damkar" -> Icons.Default.LocalFireDepartment
        "ambulance", "hospital", "medis", "rsud" -> Icons.Default.LocalHospital
        "sar", "basarnas" -> Icons.Default.Emergency
        "power", "pln" -> Icons.Default.Power
        "support", "pusdalops" -> Icons.Default.SupportAgent
        else -> Icons.Default.PhoneInTalk
    }
}

private fun parseSafeHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFFEA580C)
    }
}
