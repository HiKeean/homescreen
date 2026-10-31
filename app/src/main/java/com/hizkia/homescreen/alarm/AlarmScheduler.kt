package com.hizkia.homescreen.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hizkia.homescreen.R
import com.hizkia.homescreen.agenda.CalendarRepository
import com.hizkia.homescreen.launcher.HomeActivity
import java.time.ZoneId

/**
 * Menjadwalkan semua alarm app lewat AlarmManager:
 * - alarm bangun (setAlarmClock: tembus Doze, tampil sebagai alarm di status bar),
 * - "siapkan suara" (nyalakan suara HP sebelum alarm bangun),
 * - pengingat acara (notifikasi atau layar penuh),
 * - pemindaian ulang harian untuk acara baru.
 */
object AlarmScheduler {
    const val ACTION_WAKE = "com.hizkia.homescreen.alarm.WAKE"
    const val ACTION_PREPARE = "com.hizkia.homescreen.alarm.PREPARE"
    const val ACTION_EVENT = "com.hizkia.homescreen.alarm.EVENT"
    const val ACTION_SNOOZE_FIRE = "com.hizkia.homescreen.alarm.SNOOZE_FIRE"
    const val ACTION_RESCAN = "com.hizkia.homescreen.alarm.RESCAN"

    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_KIND = "kind"
    const val EXTRA_MODE = "mode"

    private const val RESCAN_HOUR = 3
    private const val RESCAN_MINUTE = 30
    private const val EVENT_HORIZON_MS = 36L * 60 * 60 * 1000

    /** Dipanggil saat app dibuka, boot, perubahan setelan, atau acara baru. Aman dipanggil berulang. */
    fun rescanAll(context: Context) {
        scheduleWake(context)
        schedulePrepare(context)
        scheduleDailyRescan(context)
        rescanEvents(context)
    }

    fun scheduleWake(context: Context) {
        val s = AlarmPrefs.get(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, ACTION_WAKE, "homescreen://wake")
        val at = if (s.wakeEnabled) AlarmMath.nextTrigger(System.currentTimeMillis(), s.wakeHour, s.wakeMinute, s.days, ZoneId.systemDefault()) else null
        if (at == null) {
            am.cancel(pi)
            return
        }
        val show = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
    }

    fun schedulePrepare(context: Context) {
        val s = AlarmPrefs.get(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, ACTION_PREPARE, "homescreen://prepare")
        val at = if (s.wakeEnabled && s.prepareEnabled) {
            AlarmMath.nextTrigger(System.currentTimeMillis(), s.prepareHour, s.prepareMinute, s.days, ZoneId.systemDefault())
        } else null
        if (at == null) {
            am.cancel(pi)
            return
        }
        setExact(am, at, pi)
    }

    private fun scheduleDailyRescan(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, ACTION_RESCAN, "homescreen://rescan")
        val at = AlarmMath.nextTrigger(System.currentTimeMillis(), RESCAN_HOUR, RESCAN_MINUTE, (1..7).toSet(), ZoneId.systemDefault()) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Tunda alarm yang sedang bunyi; akan bunyi lagi setelah [AlarmSettings.snoozeMin] menit. */
    fun scheduleSnooze(context: Context, kind: String, title: String, text: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val snoozeMin = AlarmPrefs.get(context).snoozeMin
        val pi = pending(context, ACTION_SNOOZE_FIRE, "homescreen://snooze") {
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        }
        val show = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + snoozeMin * 60_000L, show), pi)
    }

    /** Blocking: panggil dari thread background. */
    fun rescanEvents(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        AlarmPrefs.scheduledKeys(context).forEach { key ->
            existing(context, ACTION_EVENT, "homescreen://event/$key")?.let {
                am.cancel(it)
                it.cancel()
            }
        }
        if (!CalendarRepository.hasReadPermission(context)) {
            AlarmPrefs.setScheduledKeys(context, emptySet())
            return
        }

        val now = System.currentTimeMillis()
        val s = AlarmPrefs.get(context)
        val events = CalendarRepository.eventsBlocking(context, now, now + EVENT_HORIZON_MS)
        val planned = ReminderPlan.plan(events, now, s.eventMode, s.eventLeadMin) { AlarmPrefs.eventOverride(context, it) }

        planned.forEach { r ->
            val pi = pending(context, ACTION_EVENT, "homescreen://event/${r.key}") {
                putExtra(EXTRA_TITLE, r.title)
                putExtra(EXTRA_TEXT, eventText(context, r.beginMs, r.location))
                putExtra(EXTRA_MODE, r.mode.name)
            }
            setExact(am, r.triggerMs, pi)
        }
        AlarmPrefs.setScheduledKeys(context, planned.map { it.key }.toSet())
    }

    private fun eventText(context: Context, beginMs: Long, location: String): String {
        val time = com.hizkia.homescreen.agenda.formatTime(beginMs)
        return listOf(context.getString(R.string.event_starts_at, time), location).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun setExact(am: AlarmManager, at: Long, pi: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun intent(context: Context, action: String, data: String) =
        Intent(context, AlarmReceiver::class.java).setAction(action).setData(Uri.parse(data))

    private fun pending(context: Context, action: String, data: String, extras: (Intent.() -> Unit)? = null): PendingIntent {
        val i = intent(context, action, data)
        extras?.let(i::apply)
        return PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun existing(context: Context, action: String, data: String): PendingIntent? =
        PendingIntent.getBroadcast(context, 0, intent(context, action, data), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
}
