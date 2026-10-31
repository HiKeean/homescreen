package com.hizkia.homescreen.agenda

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val SWIPE_PX = 60f

fun formatTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(timeFormat)

@Composable
fun CalEvent.timeLabel(): String = if (allDay) stringResource(R.string.all_day) else "${formatTime(begin)} - ${formatTime(end)}"

@Composable
fun AgendaScreen(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    calendarTick: Int,
    onAddEvent: () -> Unit,
    onOpenEvent: (CalEvent) -> Unit,
    onDeleteEvent: (CalEvent) -> Unit,
    onOpenTodo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val monthFormat = rememberDateFormatter("MMMMyyyy")
    val dayFormat = rememberDateFormatter("EEEEdMMMM")
    var expanded by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CalEvent?>(null) }
    val month = YearMonth.from(selectedDate)
    val gridStart = month.atDay(1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }

    // Satu query untuk seluruh grid bulan; strip minggu memakai data yang sama.
    val events by produceState(emptyList<CalEvent>(), month, calendarTick, hasPermission) {
        value = if (hasPermission) CalendarRepository.events(context, gridStart.startMs(), gridStart.plusDays(42).startMs()) else emptyList()
    }
    val dayEvents = remember(events, selectedDate) { events.filter { it.overlaps(selectedDate) } }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.back_home), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
            Text(month.format(monthFormat).replaceFirstChar { it.uppercase() }, color = Palette.Muted, fontSize = 14.sp)
        }

        TabSwitch(agendaSelected = true, onAgenda = {}, onTodo = onOpenTodo)

        if (!hasPermission) {
            PermissionCard(onRequestPermission)
        } else {
            CalendarCard(
                selectedDate = selectedDate,
                events = events,
                expanded = expanded,
                gridStart = gridStart,
                onSelectDate = onSelectDate,
                onExpandedChange = { expanded = it },
            )
        }

        Row(
            Modifier.fillMaxWidth().clickableNoRipple { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedDate.format(dayFormat).replaceFirstChar { it.uppercase() },
                    color = Palette.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(if (expanded) "  ▴" else "  ▾", color = Palette.Mint, fontSize = 16.sp)
            }
            if (hasPermission) Text(pluralStringResource(R.plurals.n_events, dayEvents.size, dayEvents.size), color = Palette.Muted, fontSize = 13.sp)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hasPermission && dayEvents.isEmpty()) {
                Text(stringResource(R.string.no_events), color = Palette.Muted, fontSize = 14.sp)
            }
            dayEvents.forEach { e -> EventCard(e, onClick = { onOpenEvent(e) }, onLongClick = { toDelete = e }) }
            if (dayEvents.isNotEmpty()) Text(stringResource(R.string.events_hint), color = Palette.Muted, fontSize = 12.sp)
        }

        toDelete?.let { e ->
            AlertDialog(
                onDismissRequest = { toDelete = null },
                containerColor = Palette.Surface,
                titleContentColor = Palette.Text,
                textContentColor = Palette.Muted,
                title = { Text(stringResource(R.string.delete_event_q)) },
                text = {
                    Text(
                        stringResource(if (e.recurring) R.string.delete_event_msg_recurring else R.string.delete_event_msg, e.title),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onDeleteEvent(e); toDelete = null }) { Text(stringResource(R.string.delete), color = Color(0xFFF27C6B)) }
                },
                dismissButton = {
                    TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel), color = Palette.Text) }
                },
            )
        }

        if (hasPermission) {
            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Color(0xFF1A1405)),
            ) {
                Text(stringResource(R.string.add_event_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TabSwitch(agendaSelected: Boolean, onAgenda: () -> Unit, onTodo: () -> Unit) {
    Surface(color = Palette.Surface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            TabItem(stringResource(R.string.tab_agenda), agendaSelected, onAgenda, Modifier.weight(1f))
            TabItem(stringResource(R.string.tab_todo), !agendaSelected, onTodo, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Palette.Text else Color.Transparent)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Palette.Bg else Color(0xFFC3CEC8),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Surface(
        onClick = onRequest,
        color = Color(0xFF2A2210),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF4A3B14)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.calendar_permission_title), color = Color(0xFFF2D58F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.calendar_permission_body), color = Palette.Muted, fontSize = 13.sp)
        }
    }
}

/**
 * Strip minggu yang bisa dibuka jadi kalender bulan. Seret ke bawah untuk membuka, ke atas untuk menutup;
 * geser ke samping untuk pindah minggu atau bulan.
 */
@Composable
private fun CalendarCard(
    selectedDate: LocalDate,
    events: List<CalEvent>,
    expanded: Boolean,
    gridStart: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
) {
    val locale = appLocale()
    val dayLabels = remember(locale) { (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, locale) } }
    val today = LocalDate.now()
    fun hasEvents(d: LocalDate) = events.any { it.overlaps(d) }

    Surface(color = Palette.Surface, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .pointerInput(expanded, selectedDate) {
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDragStart = { dx = 0f; dy = 0f },
                        onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                        onDragEnd = {
                            when {
                                abs(dy) > abs(dx) && dy > SWIPE_PX -> onExpandedChange(true)
                                abs(dy) > abs(dx) && dy < -SWIPE_PX -> onExpandedChange(false)
                                abs(dx) > SWIPE_PX -> {
                                    val dir = if (dx < 0) 1L else -1L
                                    onSelectDate(if (expanded) selectedDate.plusMonths(dir) else selectedDate.plusWeeks(dir))
                                }
                            }
                        },
                    )
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                dayLabels.forEach {
                    Text(it, color = Palette.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val rows = if (expanded) {
                (0 until 6).map { r -> (0 until 7).map { c -> gridStart.plusDays((r * 7 + c).toLong()) } }
            } else {
                val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                listOf((0 until 7).map { monday.plusDays(it.toLong()) })
            }
            rows.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        DayCell(
                            date = d,
                            selected = d == selectedDate,
                            isToday = d == today,
                            muted = expanded && YearMonth.from(d) != YearMonth.from(selectedDate),
                            dot = hasEvents(d),
                            onClick = { onSelectDate(d) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF3A4A43)))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    muted: Boolean,
    dot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.height(48.dp).clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) Palette.Text else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    selected -> Palette.Bg
                    muted -> Color(0xFF5C6B63)
                    isToday -> Palette.Mint
                    else -> Palette.Text
                },
                fontSize = 15.sp,
                fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
            )
        }
        Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(if (dot) Palette.Mint else Color.Transparent))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun EventCard(event: CalEvent, compact: Boolean = false, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
    Surface(
        color = Palette.Surface,
        shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
        border = BorderStroke(1.dp, Palette.Border),
        modifier = if (onClick != null || onLongClick != null) Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick) else Modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(if (compact) 34.dp else 42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(event.color or 0xFF000000.toInt())),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(event.title, color = Palette.Text, fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.Bold)
                val sub = listOf(event.timeLabel(), event.location).filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, color = Palette.Muted, fontSize = 13.sp)
            }
            if (!compact && event.calendarName.isNotBlank()) {
                Text(event.calendarName, color = Palette.Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
