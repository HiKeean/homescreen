package com.hizkia.homescreen.alarm

import androidx.annotation.StringRes
import com.hizkia.homescreen.R
import com.hizkia.homescreen.agenda.CalEvent
import java.time.Instant
import java.time.ZoneId

enum class ReminderMode(@StringRes val labelRes: Int) {
    OFF(R.string.off),
    NOTIF(R.string.reminder_notification),
    FULLSCREEN(R.string.reminder_fullscreen),
}

data class AlarmSettings(
    val wakeEnabled: Boolean = false,
    val wakeHour: Int = 5,
    val wakeMinute: Int = 0,
    /** Hari ISO 1 = Senin ... 7 = Minggu, berlaku untuk alarm bangun dan siapkan suara. */
    val days: Set<Int> = (1..7).toSet(),
    val prepareEnabled: Boolean = true,
    val prepareHour: Int = 4,
    val prepareMinute: Int = 0,
    /** Uri file nada pilihan user; null = nada bawaan yang dibuat aplikasi. */
    val soundUri: String? = null,
    val snoozeMin: Int = 5,
    val eventMode: ReminderMode = ReminderMode.NOTIF,
    val eventLeadMin: Int = 10,
)

object AlarmMath {
    /** Waktu bunyi berikutnya (epoch millis) setelah [nowMs] pada hari-hari yang dipilih, atau null kalau tidak ada hari. */
    fun nextTrigger(nowMs: Long, hour: Int, minute: Int, isoDays: Set<Int>, zone: ZoneId): Long? {
        if (isoDays.isEmpty()) return null
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            if (date.dayOfWeek.value !in isoDays) continue
            val at = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
            if (at > nowMs) return at
        }
        return null
    }
}

data class PlannedReminder(
    val key: String,
    val triggerMs: Long,
    val eventId: Long,
    val title: String,
    val beginMs: Long,
    val location: String,
    val mode: ReminderMode,
)

object ReminderPlan {
    /**
     * Pengingat yang harus dijadwalkan. Acara seharian dilewati, mode Mati dilewati,
     * dan pengingat yang waktunya sudah lewat tidak dijadwalkan ulang.
     */
    fun plan(
        events: List<CalEvent>,
        nowMs: Long,
        defaultMode: ReminderMode,
        leadMin: Int,
        override: (eventId: Long) -> ReminderMode?,
    ): List<PlannedReminder> = events.mapNotNull { e ->
        if (e.allDay) return@mapNotNull null
        val mode = override(e.id) ?: defaultMode
        if (mode == ReminderMode.OFF) return@mapNotNull null
        val trigger = e.begin - leadMin * 60_000L
        if (trigger <= nowMs) return@mapNotNull null
        PlannedReminder("${e.id}/${e.begin}", trigger, e.id, e.title, e.begin, e.location, mode)
    }
}
