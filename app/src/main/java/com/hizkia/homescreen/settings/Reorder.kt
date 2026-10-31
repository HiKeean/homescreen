package com.hizkia.homescreen.settings

/** Menggeser elemen di [index] sebanyak [delta] posisi. Di luar batas = daftar tidak berubah. */
fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().also { it.add(target, it.removeAt(index)) }
}
