package com.hizkia.homescreen.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Mendeteksi geseran vertikal yang "sisa": jari terus bergeser padahal isi sudah tidak bisa di-scroll lagi
 * (area kosong, atau sudah mentok di ujung). Geseran yang masih dipakai untuk scroll tidak terhitung,
 * jadi tidak bentrok dengan scroll biasa.
 *
 * @param upward true = geser ke atas (mis. Home membuka Semua app); false = geser ke bawah (mis. Semua app kembali ke Home).
 * @param thresholdPx sisa geseran (px) yang dibutuhkan sebelum terpicu.
 * @param enabled hanya aktif di layar yang diinginkan.
 */
class LeftoverSwipe(
    private val thresholdPx: Float,
    private val upward: Boolean,
    private val enabled: () -> Boolean,
    private val onTrigger: () -> Unit,
) : NestedScrollConnection {
    private var leftover = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || !enabled()) return Offset.Zero
        val delta = if (upward) -available.y else available.y
        if (delta > 0f) {
            leftover += delta
            if (leftover >= thresholdPx) {
                leftover = 0f
                onTrigger()
            }
        } else {
            leftover = 0f
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        leftover = 0f
        return Velocity.Zero
    }
}
