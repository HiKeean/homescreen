package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import com.hizkia.homescreen.ui.rememberDateFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.policy.RulesState
import com.hizkia.homescreen.policy.SosmedRules
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val STEP_MIN = 15
private const val MIN_LIMIT = 15
private const val MAX_LIMIT = 480

@Composable
fun RulesScreen(
    state: RulesState,
    onSave: (SosmedRules) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = appLocale()
    val dayLabels = remember(locale) { (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, locale) } }
    val pendingFormat = rememberDateFormatter("EEEEdMMMHm")
    val target = state.target
    var limit by remember(target) { mutableStateOf(target.limitMin) }
    var days by remember(target) { mutableStateOf(target.activeDays) }
    val changed = limit != target.limitMin || days != target.activeDays

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.social_limit_title), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card {
                Text(stringResource(R.string.daily_limit_shared), color = Palette.Muted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−", enabled = limit > MIN_LIMIT) { limit = (limit - STEP_MIN).coerceAtLeast(MIN_LIMIT) }
                    Text(
                        text = stringResource(R.string.minutes_n, limit),
                        color = Palette.Mint,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    StepButton("+", enabled = limit < MAX_LIMIT) { limit = (limit + STEP_MIN).coerceAtMost(MAX_LIMIT) }
                }
            }

            Card {
                Text(stringResource(R.string.active_days), color = Palette.Muted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayLabels.forEachIndexed { i, label ->
                        val iso = i + 1
                        val on = iso in days
                        Surface(
                            onClick = { days = if (on) days - iso else days + iso },
                            shape = CircleShape,
                            color = if (on) Palette.Mint else Color(0xFF22302A),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    label,
                                    color = if (on) Palette.Bg else Palette.Muted,
                                    fontSize = 12.sp,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Text(stringResource(R.string.days_note), color = Palette.Muted, fontSize = 13.sp)
            }

            if (state.pending != null) {
                Notice(stringResource(R.string.pending_applies, Instant.ofEpochMilli(state.pendingAtMs).atZone(ZoneId.systemDefault()).format(pendingFormat)))
            }
            Notice(stringResource(R.string.rules_note))
        }

        Button(
            onClick = { onSave(target.copy(limitMin = limit, activeDays = days)) },
            enabled = changed && days.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Text,
                contentColor = Palette.Bg,
                disabledContainerColor = Palette.Surface,
                disabledContentColor = Palette.Muted,
            ),
        ) {
            Text(stringResource(R.string.save_changes), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color(0xFF22302A),
        modifier = Modifier.size(48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = if (enabled) Palette.Text else Palette.Muted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun Notice(text: String) {
    Surface(
        color = Color(0xFF2A2210),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF4A3B14)),
    ) {
        Text(text, color = Color(0xFFF2D58F), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}
