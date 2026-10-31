package com.hizkia.homescreen.apps

import androidx.annotation.StringRes
import com.hizkia.homescreen.R
import java.util.Locale

/**
 * @param lastUsedMs terakhir dibuka; kalau belum pernah dibuka dipakai waktu pemasangan.
 * @param everOpened false = tidak ada catatan pemakaian sama sekali (hanya terpasang).
 */
data class AppUsage(
    val pkg: String,
    val lastUsedMs: Long,
    val everOpened: Boolean,
    val sizeBytes: Long,
)

enum class RareSort(@StringRes val labelRes: Int) {
    OLDEST(R.string.sort_oldest),
    LARGEST(R.string.sort_largest),
}

object RareApps {
    const val MIN_DAYS = 30
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MB = 1024L * 1024
    private const val GB = 1024L * MB

    fun daysSince(lastUsedMs: Long, nowMs: Long): Int = ((nowMs - lastUsedMs) / DAY_MS).toInt().coerceAtLeast(0)

    /** App yang tidak dibuka minimal [minDays] hari, diurutkan sesuai [sort]. */
    fun rank(all: List<AppUsage>, nowMs: Long, sort: RareSort, minDays: Int = MIN_DAYS): List<AppUsage> {
        val rare = all.filter { daysSince(it.lastUsedMs, nowMs) >= minDays }
        return when (sort) {
            RareSort.OLDEST -> rare.sortedBy { it.lastUsedMs }
            RareSort.LARGEST -> rare.sortedByDescending { it.sizeBytes }
        }
    }

    fun totalBytes(list: List<AppUsage>): Long = list.sumOf { it.sizeBytes }

    fun formatSize(bytes: Long, locale: Locale): String {
        return when {
            bytes >= GB -> String.format(locale, "%.1f GB", bytes.toDouble() / GB)
            bytes >= MB -> String.format(locale, "%d MB", bytes / MB)
            else -> "< 1 MB"
        }
    }
}
