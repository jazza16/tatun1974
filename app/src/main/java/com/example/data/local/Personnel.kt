package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

    val ALL_OFFICIAL_PERSONNEL = LEADERSHIP + OPERATORS

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
