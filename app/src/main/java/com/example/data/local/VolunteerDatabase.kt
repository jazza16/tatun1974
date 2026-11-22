package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Siklus Status Relawan Kebencanaan (Pending -> Verified -> Assigned -> Done) + Rejected.
 */
enum class VolunteerStatus(val code: String, val displayLabel: String) {
    PENDING("Pending", "Menunggu Verifikasi"),
    VERIFIED("Verified", "Terverifikasi"),
    ASSIGNED("Assigned", "Ditugaskan"),
    DONE("Done", "Selesai Bertugas"),
    REJECTED("Rejected", "Ditolak");

    companion object {
        fun fromCode(code: String): VolunteerStatus {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: PENDING
        }
    }
}

/**
 * Daftar Keahlian Standar Relawan Pusdalops-PB BPBD Kabupaten Mukomuko.
 */
object VolunteerSkills {
    const val MEDIS = "Medis"
    const val DAPUR_UMUM = "Dapur Umum"
    const val SAR = "SAR (Pencarian & Penyelamatan)"
    const val PSIKOSOSIAL = "Psikososial"
    const val LOGISTIK = "Logistik"
    const val ALAT_BERAT = "Alat Berat"

    val ALL_SKILLS = listOf(
        MEDIS,
        DAPUR_UMUM,
        SAR,
        PSIKOSOSIAL,
        LOGISTIK,
        ALAT_BERAT
    )

    val FILTER_SKILLS = listOf(
        "Semua",
        MEDIS,
        DAPUR_UMUM,
        "SAR",
        PSIKOSOSIAL,
        LOGISTIK,
        ALAT_BERAT
    )

    fun matchesSkillFilter(skillsCsv: String, selectedSkillFilter: String): Boolean {
        if (selectedSkillFilter.isBlank() || selectedSkillFilter.equals("Semua", ignoreCase = true)) {
            return true
        }
        val normalizedFilter = selectedSkillFilter.trim().lowercase()
        return skillsCsv.split("|", ",")
            .map { it.trim().lowercase() }
            .any { skill ->
                skill == normalizedFilter ||
                    (normalizedFilter == "sar" && skill.contains("sar")) ||
                    skill.contains(normalizedFilter)
            }
    }
}

/**
 * Daftar Posko & Titik Koordinat Sebaran Relawan di 15 Kecamatan Kabupaten Mukomuko.
 */
data class VolunteerPoskoPoint(
    val districtName: String,
    val poskoName: String,
    val latitude: Double,
    val longitude: Double,
    val mapNormalizedX: Float,
    val mapNormalizedY: Float
)

object MukomukoVolunteerPoskoCatalog {
    val POSKO_LIST = listOf(
        VolunteerPoskoPoint("Kota Mukomuko", "Posko Utama Pusdalops Pasar Mukomuko", -2.5830, 101.1240, 0.32f, 0.35f),
        VolunteerPoskoPoint("Ipuh", "Posko Taktis Pantai Pasar Ipuh", -3.0012, 101.5123, 0.72f, 0.78f),
        VolunteerPoskoPoint("Teramang Jaya", "Posko Siaga Banjir Pasar Bantal", -2.7623, 101.2945, 0.50f, 0.56f),
        VolunteerPoskoPoint("Penarik", "Posko Logistik & Dapur Umum Penarik", -2.6810, 101.3210, 0.54f, 0.44f),
        VolunteerPoskoPoint("Lubuk Pinang", "Posko Siaga Bendung Manjunto Lubuk Pinang", -2.4510, 101.1520, 0.24f, 0.20f),
        VolunteerPoskoPoint("Pondok Suguh", "Posko Evakuasi Pesisir Pondok Suguh", -2.8850, 101.3910, 0.62f, 0.68f),
        VolunteerPoskoPoint("Air Dikit", "Posko Siaga Sari Bulan Air Dikit", -2.6340, 101.2110, 0.41f, 0.41f),
        VolunteerPoskoPoint("Selagan Raya", "Posko Hulu Sungai Jerinjing", -2.5920, 101.2850, 0.49f, 0.33f),
        VolunteerPoskoPoint("XIV Koto", "Posko Tanggap Darurat Lubuk Sanai", -2.5480, 101.1810, 0.37f, 0.29f),
        VolunteerPoskoPoint("Malin Deman", "Posko Perbukitan Talang Arah", -2.9210, 101.6210, 0.85f, 0.71f),
        VolunteerPoskoPoint("V Koto", "Posko Siaga Lalang Luas V Koto", -2.5120, 101.2390, 0.43f, 0.23f),
        VolunteerPoskoPoint("Teras Terunjam", "Posko Terpadu Teras Terunjam", -2.7120, 101.3650, 0.58f, 0.51f),
        VolunteerPoskoPoint("Sungai Rumbai", "Posko Siaga Retak Mudik", -2.9520, 101.4710, 0.68f, 0.74f),
        VolunteerPoskoPoint("Air Rami", "Posko Bukit Harapan Air Rami", -2.8410, 101.5310, 0.76f, 0.63f),
        VolunteerPoskoPoint("Air Manjunto", "Posko Agropolitan Tirta Makmur", -2.5590, 101.2240, 0.44f, 0.27f)
    )

    val SHIFT_OPTIONS = listOf(
        "Shift Pagi (08:00 - 16:00 WIB)",
        "Shift Sore/Malam (16:00 - 00:00 WIB)",
        "Shift Siaga Malam (00:00 - 08:00 WIB)",
        "Siaga Penuh 24 Jam (Tanggap Darurat)"
    )

    val LEGAL_COMMUNITY_ORGANIZATIONS = setOf(
        "YAKI INDONESIA",
        "KPS Selagan-Manjunto",
        "FPL (Forum Peduli Lingkungan)"
    )

    fun isLegalIncorporatedOrganization(orgName: String): Boolean {
        val cleaned = orgName.trim()
        return LEGAL_COMMUNITY_ORGANIZATIONS.any { it.equals(cleaned, ignoreCase = true) }
    }

    val ORGANIZATION_PRESETS = listOf(
        "Relawan Individu",
        "YAKI INDONESIA",
        "KPS Selagan-Manjunto",
        "FPL (Forum Peduli Lingkungan)",
        "PMI Kab. Mukomuko",
        "Tagana Dinsos",
        "Karang Taruna",
        "BASARNAS / Potensi SAR",
        "MDMC Muhammadiyah",
        "RAPI / ORARI Komunikasi",
        "Pramuka Peduli"
    )
}

/**
 * Helper Keamanan, Privasi NIK, & Format Waktu Kehadiran Posko (WIB).
 * NIK dan No. HP relawan HANYA dapat dilihat utuh oleh Admin (Pusdalops) yang sudah login.
 * Pada tampilan publik / list biasa, NIK disamarkan menjadi format "1234********" dan No. HP disamarkan.
 */
object VolunteerPrivacyHelper {
    const val DUPLICATE_NIK_ALERT_MESSAGE = "NIK sudah terdaftar, silakan hubungi admin."

    fun maskNik(nik: String, isAdminLoggedIn: Boolean): String {
        val cleaned = nik.trim()
        if (cleaned.isEmpty()) return "-"
        if (isAdminLoggedIn) return cleaned
        return if (cleaned.length >= 4) {
            cleaned.take(4) + "********"
        } else {
            "1234********"
        }
    }

    fun maskPhone(phone: String, isAdminLoggedIn: Boolean): String {
        val cleaned = phone.trim()
        if (cleaned.isEmpty()) return "-"
        if (isAdminLoggedIn) return cleaned
        return when {
            cleaned.length >= 8 -> cleaned.take(4) + "****" + cleaned.takeLast(2)
            cleaned.length >= 4 -> cleaned.take(2) + "****"
            else -> "****"
        }
    }

    fun formatWibTimeOnly(timestampMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("HH.mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            "${sdf.format(Date(timestampMs))} WIB"
        } catch (_: Exception) {
            "08.00 WIB"
        }
    }

    fun formatWibDateTime(timestampMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH.mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            "${sdf.format(Date(timestampMs))} WIB"
        } catch (_: Exception) {
            "-"
        }
    }
}

/**
 * Entity Room SQLite untuk penyimpanan Offline-First Data Relawan Kebencanaan.
 * NIK (16 digit) berfungsi sebagai Primary Key untuk menjamin Anti Data Ganda.
 */
@Entity(tableName = "bpbd_volunteers")
data class VolunteerEntity(
    @PrimaryKey
    val nik: String, // Primary Key Wajib (16 Digit NIK KTP) - Anti Data Ganda
    val id: String, // Kode Registrasi Relawan (misal: REL-MMK-2026-001)
    val fullName: String,
    val phoneNumber: String,
    val organization: String,
    val skillsCsv: String, // Disimpan dipisahkan tanda '|' misal "Medis|Psikososial"
    val homeDistrict: String,
    val assignedDistrict: String = "",
    val assignedPoskoName: String = "",
    val assignedShift: String = "",
    val status: String = VolunteerStatus.PENDING.code, // Pending -> Verified -> Assigned -> Done
    val isActive: Boolean = true, // Aktif / Non-aktif
    val needsAssistance: Boolean = false, // Penanda darurat "Butuh Bantuan" di lapangan
    val assistanceNote: String = "",
    val selfiePhotoUri: String? = null,
    val certificatePhotoUri: String? = null,
    val waktuCheckIn: Long? = null, // Timestamp server saat relawan klik [ CHECK-IN POSKO ]
    val waktuCheckOut: Long? = null, // Timestamp server saat relawan klik [ CHECK-OUT POSKO ]
    val suratTugasNumber: String = "", // Nomor Surat Tugas Resmi BPBD Mukomuko
    val suratTugasPdfPath: String? = null, // Path File PDF Surat Tugas Digital
    val registeredAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val verifiedByOfficer: String = "",
    val isSyncedToCloud: Boolean = false
) {
    fun skillsList(): List<String> {
        return skillsCsv.split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun statusEnum(): VolunteerStatus = VolunteerStatus.fromCode(status)

    fun effectivePoskoDistrict(): String {
        return assignedDistrict.ifBlank { homeDistrict.ifBlank { "Kota Mukomuko" } }
    }

    fun effectivePoskoName(): String {
        return assignedPoskoName.ifBlank {
            MukomukoVolunteerPoskoCatalog.POSKO_LIST.firstOrNull {
                it.districtName.equals(effectivePoskoDistrict(), ignoreCase = true)
            }?.poskoName ?: "Posko Utama Pusdalops Pasar Mukomuko"
        }
    }

    fun isCurrentlyCheckedInAtPosko(): Boolean {
        return statusEnum() == VolunteerStatus.ASSIGNED &&
            waktuCheckIn != null &&
            (waktuCheckOut == null || waktuCheckIn > waktuCheckOut)
    }

    /**
     * Menghasilkan teks status kehadiran real-time di Posko:
     * Misal: "Sedang Bertugas di Posko Utama Pusdalops Pasar Mukomuko sejak 08.00 WIB"
     */
    fun realtimeAttendanceStatusText(): String {
        val poskoLabel = effectivePoskoName()
        val checkIn = waktuCheckIn
        val checkOut = waktuCheckOut
        return when {
            checkIn != null && (checkOut == null || checkIn > checkOut) -> {
                "Sedang Bertugas di $poskoLabel sejak ${VolunteerPrivacyHelper.formatWibTimeOnly(checkIn)}"
            }
            checkIn != null && checkOut != null && checkOut >= checkIn -> {
                "Telah Check-out dari $poskoLabel pada ${VolunteerPrivacyHelper.formatWibTimeOnly(checkOut)} (Check-in: ${VolunteerPrivacyHelper.formatWibTimeOnly(checkIn)})"
            }
            statusEnum() == VolunteerStatus.ASSIGNED -> {
                "Ditugaskan di $poskoLabel • Belum Check-in Kehadiran"
            }
            else -> {
                "Status: ${statusEnum().displayLabel}"
            }
        }
    }
}

@Dao
interface VolunteerDao {
    @Query("SELECT * FROM bpbd_volunteers ORDER BY needsAssistance DESC, updatedAt DESC")
    fun observeAllVolunteers(): Flow<List<VolunteerEntity>>

    @Query("SELECT * FROM bpbd_volunteers ORDER BY needsAssistance DESC, updatedAt DESC")
    suspend fun getAllVolunteersOnce(): List<VolunteerEntity>

    @Query("SELECT * FROM bpbd_volunteers WHERE nik = :nik LIMIT 1")
    suspend fun getVolunteerByNik(nik: String): VolunteerEntity?

    @Query("SELECT * FROM bpbd_volunteers WHERE id = :id OR nik = :id LIMIT 1")
    suspend fun getVolunteerByIdOrNik(id: String): VolunteerEntity?

    @Query("SELECT COUNT(*) > 0 FROM bpbd_volunteers WHERE nik = :nik")
    suspend fun existsByNik(nik: String): Boolean

    @Query("SELECT * FROM bpbd_volunteers WHERE isSyncedToCloud = 0")
    suspend fun getUnsyncedVolunteers(): List<VolunteerEntity>

    @Query("SELECT COUNT(*) FROM bpbd_volunteers")
    suspend fun getCount(): Int

    /**
     * Insert pendaftaran baru dengan ABORT jika Primary Key NIK sudah ada (Anti Data Ganda).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNewVolunteer(volunteer: VolunteerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVolunteer(volunteer: VolunteerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(volunteers: List<VolunteerEntity>)

    @Query("UPDATE bpbd_volunteers SET isSyncedToCloud = 1, updatedAt = :updatedAt WHERE nik = :nik")
    suspend fun markSyncedToCloud(nik: String, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET status = :newStatus, 
            isActive = :isActive, 
            verifiedByOfficer = :officerName, 
            updatedAt = :updatedAt, 
            isSyncedToCloud = 0 
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateVerificationStatus(
        nik: String,
        newStatus: String,
        isActive: Boolean,
        officerName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET status = :newStatus, 
            assignedDistrict = :district, 
            assignedPoskoName = :poskoName, 
            assignedShift = :shift, 
            suratTugasNumber = :suratTugasNumber,
            suratTugasPdfPath = :suratTugasPdfPath,
            isActive = 1, 
            verifiedByOfficer = :officerName, 
            updatedAt = :updatedAt, 
            isSyncedToCloud = 0 
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateAssignmentWithPdf(
        nik: String,
        newStatus: String,
        district: String,
        poskoName: String,
        shift: String,
        suratTugasNumber: String,
        suratTugasPdfPath: String?,
        officerName: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET waktuCheckIn = :waktuCheckIn, 
            waktuCheckOut = NULL, 
            updatedAt = :updatedAt, 
            isSyncedToCloud = 0 
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateCheckInPosko(
        nik: String,
        waktuCheckIn: Long,
        updatedAt: Long = waktuCheckIn
    )

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET waktuCheckOut = :waktuCheckOut, 
            updatedAt = :updatedAt, 
            isSyncedToCloud = 0 
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateCheckOutPosko(
        nik: String,
        waktuCheckOut: Long,
        updatedAt: Long = waktuCheckOut
    )

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET suratTugasPdfPath = :pdfPath,
            suratTugasNumber = :suratTugasNumber,
            updatedAt = :updatedAt
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateSuratTugasPdfPath(
        nik: String,
        pdfPath: String,
        suratTugasNumber: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE bpbd_volunteers 
        SET needsAssistance = :needsAssistance, 
            assistanceNote = :note, 
            updatedAt = :updatedAt, 
            isSyncedToCloud = 0 
        WHERE nik = :nik OR id = :nik
        """
    )
    suspend fun updateNeedsAssistance(
        nik: String,
        needsAssistance: Boolean,
        note: String,
        updatedAt: Long = System.currentTimeMillis()
    )
}

/**
 * 5 Data Dummy Relawan Kabupaten Mukomuko dengan Keahlian & Status Berbeda
 * agar Dashboard, Peta Sebaran, Check-in Posko, dan Surat Tugas Digital langsung terlihat hidup.
 */
object InitialDummyVolunteers {
    private fun buildMorningEightWibEpoch(referenceMs: Long): Long {
        return try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
                timeInMillis = referenceMs
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            referenceMs - 3_600_000L * 2
        }
    }

    fun getDummyVolunteers(nowMs: Long = 1795260000000L): List<VolunteerEntity> {
        val eightAmWib = buildMorningEightWibEpoch(nowMs)
        return listOf(
            VolunteerEntity(
                nik = "1706011408910001",
                id = "REL-MMK-2026-001",
                fullName = "dr. Rahmat Hidayatullah",
                phoneNumber = "081278904411",
                organization = "PMI Kab. Mukomuko",
                skillsCsv = "${VolunteerSkills.MEDIS}|${VolunteerSkills.PSIKOSOSIAL}",
                homeDistrict = "Kota Mukomuko",
                assignedDistrict = "Kota Mukomuko",
                assignedPoskoName = "Posko Utama Pusdalops Pasar Mukomuko",
                assignedShift = "Shift Pagi (08:00 - 16:00 WIB)",
                status = VolunteerStatus.ASSIGNED.code,
                isActive = true,
                needsAssistance = false,
                assistanceNote = "",
                selfiePhotoUri = "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=300&auto=format&fit=crop&q=80",
                certificatePhotoUri = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=400&auto=format&fit=crop&q=80",
                waktuCheckIn = eightAmWib,
                waktuCheckOut = null,
                suratTugasNumber = "800/ST-REL/BPBD-MMK/XI/2026/001",
                suratTugasPdfPath = null,
                registeredAt = nowMs - 86_400_000L * 4,
                updatedAt = nowMs - 3_600_000L * 2,
                verifiedByOfficer = "HITATUN ARAZAK",
                isSyncedToCloud = true
            ),
            VolunteerEntity(
                nik = "1706035204950002",
                id = "REL-MMK-2026-002",
                fullName = "Siti Aminah Tanjung",
                phoneNumber = "081367229104",
                organization = "Tagana Dinsos",
                skillsCsv = "${VolunteerSkills.DAPUR_UMUM}|${VolunteerSkills.LOGISTIK}",
                homeDistrict = "Penarik",
                assignedDistrict = "Penarik",
                assignedPoskoName = "Posko Logistik & Dapur Umum Penarik",
                assignedShift = "Shift Sore/Malam (16:00 - 00:00 WIB)",
                status = VolunteerStatus.ASSIGNED.code,
                isActive = true,
                needsAssistance = false,
                assistanceNote = "",
                selfiePhotoUri = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=300&auto=format&fit=crop&q=80",
                certificatePhotoUri = null,
                waktuCheckIn = eightAmWib,
                waktuCheckOut = null,
                suratTugasNumber = "800/ST-REL/BPBD-MMK/XI/2026/002",
                suratTugasPdfPath = null,
                registeredAt = nowMs - 86_400_000L * 3,
                updatedAt = nowMs - 3_600_000L * 5,
                verifiedByOfficer = "HITATUN ARAZAK",
                isSyncedToCloud = true
            ),
            VolunteerEntity(
                nik = "1706020911930003",
                id = "REL-MMK-2026-003",
                fullName = "Hendra Saputra Pratama",
                phoneNumber = "085268113390",
                organization = "BASARNAS / Potensi SAR",
                skillsCsv = "${VolunteerSkills.SAR}|${VolunteerSkills.ALAT_BERAT}",
                homeDistrict = "Ipuh",
                assignedDistrict = "Teramang Jaya",
                assignedPoskoName = "Posko Siaga Banjir Pasar Bantal",
                assignedShift = "Siaga Penuh 24 Jam (Tanggap Darurat)",
                status = VolunteerStatus.ASSIGNED.code,
                isActive = true,
                needsAssistance = true,
                assistanceNote = "Butuh tambahan perahu karet LCR & BBM di titik evakuasi Pasar Bantal",
                selfiePhotoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
                certificatePhotoUri = "https://images.unsplash.com/photo-1544717305-2782549b5136?w=400&auto=format&fit=crop&q=80",
                waktuCheckIn = eightAmWib,
                waktuCheckOut = null,
                suratTugasNumber = "800/ST-REL/BPBD-MMK/XI/2026/003",
                suratTugasPdfPath = null,
                registeredAt = nowMs - 86_400_000L * 5,
                updatedAt = nowMs - 1_800_000L,
                verifiedByOfficer = "SATRIA AMANDA PUTRA",
                isSyncedToCloud = true
            ),
            VolunteerEntity(
                nik = "1706054402970004",
                id = "REL-MMK-2026-004",
                fullName = "Nadia Putri Lestari, S.Psi",
                phoneNumber = "082179458821",
                organization = "MDMC Muhammadiyah",
                skillsCsv = "${VolunteerSkills.PSIKOSOSIAL}|${VolunteerSkills.MEDIS}",
                homeDistrict = "Lubuk Pinang",
                assignedDistrict = "",
                assignedPoskoName = "",
                assignedShift = "",
                status = VolunteerStatus.VERIFIED.code,
                isActive = true,
                needsAssistance = false,
                assistanceNote = "",
                selfiePhotoUri = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=300&auto=format&fit=crop&q=80",
                certificatePhotoUri = null,
                waktuCheckIn = null,
                waktuCheckOut = null,
                suratTugasNumber = "",
                suratTugasPdfPath = null,
                registeredAt = nowMs - 86_400_000L * 2,
                updatedAt = nowMs - 3_600_000L * 8,
                verifiedByOfficer = "HITATUN ARAZAK",
                isSyncedToCloud = true
            ),
            VolunteerEntity(
                nik = "1706041807960005",
                id = "REL-MMK-2026-005",
                fullName = "Dedi Kurniawan Chaniago",
                phoneNumber = "085377102938",
                organization = "Karang Taruna",
                skillsCsv = "${VolunteerSkills.LOGISTIK}|${VolunteerSkills.ALAT_BERAT}|${VolunteerSkills.SAR}",
                homeDistrict = "Pondok Suguh",
                assignedDistrict = "",
                assignedPoskoName = "",
                assignedShift = "",
                status = VolunteerStatus.PENDING.code,
                isActive = true,
                needsAssistance = false,
                assistanceNote = "",
                selfiePhotoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
                certificatePhotoUri = null,
                waktuCheckIn = null,
                waktuCheckOut = null,
                suratTugasNumber = "",
                suratTugasPdfPath = null,
                registeredAt = nowMs - 3_600_000L * 3,
                updatedAt = nowMs - 3_600_000L * 3,
                verifiedByOfficer = "",
                isSyncedToCloud = false
            )
        )
    }
}

@Database(
    entities = [VolunteerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VolunteerDatabase : RoomDatabase() {
    abstract fun volunteerDao(): VolunteerDao

    companion object {
        const val DATABASE_NAME = "sipagar_volunteers.db"

        @Volatile
        private var INSTANCE: VolunteerDatabase? = null

        fun getInstance(context: Context): VolunteerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): VolunteerDatabase {
            return Room.databaseBuilder(
                appContext,
                VolunteerDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .allowMainThreadQueries()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedInitialVolunteersSql(db)
                    }
                })
                .build()
                .also { database ->
                    try {
                        val writableDb = database.openHelper.writableDatabase
                        val cursor = writableDb.query("SELECT COUNT(*) FROM bpbd_volunteers")
                        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                        cursor.close()
                        if (count == 0) {
                            seedInitialVolunteersSql(writableDb)
                        }
                    } catch (_: Exception) {
                    }
                }
        }

        private fun seedInitialVolunteersSql(db: SupportSQLiteDatabase) {
            try {
                for (v in InitialDummyVolunteers.getDummyVolunteers()) {
                    val sql = """
                        INSERT OR IGNORE INTO bpbd_volunteers (
                            nik, id, fullName, phoneNumber, organization, skillsCsv,
                            homeDistrict, assignedDistrict, assignedPoskoName, assignedShift,
                            status, isActive, needsAssistance, assistanceNote,
                            selfiePhotoUri, certificatePhotoUri, waktuCheckIn, waktuCheckOut,
                            suratTugasNumber, suratTugasPdfPath,
                            registeredAt, updatedAt, verifiedByOfficer, isSyncedToCloud
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    db.execSQL(
                        sql,
                        arrayOf<Any?>(
                            v.nik,
                            v.id,
                            v.fullName,
                            v.phoneNumber,
                            v.organization,
                            v.skillsCsv,
                            v.homeDistrict,
                            v.assignedDistrict,
                            v.assignedPoskoName,
                            v.assignedShift,
                            v.status,
                            if (v.isActive) 1 else 0,
                            if (v.needsAssistance) 1 else 0,
                            v.assistanceNote,
                            v.selfiePhotoUri,
                            v.certificatePhotoUri,
                            v.waktuCheckIn,
                            v.waktuCheckOut,
                            v.suratTugasNumber,
                            v.suratTugasPdfPath,
                            v.registeredAt,
                            v.updatedAt,
                            v.verifiedByOfficer,
                            if (v.isSyncedToCloud) 1 else 0
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
