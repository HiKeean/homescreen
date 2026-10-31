package com.hizkia.homescreen.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.hizkia.homescreen.R
import com.hizkia.homescreen.launcher.HomeActivity
import kotlin.concurrent.thread

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        // Kerjaan kecil tapi menyentuh disk/content provider: jangan di main thread.
        thread {
            try {
                handle(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handle(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_WAKE -> {
                AlarmService.start(context, AlarmService.KIND_WAKE, context.getString(R.string.morning_routines), "")
                AlarmScheduler.scheduleWake(context)
                AlarmScheduler.schedulePrepare(context)
            }
            AlarmScheduler.ACTION_PREPARE -> {
                SoundPrep.prepare(context)
                AlarmScheduler.schedulePrepare(context)
            }
            AlarmScheduler.ACTION_EVENT -> {
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
                val text = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty()
                when (runCatching { ReminderMode.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_MODE).orEmpty()) }.getOrNull()) {
                    ReminderMode.FULLSCREEN -> AlarmService.start(context, AlarmService.KIND_EVENT, title, text)
                    ReminderMode.NOTIF -> EventNotifier.post(context, title, text)
                    else -> Unit
                }
            }
            AlarmScheduler.ACTION_SNOOZE_FIRE -> AlarmService.start(
                context,
                intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmService.KIND_WAKE,
                intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty(),
            )
            AlarmScheduler.ACTION_RESCAN,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> AlarmScheduler.rescanAll(context)
        }
    }
}

/** Menyalakan suara HP sebelum alarm bangun: keluar dari senyap/DND (jika diizinkan) dan volume alarm penuh. */
object SoundPrep {
    private const val RING_FLOOR_PERCENT = 70

    fun prepare(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val am = context.getSystemService(AudioManager::class.java)
        // Mematikan DND butuh izin "Akses Jangan Ganggu"; tanpa itu hanya bisa mengubah mode dering.
        if (nm.isNotificationPolicyAccessGranted) {
            runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) }
        }
        runCatching { am.ringerMode = AudioManager.RINGER_MODE_NORMAL }
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0) }
        runCatching {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            val floor = max * RING_FLOOR_PERCENT / 100
            if (am.getStreamVolume(AudioManager.STREAM_RING) < floor) am.setStreamVolume(AudioManager.STREAM_RING, floor, 0)
        }
    }

    fun raiseAlarmVolumeToAtLeast(context: Context, percent: Int) {
        val am = context.getSystemService(AudioManager::class.java)
        runCatching {
            val floor = am.getStreamMaxVolume(AudioManager.STREAM_ALARM) * percent / 100
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) < floor) am.setStreamVolume(AudioManager.STREAM_ALARM, floor, 0)
        }
    }
}

object EventNotifier {
    private const val CHANNEL = "event_reminder"

    fun post(context: Context, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.event_reminders), NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(title.hashCode(), n)
    }
}
