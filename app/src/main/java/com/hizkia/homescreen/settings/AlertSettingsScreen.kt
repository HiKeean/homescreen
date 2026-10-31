package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.alerts.AlertSettings
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

private val MagnitudeOptions = listOf(4.0, 5.0, 5.5, 6.0)
private val RadiusOptions = listOf<Int?>(100, 300, 500, null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertSettingsScreen(
    settings: AlertSettings,
    hasCity: Boolean,
    notificationsMissing: Boolean,
    onChange: (AlertSettings) -> Unit,
    onSendSample: () -> Unit,
    onOpenWeather: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.alerts_title), color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        if (notificationsMissing) Notice(stringResource(R.string.alerts_notif_missing))
        if (!hasCity && (settings.rain || settings.storm)) {
            Notice(stringResource(R.string.alerts_need_city), onClick = onOpenWeather)
        }

        Card {
            SwitchRow(stringResource(R.string.alert_rain_title), settings.rain) { onChange(settings.copy(rain = it)) }
            Divider()
            SwitchRow(stringResource(R.string.alert_storm_switch), settings.storm) { onChange(settings.copy(storm = it)) }
            Divider()
            SwitchRow(stringResource(R.string.alert_quake_switch), settings.quake) { onChange(settings.copy(quake = it)) }
        }
        Text(
            stringResource(R.string.alerts_forecast_note),
            color = Palette.Muted, fontSize = 12.sp, lineHeight = 17.sp,
        )

        if (settings.quake) {
            Card {
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.min_magnitude), color = Palette.Text, fontSize = 16.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MagnitudeOptions.forEach { m ->
                            Chip("M ≥ $m", m == settings.minMagnitude) { onChange(settings.copy(minMagnitude = m)) }
                        }
                    }
                }
                Divider()
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.distance_from_city), color = Palette.Text, fontSize = 16.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadiusOptions.forEach { r ->
                            Chip(r?.let { "$it km" } ?: stringResource(R.string.all), r == settings.radiusKm) { onChange(settings.copy(radiusKm = r)) }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.quake_note),
                color = Palette.Muted, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }

        Card {
            Row(Modifier.fillMaxWidth().height(54.dp).clickableNoRipple(onSendSample), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.send_sample_alert), color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("›", color = Palette.Muted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Mint, checkedThumbColor = Palette.Bg))
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

@Composable
private fun Notice(text: String, onClick: (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Text(text, color = Color(0xFFF2D58F), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color(0xFF2A2210), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF4A3B14)), content = content)
    } else {
        Surface(color = Color(0xFF2A2210), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF4A3B14)), content = content)
    }
}
