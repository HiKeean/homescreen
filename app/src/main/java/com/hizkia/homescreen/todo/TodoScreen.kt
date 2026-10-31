package com.hizkia.homescreen.todo

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.agenda.TabSwitch
import com.hizkia.homescreen.data.Todo
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Due(@StringRes val labelRes: Int) { Today(R.string.today), Tomorrow(R.string.tomorrow), Later(R.string.later) }


@Composable
fun dueLabel(todo: Todo, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String? {
    val dueFormat = rememberDateFormatter("EEEdMMM")
    val due = todo.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: return null
    return when {
        due < today -> {
            val n = (today.toEpochDay() - due.toEpochDay()).toInt()
            pluralStringResource(R.plurals.n_overdue_days, n, n)
        }
        due == today -> stringResource(R.string.today)
        due == today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> due.format(dueFormat)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    todos: List<Todo>,
    onAdd: (title: String, dueAt: Long?) -> Unit,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onOpenAgenda: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val groups = remember(todos, today) { groupTodos(todos, today, ZoneId.systemDefault(), System.currentTimeMillis()) }
    val openCount = groups.today.size + groups.week.size + groups.later.size
    var text by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(Due.Today) }

    fun submit() {
        val title = text.trim()
        if (title.isEmpty()) return
        val dueAt = when (due) {
            Due.Today -> startOfDayMs(today)
            Due.Tomorrow -> startOfDayMs(today.plusDays(1))
            Due.Later -> null
        }
        onAdd(title, dueAt)
        text = ""
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.back_home), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
            Text(stringResource(R.string.not_done_count, openCount), color = Palette.Muted, fontSize = 14.sp)
        }

        TabSwitch(agendaSelected = false, onAgenda = onOpenAgenda, onTodo = {})

        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.add_todo), color = Palette.Muted) },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                cursorColor = Palette.Mint,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Due.entries.forEach { d ->
                val selected = d == due
                Surface(
                    onClick = { due = d },
                    color = if (selected) Color(0xFF2A2210) else Palette.Surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
                    modifier = Modifier.height(36.dp),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(d.labelRes), color = if (selected) Palette.Amber else Palette.Text, fontSize = 13.sp)
                    }
                }
            }
            Text(
                stringResource(R.string.add),
                color = if (text.isBlank()) Palette.Muted else Palette.Mint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clickableNoRipple(::submit),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (todos.isEmpty()) Text(stringResource(R.string.no_todos), color = Palette.Muted, fontSize = 14.sp)
            Group(stringResource(R.string.today), Palette.Amber, groups.today, today, onToggle, onDelete)
            Group(stringResource(R.string.this_week), Palette.Muted, groups.week, today, onToggle, onDelete)
            Group(stringResource(R.string.later), Palette.Muted, groups.later, today, onToggle, onDelete)
            Group(stringResource(R.string.done), Palette.Muted, groups.done, today, onToggle, onDelete)
            if (todos.isNotEmpty()) Text(stringResource(R.string.long_press_delete), color = Palette.Muted, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Group(
    title: String,
    titleColor: Color,
    items: List<Todo>,
    today: LocalDate,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items.forEach { t ->
                    val done = t.doneAt != null
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .combinedClickable(onClick = { onToggle(t) }, onLongClick = { onDelete(t) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Palette.Mint, checkmarkColor = Palette.Bg),
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                t.title,
                                color = if (done) Palette.Muted else Palette.Text,
                                fontSize = 16.sp,
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                            )
                            if (!done) dueLabel(t, today)?.let { Text(it, color = Palette.Muted, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}
