package com.hizkia.homescreen.enforcement

import java.time.LocalDate

/**
 * Saklar sementara untuk pengembangan, sengaja tidak ada tombolnya di dalam app.
 *
 * [SOSMED_PAUSED_ON]: limit sosmed tidak diberlakukan HANYA pada tanggal ini, lalu otomatis aktif lagi
 * keesokan harinya. null = limit selalu aktif (kondisi normal).
 */
object DevFlags {
    private val SOSMED_PAUSED_ON: LocalDate? = null

    fun sosmedLimitPausedToday(): Boolean = SOSMED_PAUSED_ON != null && LocalDate.now() == SOSMED_PAUSED_ON
}
