package com.hizkia.homescreen.weather

import android.content.Context

/** Kota pilihan user dan cuaca terakhir (cache) supaya Home langsung menampilkan sesuatu saat dibuka. */
object WeatherStore {
    private const val FILE = "weather"
    const val STALE_AFTER_MS = 30L * 60 * 1000

    fun city(context: Context): City? {
        val p = prefs(context)
        val name = p.getString("name", null) ?: return null
        return City(name, p.getString("region", "").orEmpty(), p.getString("country", "").orEmpty(), p.getFloat("lat", 0f).toDouble(), p.getFloat("lon", 0f).toDouble())
    }

    fun isAuto(context: Context): Boolean = prefs(context).getBoolean("auto", false)

    /** @param auto true = lokasi mengikuti HP; false = kota dipilih manual. */
    fun saveCity(context: Context, city: City, auto: Boolean = false) {
        prefs(context).edit()
            .putBoolean("auto", auto)
            .putString("name", city.name).putString("region", city.region).putString("country", city.country)
            .putFloat("lat", city.lat.toFloat()).putFloat("lon", city.lon.toFloat())
            .remove("temp").remove("code").remove("at")
            .apply()
    }

    /** Memperbarui kota otomatis. Cache cuaca hanya dibuang kalau pindah cukup jauh. @return kota yang berlaku. */
    fun updateAutoCity(context: Context, fresh: City): City {
        val old = city(context)
        if (old != null && isAuto(context) && !LocationMath.moved(old.lat, old.lon, fresh.lat, fresh.lon)) return old
        saveCity(context, fresh, auto = true)
        return fresh
    }

    fun cached(context: Context): Pair<Reading, Long>? {
        val p = prefs(context)
        if (!p.contains("at")) return null
        return Reading(p.getFloat("temp", 0f).toDouble(), p.getInt("code", 0)) to p.getLong("at", 0L)
    }

    fun saveReading(context: Context, reading: Reading, atMs: Long = System.currentTimeMillis()) {
        prefs(context).edit().putFloat("temp", reading.tempC.toFloat()).putInt("code", reading.code).putLong("at", atMs).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
