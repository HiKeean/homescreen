package com.hizkia.homescreen.data

import android.content.Context

/** Izin sementara membuka app bank setelah mencatat pengeluaran. package -> epoch millis habisnya. */
object GrantStore {
    const val GRANT_MINUTES = 3L
    private const val FILE = "grants"

    fun grant(context: Context, pkg: String, now: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(pkg, now + GRANT_MINUTES * 60_000).apply()
    }

    /** Perpanjang izin yang masih berlaku (user masih di app bank). Tidak menghidupkan izin yang sudah habis. */
    fun extend(context: Context, pkg: String, now: Long = System.currentTimeMillis()) {
        if (expiry(context, pkg) > now) grant(context, pkg, now)
    }

    fun expiry(context: Context, pkg: String): Long = prefs(context).getLong(pkg, 0L)

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
