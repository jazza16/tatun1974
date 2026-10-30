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
    const val ROLE_OPERATOR = "Operator Pusdalops"

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

    fun toOfficerProfiles(): List<OfficerProfile> {
        return OPERATORS.map {
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
}
