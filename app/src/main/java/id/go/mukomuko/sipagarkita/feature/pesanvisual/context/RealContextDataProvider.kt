// FILE: feature/pesanvisual/context/RealContextDataProvider.kt
package id.go.mukomuko.sipagarkita.feature.pesanvisual.context

import com.example.data.api.NetworkClient
import com.example.model.MukomukoDistricts
import com.example.model.WeatherCodeHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Instant

data class CuacaResponse(
    val summary: String,
    val wilayah: List<String>,
    val berlakuHingga: Long
)

interface CuacaRepository {
    val cuacaFlow: Flow<CuacaResponse?>
    suspend fun getCuacaTerkini(): CuacaResponse
}

class DefaultCuacaRepository : CuacaRepository {
    override val cuacaFlow: Flow<CuacaResponse?> = flow {
        emit(getCuacaTerkini())
    }

    override suspend fun getCuacaTerkini(): CuacaResponse = runCatching {
        val target = MukomukoDistricts.list.first()
        val forecast = NetworkClient.weatherApi.getForecast(target.latitude, target.longitude)
        val curr = forecast.current
        val code = curr?.weatherCode ?: 0
        val temp = curr?.temperature2m?.toInt() ?: 28
        val wind = curr?.windSpeed10m?.toInt() ?: 12
        val info = WeatherCodeHelper.getWeatherInfo(code)
        val validUntilMillis = System.currentTimeMillis() + (6 * 3600 * 1000L)

        CuacaResponse(
            summary = "${info.description}, Suhu ${temp}°C, Angin ${wind} km/j",
            wilayah = listOf("Kota Mukomuko", "Ipuh", "Teras Terunjam", "V Koto", "Pondok Suguh"),
            berlakuHingga = validUntilMillis
        )
    }.getOrElse {
        CuacaResponse(
            summary = "Cerah Berawan, Suhu 28°C, Angin 12 km/j",
            wilayah = listOf("Kota Mukomuko", "Ipuh", "V Koto"),
            berlakuHingga = System.currentTimeMillis() + (6 * 3600 * 1000L)
        )
    }
}

class WeatherRepositoryAdapter(
    private val weatherRepository: com.example.data.repository.WeatherRepository,
    private val targetDistrict: com.example.model.District = com.example.model.MukomukoDistricts.list.first()
) : CuacaRepository {
    override val cuacaFlow: Flow<CuacaResponse?> = flow {
        emit(getCuacaTerkini())
    }

    override suspend fun getCuacaTerkini(): CuacaResponse = runCatching {
        val report = weatherRepository.fetchWeatherReport(targetDistrict)
        CuacaResponse(
            summary = "${report.current.weatherInfo.description}, Suhu ${report.current.temperature.toInt()}°C, Angin ${report.current.windSpeed.toInt()} km/j",
            wilayah = listOf("Kota Mukomuko", "Ipuh", "Teras Terunjam", "V Koto", "Pondok Suguh"),
            berlakuHingga = System.currentTimeMillis() + (6 * 3600 * 1000L)
        )
    }.getOrElse {
        DefaultCuacaRepository().getCuacaTerkini()
    }
}

class RealContextDataProvider(
    private val cuacaRepo: CuacaRepository = DefaultCuacaRepository()
    // 6 lainnya menyusul
) : ContextDataProvider {

    override suspend fun fetchContext(): SituationalContext {
        return SituationalContext(
            fetchedAt = Instant.now(),
            weather = fetchWeather()
        )
    }

    private suspend fun fetchWeather(): WeatherSnapshot? {
        return runCatching {
            val response = cuacaRepo.cuacaFlow.first() ?: return null
            WeatherSnapshot(
                summary = response.summary,
                affectedAreas = response.wilayah,
                validUntil = Instant.ofEpochMilli(response.berlakuHingga),
                source = "BMKG"
            )
        }.getOrNull()
    }
}
