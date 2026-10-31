package com.hizkia.homescreen.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyEngineTest {
    private val banks = setOf("bank.a", "bank.b")
    private val monday = 1
    private val saturday = 6

    private fun engine(
        grants: Map<String, Long> = emptyMap(),
        sosmed: SosmedRules = SosmedRules.DEFAULT,
        usedMs: Long = 0L,
    ) = PolicyEngine(bankPackages = banks, sosmed = sosmed, sosmedUsedMs = { usedMs }, grantExpiryMs = { grants[it] ?: 0L })

    private val social = SosmedRules(packages = setOf("app.ig", "app.tt"), limitMin = 60, activeDays = setOf(1, 2, 3, 4, 5))

    @Test
    fun `app biasa selalu diizinkan`() {
        assertEquals(Decision.Allow, engine().decide("com.whatsapp", 1_000, monday))
    }

    @Test
    fun `app bank tanpa izin diblokir`() {
        assertEquals(Decision.Block(BlockReason.BANK_NEEDS_EXPENSE), engine().decide("bank.a", 1_000, monday))
    }

    @Test
    fun `app bank dengan izin yang masih berlaku diizinkan`() {
        assertEquals(Decision.Allow, engine(mapOf("bank.a" to 5_000)).decide("bank.a", 1_000, monday))
    }

    @Test
    fun `izin yang sudah habis diblokir lagi`() {
        assertEquals(
            Decision.Block(BlockReason.BANK_NEEDS_EXPENSE),
            engine(mapOf("bank.a" to 1_000)).decide("bank.a", 1_000, monday),
        )
    }

    @Test
    fun `izin satu bank tidak berlaku untuk bank lain`() {
        assertEquals(
            Decision.Block(BlockReason.BANK_NEEDS_EXPENSE),
            engine(mapOf("bank.a" to 9_000)).decide("bank.b", 1_000, monday),
        )
    }

    @Test
    fun `sosmed di bawah batas diizinkan`() {
        assertEquals(Decision.Allow, engine(sosmed = social, usedMs = 59 * 60_000L).decide("app.ig", 1_000, monday))
    }

    @Test
    fun `sosmed tepat di batas diblokir`() {
        assertEquals(
            Decision.Block(BlockReason.SOSMED_LIMIT),
            engine(sosmed = social, usedMs = 60 * 60_000L).decide("app.tt", 1_000, monday),
        )
    }

    @Test
    fun `sosmed di hari tidak aktif tidak dibatasi`() {
        assertEquals(Decision.Allow, engine(sosmed = social, usedMs = 999 * 60_000L).decide("app.ig", 1_000, saturday))
    }

    @Test
    fun `app non sosmed tidak kena batas sosmed`() {
        assertEquals(Decision.Allow, engine(sosmed = social, usedMs = 999 * 60_000L).decide("com.maps", 1_000, monday))
    }
}
