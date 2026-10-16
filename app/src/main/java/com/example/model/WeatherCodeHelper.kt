package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.WeatherCloudGrey
import com.example.ui.theme.WeatherRainBlue
import com.example.ui.theme.WeatherStormPurple
import com.example.ui.theme.WeatherSunGold

data class WeatherInfo(
    val conditionText: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

object WeatherCodeHelper {
    fun getWeatherInfo(code: Int): WeatherInfo {
        return when (code) {
            0 -> WeatherInfo(
                conditionText = "Cerah",
                icon = Icons.Default.WbSunny,
                color = WeatherSunGold,
                description = "Langit cerah benderang tanpa awan tebal, cocok untuk penjemuran gabah & sawit."
            )
            1 -> WeatherInfo(
                conditionText = "Cerah Berawan",
                icon = Icons.Default.WbCloudy,
                color = WeatherSunGold,
                description = "Sebagian besar cerah dengan sedikit awan putih tipis di pesisir."
            )
            2 -> WeatherInfo(
                conditionText = "Berawan",
                icon = Icons.Default.WbCloudy,
                color = WeatherCloudGrey,
                description = "Awan menutupi sebagian langit Mukomuko, cuaca terasa sejuk dan teduh."
            )
            3 -> WeatherInfo(
                conditionText = "Mendung / Awan Tebal",
                icon = Icons.Default.Cloud,
                color = WeatherCloudGrey,
                description = "Langit kelabu tertutup awan tebal, potensi hujan dalam beberapa jam ke depan."
            )
            45, 48 -> WeatherInfo(
                conditionText = "Berkabut",
                icon = Icons.Default.Air,
                color = WeatherCloudGrey,
                description = "Kabut pagi di perbukitan & perkebunan Mukomuko, jarak pandang berkurang."
            )
            51, 53, 55 -> WeatherInfo(
                conditionText = "Gerimis",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Hujan rintik-rintik ringan, jalanan lintas barat cenderung basah dan licin."
            )
            61 -> WeatherInfo(
                conditionText = "Hujan Ringan",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Curah hujan intensitas ringan mengguyur sebagian wilayah."
            )
            63 -> WeatherInfo(
                conditionText = "Hujan Sedang",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Hujan cukup merata, gunakan payung atau jas hujan saat berkendara."
            )
            65 -> WeatherInfo(
                conditionText = "Hujan Lebat",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Hujan deras berdurasi lama, waspadai genangan dan debit air sungai meningkat."
            )
            80, 81 -> WeatherInfo(
                conditionText = "Hujan Lokal",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Hujan berpindah cepat akibat pertemuan angin laut dan daratan."
            )
            82 -> WeatherInfo(
                conditionText = "Hujan Sangat Lebat",
                icon = Icons.Default.WaterDrop,
                color = WeatherRainBlue,
                description = "Hujan sangat deras, hindari melintasi jembatan sempit atau aliran muara."
            )
            95 -> WeatherInfo(
                conditionText = "Hujan Petir",
                icon = Icons.Default.Thunderstorm,
                color = WeatherStormPurple,
                description = "Hujan disertai kilat & petir, waspadai pohon tumbang dan angin kencang."
            )
            96, 99 -> WeatherInfo(
                conditionText = "Badai Guntur",
                icon = Icons.Default.Thunderstorm,
                color = WeatherStormPurple,
                description = "Cuaca ekstrem disertai kilat intensif, tunda melaut bagi nelayan pantai."
            )
            else -> WeatherInfo(
                conditionText = "Sebagian Berawan",
                icon = Icons.Default.WbCloudy,
                color = WeatherCloudGrey,
                description = "Kondisi cuaca tropis khas pesisir barat Sumatera."
            )
        }
    }
}
