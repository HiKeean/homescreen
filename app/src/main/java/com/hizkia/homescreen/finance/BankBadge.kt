package com.hizkia.homescreen.finance

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ikon kecil app bank di samping catatan pengeluaran. Kosong kalau tunai / ikon tidak ditemukan. */
@Composable
fun BankBadge(icon: ImageBitmap?, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    if (icon == null) return
    Image(
        bitmap = icon,
        contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(6.dp)),
    )
}
