package com.hizkia.homescreen.alarm

import com.hizkia.homescreen.agenda.CalEvent
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmModelsTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    private fun at(date: LocalDate, h: Int, m: Int) = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `alarm hari ini jika jamnya belum lewat`() {
        val d = LocalDate.of(2026, 11, 2) // Senin
        val now = at(d, 3, 0)
        assertEquals(at(d, 5, 0), AlarmMath.nextTrigger(now, 5, 0, (1..7).toSet(), zone))
    }

    @Test
    fun `alarm besok jika jamnya sudah lewat`() {
        val d = LocalDate.of(2026, 11, 2)
        val now = at(d, 6, 0)
        assertEquals(at(d.plusDays(1), 5, 0), AlarmMath.nextTrigger(now, 5, 0, (1..7).toSet(), zone))
    }

    @Test
    fun `melompati hari yang tidak dipilih`() {
        val friday = LocalDate.of(2026, 11, 6)
        val now = at(friday, 6, 0)
        // Hanya Senin–Jumat: setelah Jumat berikutnya adalah Senin.
        assertEquals(at(friday.plusDays(3), 5, 0), AlarmMath.nextTrigger(now, 5, 0, setOf(1, 2, 3, 4, 5), zone))
    }

    @Test
    fun `tanpa hari tidak ada alarm`() {
        assertNull(AlarmMath.nextTrigger(0, 5, 0, emptySet(), zone))
    }

    private fun event(id: Long, begin: Long, allDay: Boolean = false) =
        CalEvent(id, "e$id", begin, begin + 3_600_000, allDay, "", 0, "")

    @Test
    fun `pengingat dijadwalkan sebelum acara sesuai lead`() {
        val plans = ReminderPlan.plan(listOf(event(1, 10_000_000)), nowMs = 0, defaultMode = ReminderMode.NOTIF, leadMin = 10) { null }
        assertEquals(10_000_000 - 600_000L, plans.single().triggerMs)
    }

    @Test
    fun `acara seharian dan mode mati dilewati`() {
        val events = listOf(event(1, 10_000_000, allDay = true), event(2, 10_000_000))
        val plans = ReminderPlan.plan(events, 0, ReminderMode.NOTIF, 10) { if (it == 2L) ReminderMode.OFF else null }
        assertEquals(emptyList<PlannedReminder>(), plans)
    }

    @Test
    fun `pengingat yang sudah lewat tidak dijadwalkan lagi`() {
        val plans = ReminderPlan.plan(listOf(event(1, 1_000_000)), nowMs = 500_000, defaultMode = ReminderMode.NOTIF, leadMin = 10) { null }
        assertEquals(emptyList<PlannedReminder>(), plans)
    }

    @Test
    fun `pilihan per acara mengalahkan default`() {
        val plans = ReminderPlan.plan(listOf(event(1, 10_000_000)), 0, ReminderMode.NOTIF, 10) { ReminderMode.FULLSCREEN }
        assertEquals(ReminderMode.FULLSCREEN, plans.single().mode)
    }
}
