package com.hizkia.homescreen.agenda

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.data.Todo
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Ruang kosong di bawah supaya isi terakhir tidak tertutup titik halaman.
private val BOTTOM_CLEARANCE = 72.dp


/** Halaman Home ke-2: ringkasan agenda hari ini dan to-do yang jatuh tempo. */
@Composable
fun HomeAgendaPage(
    today: LocalDate,
    events: List<CalEvent>,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    todos: List<Todo>,
    doneToday: List<Todo>,
    onToggleTodo: (Todo) -> Unit,
    onOpenEvent: (CalEvent) -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenTodo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Yang belum selesai selalu di atas; yang selesai hari ini dicoret di bawahnya.
    val rows = todos + doneToday
    val dayFormat = rememberDateFormatter("EEEEdMMMM")

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = BOTTOM_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.padding(top = 32.dp)) {
            Text(stringResource(R.string.tab_agenda), color = Palette.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(today.format(dayFormat).replaceFirstChar { it.uppercase() }, color = Palette.Muted, fontSize = 15.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(stringResource(R.string.today), stringResource(R.string.open_agenda), onOpenAgenda)
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
                    when {
                        !hasPermission -> Text(
                            stringResource(R.string.allow_calendar_arrow),
                            color = Palette.Amber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clickableNoRipple(onRequestPermission).padding(vertical = 14.dp),
                        )
                        events.isEmpty() -> Text(stringResource(R.string.no_events_today), color = Palette.Muted, fontSize = 15.sp, modifier = Modifier.padding(vertical = 14.dp))
                        else -> events.forEach { e ->
                            Row(
                                Modifier.fillMaxWidth().clickableNoRipple { onOpenEvent(e) }.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    if (e.allDay) stringResource(R.string.all_day) else formatTime(e.begin),
                                    color = Palette.Mint,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(64.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, color = Palette.Text, fontSize = 16.sp)
                                    if (e.location.isNotBlank()) Text(e.location, color = Palette.Muted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(stringResource(R.string.tab_todo), stringResource(R.string.open_todo), onOpenTodo)
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (rows.isEmpty()) {
                        Text(stringResource(R.string.no_todos_today), color = Palette.Muted, fontSize = 15.sp, modifier = Modifier.padding(vertical = 14.dp))
                    }
                    rows.forEach { t ->
                        val done = t.doneAt != null
                        Row(
                            Modifier.fillMaxWidth().height(48.dp).clickableNoRipple { onToggleTodo(t) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = done,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Palette.Mint, checkmarkColor = Color(0xFF0E1311)),
                            )
                            Text(
                                t.title,
                                color = if (done) Palette.Muted else Palette.Text,
                                fontSize = 16.sp,
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, color = Palette.Muted, fontSize = 13.sp)
        Text(action, color = Palette.Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickableNoRipple(onAction))
    }
}
