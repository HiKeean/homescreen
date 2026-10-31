package com.hizkia.homescreen.apps

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.UserHandle
import android.os.storage.StorageManager

/** Membaca terakhir-dibuka dan ukuran tiap app. Butuh izin Akses penggunaan; tanpa itu hasilnya kosong atau tanpa ukuran. */
object RareAppsRepository {
    private const val TWO_YEARS_MS = 2L * 365 * 24 * 60 * 60 * 1000

    /** Blocking dan cukup berat (menanyakan ukuran tiap app): panggil dari thread background. */
    fun load(context: Context, packages: Collection<String>): List<AppUsage> {
        val now = System.currentTimeMillis()
        val lastUsed = HashMap<String, Long>()
        context.getSystemService(UsageStatsManager::class.java)
            .queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, now - TWO_YEARS_MS, now)
            ?.forEach { s -> lastUsed.merge(s.packageName, s.lastTimeUsed, ::maxOf) }

        val storage = context.getSystemService(StorageStatsManager::class.java)
        val pm = context.packageManager
        return packages.mapNotNull { pkg ->
            val info = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
            val uid = info.applicationInfo?.uid ?: return@mapNotNull null
            val size = runCatching {
                val st = storage.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, UserHandle.getUserHandleForUid(uid))
                st.appBytes + st.dataBytes + st.cacheBytes
            }.getOrDefault(0L)
            val used = lastUsed[pkg] ?: 0L
            AppUsage(pkg, lastUsedMs = maxOf(used, info.firstInstallTime), everOpened = used > 0, sizeBytes = size)
        }
    }
}
