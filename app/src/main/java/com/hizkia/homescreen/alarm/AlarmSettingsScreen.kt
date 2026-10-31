package com.hizkia.homescreen.alarm

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import androidx.compose.runtime.remember
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.settings.PermissionRow
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

private val LeadOptions = listOf(0, 5, 10, 15, 30, 60)
private val SnoozeOptions = listOf(3, 5, 10)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmSettingsScreen(
    settings: AlarmSettings,
    soundName: String?,
    permissions: List<PermissionRow>,
    notificationsMissing: Boolean,
    onChange: (AlarmSettings) -> Unit,
    onPickSound: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = appLocale()
    val dayLabels = remember(locale) { (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, locale) } }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.alarm_reminders_title), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        if (notificationsMissing) {
            Surface(
                color = Color(0xFF2A2210),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF4A3B14)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.notif_not_allowed), color = Color(0xFFF2D58F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.notif_not_allowed_body),
                        color = Palette.Muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        Section(stringResource(R.string.morning_routines)) {
            SwitchRow(stringResource(R.string.enable_morning), settings.wakeEnabled) { onChange(settings.copy(wakeEnabled = it)) }
            Divider()
            ValueRow(stringResource(R.string.alarm_time), "%02d:%02d".format(settings.wakeHour, settings.wakeMinute)) {
                TimePickerDialog(context, { _, h, m -> onChange(settings.copy(wakeHour = h, wakeMinute = m)) }, settings.wakeHour, settings.wakeMinute, true).show()
            }
            Divider()
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.days), color = Palette.Text, fontSize = 16.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayLabels.forEachIndexed { i, label ->
                        val iso = i + 1
                        val on = iso in settings.days
                        Surface(
                            onClick = { onChange(settings.copy(days = if (on) settings.days - iso else settings.days + iso)) },
                            shape = CircleShape,
                            color = if (on) Palette.Mint else Color(0xFF22302A),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(label, color = if (on) Palette.Bg else Palette.Muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Divider()
            SwitchRow(stringResource(R.string.prep_sound_switch), settings.prepareEnabled) { onChange(settings.copy(prepareEnabled = it)) }
            Divider()
            ValueRow(stringResource(R.string.prep_time), "%02d:%02d".format(settings.prepareHour, settings.prepareMinute)) {
                TimePickerDialog(context, { _, h, m -> onChange(settings.copy(prepareHour = h, prepareMinute = m)) }, settings.prepareHour, settings.prepareMinute, true).show()
            }
            Divider()
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.snooze), color = Palette.Text, fontSize = 16.sp)
                Chips(SnoozeOptions, settings.snoozeMin, { stringResource(R.string.min_short, it) }) { onChange(settings.copy(snoozeMin = it)) }
            }
        }

        Section(stringResource(R.string.alarm_sound_section)) {
            ValueRow(stringResource(R.string.tone), soundName ?: stringResource(R.string.built_in_loud), onPickSound)
            if (settings.soundUri != null) {
                Divider()
                ValueRow(stringResource(R.string.use_built_in_again), "") { onChange(settings.copy(soundUri = null)) }
            }
            Divider()
            ValueRow(stringResource(R.string.test_ring), "", onTest)
        }
        Text(
            stringResource(R.string.tone_note),
            color = Palette.Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Section(stringResource(R.string.event_reminders)) {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.default_style), color = Palette.Text, fontSize = 16.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderMode.entries.forEach { m ->
                        Chip(stringResource(m.labelRes), m == settings.eventMode) { onChange(settings.copy(eventMode = m)) }
                    }
                }
            }
            Divider()
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.minutes_before), color = Palette.Text, fontSize = 16.sp)
                Chips(LeadOptions, settings.eventLeadMin, { if (it == 0) stringResource(R.string.at_start) else stringResource(R.string.min_short, it) }) { onChange(settings.copy(eventLeadMin = it)) }
            }
        }
        Text(
            stringResource(R.string.event_reminder_note),
            color = Palette.Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        Section(stringResource(R.string.permissions)) {
            permissions.forEachIndexed { i, p ->
                Row(
                    Modifier.fillMaxWidth().height(54.dp).clickableNoRipple { if (!p.granted) p.onFix() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).background(if (p.granted) Palette.Mint else Palette.Amber, CircleShape))
                    Text(p.label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text(
                        if (p.granted) stringResource(R.string.on_label) else stringResource(R.string.enable_arrow),
                        color = if (p.granted) Palette.Muted else Palette.Amber,
                        fontSize = 14.sp,
                        fontWeight = if (p.granted) FontWeight.Normal else FontWeight.Bold,
                    )
                }
                if (i < permissions.lastIndex) Divider()
            }
        }
        Text(
            stringResource(R.string.dnd_note),
            color = Palette.Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Palette.Muted, fontSize = 13.sp)
        Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Mint, checkedThumbColor = Palette.Bg),
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).clickableNoRipple(onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text("$value ›", color = Palette.Muted, fontSize = 14.sp) else Text("›", color = Palette.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Border))
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF2A2210) else Palette.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
        modifier = Modifier.height(38.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (selected) Palette.Amber else Palette.Text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { Chip(label(it), it == selected) { onSelect(it) } }
    }
}
