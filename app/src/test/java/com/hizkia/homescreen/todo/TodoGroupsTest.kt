package com.hizkia.homescreen.todo

import com.hizkia.homescreen.data.Todo
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoGroupsTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 10, 31)
    private val now = startOfDayMs(today, zone) + 10 * 3_600_000L

    private fun todo(id: Long, due: LocalDate?, done: Long? = null) =
        Todo(id = id, title = "t$id", dueAt = due?.let { startOfDayMs(it, zone) }, doneAt = done, createdAt = id)

    private fun ids(list: List<Todo>) = list.map { it.id }

    @Test
    fun `hari ini mencakup tenggat hari ini dan yang terlewat`() {
        val g = groupTodos(listOf(todo(1, today), todo(2, today.minusDays(3))), today, zone, now)
        assertEquals(listOf(2L, 1L), ids(g.today))
    }

    @Test
    fun `minggu ini adalah 1 sampai 7 hari ke depan`() {
        val g = groupTodos(
            listOf(todo(1, today.plusDays(1)), todo(2, today.plusDays(7)), todo(3, today.plusDays(8))),
            today, zone, now,
        )
        assertEquals(listOf(1L, 2L), ids(g.week))
        assertEquals(listOf(3L), ids(g.later))
    }

    @Test
    fun `tanpa tenggat masuk nanti`() {
        val g = groupTodos(listOf(todo(1, null)), today, zone, now)
        assertEquals(listOf(1L), ids(g.later))
        assertEquals(emptyList<Long>(), ids(g.today))
    }

    @Test
    fun `selesai hanya 7 hari terakhir dan tidak muncul di grup lain`() {
        val recent = todo(1, today, done = now - 3_600_000L)
        val old = todo(2, today, done = now - 8 * 24 * 3_600_000L)
        val g = groupTodos(listOf(recent, old), today, zone, now)
        assertEquals(listOf(1L), ids(g.done))
        assertEquals(emptyList<Long>(), ids(g.today))
    }
}
