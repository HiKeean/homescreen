package com.hizkia.homescreen.weather

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Open-Meteo: gratis, tanpa akun dan tanpa API key. Hanya nama kota/koordinat yang dikirim. */
object WeatherRepository {
    private const val TIMEOUT_MS = 8_000

    suspend fun searchCities(query: String): List<City>? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${Uri.encode(query.trim())}&count=6&language=id&format=json"
        get(url)?.let { runCatching { WeatherParse.cities(it) }.getOrNull() }
    }

    suspend fun current(city: City): Reading? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,weather_code&timezone=auto"
        get(url)?.let { runCatching { WeatherParse.current(it) }.getOrNull() }
    }

    /** @return isi respons, atau null kalau tidak ada jaringan / server error. */
    internal fun get(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
