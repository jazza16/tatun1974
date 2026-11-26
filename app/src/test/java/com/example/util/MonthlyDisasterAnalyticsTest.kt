package com.example.util

import com.example.model.CitizenDisasterReport
import com.example.ui.components.DisasterChartCategory
import com.example.ui.components.buildMonthlyDisasterSummaries
import com.example.ui.components.buildPieSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthlyDisasterAnalyticsTest {

    private fun epochMillisFor(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `november reference excludes december for active year`() {
        val novRef = epochMillisFor(2026, Calendar.NOVEMBER, 26)
        val summaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2026,
            districtFilter = null,
            referenceNowMillis = novRef
        )

        assertEquals(11, summaries.size)
        assertEquals("Jan", summaries.first().monthShort)
        assertEquals("Nov", summaries.last().monthShort)
        assertFalse(summaries.any { it.monthShort == "Des" })
    }

    @Test
    fun `december reference automatically includes december for active year`() {
        val decRef = epochMillisFor(2026, Calendar.DECEMBER, 1)
        val summaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2026,
            districtFilter = null,
            referenceNowMillis = decRef
        )

        assertEquals(12, summaries.size)
        assertEquals("Des", summaries.last().monthShort)
    }

    @Test
    fun `past year retains all twelve months`() {
        val novRef = epochMillisFor(2026, Calendar.NOVEMBER, 26)
        val summaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2025,
            districtFilter = null,
            referenceNowMillis = novRef
        )

        assertEquals(12, summaries.size)
        assertTrue(summaries.any { it.monthShort == "Des" })
    }

    @Test
    fun `future year returns empty list`() {
        val novRef = epochMillisFor(2026, Calendar.NOVEMBER, 26)
        val summaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2027,
            districtFilter = null,
            referenceNowMillis = novRef
        )

        assertTrue(summaries.isEmpty())
    }

    @Test
    fun `default real data mode returns zero counts when reports empty without mixing baseline`() {
        val novRef = epochMillisFor(2026, Calendar.NOVEMBER, 26)
        val summaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2026,
            districtFilter = null,
            referenceNowMillis = novRef
        )

        assertEquals(0, summaries.sumOf { it.totalIncidents })
        assertEquals(0, summaries.sumOf { it.casualtiesAffected })
        assertEquals(0, summaries.sumOf { it.resolvedCount })
        assertTrue(summaries.all { it.topDistrict == "-" })
        assertTrue(buildPieSlices(summaries, emptyList(), pieMode = 0).isEmpty())
        assertTrue(buildPieSlices(summaries, emptyList(), pieMode = 1).isEmpty())
        assertTrue(buildPieSlices(summaries, emptyList(), pieMode = 2).isEmpty())
    }

    @Test
    fun `default real data mode aggregates exact report casualties and ignores tombstones`() {
        val novRef = epochMillisFor(2026, Calendar.NOVEMBER, 26)
        val reports = listOf(
            CitizenDisasterReport(
                id = "r1",
                disasterType = "Banjir",
                incidentTime = "05 Nov 2026 10:00",
                incidentLocation = "Jalan Lintas Ipuh",
                district = "Ipuh",
                village = "Medan Jaya",
                latitude = -3.007,
                longitude = 101.498,
                timestamp = epochMillisFor(2026, Calendar.NOVEMBER, 5),
                casualtiesDead = 0,
                casualtiesInjured = 2,
                casualtiesMissing = 1,
                status = "Selesai"
            ),
            CitizenDisasterReport(
                id = "r2",
                disasterType = "Tanah Longsor",
                incidentTime = "10 Nov 2026 10:00",
                incidentLocation = "Tebing Sungai Malin",
                district = "Malin Deman",
                village = "Talang Arah",
                latitude = -3.095,
                longitude = 101.552,
                timestamp = epochMillisFor(2026, Calendar.NOVEMBER, 10),
                casualtiesDead = 0,
                casualtiesInjured = 0,
                casualtiesMissing = 0,
                status = "Diproses"
            ),
            CitizenDisasterReport(
                id = "r3",
                disasterType = "Banjir",
                incidentTime = "12 Nov 2026 10:00",
                incidentLocation = "Pasar Mukomuko",
                district = "Kota Mukomuko",
                village = "Bandar Ratu",
                latitude = -2.576,
                longitude = 101.119,
                timestamp = epochMillisFor(2026, Calendar.NOVEMBER, 12),
                casualtiesDead = 9,
                status = "DIHAPUS_RESET_V110"
            )
        )

        val summaries = buildMonthlyDisasterSummaries(
            reports = reports,
            selectedYear = 2026,
            districtFilter = null,
            referenceNowMillis = novRef
        )
        val nov = summaries.last()
        assertEquals(2, nov.totalIncidents)
        assertEquals(1, nov.countsByCategory[DisasterChartCategory.BANJIR])
        assertEquals(1, nov.countsByCategory[DisasterChartCategory.LONGSOR])
        assertEquals(3, nov.casualtiesAffected) // Exact 2+1+0 without fabricated +5 or *14
        assertEquals(1, nov.resolvedCount)

        val baselineSummaries = buildMonthlyDisasterSummaries(
            reports = emptyList(),
            selectedYear = 2026,
            districtFilter = null,
            referenceNowMillis = novRef,
            includeHistoricalBaseline = true
        )
        assertTrue(baselineSummaries.sumOf { it.totalIncidents } > 0)
    }
}
