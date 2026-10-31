package com.hizkia.homescreen.launcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Membuka pencarian di app Google. */
object GoogleSearch {
    private const val PACKAGE = "com.google.android.googlequicksearchbox"

    /** @return false kalau app Google tidak ditemukan. */
    fun open(context: Context): Boolean {
        // Langsung ke layar pencarian; kalau komponennya tidak ada/ditolak, buka app Google biasa.
        val direct = Intent().setComponent(ComponentName(PACKAGE, "$PACKAGE.SearchActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(direct)
            return true
        } catch (e: ActivityNotFoundException) {
            // lanjut ke cadangan
        } catch (e: SecurityException) {
            // lanjut ke cadangan
        }
        val launch = context.packageManager.getLaunchIntentForPackage(PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        context.startActivity(launch)
        return true
    }
}
