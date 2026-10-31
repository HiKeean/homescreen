package com.hizkia.homescreen.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bahasa app = bahasa sistem HP bila salah satu dari Inggris, Indonesia, atau Jerman; selain itu Inggris.
 * Teks diambil Android dari res/values (Inggris), values-id, dan values-de; objek ini untuk format tanggal & angka.
 */
object AppLocale {
    private val SUPPORTED = setOf("en", "id", "de")

    fun of(context: Context): Locale {
        val system = context.resources.configuration.locales[0]
        // Android lama memakai kode "in" untuk Indonesia.
        val language = if (system.language == "in") "id" else system.language
        return if (language in SUPPORTED) system else Locale.ENGLISH
    }

    /** Pola tanggal yang lazim untuk bahasa itu, mis. skeleton "EEEEdMMMM" -> "EEEE, d MMMM" (id) atau "EEEE, d. MMMM" (de). */
    fun formatter(locale: Locale, skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
fun appLocale(): Locale {
    val config = LocalConfiguration.current
    val context = LocalContext.current
    return remember(config) { AppLocale.of(context) }
}

/** Hasil di-cache per bahasa dan pola. */
@Composable
fun rememberDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = appLocale()
    return remember(locale, skeleton) { AppLocale.formatter(locale, skeleton) }
}
