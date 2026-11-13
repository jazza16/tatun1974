package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EmergencyContactItem(
    val name: String,
    val category: String, // SAR & Bencana, Medis, Kepolisian, Damkar, Teknis
    val phoneNumber: String,
    val displayPhone: String,
    val description: String,
    val address: String,
    val icon: ImageVector,
    val themeColor: Color,
    val isTollFree: Boolean = false
)

private val MUKOMUKO_EMERGENCY_CONTACTS = listOf(
    EmergencyContactItem(
        name = "Call Center Bencana BPBD Mukomuko / BNPB",
        category = "SAR & Bencana",
        phoneNumber = "117",
        displayPhone = "117 (Bebas Pulsa)",
        description = "Layanan gawat darurat bencana alam, evakuasi banjir luapan, pohon tumbang, gempa & tsunami.",
        address = "Posko Pusdalops PB Pemda Mukomuko",
        icon = Icons.Default.Shield,
        themeColor = Color(0xFFDC2626),
        isTollFree = true
    ),
    EmergencyContactItem(
        name = "Pusdalops BPBD Kabupaten Mukomuko",
        category = "SAR & Bencana",
        phoneNumber = "073771112",
        displayPhone = "(0737) 71112",
        description = "Pos Komando Penanggulangan Bencana Daerah Mukomuko siaga operasional 24 jam.",
        address = "Jl. Danau Nibung, Kompleks Perkantoran Pemkab Mukomuko",
        icon = Icons.Default.SupportAgent,
        themeColor = Color(0xFFEA580C)
    ),
    EmergencyContactItem(
        name = "Polres Mukomuko (Kepolisian)",
        category = "Kepolisian",
        phoneNumber = "110",
        displayPhone = "110",
        description = "POLISI BERSAMA MASYARAKAT • Sentra Pelayanan Kepolisian Terpadu (SPKT) keamanan warga, laka lintas, dan pengaduan masyarakat.",
        address = "Gedung Pelayanan Terpadu Sarja Arya Racana, Bandar Ratu, Kota Mukomuko",
        icon = Icons.Default.LocalPolice,
        themeColor = Color(0xFF1D4ED8),
        isTollFree = true
    ),
    EmergencyContactItem(
        name = "RSUD Kabupaten Mukomuko (IGD 24 Jam)",
        category = "Medis",
        phoneNumber = "073771018",
        displayPhone = "(0737) 71018 / 0812-7890-1234",
        description = "Instalasi Gawat Darurat rumah sakit rujukan utama dengan armada ambulans siaga bencana.",
        address = "Jl. Danau Nibung Km. 4, Mukomuko",
        icon = Icons.Default.LocalHospital,
        themeColor = Color(0xFF15803D)
    ),
    EmergencyContactItem(
        name = "Dinas Pemadam Kebakaran & Penyelamatan",
        category = "Damkar",
        phoneNumber = "113",
        displayPhone = "113 / (0737) 71113",
        description = "Evakuasi sarang tawon, penanganan karhutla, kebakaran permukiman dan penyelamatan satwa berbahaya.",
        address = "Jl. Imam Bonjol, Pasar Mukomuko",
        icon = Icons.Default.LocalFireDepartment,
        themeColor = Color(0xFFB91C1C),
        isTollFree = true
    ),
    EmergencyContactItem(
        name = "Basarnas Pos SAR Mukomuko / Bengkulu",
        category = "SAR & Bencana",
        phoneNumber = "115",
        displayPhone = "115 / (0736) 21115",
        description = "Operasi pencarian orang hilang, pertolongan kapal/perahu nelayan karam di perairan laut Mukomuko.",
        address = "Dermaga Pelabuhan Pasar Ipuh & Pos SAR Pantai Mukomuko",
        icon = Icons.Default.Emergency,
        themeColor = Color(0xFFD97706),
        isTollFree = true
    ),
    EmergencyContactItem(
        name = "PMI (Palang Merah Indonesia) Mukomuko",
        category = "Medis",
        phoneNumber = "073771050",
        displayPhone = "(0737) 71050",
        description = "Dapur umum darurat, unit transfusi darah, dan pertolongan pertama evakuasi korban luka.",
        address = "Jl. Fatmawati, Kota Mukomuko",
        icon = Icons.Default.LocalHospital,
        themeColor = Color(0xFFDC2626)
    ),
    EmergencyContactItem(
        name = "PLN Unit Layanan Mukomuko (Gangguan Kelistrikan)",
        category = "Teknis",
        phoneNumber = "123",
        displayPhone = "123 (Kode Area 0737)",
        description = "Pemadaman darurat saat tiang roboh, kawat putus tertimpa dahan pohon, dan banjir merendam gardu.",
        address = "Jl. Soekarno-Hatta, Mukomuko",
        icon = Icons.Default.Power,
        themeColor = Color(0xFF0284C7)
    ),
    EmergencyContactItem(
        name = "Puskesmas Perawatan Ipuh (IGD Rawat Inap)",
        category = "Medis",
        phoneNumber = "073771190",
        displayPhone = "(0737) 71190",
        description = "Pusat rujukan medis darurat terdekat bagi warga pesisir selatan Mukomuko.",
        address = "Jl. Lintas Barat, Medan Jaya, Ipuh",
        icon = Icons.Default.LocalHospital,
        themeColor = Color(0xFF059669)
    )
)

/**
 * 'Kontak Penting' Emergency Directory for Mukomuko Regency
 */
@Composable
fun CitizenEmergencyContactsView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "SAR & Bencana", "Medis", "Kepolisian", "Damkar", "Teknis")

    val filteredContacts = remember(selectedCategory) {
        if (selectedCategory == "Semua") {
            MUKOMUKO_EMERGENCY_CONTACTS
        } else {
            MUKOMUKO_EMERGENCY_CONTACTS.filter { it.category == selectedCategory }
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .testTag("citizen_emergency_contacts_view")
    ) {
        val columnCount = if (maxWidth >= 600.dp) 2 else 1
        val chunkedContacts = remember(filteredContacts, columnCount) {
            filteredContacts.chunked(columnCount)
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Info Banner
            Surface(
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDC2626), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Direktori Kontak Darurat Mukomuko",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Ketuk tombol 'Panggil' untuk langsung membuka dialer ponsel ke instansi terkait.",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }

            // Horizontal Category Filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFDC2626) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFDC2626) else Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            // Contact Items List (Adaptive 1 or 2 Columns)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(chunkedContacts) { rowItems ->
                    if (columnCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    EmergencyContactCard(
                                        contact = item,
                                        onDial = {
                                            EmergencyDialer.dial(context, item.phoneNumber)
                                            Toast.makeText(context, "Membuka dialer untuk ${item.name}...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            if (rowItems.size < columnCount) {
                                for (i in 0 until (columnCount - rowItems.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        val item = rowItems.first()
                        EmergencyContactCard(
                            contact = item,
                            onDial = {
                                EmergencyDialer.dial(context, item.phoneNumber)
                                Toast.makeText(context, "Membuka dialer untuk ${item.name}...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContactItem,
    onDial: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(contact.themeColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = contact.icon,
                            contentDescription = null,
                            tint = contact.themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = contact.themeColor.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = contact.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contact.themeColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Badge Panggil Mode Bulat (Identik dengan Mode Bulat pada Judul Direktori Kontak)
                Surface(
                    shape = CircleShape,
                    color = contact.themeColor,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onDial)
                        .testTag("dial_badge_${contact.phoneNumber}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Panggil ${contact.name}",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val annotatedDescription = buildAnnotatedString {
                val boldPhrase = "POLISI BERSAMA MASYARAKAT"
                val startIndex = contact.description.indexOf(boldPhrase)
                if (startIndex >= 0) {
                    append(contact.description.substring(0, startIndex))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldPhrase)
                    }
                    append(contact.description.substring(startIndex + boldPhrase.length))
                } else {
                    append(contact.description)
                }
            }

            Text(
                text = annotatedDescription,
                fontSize = 11.sp,
                color = Color(0xFF4B5563),
                lineHeight = 15.sp
            )

            HorizontalDivider(
                color = Color(0xFFF3F4F6),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contact.displayPhone,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = contact.themeColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val query = if (contact.name.contains("Polres Mukomuko", ignoreCase = true)) {
                            "Polres+Mukomuko+Bandar+Ratu"
                        } else {
                            Uri.encode("${contact.name} ${contact.address}")
                        }
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = contact.address.take(24) + if (contact.address.length > 24) "..." else "",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}
