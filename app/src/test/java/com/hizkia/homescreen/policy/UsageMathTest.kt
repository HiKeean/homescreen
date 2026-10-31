package com.hizkia.homescreen.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMathTest {
    private fun r(pkg: String, ts: Long) = UsageEv(pkg, resumed = true, ts = ts)
    private fun p(pkg: String, ts: Long) = UsageEv(pkg, resumed = false, ts = ts)

    @Test
    fun `satu sesi dihitung dari resume sampai pause`() {
        val total = UsageMath.foregroundMs(listOf(r("a", 1_000), p("a", 4_000)), nowMs = 10_000)
        assertEquals(3_000L, total["a"])
    }

    @Test
    fun `beberapa sesi dijumlahkan`() {
        val events = listOf(r("a", 0), p("a", 1_000), r("a", 5_000), p("a", 7_000))
        assertEquals(3_000L, UsageMath.foregroundMs(events, nowMs = 10_000)["a"])
    }

    @Test
    fun `pindah activity di app yang sama tidak menghitung dobel`() {
        // A1 resume, A2 resume (tumpang tindih), A1 pause, A2 pause
        val events = listOf(r("a", 0), r("a", 1_000), p("a", 1_500), p("a", 4_000))
        assertEquals(4_000L, UsageMath.foregroundMs(events, nowMs = 10_000)["a"])
    }

    @Test
    fun `app yang masih di depan dihitung sampai sekarang`() {
        assertEquals(6_000L, UsageMath.foregroundMs(listOf(r("a", 4_000)), nowMs = 10_000)["a"])
    }

    @Test
    fun `pause tanpa resume diabaikan dan app terpisah`() {
        val events = listOf(p("a", 500), r("b", 1_000), p("b", 3_000))
        val total = UsageMath.foregroundMs(events, nowMs = 10_000)
        assertEquals(null, total["a"])
        assertEquals(2_000L, total["b"])
    }
}
