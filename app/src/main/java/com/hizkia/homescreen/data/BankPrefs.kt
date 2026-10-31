package com.hizkia.homescreen.data

import android.content.Context

/** Daftar package app yang dianggap bank. Disimpan di SharedPreferences (kecil, tanpa dependency tambahan). */
object BankPrefs {
    private const val FILE = "settings"
    private const val KEY = "bank_packages"

    fun get(context: Context): Set<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun set(context: Context, packages: Set<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putStringSet(KEY, packages).apply()
    }
}
