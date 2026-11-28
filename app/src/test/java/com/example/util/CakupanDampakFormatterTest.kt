package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CakupanDampakFormatterTest {

    @Test
    fun formatStructuredCreatesTwoDistinctNumberedLinesWithoutEmDash() {
        val lokasi = "Sultan Gelumat Syah Belakang SPBU Bandar Ratu, Desa Kelurahan Bandar Ratu, Kec. Kota Mukomuko"
        val deskripsi = "Akibat Curah Hujan yng tinggi menyebabkn Drainase Pembuangan tersumbat, dimohon kepada BPBD, untuk menindak lanjuti."

        val formatted = CakupanDampakFormatter.formatStructured(lokasi, deskripsi)
        val lines = formatted.lines()

        assertEquals(2, lines.size)
        assertEquals("1. Lokasi Kejadian: $lokasi", lines[0])
        assertEquals("2. Deskripsi Kejadian/Bencana: $deskripsi", lines[1])
        assertFalse("Must never merge location and description with em-dash", formatted.contains(" — "))
    }

    @Test
    fun legacyMergedEmDashStringSplitsCleanlyIntoTwoPdfLines() {
        val legacy = "Sultan Gelumat Syah Belakang SPBU Bandar Ratu, Desa Kelurahan Bandar Ratu, Kec. Kota Mukomuko — Akibat Curah Hujan yng tinggi menyebabkn Drainase Pembuangan tersumbat, dimohon kepada BPBD, untuk menindak lanjuti."

        val pdfLines = CakupanDampakFormatter.toPdfLines(legacy)
        assertEquals(2, pdfLines.size)
        assertTrue(pdfLines[0].startsWith("1. Lokasi Kejadian: Sultan Gelumat Syah"))
        assertTrue(pdfLines[1].startsWith("2. Deskripsi Kejadian/Bencana: Akibat Curah Hujan"))
        assertFalse(pdfLines[0].contains(" — "))
        assertFalse(pdfLines[1].contains(" — "))
    }

    @Test
    fun parseAndRoundTripPreservesBothFieldsWithoutDuplicatingPrefixes() {
        val structured = CakupanDampakFormatter.formatStructured(
            lokasiKejadian = "Desa Pasar Ipuh, Kec. Ipuh",
            deskripsiKejadian = "Gelombang pasang merendam 12 rumah nelayan"
        )
        val parsed = CakupanDampakFormatter.parse(structured)
        assertEquals("Desa Pasar Ipuh, Kec. Ipuh", parsed.lokasiKejadian)
        assertEquals("Gelombang pasang merendam 12 rumah nelayan", parsed.deskripsiKejadian)

        val roundTripped = CakupanDampakFormatter.formatStructured(parsed.lokasiKejadian, parsed.deskripsiKejadian)
        assertEquals(structured, roundTripped)
    }
}
