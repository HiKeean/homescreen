package com.hizkia.homescreen.finance

import com.hizkia.homescreen.data.Expense
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class CategoryTotal(val category: String, val amount: Long, val share: Float)

/** Total pengeluaran per kategori pada [month], terbesar dulu. [share] = bagian dari total bulan itu (0..1). */
fun categoryTotals(expenses: List<Expense>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): List<CategoryTotal> {
    val inMonth = expenses.filter { YearMonth.from(Instant.ofEpochMilli(it.createdAt).atZone(zone)) == month }
    val total = inMonth.sumOf { it.amount }
    if (total == 0L) return emptyList()
    return inMonth.groupBy { it.category }
        .map { (category, list) ->
            val sum = list.sumOf { it.amount }
            CategoryTotal(category, sum, sum.toFloat() / total)
        }
        .sortedByDescending { it.amount }
}
