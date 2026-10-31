package com.hizkia.homescreen.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleChangeTest {
    private val base = SosmedRules(packages = setOf("a", "b"), limitMin = 60, activeDays = setOf(1, 2, 3, 4, 5))
    private val start = RulesState(effective = base)
    private val now = 1_000_000L

    @Test
    fun `mengetatkan langsung berlaku`() {
        val tighter = base.copy(packages = base.packages + "c", limitMin = 30, activeDays = base.activeDays + 6)
        val next = RuleChange.propose(start, tighter, now)
        assertEquals(tighter, next.effective)
        assertNull(next.pending)
    }

    @Test
    fun `menaikkan batas baru berlaku setelah 24 jam`() {
        val looser = base.copy(limitMin = 120)
        val next = RuleChange.propose(start, looser, now)
        assertEquals(base, next.effective)
        assertEquals(looser, next.pending)
        assertEquals(now + RuleChange.DELAY_MS, next.pendingAtMs)
    }

    @Test
    fun `mencabut app dan hari ditunda tapi tambahan app tetap langsung`() {
        val mixed = SosmedRules(packages = setOf("a", "c"), limitMin = 60, activeDays = setOf(1, 2, 3))
        val next = RuleChange.propose(start, mixed, now)
        assertEquals(setOf("a", "b", "c"), next.effective.packages)
        assertEquals(setOf(1, 2, 3, 4, 5), next.effective.activeDays)
        assertEquals(mixed, next.pending)
    }

    @Test
    fun `perubahan tertunda berlaku setelah waktunya`() {
        val looser = base.copy(limitMin = 120)
        val pendingState = RuleChange.propose(start, looser, now)
        assertEquals(base, RuleChange.settle(pendingState, now + RuleChange.DELAY_MS - 1).effective)
        assertEquals(looser, RuleChange.settle(pendingState, now + RuleChange.DELAY_MS).effective)
        assertNull(RuleChange.settle(pendingState, now + RuleChange.DELAY_MS).pending)
    }

    @Test
    fun `mengembalikan ke aturan berlaku membatalkan perubahan tertunda`() {
        val pendingState = RuleChange.propose(start, base.copy(limitMin = 120), now)
        val back = RuleChange.propose(pendingState, base, now + 1_000)
        assertEquals(base, back.effective)
        assertNull(back.pending)
    }
}
