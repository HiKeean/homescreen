package com.hizkia.homescreen.agenda

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.hizkia.homescreen.R
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String,
    val color: Int,
    val calendarName: String,
    /** Acara berulang: menghapusnya menghapus seluruh rangkaian. */
    val recurring: Boolean = false,
    val description: String = "",
)

data class CalInfo(val id: Long, val name: String, val color: Int)

fun LocalDate.startMs(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/** Apakah acara ini menyentuh hari [date] (zona lokal). */
fun CalEvent.overlaps(date: LocalDate): Boolean {
    val dayStart = date.startMs()
    val dayEnd = date.plusDays(1).startMs()
    return begin < dayEnd && maxOf(end, begin + 1) > dayStart
}

/** Membaca dan menulis acara lewat kalender HP (CalendarContract), jadi ikut tersinkron dengan Google/Samsung. */
object CalendarRepository {
    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun events(context: Context, fromMs: Long, toMs: Long): List<CalEvent> =
        withContext(Dispatchers.IO) { eventsBlocking(context, fromMs, toMs) }

    /** Versi tanpa coroutine, untuk dipanggil dari BroadcastReceiver/thread sendiri. */
    fun eventsBlocking(context: Context, fromMs: Long, toMs: Long): List<CalEvent> {
        if (!hasReadPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DESCRIPTION,
        )
        val result = ArrayList<CalEvent>()
        runCatching {
            CalendarContract.Instances.query(context.contentResolver, projection, fromMs, toMs)?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(4) == 1
                    var begin = c.getLong(2)
                    var end = c.getLong(3)
                    if (allDay) {
                        // Acara seharian disimpan sebagai tengah malam UTC; geser ke tengah malam lokal.
                        begin = utcMidnightToLocal(begin)
                        end = utcMidnightToLocal(end)
                    }
                    result += CalEvent(
                        id = c.getLong(0),
                        title = c.getString(1)?.takeIf { it.isNotBlank() } ?: context.getString(R.string.untitled),
                        begin = begin,
                        end = end,
                        allDay = allDay,
                        location = c.getString(5).orEmpty(),
                        color = c.getInt(6),
                        calendarName = c.getString(7).orEmpty(),
                        recurring = !c.getString(8).isNullOrBlank(),
                        description = c.getString(9).orEmpty(),
                    )
                }
            }
        }
        return result.sortedWith(compareByDescending<CalEvent> { it.allDay }.thenBy { it.begin })
    }

    suspend fun writableCalendars(context: Context): List<CalInfo> = withContext(Dispatchers.IO) {
        if (!hasReadPermission(context)) return@withContext emptyList()
        val result = ArrayList<CalInfo>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.CALENDAR_COLOR,
                ),
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.VISIBLE} = 1",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null,
            )?.use { c ->
                while (c.moveToNext()) result += CalInfo(c.getLong(0), c.getString(1).orEmpty(), c.getInt(2))
            }
        }
        result
    }

    /** @return id acara baru, atau null kalau gagal ditulis ke kalender HP. */
    suspend fun addEvent(
        context: Context,
        calendarId: Long,
        title: String,
        beginMs: Long,
        endMs: Long,
        location: String,
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasWritePermission(context)) return@withContext null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, beginMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            if (location.isNotBlank()) put(CalendarContract.Events.EVENT_LOCATION, location)
        }
        runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }
            .getOrNull()
            ?.let { ContentUris.parseId(it) }
            ?.takeIf { it > 0 }
    }

    /** @return true kalau acara terhapus; false kalau kalendernya hanya-baca atau izin tulis tidak ada. */
    suspend fun deleteEvent(context: Context, eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasWritePermission(context)) return@withContext false
        runCatching {
            context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), null, null) > 0
        }.getOrDefault(false)
    }

    private fun utcMidnightToLocal(utcMs: Long): Long =
        Instant.ofEpochMilli(utcMs).atZone(ZoneOffset.UTC).toLocalDate().startMs()
}
