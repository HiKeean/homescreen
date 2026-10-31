package com.hizkia.homescreen.alarm

import android.content.Context

object AlarmPrefs {
    private const val FILE = "alarm"

    fun get(context: Context): AlarmSettings {
        val p = prefs(context)
        val d = AlarmSettings()
        return AlarmSettings(
            wakeEnabled = p.getBoolean("wake_on", d.wakeEnabled),
            wakeHour = p.getInt("wake_h", d.wakeHour),
            wakeMinute = p.getInt("wake_m", d.wakeMinute),
            days = p.getString("days", null)?.mapNotNull { it.digitToIntOrNull() }?.toSet() ?: d.days,
            prepareEnabled = p.getBoolean("prep_on", d.prepareEnabled),
            prepareHour = p.getInt("prep_h", d.prepareHour),
            prepareMinute = p.getInt("prep_m", d.prepareMinute),
            soundUri = p.getString("sound", null),
            snoozeMin = p.getInt("snooze", d.snoozeMin),
            eventMode = runCatching { ReminderMode.valueOf(p.getString("ev_mode", d.eventMode.name)!!) }.getOrDefault(d.eventMode),
            eventLeadMin = p.getInt("ev_lead", d.eventLeadMin),
        )
    }

    fun save(context: Context, s: AlarmSettings) {
        prefs(context).edit()
            .putBoolean("wake_on", s.wakeEnabled)
            .putInt("wake_h", s.wakeHour)
            .putInt("wake_m", s.wakeMinute)
            .putString("days", s.days.sorted().joinToString(""))
            .putBoolean("prep_on", s.prepareEnabled)
            .putInt("prep_h", s.prepareHour)
            .putInt("prep_m", s.prepareMinute)
            .putString("sound", s.soundUri)
            .putInt("snooze", s.snoozeMin)
            .putString("ev_mode", s.eventMode.name)
            .putInt("ev_lead", s.eventLeadMin)
            .apply()
    }

    /** Pilihan pengingat khusus untuk satu acara (menimpa default). */
    fun eventOverride(context: Context, eventId: Long): ReminderMode? =
        prefs(context).getString("ov_$eventId", null)?.let { runCatching { ReminderMode.valueOf(it) }.getOrNull() }

    fun setEventOverride(context: Context, eventId: Long, mode: ReminderMode?) {
        prefs(context).edit().apply {
            if (mode == null) remove("ov_$eventId") else putString("ov_$eventId", mode.name)
        }.apply()
    }

    /** Kunci pengingat acara yang sedang terjadwal, supaya yang basi bisa dibatalkan saat jadwal dihitung ulang. */
    fun scheduledKeys(context: Context): Set<String> = prefs(context).getStringSet("sched", emptySet())?.toSet() ?: emptySet()

    fun setScheduledKeys(context: Context, keys: Set<String>) {
        prefs(context).edit().putStringSet("sched", keys).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
