package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hizkia.homescreen.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

data class PermissionRow(val label: String, val granted: Boolean, val onFix: () -> Unit)

@Composable
fun SettingsScreen(
    limitSummary: String,
    sosmedCount: Int,
    bankCount: Int,
    permissions: List<PermissionRow>,
    onOpenRules: () -> Unit,
    onPickSosmed: () -> Unit,
    onPickBanks: () -> Unit,
    alarmSummary: String,
    alertsSummary: String,
    onOpenAlerts: () -> Unit,
    pinnedSummary: String,
    onPickPinned: () -> Unit,
    weatherSummary: String,
    onOpenWeather: () -> Unit,
    mapsSummary: String,
    onOpenMaps: () -> Unit,
    onOpenAlarm: () -> Unit,
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
        Text(stringResource(R.string.back), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.settings), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Section(stringResource(R.string.section_limits)) {
            NavRow(stringResource(R.string.limits_and_days), limitSummary, onOpenRules)
            NavRow(stringResource(R.string.limited_social_apps), pluralStringResource(R.plurals.n_apps, sosmedCount, sosmedCount), onPickSosmed)
            NavRow(stringResource(R.string.bank_apps_required), pluralStringResource(R.plurals.n_apps, bankCount, bankCount), onPickBanks, divider = false)
        }

        Section(stringResource(R.string.home_word)) {
            NavRow(stringResource(R.string.home_quick_apps), pinnedSummary, onPickPinned)
            NavRow(stringResource(R.string.weather), weatherSummary, onOpenWeather)
            NavRow(stringResource(R.string.maps_shortcuts), mapsSummary, onOpenMaps, divider = false)
        }

        Section(stringResource(R.string.section_alarm)) {
            NavRow(stringResource(R.string.alarm_reminders_title), alarmSummary, onOpenAlarm)
            NavRow(stringResource(R.string.alerts_title), alertsSummary, onOpenAlerts, divider = false)
        }

        Section(stringResource(R.string.section_permissions)) {
            permissions.forEachIndexed { i, p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickableNoRipple { if (!p.granted) p.onFix() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(if (p.granted) Palette.Mint else Palette.Amber),
                    )
                    Text(p.label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text(
                        text = if (p.granted) stringResource(R.string.on_label) else stringResource(R.string.enable_arrow),
                        color = if (p.granted) Palette.Muted else Palette.Amber,
                        fontSize = 14.sp,
                        fontWeight = if (p.granted) FontWeight.Normal else FontWeight.Bold,
                    )
                }
                if (i < permissions.lastIndex) Divider()
            }
        }
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
private fun NavRow(label: String, value: String, onClick: () -> Unit, divider: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).clickableNoRipple(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text("$value ›", color = Palette.Muted, fontSize = 14.sp)
    }
    if (divider) Divider()
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Border))
}
