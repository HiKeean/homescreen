package com.hizkia.homescreen.finance

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Kategori bawaan. Nilai ini ID yang TERSIMPAN di database (sejak awal berbahasa Indonesia),
 * jadi tetap dipakai apa adanya supaya catatan lama tidak rusak; yang diterjemahkan hanya tampilannya.
 */
val Categories = listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Lainnya")

@StringRes
fun categoryLabelRes(stored: String): Int? = when (stored) {
    "Makan" -> R.string.cat_food
    "Transport" -> R.string.cat_transport
    "Belanja" -> R.string.cat_shopping
    "Tagihan" -> R.string.cat_bills
    "Hiburan" -> R.string.cat_fun
    "Lainnya" -> R.string.cat_other
    else -> null
}

@Composable
fun categoryLabel(stored: String): String = categoryLabelRes(stored)?.let { stringResource(it) } ?: stored

/** Mata uang tetap rupiah; hanya pemisah ribuan yang mengikuti bahasa (85.000 / 85,000). */
fun formatRupiah(amount: Long, locale: Locale): String = "Rp ${NumberFormat.getIntegerInstance(locale).format(amount)}"

fun groupingSeparator(locale: Locale): Char = DecimalFormatSymbols.getInstance(locale).groupingSeparator
