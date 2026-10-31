package com.hizkia.homescreen.policy

enum class BlockReason { BANK_NEEDS_EXPENSE, SOSMED_LIMIT }

sealed interface Decision {
    data object Allow : Decision
    data class Block(val reason: BlockReason) : Decision
}

/**
 * Satu sumber aturan untuk launcher dan enforcer. Murni Kotlin supaya mudah diuji.
 *
 * @param grantExpiryMs epoch millis habisnya izin untuk sebuah package bank; 0 kalau tidak ada.
 * @param sosmedUsedMs total pemakaian sosmed hari ini; hanya dipanggil kalau memang perlu dihitung.
 */
class PolicyEngine(
    private val bankPackages: Set<String>,
    private val sosmed: SosmedRules = SosmedRules.DEFAULT,
    private val sosmedUsedMs: () -> Long = { 0L },
    private val grantExpiryMs: (String) -> Long,
) {
    /** @param isoDay hari ini: 1 = Senin ... 7 = Minggu. */
    fun decide(pkg: String, nowMs: Long, isoDay: Int): Decision = when {
        pkg in bankPackages ->
            if (grantExpiryMs(pkg) > nowMs) Decision.Allow else Decision.Block(BlockReason.BANK_NEEDS_EXPENSE)
        pkg in sosmed.packages && isoDay in sosmed.activeDays && sosmedUsedMs() >= sosmed.limitMin * 60_000L ->
            Decision.Block(BlockReason.SOSMED_LIMIT)
        else -> Decision.Allow
    }
}
