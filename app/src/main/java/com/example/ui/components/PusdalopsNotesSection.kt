package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerNoteEntity
import com.example.model.Note
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Modul Catatan Operasional Pusdalops BPBD Kabupaten Mukomuko.
 * Responsif & Adaptif untuk semua ukuran layar (Phone, Tablet, Foldable):
 * - Layar Tablet/Foldable (lebar >= 600dp): Tampilan List-Detail Berdampingan (Side-by-Side).
 * - Layar Ponsel (lebar < 600dp): Tampilan Terpisah dengan Navigasi BackHandler.
 * - Dilengkapi Asisten AI Pusdalops untuk ringkasan catatan operasional dan draf SOP.
 */
@Composable
fun PusdalopsNotesSection(
    officerName: String = "Petugas Pusdalops",
    officerRole: String = "OPERATOR",
    currentDistrict: String = "Mukomuko",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }
    val noteDao = remember { database.noteDao() }

    val notesEntities by noteDao.getAllNotesFlow().collectAsState(initial = emptyList())
    val notes = remember(notesEntities) { notesEntities.map { it.toModel() } }

    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    // Seed default notes if empty
    LaunchedEffect(notesEntities) {
        if (notesEntities.isEmpty()) {
            val defaultNotes = listOf(
                Note(
                    id = "note_sop_01",
                    title = "SOP Kesiapsiagaan Gelombang Tinggi & Rob Mukomuko",
                    content = "1. Pantau radar maritim BMKG Stasiun Maritim Teluk Bayur setiap 3 jam.\n2. Siagakan tim TRC Posko Pesisir di Kec. Kota Mukomuko, Teramang Jaya, dan Ipuh.\n3. Berikan imbauan kepada nelayan tradisional agar tidak melaut saat tinggi gelombang melebihi 2.5 meter.\n4. Koordinasi jalur evakuasi warga pesisir ke shelter darurat desa terdekat.",
                    category = "Instruksi Pimpinan",
                    priority = "Tinggi",
                    authorName = "Kalaksa BPBD",
                    authorRole = "KEPALA PELAKSANA",
                    isPinned = true,
                    tags = "SOP, Rob, Pesisir, Evakuasi",
                    district = "Kota Mukomuko"
                ),
                Note(
                    id = "note_log_02",
                    title = "Pengecekan Logistik Tenda Pengungsian & Perahu Karet",
                    content = "Gudang Logistik BPBD Mukomuko:\n- Tenda pengungsi keluarga: 15 unit (kondisi baik)\n- Perahu karet mesin tempel: 3 unit (1 unit perlu servis oli mesin)\n- Pelampung lifejacket: 40 pcs\n- Genset darurat portable: 4 unit siap pakai.\nRekomendasi: Lakukan pengadaan tambahan terpal dan matras gulung.",
                    category = "Logistik & Sarpras",
                    priority = "Sedang",
                    authorName = "Kabid KL BPBD",
                    authorRole = "KABID LOGISTIK",
                    isPinned = false,
                    tags = "Logistik, Gudang, Perahu, Tenda",
                    district = "Mukomuko"
                ),
                Note(
                    id = "note_bmkg_03",
                    title = "Pusdalops Briefing Cuaca Harian & Gempa WRS BMKG",
                    content = "Hasil koordinasi BMKG Stasiun Geofisika Kepahiang dan Stasiun Klimatologi Pulau Baai:\n- Potensi hujan lebat disertai kilat petir di wilayah hulu sungai (Selagan Raya, V Koto).\n- Potensi debit air Sungai Manjuto meningkat pada sore hari.\n- Alat sensor WRS NewGen di Kantor BPBD status normal dan online 24 jam.",
                    category = "Cuaca & BMKG",
                    priority = "Sedang",
                    authorName = "Operator Pusdalops",
                    authorRole = "OPERATOR",
                    isPinned = false,
                    tags = "BMKG, Cuaca, Sungai Manjuto, WRS",
                    district = "V Koto"
                )
            )
            for (dn in defaultNotes) {
                noteDao.insertNote(OfficerNoteEntity.fromModel(dn))
            }
        } else {
            // Migrate any previously saved notes that still carry the "- AI" header suffix
            for (entity in notesEntities) {
                if (entity.content.contains("[RINGKASAN EKSEKUTIF PUSDALOPS - AI]") ||
                    entity.content.contains("[DRAF REKOMENDASI SOP PUSDALOPS - AI]")
                ) {
                    val cleanedContent = entity.content
                        .replace("[RINGKASAN EKSEKUTIF PUSDALOPS - AI]", "[RINGKASAN EKSEKUTIF PUSDALOPS]")
                        .replace("[DRAF REKOMENDASI SOP PUSDALOPS - AI]", "[DRAF REKOMENDASI SOP PUSDALOPS]")
                    noteDao.insertNote(entity.copy(content = cleanedContent))
                }
            }
        }
    }

    // Filtered Notes
    val filteredNotes = remember(notes, searchQuery, selectedCategoryFilter) {
        notes.filter { note ->
            val matchCat = selectedCategoryFilter == "Semua" || note.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.tags.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    // Ensure selected note is valid
    val currentSelectedNote = remember(notes, selectedNoteId) {
        notes.find { it.id == selectedNoteId }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("pusdalops_notes_container")
    ) {
        val isTabletOrFoldable = maxWidth >= 600.dp

        // If tablet/wide screen and no note is selected yet, select first note by default
        LaunchedEffect(isTabletOrFoldable, filteredNotes) {
            if (isTabletOrFoldable && selectedNoteId == null && filteredNotes.isNotEmpty()) {
                selectedNoteId = filteredNotes.first().id
            }
        }

        if (isTabletOrFoldable) {
            // TABLET / FOLDABLE (SIDE-BY-SIDE LIST-DETAIL VIEW)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                // LEFT PANE: Notes List
                Box(
                    modifier = Modifier
                        .width(350.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                ) {
                    NotesListView(
                        notes = filteredNotes,
                        selectedNoteId = selectedNoteId,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategoryFilter,
                        onSearchChange = { searchQuery = it },
                        onCategorySelect = { selectedCategoryFilter = it },
                        onNoteClick = { selectedNoteId = it.id },
                        onAddNewNote = {
                            val newNote = Note(
                                id = UUID.randomUUID().toString(),
                                title = "Catatan Baru",
                                content = "",
                                category = "Operasional",
                                priority = "Sedang",
                                authorName = officerName,
                                authorRole = officerRole,
                                district = currentDistrict
                            )
                            coroutineScope.launch {
                                noteDao.insertNote(OfficerNoteEntity.fromModel(newNote))
                                selectedNoteId = newNote.id
                            }
                        },
                        onTogglePin = { note ->
                            coroutineScope.launch {
                                noteDao.updatePinStatus(note.id, !note.isPinned)
                            }
                        }
                    )
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE2E8F0)
                )

                // RIGHT PANE: Note Editor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFF8FAFC))
                ) {
                    if (currentSelectedNote != null) {
                        NoteEditorView(
                            note = currentSelectedNote,
                            isWideScreen = true,
                            onSaveNote = { updatedNote ->
                                coroutineScope.launch {
                                    noteDao.insertNote(OfficerNoteEntity.fromModel(updatedNote))
                                    Toast.makeText(context, "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteNote = { noteToDelete ->
                                coroutineScope.launch {
                                    noteDao.deleteNoteById(noteToDelete.id)
                                    selectedNoteId = filteredNotes.firstOrNull { it.id != noteToDelete.id }?.id
                                    Toast.makeText(context, "Catatan dihapus", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBackClick = {}
                        )
                    } else {
                        EmptyNoteEditorPlaceholder(
                            onAddNew = {
                                val newNote = Note(
                                    id = UUID.randomUUID().toString(),
                                    title = "Catatan Lapangan Baru",
                                    content = "",
                                    category = "Operasional",
                                    priority = "Sedang",
                                    authorName = officerName,
                                    authorRole = officerRole,
                                    district = currentDistrict
                                )
                                coroutineScope.launch {
                                    noteDao.insertNote(OfficerNoteEntity.fromModel(newNote))
                                    selectedNoteId = newNote.id
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // PHONE / COMPACT SCREEN (SEPARATE SCREENS WITH BACKHANDLER)
            if (currentSelectedNote != null) {
                // Intercept hardware/system back button
                BackHandler {
                    selectedNoteId = null
                }

                NoteEditorView(
                    note = currentSelectedNote,
                    isWideScreen = false,
                    onSaveNote = { updatedNote ->
                        coroutineScope.launch {
                            noteDao.insertNote(OfficerNoteEntity.fromModel(updatedNote))
                            Toast.makeText(context, "Catatan disimpan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteNote = { noteToDelete ->
                        coroutineScope.launch {
                            noteDao.deleteNoteById(noteToDelete.id)
                            selectedNoteId = null
                            Toast.makeText(context, "Catatan dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBackClick = {
                        selectedNoteId = null
                    }
                )
            } else {
                NotesListView(
                    notes = filteredNotes,
                    selectedNoteId = null,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategoryFilter,
                    onSearchChange = { searchQuery = it },
                    onCategorySelect = { selectedCategoryFilter = it },
                    onNoteClick = { selectedNoteId = it.id },
                    onAddNewNote = {
                        val newNote = Note(
                            id = UUID.randomUUID().toString(),
                            title = "Catatan Lapangan",
                            content = "",
                            category = "Operasional",
                            priority = "Sedang",
                            authorName = officerName,
                            authorRole = officerRole,
                            district = currentDistrict
                        )
                        coroutineScope.launch {
                            noteDao.insertNote(OfficerNoteEntity.fromModel(newNote))
                            selectedNoteId = newNote.id
                        }
                    },
                    onTogglePin = { note ->
                        coroutineScope.launch {
                            noteDao.updatePinStatus(note.id, !note.isPinned)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Tampilan Daftar Catatan (List Pane)
 */
@Composable
private fun NotesListView(
    notes: List<Note>,
    selectedNoteId: String?,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onAddNewNote: () -> Unit,
    onTogglePin: (Note) -> Unit
) {
    val categories = listOf("Semua", "Operasional", "Insiden Bencana", "Logistik & Sarpras", "Instruksi Pimpinan", "Cuaca & BMKG")

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Header Card with Title, Locked Badge & Add Action
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Catatan Operasional",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Logbook Tim & Lapangan BPBD",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Locked height 24.dp badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = "${notes.size} Catatan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Search & Filter Bar
        val searchKeyboardActions = rememberFormKeyboardActions()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari catatan atau tag...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = formDoneKeyboardOptions(),
                keyboardActions = searchKeyboardActions,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    cursorColor = OrangePrimary,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedIndicatorColor = OrangePrimary,
                    unfocusedIndicatorColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .formScrollPadding()
                    .testTag("note_search_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Horizontal Scroll
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) OrangePrimary else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) OrangePrimary else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .clickable { onCategorySelect(cat) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
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

        // 3. Add Note Quick Action Button
        Button(
            onClick = onAddNewNote,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .heightIn(min = 42.dp)
                .testTag("btn_create_note")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Buat Catatan Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Notes List Items
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Belum Ada Catatan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Tekan tombol di atas untuk membuat catatan koordinasi baru.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("notes_lazy_column"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val isSelected = note.id == selectedNoteId
                    NoteListItemCard(
                        note = note,
                        isSelected = isSelected,
                        onClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) }
                    )
                }
            }
        }
    }
}

/**
 * Kartu Item Catatan dalam Daftar dengan locked badge height 24.dp
 */
@Composable
private fun NoteListItemCard(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (note.priority) {
        "Tinggi" -> Color(0xFFDC2626)
        "Sedang" -> OrangePrimary
        else -> Color(0xFF15803D)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID")) }
    val formattedDate = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF7ED) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) OrangePrimary else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_item_${note.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Title & Pin Action in a clean horizontal line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title.ifBlank { "Tanpa Judul" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OrangePrimary else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Lepas Pin" else "Sematkan",
                        tint = if (note.isPinned) OrangePrimary else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Excerpt snippet
            Text(
                text = note.content.ifBlank { "(Catatan belum memiliki isi detail)" },
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Badges with locked 24.dp height & Date in clean single line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Badge (24.dp height locked)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.08f),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = note.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }

                    // Priority Badge (24.dp height locked)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.12f),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = note.priority,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Tampilan Editor Catatan (Detail Pane)
 */
@Composable
private fun NoteEditorView(
    note: Note,
    isWideScreen: Boolean,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) {
        mutableStateOf(
            note.content
                .replace("[RINGKASAN EKSEKUTIF PUSDALOPS - AI]", "[RINGKASAN EKSEKUTIF PUSDALOPS]")
                .replace("[DRAF REKOMENDASI SOP PUSDALOPS - AI]", "[DRAF REKOMENDASI SOP PUSDALOPS]")
        )
    }
    var category by remember(note.id) { mutableStateOf(note.category) }
    var priority by remember(note.id) { mutableStateOf(note.priority) }
    var tags by remember(note.id) { mutableStateOf(note.tags) }
    var isPinned by remember(note.id) { mutableStateOf(note.isPinned) }

    var isAiProcessing by remember { mutableStateOf(false) }
    var aiNotificationMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Operasional", "Insiden Bencana", "Logistik & Sarpras", "Instruksi Pimpinan", "Cuaca & BMKG")
    val priorities = listOf("Tinggi", "Sedang", "Rendah")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("note_editor_view")
    ) {
        // 1. Top Editor Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isWideScreen) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Daftar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column {
                        Text(
                            text = if (isWideScreen) "Editor Catatan Lapangan" else "Detail Catatan",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Oleh: ${note.authorName} (${note.authorRole})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Delete Button
                    IconButton(
                        onClick = { onDeleteNote(note) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Catatan",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val updated = note.copy(
                                title = title.ifBlank { "Catatan Tanpa Judul" },
                                content = content,
                                category = category,
                                priority = priority,
                                tags = tags,
                                isPinned = isPinned,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveNote(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Main Editor Content Scroll
        val noteKeyboardActions = rememberFormKeyboardActions()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Judul Catatan Operasional...", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = formNextKeyboardOptions(),
                keyboardActions = noteKeyboardActions,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF64748B),
                    unfocusedIndicatorColor = Color(0xFFE2E8F0)
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .formScrollPadding()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips: Category & Priority Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pills
                categories.forEach { cat ->
                    val isSel = category == cat
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .clickable { category = cat }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Priority Pills
                priorities.forEach { prio ->
                    val isSel = priority == prio
                    val color = when (prio) {
                        "Tinggi" -> Color(0xFFDC2626)
                        "Sedang" -> OrangePrimary
                        else -> Color(0xFF15803D)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) color else color.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) color else color.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .clickable { priority = prio }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = prio,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. AI SMART ASSISTANT TOOLBAR FOR NOTES
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0284C7).copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Asisten AI Pusdalops",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }

                        if (isAiProcessing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Analisa Kesimpulan
                        OutlinedButton(
                            onClick = {
                                if (content.isBlank()) {
                                    aiNotificationMessage = "Isi catatan masih kosong, tuliskan poin situasi terlebih dahulu."
                                    return@OutlinedButton
                                }
                                isAiProcessing = true
                                // Rule-based Pusdalops executive extraction
                                val lines = content.lines().filter { it.isNotBlank() }
                                val summary = buildString {
                                    appendLine("\n\n📋 [RINGKASAN EKSEKUTIF PUSDALOPS]")
                                    appendLine("• Topik: $title (${category.uppercase()})")
                                    appendLine("• Prioritas Penanganan: $priority")
                                    appendLine("• Poin Kunci Lapangan: ${lines.take(3).joinToString(" / ") { it.take(60) }}")
                                    append("• Status Koordinasi: Tim siaga 24 jam di Posko BPBD Mukomuko.")
                                }
                                content += summary
                                isAiProcessing = false
                                aiNotificationMessage = "Analisa kesimpulan ringkasan eksekutif telah ditambahkan di bagian bawah catatan."
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Analisa Kesimpulan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Action 2: Draf SOP Rekomendasi
                        OutlinedButton(
                            onClick = {
                                isAiProcessing = true
                                val sopDraft = buildString {
                                    appendLine("\n\n🛡️ [DRAF REKOMENDASI SOP PUSDALOPS]")
                                    appendLine("1. Pastikan komunikasi radio HT frekuensi 143.550 MHz aktif.")
                                    appendLine("2. Konfirmasi update data korban / kerusakan dengan pihak Desa dan Camat.")
                                    appendLine("3. Siapkan laporan situasi (Lapsit) harian untuk dikirim ke BNPB dan Bupati Mukomuko.")
                                    append("4. Lakukan evaluasi logistik darurat jika curah hujan terus berlanjut.")
                                }
                                content += sopDraft
                                isAiProcessing = false
                                aiNotificationMessage = "Draf SOP rekomendasi tindakan telah ditambahkan ke catatan."
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Draf SOP Rekomendasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (aiNotificationMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = aiNotificationMessage ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content Multi-Line Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        text = "Tulis detail catatan kejadian lapangan, jumlah logistik, koordinasi posko, atau instruksi pimpinan di sini...",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                minLines = 14,
                keyboardOptions = formNextKeyboardOptions(),
                keyboardActions = noteKeyboardActions,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = OrangePrimary,
                    unfocusedIndicatorColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .formScrollPadding()
                    .testTag("note_content_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tags input
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Label / Tag (misal: Logistik, Pesisir, Tenda, Sungai)", fontSize = 11.5.sp) },
                singleLine = true,
                keyboardOptions = formDoneKeyboardOptions(),
                keyboardActions = noteKeyboardActions,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFCBD5E1),
                    unfocusedIndicatorColor = Color(0xFFE2E8F0)
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .formScrollPadding()
            )
        }
    }
}

/**
 * Placeholder jika belum ada catatan yang dipilih di Tablet
 */
@Composable
private fun EmptyNoteEditorPlaceholder(
    onAddNew: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = OrangePrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Pilih Catatan untuk Membaca atau Mengedit",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilih salah satu catatan di panel kiri, atau buat catatan operasional lapangan baru.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buat Catatan Lapangan Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
