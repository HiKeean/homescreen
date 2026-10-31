package com.hizkia.homescreen.alerts

import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONObject

data class Hour(val epochSec: Long, val precipProb: Int, val precipMm: Double, val code: Int)

data class Quake(
    val id: String,
    val timeMs: Long,
    val magnitude: Double,
    val depth: String,
    val region: String,
    val potential: String,
    val lat: Double,
    val lon: Double,
)

/** Semua mati secara default: pengecekan berkala memakai jaringan dan baterai, jadi harus dipilih user. */
data class AlertSettings(
    val rain: Boolean = false,
    val storm: Boolean = false,
    val quake: Boolean = false,
    val minMagnitude: Double = 5.0,
    /** null = tidak dibatasi jarak. */
    val radiusKm: Int? = 500,
) {
    val enabledCount: Int get() = listOf(rain, storm, quake).count { it }
}

enum class AlertKind { RAIN, STORM, QUAKE }

/**
 * Data peringatan tanpa teks, supaya teksnya bisa dibuat sesuai bahasa HP saat ditampilkan ([AlertText]).
 * [hour] untuk hujan/badai, [quake] dan [distanceKm] untuk gempa.
 */
data class Alert(
    val kind: AlertKind,
    val hour: Hour? = null,
    val nowMs: Long = 0L,
    val quake: Quake? = null,
    val distanceKm: Int? = null,
)

object AlertEvaluator {
    const val LOOKAHEAD_HOURS = 3
    const val RAIN_MM = 0.5
    const val RAIN_PROB = 60
    /** Gempa sebesar ini diberitahukan walau di luar radius. */
    const val BIG_QUAKE = 6.5
    /** Gempa yang lebih tua dari ini diabaikan, supaya tidak ada pemberitahuan basi. */
    const val MAX_QUAKE_AGE_MS = 3L * 60 * 60 * 1000

    private val RAIN_CODES = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
    private val STORM_CODES = setOf(95, 96, 99)

    /** Badai petir didahulukan atas hujan. null = tidak ada yang perlu diperingatkan. */
    @Suppress("UNUSED_PARAMETER")
    fun weather(hours: List<Hour>, nowMs: Long, s: AlertSettings, zone: ZoneId): Alert? {
        val horizon = nowMs + LOOKAHEAD_HOURS * 3_600_000L
        val upcoming = hours.filter { h ->
            val start = h.epochSec * 1000
            start + 3_600_000L > nowMs && start < horizon
        }
        if (s.storm) {
            upcoming.firstOrNull { it.code in STORM_CODES }?.let { return Alert(AlertKind.STORM, hour = it, nowMs = nowMs) }
        }
        if (s.rain) {
            upcoming.firstOrNull { it.precipMm >= RAIN_MM || (it.precipProb >= RAIN_PROB && it.code in RAIN_CODES) }
                ?.let { return Alert(AlertKind.RAIN, hour = it, nowMs = nowMs) }
        }
        return null
    }

    /**
     * @param cityLat/cityLon posisi kota pilihan user, atau null kalau belum diatur (jarak tidak difilter).
     * @param lastSeenId id gempa yang sudah pernah diproses.
     */
    fun quake(q: Quake, s: AlertSettings, cityLat: Double?, cityLon: Double?, nowMs: Long, lastSeenId: String?): Alert? {
        if (!s.quake || q.id == lastSeenId) return null
        if (nowMs - q.timeMs > MAX_QUAKE_AGE_MS) return null
        if (q.magnitude < s.minMagnitude) return null
        val km = if (cityLat != null && cityLon != null) distanceKm(cityLat, cityLon, q.lat, q.lon) else null
        if (s.radiusKm != null && km != null && km > s.radiusKm && q.magnitude < BIG_QUAKE) return null
        return Alert(AlertKind.QUAKE, nowMs = nowMs, quake = q, distanceKm = km?.toInt())
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

object AlertParse {
    /** Respons Open-Meteo dengan `timeformat=unixtime`. */
    fun hours(json: String): List<Hour> {
        val h = JSONObject(json).optJSONObject("hourly") ?: return emptyList()
        val time = h.optJSONArray("time") ?: return emptyList()
        val prob = h.optJSONArray("precipitation_probability")
        val mm = h.optJSONArray("precipitation")
        val code = h.optJSONArray("weather_code")
        return (0 until time.length()).map { i ->
            Hour(time.getLong(i), prob?.optInt(i, 0) ?: 0, mm?.optDouble(i, 0.0) ?: 0.0, code?.optInt(i, 0) ?: 0)
        }
    }

    /** Respons BMKG `autogempa.json`. */
    fun quake(json: String): Quake? {
        val g = JSONObject(json).optJSONObject("Infogempa")?.optJSONObject("gempa") ?: return null
        val dateTime = g.optString("DateTime")
        val coords = g.optString("Coordinates").split(',')
        val mag = g.optString("Magnitude").toDoubleOrNull() ?: return null
        val lat = coords.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
        val lon = coords.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
        val timeMs = runCatching { OffsetDateTime.parse(dateTime).toInstant().toEpochMilli() }.getOrNull() ?: return null
        return Quake(
            id = dateTime + g.optString("Coordinates"),
            timeMs = timeMs,
            magnitude = mag,
            depth = g.optString("Kedalaman"),
            region = g.optString("Wilayah"),
            potential = g.optString("Potensi"),
            lat = lat,
            lon = lon,
        )
    }
}
