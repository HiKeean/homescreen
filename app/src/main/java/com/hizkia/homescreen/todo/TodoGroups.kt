package com.hizkia.homescreen.todo

import com.hizkia.homescreen.data.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodoGroups(
    val today: List<Todo>,
    val week: List<Todo>,
    val later: List<Todo>,
    val done: List<Todo>,
)

private const val DONE_KEEP_DAYS = 7L

/**
 * Hari ini = jatuh tempo hari ini atau terlewat; Minggu ini = 1–7 hari ke depan;
 * Nanti = tanpa tenggat atau lebih jauh. Selesai hanya menampilkan 7 hari terakhir.
 */
fun groupTodos(all: List<Todo>, today: LocalDate, zone: ZoneId, nowMs: Long): TodoGroups {
    fun Todo.dueDate(): LocalDate? = dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    val open = all.filter { it.doneAt == null }
    val weekEnd = today.plusDays(7)
    val doneCutoff = nowMs - DONE_KEEP_DAYS * 24 * 60 * 60 * 1000

    return TodoGroups(
        today = open.filter { (it.dueDate() ?: LocalDate.MAX) <= today }.sortedBy { it.dueAt },
        week = open.filter { d -> d.dueDate()?.let { it > today && it <= weekEnd } == true }.sortedBy { it.dueAt },
        later = open.filter { d -> d.dueDate().let { it == null || it > weekEnd } }.sortedByDescending { it.createdAt },
        done = all.filter { (it.doneAt ?: 0L) >= doneCutoff }.sortedByDescending { it.doneAt },
    )
}

fun startOfDayMs(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()
