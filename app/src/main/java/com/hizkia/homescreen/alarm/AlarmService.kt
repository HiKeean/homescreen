package com.hizkia.homescreen.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ServiceCompat
import com.hizkia.homescreen.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Status bunyi yang bisa diamati layar alarm supaya ikut menutup saat alarm dimatikan dari notifikasi. */
object AlarmState {
    private val _ringing = MutableStateFlow(false)
    val ringing: StateFlow<Boolean> = _ringing
    fun set(value: Boolean) { _ringing.value = value }
}

/**
 * Foreground service yang membunyikan alarm: nada di stream ALARM + getar + notifikasi layar penuh,
 * dan mencoba membuka [AlarmActivity] langsung (diizinkan karena app punya izin overlay).
 */
class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var kind = KIND_WAKE
    private var title = ""
    private var text = ""

    private val autoStop = Runnable { stopRinging() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(EXTRA_CMD)) {
            CMD_STOP -> stopRinging()
            CMD_SNOOZE -> {
                AlarmScheduler.scheduleSnooze(this, kind, title, text)
                stopRinging()
            }
            else -> {
                kind = intent?.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: KIND_WAKE
                title = intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
                text = intent?.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty()
                startRinging()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        AlarmTone.stop()
        super.onDestroy()
    }

    private fun startRinging() {
        val settings = AlarmPrefs.get(this)
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(settings.snoozeMin), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "homescreen:alarm")
            .apply { acquire(MAX_RING_MS + 5_000) }

        // Alarm bangun: paksa suara nyala dan volume penuh. Pengingat acara: cukup pastikan terdengar.
        if (kind == KIND_EVENT) SoundPrep.raiseAlarmVolumeToAtLeast(this, 60) else SoundPrep.prepare(this)
        AlarmTone.start(this, settings.soundUri)
        getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 400), 0))
        AlarmState.set(true)

        runCatching { startActivity(alarmActivityIntent(this)) }
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, MAX_RING_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        AlarmTone.stop()
        getSystemService(Vibrator::class.java)?.cancel()
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        AlarmState.set(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun alarmActivityIntent(context: Context) =
        Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            .putExtra(AlarmScheduler.EXTRA_KIND, kind)
            .putExtra(AlarmScheduler.EXTRA_TITLE, title)
            .putExtra(AlarmScheduler.EXTRA_TEXT, text)
            .putExtra(EXTRA_SNOOZE_MIN, AlarmPrefs.get(context).snoozeMin)

    private fun buildNotification(snoozeMin: Int): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                setBypassDnd(true)
            },
        )
        val full = PendingIntent.getActivity(this, 0, alarmActivityIntent(this), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title.ifBlank { getString(R.string.alarm) })
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, getString(R.string.snooze_short, snoozeMin), commandIntent(this, CMD_SNOOZE))
            .addAction(0, getString(R.string.stop), commandIntent(this, CMD_STOP))
            .build()
    }

    companion object {
        const val KIND_WAKE = "WAKE"
        const val KIND_EVENT = "EVENT"
        const val KIND_TEST = "TEST"
        const val EXTRA_CMD = "cmd"
        const val CMD_STOP = "stop"
        const val CMD_SNOOZE = "snooze"
        const val EXTRA_SNOOZE_MIN = "snooze_min"
        private const val CHANNEL = "alarm_ring"
        private const val NOTIFICATION_ID = 2
        private const val MAX_RING_MS = 10L * 60 * 1000

        fun start(context: Context, kind: String, title: String, text: String) {
            context.startForegroundService(
                Intent(context, AlarmService::class.java)
                    .putExtra(AlarmScheduler.EXTRA_KIND, kind)
                    .putExtra(AlarmScheduler.EXTRA_TITLE, title)
                    .putExtra(AlarmScheduler.EXTRA_TEXT, text),
            )
        }

        fun command(context: Context, cmd: String) {
            context.startService(Intent(context, AlarmService::class.java).putExtra(EXTRA_CMD, cmd))
        }

        private fun commandIntent(context: Context, cmd: String): PendingIntent =
            PendingIntent.getService(
                context,
                cmd.hashCode(),
                Intent(context, AlarmService::class.java).putExtra(EXTRA_CMD, cmd),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
