package com.hizkia.homescreen.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Membuka Google Maps lewat deep link. Tanpa API key dan tanpa membaca lokasi. */
object MapsLauncher {
    private const val MAPS = "com.google.android.apps.maps"

    /** @return false kalau Google Maps tidak terpasang. */
    fun navigateTo(context: Context, address: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(address)}")).setPackage(MAPS))

    fun openSearch(context: Context): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).setPackage(MAPS))

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
