package com.hizkia.homescreen.data

import android.content.Context

/** App yang dipasang sebagai Home quick apps (maksimal [MAX]), urutannya = urutan kiri ke kanan di Home. */
object PinnedPrefs {
    const val MAX = 5
    private const val FILE = "pinned"
    private const val KEY_ORDER = "order"
    private const val KEY_OLD_SET = "packages"

    fun get(context: Context): List<String> {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        p.getString(KEY_ORDER, null)?.let { return it.split(',').filter(String::isNotBlank) }
        // Versi lama menyimpan himpunan tanpa urutan; pakai urutan abjad sebagai awal.
        return p.getStringSet(KEY_OLD_SET, emptySet())?.sorted().orEmpty()
    }

    fun set(context: Context, packages: List<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDER, packages.distinct().take(MAX).joinToString(","))
            .apply()
    }
}
