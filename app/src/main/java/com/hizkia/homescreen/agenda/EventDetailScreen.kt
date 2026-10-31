package com.hizkia.homescreen.agenda

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


private fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Teks waktu acara: satu hari, lintas hari, atau seharian. */
@Composable
fun whenText(e: CalEvent): String {
    val longDate = rememberDateFormatter("EEEEdMMMMyyyy")
    val shortDate = rememberDateFormatter("EEEdMMM")
    val allDayText = stringResource(R.string.all_day)
    val start = localDate(e.begin)
    if (e.allDay) {
        // Akhir acara seharian eksklusif (tengah malam hari berikutnya).
        val last = localDate(maxOf(e.end - 1, e.begin))
        return if (last == start) "${start.format(longDate)} · $allDayText"
        else "${start.format(shortDate)} - ${last.format(shortDate)} · $allDayText"
    }
    val end = localDate(e.end)
    return if (end == start) "${start.format(longDate)}\n${formatTime(e.begin)} - ${formatTime(e.end)}"
    else "${start.format(shortDate)} ${formatTime(e.begin)}\n${end.format(shortDate)} ${formatTime(e.end)}"
}

@Composable
fun EventDetailScreen(
    event: CalEvent,
    reminderLabel: String,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.back_agenda), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .width(5.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(event.color or 0xFF000000.toInt())),
                )
                Text(event.title, color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)
            }

            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    val rows = buildList {
                        add(stringResource(R.string.detail_when) to whenText(event))
                        if (event.location.isNotBlank()) add(stringResource(R.string.detail_location) to event.location)
                        add(stringResource(R.string.reminder) to reminderLabel)
                        if (event.recurring) add(stringResource(R.string.detail_repeats) to stringResource(R.string.detail_repeats_yes))
                    }
                    rows.forEachIndexed { i, (label, value) ->
                        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(label, color = Palette.Muted, fontSize = 12.sp)
                            Text(value, color = Palette.Text, fontSize = 16.sp, lineHeight = 22.sp)
                        }
                        if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Border))
                    }
                }
            }

            if (event.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.detail_notes), color = Palette.Muted, fontSize = 13.sp)
                    Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                        Text(event.description, color = Palette.Text, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().padding(18.dp))
                    }
                }
            }

            if (event.calendarName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(event.color or 0xFF000000.toInt())))
                    Text(event.calendarName, color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Surface(
            onClick = { confirm = true },
            color = Color(0xFF2B1815),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF4A2A25)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.delete_event), color = Color(0xFFF27C6B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Palette.Surface,
            titleContentColor = Palette.Text,
            textContentColor = Palette.Muted,
            title = { Text(stringResource(R.string.delete_event_q)) },
            text = {
                Text(stringResource(if (event.recurring) R.string.delete_event_msg_recurring else R.string.delete_event_msg, event.title))
            },
            confirmButton = {
                TextButton(onClick = { confirm = false; onDelete() }) { Text(stringResource(R.string.delete), color = Color(0xFFF27C6B)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel), color = Palette.Text) }
            },
        )
    }
}
