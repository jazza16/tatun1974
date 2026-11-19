package com.example.util

import com.example.model.DocumentSettings
import com.example.model.GelarFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfficerNameFormatterTest {

    @Test
    fun `formatLongOfficerName matches exact specification with default maxWords = 2`() {
        val dummyCases = listOf(
            "AINUN MARDIATI CANIAGO SPd" to "AINUN MARDIATI C SPd",
            "BUDI SANTOSO" to "BUDI SANTOSO",
            "SITI RAHMAWATI S.Kom" to "SITI RAHMAWATI S.Kom",
            "MUHAMMAD ALIF PRATAMA PUTRA S.Pd M.Si" to "MUHAMMAD ALIF P P S.Pd M.Si",
            "DEWI SARTIKA LESTARI, S.Kep., Ns." to "DEWI SARTIKA L, S.Kep., Ns.",
            "RURI IRWANDI, ST, MT" to "RURI IRWANDI, ST, MT",
            "AHMAD FAUZI NUGROHO SE MM" to "AHMAD FAUZI N SE MM",
            "RINA KUSUMA DEWI A.Md.Keb" to "RINA KUSUMA D A.Md.Keb",
            "HITATUN" to "HITATUN"
        )

        for ((input, expected) in dummyCases) {
            assertEquals(
                "Mismatch for input='$input'",
                expected,
                OfficerNameFormatter.formatLongOfficerName(input)
            )
        }
    }

    @Test
    fun `formatLongOfficerName respects dynamic maxWords parameter`() {
        val input = "AINUN MARDIATI CANIAGO SPd"
        assertEquals("AINUN M C SPd", OfficerNameFormatter.formatLongOfficerName(input, maxWords = 1))
        assertEquals("AINUN MARDIATI C SPd", OfficerNameFormatter.formatLongOfficerName(input, maxWords = 2))
        assertEquals("AINUN MARDIATI CANIAGO SPd", OfficerNameFormatter.formatLongOfficerName(input, maxWords = 3))

        val commaInput = "MUHAMMAD RIZKY PRATAMA PUTRA, S.Kom., M.Si."
        assertEquals("MUHAMMAD R P P, S.Kom., M.Si.", OfficerNameFormatter.formatLongOfficerName(commaInput, maxWords = 1))
        assertEquals("MUHAMMAD RIZKY P P, S.Kom., M.Si.", OfficerNameFormatter.formatLongOfficerName(commaInput, maxWords = 2))
        assertEquals("MUHAMMAD RIZKY PRATAMA P, S.Kom., M.Si.", OfficerNameFormatter.formatLongOfficerName(commaInput, maxWords = 3))
    }

    @Test
    fun `DocumentSettings parses max_words and abbreviate_long_names from GET document-settings JSON`() {
        val json = """
            {
              "gelar_format": "ABBREVIATE_NAME",
              "max_words": 2,
              "abbreviate_long_names": true
            }
        """.trimIndent()
        val settings = DocumentSettings.fromJson(json)!!
        assertEquals(2, settings.maxWords)
        assertEquals(GelarFormat.ABBREVIATE_NAME, settings.gelarFormat)
        assertEquals(
            "AINUN MARDIATI C SPd",
            settings.formatRawFullName("AINUN MARDIATI CANIAGO SPd")
        )

        val settingsOneWord = DocumentSettings.fromJson("""{"gelar_format": "ABBREVIATE_NAME", "max_words": 1}""")!!
        assertEquals(
            "AINUN M C SPd",
            settingsOneWord.formatRawFullName("AINUN MARDIATI CANIAGO SPd")
        )
    }
}
