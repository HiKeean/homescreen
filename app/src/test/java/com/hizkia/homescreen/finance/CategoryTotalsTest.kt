package com.hizkia.homescreen.finance

import com.hizkia.homescreen.data.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTotalsTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    private fun e(category: String, amount: Long, date: LocalDate) =
        Expense(amount = amount, category = category, note = "", createdAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())

    @Test
    fun `dijumlah per kategori dan diurutkan terbesar dulu`() {
        val oct = LocalDate.of(2026, 10, 5)
        val list = listOf(e("Makan", 20_000, oct), e("Transport", 60_000, oct), e("Makan", 30_000, oct))
        val result = categoryTotals(list, YearMonth.of(2026, 10), zone)
        assertEquals(listOf("Transport", "Makan"), result.map { it.category })
        assertEquals(50_000L, result[1].amount)
        assertEquals(60_000f / 110_000f, result[0].share, 0.001f)
    }

    @Test
    fun `hanya bulan yang diminta`() {
        val list = listOf(e("Makan", 10_000, LocalDate.of(2026, 9, 30)), e("Makan", 20_000, LocalDate.of(2026, 10, 1)))
        assertEquals(20_000L, categoryTotals(list, YearMonth.of(2026, 10), zone).single().amount)
    }

    @Test
    fun `bulan kosong menghasilkan daftar kosong`() {
        assertEquals(emptyList<CategoryTotal>(), categoryTotals(emptyList(), YearMonth.of(2026, 10), zone))
    }
}
