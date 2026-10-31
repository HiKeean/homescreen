package com.hizkia.homescreen.policy

/**
 * Aturan batas sosmed. [activeDays] memakai nomor hari ISO (1 = Senin ... 7 = Minggu).
 * Satu batas menit dipakai bersama oleh semua app di [packages].
 */
data class SosmedRules(
    val packages: Set<String>,
    val limitMin: Int,
    val activeDays: Set<Int>,
) {
    companion object {
        val DEFAULT = SosmedRules(packages = emptySet(), limitMin = 60, activeDays = setOf(1, 2, 3, 4, 5))
    }
}

/** [pending] adalah aturan tujuan yang baru berlaku di [pendingAtMs] (karena ada bagian yang melonggarkan). */
data class RulesState(
    val effective: SosmedRules,
    val pending: SosmedRules? = null,
    val pendingAtMs: Long = 0L,
) {
    /** Aturan yang dimaksud user saat ini, dipakai sebagai dasar saat mengedit lagi. */
    val target: SosmedRules get() = pending ?: effective
}

/**
 * Mengetatkan aturan langsung berlaku; melonggarkan baru berlaku setelah [DELAY_MS].
 * Tujuannya supaya tidak bisa tawar-menawar dengan diri sendiri saat lagi pengen scroll.
 */
object RuleChange {
    const val DELAY_MS = 24L * 60 * 60 * 1000

    fun settle(state: RulesState, nowMs: Long): RulesState =
        if (state.pending != null && nowMs >= state.pendingAtMs) RulesState(effective = state.pending) else state

    fun propose(state: RulesState, proposed: SosmedRules, nowMs: Long): RulesState {
        val current = settle(state, nowMs).effective
        val tightened = SosmedRules(
            packages = current.packages + proposed.packages,
            limitMin = minOf(current.limitMin, proposed.limitMin),
            activeDays = current.activeDays + proposed.activeDays,
        )
        return if (tightened == proposed) {
            RulesState(effective = proposed)
        } else {
            RulesState(effective = tightened, pending = proposed, pendingAtMs = nowMs + DELAY_MS)
        }
    }
}
