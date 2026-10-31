package com.hizkia.homescreen.data

import android.content.Context

/** Alamat tujuan untuk tombol pintasan Maps di Home. */
object MapsPrefs {
    private const val FILE = "maps"

    fun home(context: Context): String = prefs(context).getString("home", "").orEmpty()
    fun office(context: Context): String = prefs(context).getString("office", "").orEmpty()

    fun save(context: Context, home: String, office: String) {
        prefs(context).edit().putString("home", home.trim()).putString("office", office.trim()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
