package com.hizkia.homescreen.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hizkia.homescreen.weather.City
import com.hizkia.homescreen.weather.WeatherRepository
import com.hizkia.homescreen.weather.WeatherStore
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mengecek perkiraan hujan/badai dan gempa terbaru, lalu memberi tahu kalau perlu. Jalan tiap ~30 menit saat ada internet. */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val s = AlertPrefs.settings(ctx)
        val now = System.currentTimeMillis()
        val city = WeatherStore.city(ctx)

        if ((s.rain || s.storm) && city != null) {
            AlertRepository.hours(city)?.let { hours ->
                AlertEvaluator.weather(hours, now, s, ZoneId.systemDefault())?.let { alert ->
                    if (now - AlertPrefs.lastAlertAt(ctx, alert.kind) >= WEATHER_COOLDOWN_MS) {
                        AlertNotifier.post(ctx, alert)
                        AlertPrefs.setLastAlertAt(ctx, alert.kind, now)
                    }
                }
            }
        }

        if (s.quake) {
            AlertRepository.latestQuake()?.let { quake ->
                AlertEvaluator.quake(quake, s, city?.lat, city?.lon, now, AlertPrefs.lastQuakeId(ctx))?.let {
                    AlertNotifier.post(ctx, it)
                }
                // Tandai sudah diproses walau tidak memicu, supaya tidak dievaluasi terus.
                AlertPrefs.setLastQuakeId(ctx, quake.id)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "weather_quake_alerts"
        private const val WEATHER_COOLDOWN_MS = 4L * 60 * 60 * 1000
        private const val PERIOD_MIN = 30L

        /** Jadwalkan atau batalkan sesuai pengaturan. Aman dipanggil berulang. */
        fun sync(context: Context) {
            val wm = WorkManager.getInstance(context)
            if (AlertPrefs.settings(context).enabledCount == 0) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AlertWorker>(PERIOD_MIN, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

object AlertRepository {
    private const val QUAKE_URL = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"

    suspend fun hours(city: City): List<Hour>? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&hourly=precipitation_probability,precipitation,weather_code&forecast_hours=6&timeformat=unixtime&timezone=auto"
        WeatherRepository.get(url)?.let { runCatching { AlertParse.hours(it) }.getOrNull() }
    }

    suspend fun latestQuake(): Quake? = withContext(Dispatchers.IO) {
        WeatherRepository.get(QUAKE_URL)?.let { runCatching { AlertParse.quake(it) }.getOrNull() }
    }
}
