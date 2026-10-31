package com.hizkia.homescreen.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hizkia.homescreen.R
import com.hizkia.homescreen.launcher.HomeActivity
import com.hizkia.homescreen.ui.AppLocale
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Membuat teks peringatan sesuai bahasa HP. Teks wilayah gempa datang dari BMKG dan tetap berbahasa Indonesia. */
object AlertText {
    fun title(c: Context, a: Alert): String = when (a.kind) {
        AlertKind.STORM -> c.getString(R.string.alert_storm_title)
        AlertKind.RAIN -> c.getString(R.string.alert_rain_title)
        AlertKind.QUAKE -> c.getString(R.string.alert_quake_title, number(AppLocale.of(c), a.quake?.magnitude ?: 0.0))
    }

    fun body(c: Context, a: Alert, zone: ZoneId = ZoneId.systemDefault()): String {
        val locale = AppLocale.of(c)
        return when (a.kind) {
            AlertKind.STORM -> c.getString(R.string.alert_storm_body, whenText(c, locale, a, zone))
            AlertKind.RAIN -> {
                val h = a.hour
                c.getString(R.string.alert_rain_body, whenText(c, locale, a, zone), h?.precipProb ?: 0, number(locale, h?.precipMm ?: 0.0))
            }
            AlertKind.QUAKE -> {
                val q = a.quake
                val distance = a.distanceKm?.let { c.getString(R.string.alert_quake_distance, it) }.orEmpty()
                c.getString(R.string.alert_quake_body, q?.region.orEmpty(), q?.depth.orEmpty(), q?.potential.orEmpty(), distance)
            }
        }
    }

    private fun number(locale: Locale, v: Double): String = String.format(locale, "%.1f", v)

    private fun whenText(c: Context, locale: Locale, a: Alert, zone: ZoneId): String {
        val start = (a.hour?.epochSec ?: 0L) * 1000
        if (start <= a.nowMs) return c.getString(R.string.alert_when_now)
        val time = Instant.ofEpochMilli(start).atZone(zone).format(AppLocale.formatter(locale, "Hm"))
        return c.getString(R.string.alert_when_around, time)
    }
}

object AlertNotifier {
    private const val CHANNEL = "weather_quake"

    fun post(context: Context, alert: Alert) =
        post(context, alert.kind, AlertText.title(context, alert), AlertText.body(context, alert))

    fun postSample(context: Context) =
        post(context, AlertKind.RAIN, context.getString(R.string.alert_sample_title), context.getString(R.string.alert_sample_body))

    private fun post(context: Context, kind: AlertKind, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(CHANNEL.hashCode() + kind.ordinal, n)
    }
}
