package com.example.data.local

import android.content.Context
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database Entity for Official BPBD Mukomuko Personnel.
 * Optimized with high-performance indices on NIP (unique), role, shift, and name.
 */
@Entity(
    tableName = "personnel",
    indices = [
        Index(value = ["nip"], unique = true),
        Index(value = ["role"]),
        Index(value = ["shift"]),
        Index(value = ["name"]),
        Index(value = ["role", "shift"])
    ]
)
data class Personnel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nip: String,
    val role: String,
    val unit: String = "Pusdalops BPBD Kab. Mukomuko",
    val shift: String = "Shift Siaga 24 Jam",
    val phone: String = ""
) {
    fun formattedLabel(): String {
        val nipPrefix = if (nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
        return "$name ($nipPrefix: $nip)"
    }
}

/**
 * Official ASN BPBD Kabupaten Mukomuko assigned as Operator Pusdalops.
 * Pre-populated into the database credentials system with separate columns for Name and NIP.
 * This facilitates officer name selection via dropdown/picker on the login screen,
 * preventing unauthorized parties from manually typing spoofed names and NIPs.
 */
object OfficialAsnBpbdPersonnel {
    const val ROLE_PENGAWAS = "Pengawas BPBD"
    const val ROLE_SUPERVISOR = "Supervisor BPBD"
    const val ROLE_KOORD_ADMINISTRASI = "Koord. Administrasi"
    const val ROLE_MANAJER = "Manajer Pusdalops-PB"
    const val ROLE_OPERATOR = "Operator Pusdalops"
    const val ROLE_STAF_LOGISTIK_TRC = "Staf Logistik/TRC"

    val ASN_MUKOMUKO_2026_LIST: List<Pair<String, String>> = listOf(
        "A. RAHMAN" to "197906132005021002",
        "AAN FITRADINATA, SE" to "197903122006041007",
        "AHMAD HIDAYAT SYAH" to "197302182006041005",
        "AINUN MARDIATI CANIAGO, S.Pd" to "198812302025212141",
        "AMRI KURNIADI" to "197608122011011003",
        "ANDIKA JAYA PUTRA" to "199311282025211115",
        "ARIF WIJOYO, S.E" to "198606282025211024",
        "DAVID HARIYANTO, S.I.P" to "199303302025211135",
        "DESTRI GANDALIA, S.STP, M.A.P" to "198812242010102001",
        "DHUHAN PUTRA YUNARDI, S.M." to "200005282025211058",
        "DORI FRANCISKO, S.Pd." to "199512162025061002",
        "ELI SAHWATI" to "199604122025212113",
        "ERI YANTO" to "198912102025211038",
        "FEBRIZA RAHMADI" to "199502082024211009",
        "FERAHANDAYANI" to "197806182025212030",
        "FIRMANSYAH" to "199305102025211171",
        "HARI KURNIAWAN" to "198607092015021001",
        "HERMANDAHLI" to "197106092025211033",
        "HITATUN" to "197401282006041006",
        "LISMIE NOVTRIANTI" to "199711282025212144",
        "MECCA RIZKA, S.M." to "199908042025212096",
        "MEI SAHPUTRA" to "199405252019031005",
        "MUHAMMAD RIGAN JASWARI, S.Akun." to "200201252025211014",
        "NITA KUSWANI" to "197809192010012010",
        "NOKI PRADINATA, S.Agr" to "199211292025211127",
        "NOVRANDA EKA PUTRA" to "199011042019031006",
        "PUTRI MAYANG SARI" to "199906302025212010",
        "ROMI YANTI, S.Pd" to "199108092025212036",
        "RURI IRWANDI" to "197906072006041011",
        "SAMSUL BAHRI" to "198306102009041001",
        "SATRIA AMANDA PUTRA, S.M." to "200003112025211067",
        "SUMARNI" to "197502102009042001",
        "YESSY TRIYANI" to "198005042011012004",
        "YUDHA SOFRIANTO, S.A.P." to "199807202025061002"
    )

    val LEADERSHIP = listOf(
        Personnel(
            name = "RURI IRWANDI, ST, MT",
            nip = "197906072006041011",
            role = ROLE_PENGAWAS,
            unit = "BPBD Kab. Mukomuko",
            shift = "Pimpinan Siaga"
        ),
        Personnel(
            name = "HITATUN ARAZAK",
            nip = "197401282006041006",
            role = ROLE_MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam"
        ),
        Personnel(
            name = "DESTRI GANDALIA, S.STP.M.AP",
            nip = "198812242010102001",
            role = ROLE_SUPERVISOR,
            unit = "BPBD Kab. Mukomuko",
            shift = "Supervisi Posko"
        ),
        Personnel(
            name = "AAN FITRADINATA, SE",
            nip = "197903122006041007",
            role = ROLE_KOORD_ADMINISTRASI,
            unit = "BPBD Kab. Mukomuko",
            shift = "Administrasi & Logistik"
        )
    )

    val OPERATORS = listOf(
        Personnel(
            name = "YUDHA SOFRIANTO,S.A.P",
            nip = "199807202025061002",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "ERI YANTO",
            nip = "198912102025211038",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "MUHAMMAD RIGAN JASWARI",
            nip = "200201252025211014",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "AINUN MARDIATI.S.Pd.SD",
            nip = "198812302025212141",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "SATRIA AMANDA PUTRA",
            nip = "200003112025211067",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "FIRMANSYAH",
            nip = "199305102025211171",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "ARIF WIJOYO",
            nip = "198606282025211024",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "DAVID HARIANTO",
            nip = "199303302025211135",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        )
    )

    val STAF_LOGISTIK_TRC: List<Personnel> = ASN_MUKOMUKO_2026_LIST
        .filter { (_, nip) ->
            (LEADERSHIP + OPERATORS).none { it.nip.equals(nip, ignoreCase = true) }
        }
        .map { (name, nip) ->
            Personnel(
                name = name,
                nip = nip,
                role = ROLE_STAF_LOGISTIK_TRC,
                unit = "BPBD Kab. Mukomuko",
                shift = "Shift Siaga 24 Jam"
            )
        }

    val ALL_OFFICIAL_PERSONNEL = LEADERSHIP + OPERATORS + STAF_LOGISTIK_TRC

    fun toOfficerProfiles(): List<OfficerProfile> {
        return ALL_OFFICIAL_PERSONNEL.map {
            OfficerProfile(
                id = "asn-${it.nip}",
                fullName = it.name,
                nip = it.nip,
                role = it.role,
                unit = it.unit,
                shift = it.shift,
                phone = it.phone
            )
        }
    }

    fun getRoleBadgeColor(role: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
        return when {
            role.contains("Pengawas", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFEDE9FE), androidx.compose.ui.graphics.Color(0xFF6D28D9)) // Ungu
            role.contains("Supervisor", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFE0F2FE), androidx.compose.ui.graphics.Color(0xFF0369A1)) // Biru Muda
            role.contains("Koord", ignoreCase = true) || role.contains("Administrasi", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFCCFBF1), androidx.compose.ui.graphics.Color(0xFF0F766E)) // Hijau Teal
            role.contains("Manajer", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFEF3C7), androidx.compose.ui.graphics.Color(0xFFB45309)) // Amber Emas
            role.contains("Operator", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFFEDD5), androidx.compose.ui.graphics.Color(0xFFC2410C)) // Oranye BPBD
            role.contains("TRC", ignoreCase = true) || role.contains("Lapangan", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFEE2E2), androidx.compose.ui.graphics.Color(0xFFB91C1C)) // Merah Siaga
            else ->
                Pair(androidx.compose.ui.graphics.Color(0xFFF1F5F9), androidx.compose.ui.graphics.Color(0xFF475569)) // Netral Slate
        }
    }
}

/**
 * Seeding 34 data ASN BPBD Kabupaten Mukomuko ke dalam tabel `personnel` dan `officer_credentials`.
 * - Mengecek keberadaan NIP terlebih dahulu di database Room.
 * - Jika NIP belum terdaftar, menyimpan entitas baru dengan Role default 'Staf Logistik/TRC'
 *   dan PIN default 6 digit terakhir NIP.
 * - Jika NIP sudah terdaftar, dilewati (skip) agar tidak terjadi duplikasi atau menimpa PIN yang sudah diubah.
 */
fun seedDataAsnMukomuko2026(context: Context) {
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        val db = AppDatabase.getInstance(appContext)
        val personnelDao = db.personnelDao()
        val credentialDao = db.credentialDao()

        for ((rawName, rawNip) in OfficialAsnBpbdPersonnel.ASN_MUKOMUKO_2026_LIST) {
            val name = rawName.trim()
            val nip = rawNip.trim()
            if (name.isEmpty() || nip.isEmpty()) continue

            // 1. Cek dan masukkan ke tabel personnel jika NIP belum terdaftar
            val existingPersonnel = personnelDao.getPersonnelByNip(nip)
            if (existingPersonnel == null) {
                personnelDao.insertPersonnel(
                    Personnel(
                        name = name,
                        nip = nip,
                        role = OfficialAsnBpbdPersonnel.ROLE_STAF_LOGISTIK_TRC,
                        unit = "BPBD Kab. Mukomuko",
                        shift = "Shift Siaga 24 Jam"
                    )
                )
            }

            // 2. Cek dan masukkan ke tabel officer_credentials jika NIP belum terdaftar
            val existingCredential = credentialDao.getCredentialByNip(nip)
            if (existingCredential == null) {
                val defaultPin = nip.takeLast(6)
                val salt = OfficerCredentialEntity.generateCryptographicSalt()
                val newCredential = OfficerCredentialEntity(
                    nip = nip,
                    fullName = name,
                    role = existingPersonnel?.role ?: OfficialAsnBpbdPersonnel.ROLE_STAF_LOGISTIK_TRC,
                    unit = existingPersonnel?.unit ?: "BPBD Kab. Mukomuko",
                    shift = existingPersonnel?.shift ?: "Shift Siaga 24 Jam",
                    phone = existingPersonnel?.phone ?: "",
                    pinHash = OfficerCredentialEntity.hashWithSalt(defaultPin, salt),
                    salt = salt,
                    isCustomPinSet = false,
                    initialSecurePin = defaultPin
                )
                credentialDao.insertCredential(newCredential)
            }
        }
    }
}
