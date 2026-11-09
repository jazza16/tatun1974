package id.go.mukomuko.sipagarkita.feature.pesanvisual.context

import java.time.Instant

data class SituationalContext(
    val fetchedAt: Instant,
    val weather: WeatherSnapshot? = null,
    val earthquake: EarthquakeSnapshot? = null,
    val maritime: MaritimeSnapshot? = null,
    val risk: RiskSnapshot? = null,
    val evacuation: EvacuationSnapshot? = null,
    val earlyWarnings: List<EarlyWarning> = emptyList(),
    val mitigationNotes: List<String> = emptyList()
) {
    fun isEmpty(): Boolean =
        weather == null && earthquake == null && maritime == null &&
        risk == null && evacuation == null && earlyWarnings.isEmpty() &&
        mitigationNotes.isEmpty()
}

data class WeatherSnapshot(
    val summary: String,
    val affectedAreas: List<String>,
    val validUntil: Instant,
    val source: String = "BMKG"
)

data class EarthquakeSnapshot(
    val magnitude: Double,
    val location: String,
    val depthKm: Int,
    val occurredAt: Instant,
    val source: String = "InaTEWS BMKG"
) {
    fun isRecent(now: Instant = Instant.now()): Boolean {
        val hours = java.time.Duration.between(occurredAt, now).toHours()
        return hours in 0..24
    }
}

data class MaritimeSnapshot(
    val waveHeight: String,
    val warningLevel: String,
    val validUntil: Instant,
    val source: String = "BMKG Maritim"
)

data class RiskSnapshot(
    val highRiskAreas: List<String>,
    val mediumRiskAreas: List<String>,
    val source: String = "InaRISK BNPB"
)

data class EvacuationSnapshot(val routes: List<EvacuationRoute>)

data class EvacuationRoute(
    val fromArea: String,
    val toLocation: String,
    val notes: String? = null
)

data class EarlyWarning(
    val type: String,
    val severity: String,
    val summary: String,
    val source: String,
    val issuedAt: Instant
) {
    fun isFresh(now: Instant = Instant.now()): Boolean {
        val hours = java.time.Duration.between(issuedAt, now).toHours()
        return hours in 0..12
    }
}
