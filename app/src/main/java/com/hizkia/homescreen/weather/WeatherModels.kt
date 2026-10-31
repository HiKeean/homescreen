package com.hizkia.homescreen.weather

import androidx.annotation.StringRes
import com.hizkia.homescreen.R
import org.json.JSONObject

data class City(val name: String, val region: String, val country: String, val lat: Double, val lon: Double) {
    val subtitle: String get() = listOf(region, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class Reading(val tempC: Double, val code: Int)

/** Yang ditampilkan di Home untuk blok cuaca. */
sealed interface WeatherUi {
    data object NoCity : WeatherUi
    data object Loading : WeatherUi
    data object Unavailable : WeatherUi
    data class Ready(val tempC: Int, val label: String, val city: String) : WeatherUi
}

object WeatherCode {
    /** Kode cuaca WMO (dipakai Open-Meteo) ke teks, diterjemahkan lewat string resource. */
    @StringRes
    fun labelRes(code: Int): Int = when (code) {
        0 -> R.string.w_clear
        1 -> R.string.w_mostly_clear
        2 -> R.string.w_partly_cloudy
        3 -> R.string.w_overcast
        45, 48 -> R.string.w_fog
        51, 53, 55 -> R.string.w_drizzle
        56, 57 -> R.string.w_freezing_drizzle
        61 -> R.string.w_light_rain
        63 -> R.string.w_rain
        65 -> R.string.w_heavy_rain
        66, 67 -> R.string.w_freezing_rain
        71, 73, 75, 77 -> R.string.w_snow
        80 -> R.string.w_light_showers
        81 -> R.string.w_showers
        82 -> R.string.w_heavy_showers
        85, 86 -> R.string.w_snow_showers
        95 -> R.string.w_thunderstorm
        96, 99 -> R.string.w_thunder_hail
        else -> R.string.w_generic
    }
}

object WeatherParse {
    fun cities(json: String): List<City> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            City(
                name = o.optString("name"),
                region = o.optString("admin1"),
                country = o.optString("country"),
                lat = o.getDouble("latitude"),
                lon = o.getDouble("longitude"),
            )
        }
    }

    fun current(json: String): Reading? {
        val c = JSONObject(json).optJSONObject("current") ?: return null
        if (!c.has("temperature_2m") || !c.has("weather_code")) return null
        return Reading(c.getDouble("temperature_2m"), c.getInt("weather_code"))
    }
}
