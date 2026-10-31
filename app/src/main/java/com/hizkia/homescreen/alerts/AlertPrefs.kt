package com.hizkia.homescreen.alerts

import android.content.Context

object AlertPrefs {
    private const val FILE = "alerts"

    fun settings(context: Context): AlertSettings {
        val p = prefs(context)
        val d = AlertSettings()
        return AlertSettings(
            rain = p.getBoolean("rain", d.rain),
            storm = p.getBoolean("storm", d.storm),
            quake = p.getBoolean("quake", d.quake),
            minMagnitude = p.getFloat("min_mag", d.minMagnitude.toFloat()).toDouble(),
            radiusKm = p.getInt("radius", d.radiusKm ?: 0).takeIf { it > 0 },
        )
    }

    fun save(context: Context, s: AlertSettings) {
        prefs(context).edit()
            .putBoolean("rain", s.rain).putBoolean("storm", s.storm).putBoolean("quake", s.quake)
            .putFloat("min_mag", s.minMagnitude.toFloat())
            .putInt("radius", s.radiusKm ?: 0)
            .apply()
    }

    fun lastAlertAt(context: Context, kind: AlertKind): Long = prefs(context).getLong("last_${kind.name}", 0L)
    fun setLastAlertAt(context: Context, kind: AlertKind, atMs: Long) {
        prefs(context).edit().putLong("last_${kind.name}", atMs).apply()
    }

    fun lastQuakeId(context: Context): String? = prefs(context).getString("quake_id", null)
    fun setLastQuakeId(context: Context, id: String) {
        prefs(context).edit().putString("quake_id", id).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
