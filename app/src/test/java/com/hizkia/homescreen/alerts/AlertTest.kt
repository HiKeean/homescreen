package com.hizkia.homescreen.alerts

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlertTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val hourMs = 3_600_000L
    private val now = 1_800_000_000_000L // titik acuan bebas

    private fun hour(offsetHours: Int, prob: Int = 0, mm: Double = 0.0, code: Int = 1) =
        Hour(epochSec = (now + offsetHours * hourMs) / 1000, precipProb = prob, precipMm = mm, code = code)

    private val settings = AlertSettings(rain = true, storm = true, quake = true)

    @Test
    fun `semua peringatan mati secara default`() {
        assertEquals(0, AlertSettings().enabledCount)
    }

    @Test
    fun `tidak ada peringatan kalau cerah`() {
        assertNull(AlertEvaluator.weather(listOf(hour(0), hour(1), hour(2)), now, settings, zone))
    }

    @Test
    fun `hujan dalam 3 jam memicu peringatan`() {
        val alert = AlertEvaluator.weather(listOf(hour(0), hour(1, prob = 70, mm = 1.2, code = 63)), now, settings, zone)
        assertEquals(AlertKind.RAIN, alert!!.kind)
    }

    @Test
    fun `hujan di luar jendela 3 jam tidak memicu`() {
        assertNull(AlertEvaluator.weather(listOf(hour(0), hour(4, prob = 90, mm = 5.0, code = 65)), now, settings, zone))
    }

    @Test
    fun `badai petir didahulukan dari hujan`() {
        val alert = AlertEvaluator.weather(listOf(hour(0, mm = 2.0, code = 63), hour(1, code = 95)), now, settings, zone)
        assertEquals(AlertKind.STORM, alert!!.kind)
    }

    @Test
    fun `fitur yang dimatikan tidak memperingatkan`() {
        val rain = listOf(hour(1, prob = 80, mm = 3.0, code = 63))
        assertNull(AlertEvaluator.weather(rain, now, settings.copy(rain = false), zone))
        val storm = listOf(hour(1, code = 95))
        assertNull(AlertEvaluator.weather(storm, now, settings.copy(storm = false, rain = false), zone))
    }

    @Test
    fun `jam yang sudah lewat diabaikan`() {
        assertNull(AlertEvaluator.weather(listOf(hour(-3, mm = 5.0, code = 65), hour(1)), now, settings, zone))
    }

    private fun quake(mag: Double, lat: Double = -6.2, lon: Double = 106.8, ageMs: Long = 60_000) =
        Quake("id1", now - ageMs, mag, "10 km", "Laut Jawa", "Tidak berpotensi tsunami", lat, lon)

    @Test
    fun `gempa di bawah magnitudo minimum diabaikan`() {
        assertNull(AlertEvaluator.quake(quake(4.9), settings, -6.2, 106.8, now, null))
    }

    @Test
    fun `gempa dekat dan cukup besar memicu`() {
        assertNotNull(AlertEvaluator.quake(quake(5.2), settings, -6.2, 106.8, now, null))
    }

    @Test
    fun `gempa di luar radius diabaikan kecuali sangat besar`() {
        val far = quake(5.5, lat = -2.9, lon = 122.2) // ratusan km dari Jakarta, jauh di atas 500
        assertNull(AlertEvaluator.quake(far, settings, -6.2, 106.8, now, null))
        assertNotNull(AlertEvaluator.quake(far.copy(magnitude = 6.8), settings, -6.2, 106.8, now, null))
    }

    @Test
    fun `gempa yang sudah pernah diproses atau terlalu lama diabaikan`() {
        assertNull(AlertEvaluator.quake(quake(6.0), settings, -6.2, 106.8, now, lastSeenId = "id1"))
        assertNull(AlertEvaluator.quake(quake(6.0, ageMs = 4 * hourMs), settings, -6.2, 106.8, now, null))
    }

    @Test
    fun `tanpa kota jarak tidak difilter`() {
        assertNotNull(AlertEvaluator.quake(quake(5.5, lat = -2.9, lon = 122.2), settings, null, null, now, null))
    }

    @Test
    fun `jarak Jakarta ke Bandung sekitar 120 km`() {
        assertEquals(116.0, AlertEvaluator.distanceKm(-6.2, 106.8, -6.9, 107.6), 10.0)
    }

    @Test
    fun `parse respons Open-Meteo per jam`() {
        val json = """{"hourly":{"time":[1793455200,1793458800],"precipitation_probability":[69,61],"precipitation":[0.50,0.00],"weather_code":[53,1]}}"""
        val hours = AlertParse.hours(json)
        assertEquals(2, hours.size)
        assertEquals(69, hours[0].precipProb)
        assertEquals(53, hours[0].code)
    }

    @Test
    fun `parse respons BMKG`() {
        val json = """{"Infogempa":{"gempa":{"DateTime":"2026-10-31T08:23:13+00:00","Coordinates":"-2.87,122.21","Magnitude":"3.5","Kedalaman":"4 km","Wilayah":"Laut dekat Bahodopi","Potensi":"Dirasakan"}}}"""
        val q = AlertParse.quake(json)!!
        assertEquals(3.5, q.magnitude, 0.0001)
        assertEquals(-2.87, q.lat, 0.0001)
        assertEquals(122.21, q.lon, 0.0001)
        assertEquals(1793434993000L, q.timeMs)
    }

    @Test
    fun `respons BMKG rusak menghasilkan null`() {
        assertNull(AlertParse.quake("""{"Infogempa":{}}"""))
    }
}
