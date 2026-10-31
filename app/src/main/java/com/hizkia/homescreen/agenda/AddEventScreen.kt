package com.hizkia.homescreen.agenda

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.rememberDateFormatter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.alarm.ReminderMode
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    initialDate: LocalDate,
    onSave: (calendarId: Long, title: String, beginMs: Long, endMs: Long, location: String, reminder: ReminderMode?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormat = rememberDateFormatter("EEEEdMMMMyyyy")
    val calendars by produceState(emptyList<CalInfo>()) { value = CalendarRepository.writableCalendars(context) }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var start by remember { mutableStateOf(defaultStart(initialDate)) }
    var end by remember { mutableStateOf(defaultStart(initialDate).plusHours(1)) }
    var calendarId by remember { mutableStateOf<Long?>(null) }
    // null = ikut default di Setelan > Alarm & pengingat.
    var reminder by remember { mutableStateOf<ReminderMode?>(null) }
    val chosen = calendarId ?: calendars.firstOrNull()?.id

    val zone = ZoneId.systemDefault()
    val beginMs = LocalDateTime.of(date, start).atZone(zone).toInstant().toEpochMilli()
    val endMs = LocalDateTime.of(date, end).atZone(zone).toInstant().toEpochMilli()
    val canSave = title.isNotBlank() && endMs > beginMs && chosen != null

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.cancel_back), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onCancel))
        Text(stringResource(R.string.add_event_title), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            field(title, { title = it }, stringResource(R.string.event_title_hint))

            PickRow(stringResource(R.string.date), date.format(dateFormat).replaceFirstChar { it.uppercase() }) {
                DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickRow(stringResource(R.string.start), start.format(timeFormat), Modifier.weight(1f)) {
                    TimePickerDialog(context, { _, h, m ->
                        val newStart = LocalTime.of(h, m)
                        // Jaga selesai tetap setelah mulai.
                        if (!end.isAfter(newStart)) end = newStart.plusHours(1).takeIf { it.isAfter(newStart) } ?: LocalTime.of(23, 59)
                        start = newStart
                    }, start.hour, start.minute, true).show()
                }
                PickRow(stringResource(R.string.end), end.format(timeFormat), Modifier.weight(1f)) {
                    TimePickerDialog(context, { _, h, m -> end = LocalTime.of(h, m) }, end.hour, end.minute, true).show()
                }
            }

            field(location, { location = it }, stringResource(R.string.location_optional))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.reminder), color = Palette.Muted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<ReminderMode?>(null) .plus(ReminderMode.entries).forEach { m ->
                        val selected = m == reminder
                        Surface(
                            onClick = { reminder = m },
                            color = if (selected) Color(0xFF2A2210) else Palette.Surface,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    m?.let { stringResource(it.labelRes) } ?: stringResource(R.string.follow_default),
                                    color = if (selected) Palette.Amber else Palette.Text,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            if (calendars.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.save_to_calendar), color = Palette.Muted, fontSize = 13.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        calendars.forEach { cal ->
                            val selected = cal.id == chosen
                            Surface(
                                onClick = { calendarId = cal.id },
                                color = if (selected) Color(0xFF2A2210) else Palette.Surface,
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
                                modifier = Modifier.height(44.dp),
                            ) {
                                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(cal.color or 0xFF000000.toInt())))
                                    Text(
                                        cal.name,
                                        color = if (selected) Palette.Amber else Palette.Text,
                                        fontSize = 15.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.no_writable_calendar), color = Palette.Muted, fontSize = 13.sp)
            }
        }

        Button(
            onClick = { onSave(chosen!!, title.trim(), beginMs, endMs, location.trim(), reminder) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Amber,
                contentColor = Color(0xFF1A1405),
                disabledContainerColor = Palette.Surface,
                disabledContentColor = Palette.Muted,
            ),
        ) {
            Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun defaultStart(date: LocalDate): LocalTime =
    if (date == LocalDate.now()) LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0).let { if (it.hour == 0) LocalTime.of(23, 0) else it }
    else LocalTime.of(9, 0)

@Composable
private fun field(value: String, onChange: (String) -> Unit, hint: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, color = Palette.Muted) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Palette.Surface,
            unfocusedContainerColor = Palette.Surface,
            focusedTextColor = Palette.Text,
            unfocusedTextColor = Palette.Text,
            cursorColor = Palette.Mint,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PickRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Palette.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Palette.Muted, fontSize = 12.sp)
            Text(value, color = Palette.Text, fontSize = 16.sp)
        }
    }
}
