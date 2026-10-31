package com.hizkia.homescreen.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.hizkia.homescreen.policy.UsageEv
import com.hizkia.homescreen.policy.UsageMath
import java.time.LocalDate
import java.time.ZoneId

/** Membaca waktu pakai app hari ini dari UsageStats. Butuh izin Akses penggunaan; tanpa itu hasilnya kosong. */
object UsageReader {
    fun foregroundTodayMs(
        context: Context,
        packages: Set<String>,
        nowMs: Long = System.currentTimeMillis(),
    ): Map<String, Long> {
        if (packages.isEmpty()) return emptyMap()
        val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(dayStart, nowMs)
        val event = UsageEvents.Event()
        val list = ArrayList<UsageEv>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in packages) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> list += UsageEv(event.packageName, true, event.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED -> list += UsageEv(event.packageName, false, event.timeStamp)
            }
        }
        return UsageMath.foregroundMs(list, nowMs)
    }

    fun totalTodayMs(context: Context, packages: Set<String>, nowMs: Long = System.currentTimeMillis()): Long =
        foregroundTodayMs(context, packages, nowMs).values.sum()
}
