package com.hizkia.homescreen.weather

import com.hizkia.homescreen.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTest {
    @Test
    fun `kode cuaca ke teks`() {
        assertEquals(R.string.w_clear, WeatherCode.labelRes(0))
        assertEquals(R.string.w_rain, WeatherCode.labelRes(63))
        assertEquals(R.string.w_thunderstorm, WeatherCode.labelRes(95))
        assertEquals(R.string.w_generic, WeatherCode.labelRes(1234))
    }

    @Test
    fun `parse hasil pencarian kota`() {
        val json = """{"results":[{"name":"Jakarta","latitude":-6.17,"longitude":106.82,"country":"Indonesia","admin1":"Jakarta"},
            {"name":"Jakarta","latitude":1.0,"longitude":2.0}]}"""
        val cities = WeatherParse.cities(json)
        assertEquals(2, cities.size)
        assertEquals("Jakarta, Indonesia", cities[0].subtitle)
        assertEquals(-6.17, cities[0].lat, 0.0001)
        assertEquals("", cities[1].subtitle)
    }

    @Test
    fun `pencarian tanpa hasil menghasilkan daftar kosong`() {
        assertEquals(emptyList<City>(), WeatherParse.cities("""{"generationtime_ms":0.5}"""))
    }

    @Test
    fun `parse cuaca saat ini`() {
        val r = WeatherParse.current("""{"current":{"time":"2026-11-01T10:00","temperature_2m":28.4,"weather_code":3}}""")
        assertEquals(28.4, r!!.tempC, 0.0001)
        assertEquals(3, r.code)
    }

    @Test
    fun `respons tanpa data cuaca menghasilkan null`() {
        assertNull(WeatherParse.current("""{"current":{"time":"x"}}"""))
        assertNull(WeatherParse.current("""{}"""))
    }
}
