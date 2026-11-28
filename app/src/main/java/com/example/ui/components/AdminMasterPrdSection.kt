package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.BuildConfig
import com.example.ui.theme.OrangePrimary

data class PrdChecklistItem(
    val id: String,
    val title: String,
    val category: String,
    val note: String,
    val defaultCompleted: Boolean
)

/**
 * Penyimpanan status checklist Master Dokumen & PRD SIPAGAR KITA (khusus Admin)
 * berbasis SharedPreferences dengan baseline sinkron terhadap MASTER_PRD.md,
 * ROADMAP.md, dan PRE_RELEASE_PENDING_CHECKLIST.md.
 */
object AdminPrdChecklistStore {
    private const val PREFS_NAME = "sipagar_admin_prd_checklist"
    private const val KEY_PREFIX = "item_done_"

    val defaultItems: List<PrdChecklistItem> = listOf(
        // --- SUDAH SELESAI ---
        PrdChecklistItem(
            id = "prd_bab1_rbac_isolation",
            title = "Fondasi 15 Kecamatan, RBAC 8 Role & Isolasi Mode Warga",
            category = "Bab 1 • Fondasi",
            note = "Skema 15 kecamatan, 8 role (3 tier otoritas), serta penguncian modul internal di balik sesi petugas.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab2_ews_24h",
            title = "Cuaca, Maritim, Gempa WRS, Karhutla & Notifikasi 24 Jam",
            category = "Bab 2 • Warga",
            note = "Akses darurat tanpa login dengan triase 4 warna & porsi terpisah Mode Warga vs Mode Petugas.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab2_sop_gobag",
            title = "SOP Evakuasi Offline (+ Audio TTS), Kompas & Tas Siaga",
            category = "Bab 2 • Pra-Bencana",
            note = "Panduan gempa/tsunami/banjir/longsor 100% offline, pembaca suara TTS, dan checklist Go-Bag.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab3_posko_fefo",
            title = "Manajemen Posko, Logistik FEFO (<10 Kritis) & Validasi Jiwa",
            category = "Bab 3 • Posko",
            note = "Pelacakan kedaluwarsa FEFO, alarm stok kritis <10, validasi kelompok rentan, & anti-duplikasi NIK 16 digit.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab4_four_eyes_sirene",
            title = "Sirene Otoritas Berlapis (Four-Eyes Principle)",
            category = "Bab 4 • Komando",
            note = "Pemisahan pembuat draf & pejabat penyetuju (DRAFT -> MENUNGGU PERSETUJUAN -> DIAKTIFKAN).",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab5_dala_volunteer",
            title = "Modul DaLA/JITUPASNA Auto-Snapshot, PDF Resmi & Relawan TRC",
            category = "Bab 5 • Pasca-Bencana",
            note = "Tarik snapshot pengungsi Posko, cetak PDF kop 2 logo, serta registrasi & Surat Tugas relawan NIK PK.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_bab6_pin_hardening",
            title = "Anti-Reset PIN Cloud Firestore, R8 Hardening & Audit Login",
            category = "Bab 6 • Keamanan",
            note = "Verifikasi PIN Cloud PBKDF2/SHA-256, proteksi ProGuard v1.3.0, & jejak audit NIP petugas.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_trilogi_kebijakan",
            title = "Trilogi Kebijakan Daerah (KRB 2026–2030, RPB 2025–2029, RTRW 12/2025)",
            category = "v1.3.0 • Kebijakan",
            note = "12 matriks bahaya KRB, 31 strategi & 11 shelter RPB, ketentuan KKPR RTRW, & banner collapsible.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_sandbox_training",
            title = "Mode Latihan Petugas (Sandbox Simulasi) & Peta Jalur Evakuasi",
            category = "Roadmap • Teknis",
            note = "Simulasi terisolasi agar uji lapangan tidak mencemari database operasional warga.",
            defaultCompleted = true
        ),
        PrdChecklistItem(
            id = "prd_live_usage_guide",
            title = "Panduan Penggunaan Warga & Petugas Tersinkron Cloud",
            category = "v1.3.0 • Dokumentasi",
            note = "Tersinkron via app_settings/usage_guide + cache offline sehingga bisa diperbarui tanpa update APK.",
            defaultCompleted = true
        ),

        // --- BELUM SELESAI / DITUNDA / PRA-PLAY STORE ---
        PrdChecklistItem(
            id = "pending_sos_hold_3s",
            title = "Tombol Darurat SOS (Hold 3 Detik) & Peta Komando Tiket Baterai",
            category = "Ditunda • Bab 2 & 4",
            note = "Ditunda sementara sesuai arahan; menunggu aktivasi antrean sos_offline_queue & Idempotency-Key.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_quiz_simulasi",
            title = "Modul Simulasi & Kuis Kesiapsiagaan Interaktif Masyarakat",
            category = "Backlog • Pra-Bencana",
            note = "Edukasi skenario interaktif kebencanaan untuk masyarakat, pelajar, & perangkat desa 15 kecamatan.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_sms_fallback",
            title = "Mode Offline SMS Fallback Saat Jaringan Data Seluler Lumpuh",
            category = "Backlog • Teknis",
            note = "Pengiriman laporan terkompresi via gateway SMS Pusdalops saat sinyal internet terputus total.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_play_appid_branding",
            title = "Atribusi Resmi Instansi & Penggantian applicationId Final BPBD",
            category = "Pra-Play Store #1 & #3",
            note = "Penetapan applicationId permanen instansi (.go.id/bpbd) & profil resmi BPBD sebelum rilis Play Store.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_in_app_update_migration",
            title = "Migrasi ke Google Play In-App Updates & Nonaktifkan Panduan Sideload",
            category = "Pra-Play Store #2",
            note = "Ganti AppUpdater APK eksternal ke Play Core In-App Updates agar lolos kebijakan Device & Network Abuse.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_play_console_admin",
            title = "Kelengkapan Play Console: Surat Tugas, URL Privasi .go.id, Data Safety & Akun Uji",
            category = "Pra-Play Store #4 & #5",
            note = "Unggah verifikasi aplikasi pemerintah, publikasikan URL Kebijakan Privasi web, & siapkan akun demo reviewer.",
            defaultCompleted = false
        ),
        PrdChecklistItem(
            id = "pending_twotier_decision_rules",
            title = "Keputusan Arsitektur 1-APK vs 2-APK Terpisah & Audit Final Rules",
            category = "Pra-Play Store #6",
            note = "Konfirmasi resmi pimpinan BPBD serta pengetatan akhir koleksi Firestore publik.",
            defaultCompleted = false
        )
    )

    fun loadCompletionMap(context: Context): Map<String, Boolean> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return defaultItems.associate { item ->
            item.id to prefs.getBoolean("$KEY_PREFIX${item.id}", item.defaultCompleted)
        }
    }

    fun saveCompletion(context: Context, id: String, completed: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PREFIX$id", completed)
            .apply()
    }

    fun resetDefaults(context: Context): Map<String, Boolean> {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        return defaultItems.associate { it.id to it.defaultCompleted }
    }
}

private enum class ChecklistFilter {
    ALL,
    COMPLETED,
    PENDING
}

/**
 * Panel khusus Administrator di Informasi Pengembang Aplikasi:
 * Menampilkan Master Dokumen & PRD Ringkas SIPAGAR KITA serta Checklist Status Selesai / Belum Selesai.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMasterPrdSection(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = PRD Ringkas, 1 = Checklist
    var selectedFilter by remember { mutableStateOf(ChecklistFilter.ALL) }
    var completionMap by remember { mutableStateOf(AdminPrdChecklistStore.loadCompletionMap(context)) }

    val items = AdminPrdChecklistStore.defaultItems
    val completedCount = items.count { completionMap[it.id] == true }
    val pendingCount = items.size - completedCount

    val filteredItems = remember(completionMap, selectedFilter) {
        items.filter { item ->
            val isDone = completionMap[item.id] == true
            when (selectedFilter) {
                ChecklistFilter.ALL -> true
                ChecklistFilter.COMPLETED -> isDone
                ChecklistFilter.PENDING -> !isDone
            }
        }
    }

    val greenBg = Color(0xFF16A34A).copy(alpha = if (isDark) 0.24f else 0.13f)
    val greenText = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
    val pendingBg = OrangePrimary.copy(alpha = if (isDark) 0.24f else 0.13f)
    val pendingText = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)

    Surface(
        color = if (isDark) Color(0xFF11211E) else Color(0xFFF0FDF9),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.2.dp,
            if (isDark) Color(0xFF235347) else Color(0xFF99F6E4)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("developer_master_prd_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Expand/Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Master Dokumen & PRD Ringkas SIPAGAR KITA (Khusus Admin)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF99F6E4) else Color(0xFF115E59)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PrdStatusBadge(
                                text = "$completedCount Selesai",
                                backgroundColor = greenBg,
                                textColor = greenText
                            )
                            PrdStatusBadge(
                                text = "$pendingCount Belum Selesai",
                                backgroundColor = pendingBg,
                                textColor = pendingText
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    tint = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tab Switcher: PRD Ringkas vs Checklist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (selectedTab == 0) Color(0xFF0F766E) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedTab == 0) Color(0xFF0F766E) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 }
                                .testTag("tab_admin_prd_summary")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                Text(
                                    text = "1. Master PRD Ringkas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = if (selectedTab == 1) Color(0xFF0F766E) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedTab == 1) Color(0xFF0F766E) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("tab_admin_prd_checklist")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                Text(
                                    text = "2. Checklist Status",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        PrdSummaryBlock(
                            title = "Identitas & Arsitektur Sistem (v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE})",
                            body = "• Pengampu: Pusdalops-PB BPBD Kabupaten Mukomuko (15 Kecamatan).\n" +
                                "• Arsitektur Two-Tier: Memisahkan Portal Warga (tanpa login) & Konsol Petugas (8-Role RBAC, 3 Tier otoritas).\n" +
                                "• Offline-First & Anti-Data Ganda: Room SQLite + sinkron Cloud Firestore dengan validasi NIK 16-digit sebagai Primary Key.",
                            isDark = isDark
                        )
                        PrdSummaryBlock(
                            title = "Pilar 1 — Aplikasi Warga (Pra-Bencana & Darurat)",
                            body = "• Pemantauan Cuaca, Kualitas Udara, Maritim, Gempa WRS-BMKG & Hotspot Karhutla 24 jam tanpa login.\n" +
                                "• Lapor Kejadian Bencana + Kamera + GPS Verified & tombol cepat Darurat 117/112.\n" +
                                "• SOP Evakuasi Mandiri 100% Offline (Gempa, Tsunami 20-20-20, Banjir, Longsor + Audio TTS), Tas Siaga Bencana, Kompas & Peta 11 Shelter.",
                            isDark = isDark
                        )
                        PrdSummaryBlock(
                            title = "Pilar 2 — Aplikasi Petugas, Kebijakan & Tata Kelola",
                            body = "• Manajemen Posko & Logistik FEFO (status kritis <10 unit, validasi kelompok rentan, distribusi anti-ganda NIK).\n" +
                                "• Modul DaLA/JITUPASNA Auto-Snapshot Posko, Ekspor PDF Resmi Kop 2 Logo, Manajemen Relawan TRC, Four-Eyes Sirene, & Mode Latihan Sandbox.\n" +
                                "• Trilogi Kebijakan Daerah (KRB 2026–2030, RPB 2025–2029, Perda RTRW 12/2025) + Panduan Penggunaan Live Cloud tanpa update APK.",
                            isDark = isDark
                        )
                        PrdSummaryBlock(
                            title = "Fokus Antrean Berikutnya & Pra-Play Store",
                            body = "• Fitur ditunda/backlog: Tombol SOS Hold 3s + Tiket Baterai, Kuis Kesiapsiagaan Warga, dan SMS Fallback.\n" +
                                "• Pra-rilis Google Play Store: Finalisasi applicationId instansi, migrasi Play In-App Updates API, kelengkapan Play Console (.go.id Privasi, Surat Tugas, Data Safety, Akun Uji App Access).",
                            isDark = isDark
                        )
                    } else {
                        // Filter Bar + Reset
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PrdFilterPill(
                                label = "Semua (${items.size})",
                                selected = selectedFilter == ChecklistFilter.ALL,
                                onClick = { selectedFilter = ChecklistFilter.ALL },
                                isDark = isDark
                            )
                            PrdFilterPill(
                                label = "Selesai ($completedCount)",
                                selected = selectedFilter == ChecklistFilter.COMPLETED,
                                onClick = { selectedFilter = ChecklistFilter.COMPLETED },
                                isDark = isDark
                            )
                            PrdFilterPill(
                                label = "Belum ($pendingCount)",
                                selected = selectedFilter == ChecklistFilter.PENDING,
                                onClick = { selectedFilter = ChecklistFilter.PENDING },
                                isDark = isDark
                            )
                            OutlinedButton(
                                onClick = {
                                    completionMap = AdminPrdChecklistStore.resetDefaults(context)
                                },
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("btn_reset_prd_checklist"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                            }
                        }

                        Text(
                            text = "Ketuk item di bawah untuk menandai status Selesai / Belum Selesai (tersimpan di perangkat Admin):",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredItems.forEach { item ->
                                val isDone = completionMap[item.id] == true
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDone) {
                                            Color(0xFF16A34A).copy(alpha = 0.35f)
                                        } else {
                                            OrangePrimary.copy(alpha = 0.4f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val next = !isDone
                                            AdminPrdChecklistStore.saveCompletion(context, item.id, next)
                                            completionMap = completionMap.toMutableMap().apply {
                                                put(item.id, next)
                                            }
                                        }
                                        .testTag("prd_checklist_item_${item.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Checkbox(
                                            checked = isDone,
                                            onCheckedChange = { next ->
                                                AdminPrdChecklistStore.saveCompletion(context, item.id, next)
                                                completionMap = completionMap.toMutableMap().apply {
                                                    put(item.id, next)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF16A34A),
                                                uncheckedColor = OrangePrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                PrdStatusBadge(
                                                    text = if (isDone) "Selesai" else "Belum Selesai",
                                                    backgroundColor = if (isDone) greenBg else pendingBg,
                                                    textColor = if (isDone) greenText else pendingText,
                                                    icon = if (isDone) Icons.Default.CheckCircle else Icons.Default.PendingActions
                                                )
                                                PrdStatusBadge(
                                                    text = item.category,
                                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = item.title,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = item.note,
                                                fontSize = 10.5.sp,
                                                lineHeight = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun PrdSummaryBlock(
    title: String,
    body: String,
    isDark: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
            )
            Text(
                text = body,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrdStatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PrdFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Surface(
        color = if (selected) Color(0xFF0F766E) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF0F766E) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
