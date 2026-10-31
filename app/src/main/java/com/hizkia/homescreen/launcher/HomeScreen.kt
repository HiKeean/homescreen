package com.hizkia.homescreen.launcher

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import com.hizkia.homescreen.ui.rememberDateFormatter
import com.hizkia.homescreen.finance.categoryLabel
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hizkia.homescreen.weather.WeatherUi
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.hizkia.homescreen.settings.moved
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.data.Expense
import com.hizkia.homescreen.finance.BankBadge
import com.hizkia.homescreen.finance.formatRupiah
import com.hizkia.homescreen.finance.timeLabel
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private const val MAX_ROWS = 4

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Baris Home quick apps. Tap untuk membuka; tekan lama lalu geser ke kiri/kanan untuk mengubah urutan.
 * Tiap app punya slot selebar sama, jadi posisi sasaran cukup dihitung dari posisi jari.
 */
@Composable
private fun QuickAppsRow(
    pinned: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    var order by remember(pinned) { mutableStateOf(pinned) }
    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var rowWidth by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val n = order.size
    val slotW = if (n == 0) 1f else rowWidth.toFloat() / n

    Row(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidth = it.width }
            .pointerInput(n, rowWidth) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val idx = (start.x / slotW).toInt().coerceIn(0, n - 1)
                        draggingPkg = order[idx].packageName
                        dragX = start.x
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                        val pkg = draggingPkg ?: return@detectDragGesturesAfterLongPress
                        val from = order.indexOfFirst { it.packageName == pkg }
                        val to = (dragX / slotW).toInt().coerceIn(0, n - 1)
                        if (from >= 0 && to != from) order = order.moved(from, to - from)
                    },
                    onDragEnd = {
                        draggingPkg = null
                        onReorder(order.map { it.packageName })
                    },
                    onDragCancel = {
                        draggingPkg = null
                        order = pinned
                    },
                )
            },
    ) {
        order.forEachIndexed { i, app ->
            key(app.packageName) {
                val dragging = app.packageName == draggingPkg
                Box(
                    Modifier
                        .weight(1f)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            if (dragging) {
                                translationX = dragX - (i + 0.5f) * slotW
                                scaleX = 1.15f
                                scaleY = 1.15f
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(68.dp).clickableNoRipple { onLaunch(app) },
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                        )
                        Text(app.label, color = Palette.Muted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherBlock(weather: WeatherUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickableNoRipple(onClick)) {
        when (weather) {
            is WeatherUi.Ready -> {
                Text("${weather.tempC}°", color = Palette.Text, fontSize = 80.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp)
                Text("${weather.label} · ${weather.city}", color = Palette.Muted, fontSize = 16.sp, maxLines = 1)
            }
            WeatherUi.NoCity -> {
                Text(stringResource(R.string.weather_set_city), color = Palette.Amber, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.weather_set_city_hint), color = Palette.Muted, fontSize = 16.sp)
            }
            WeatherUi.Loading -> {
                Text("--°", color = Palette.Border, fontSize = 80.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp)
                Text(stringResource(R.string.weather_loading), color = Palette.Muted, fontSize = 16.sp)
            }
            WeatherUi.Unavailable -> {
                Text("--°", color = Palette.Border, fontSize = 80.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp)
                Text(stringResource(R.string.weather_unavailable), color = Palette.Muted, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SearchButton(onClick: () -> Unit) {
    val searchDesc = stringResource(R.string.search_google_desc)
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Border),
        modifier = Modifier.size(48.dp).semantics { contentDescription = searchDesc },
    ) {
        androidx.compose.foundation.Canvas(Modifier.padding(13.dp)) {
            val stroke = 2.2.dp.toPx()
            val r = size.minDimension * 0.34f
            val c = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f)
            drawCircle(Palette.Text, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            val edge = androidx.compose.ui.geometry.Offset(c.x + r * 0.72f, c.y + r * 0.72f)
            drawLine(Palette.Text, edge, androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.92f), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

enum class MapsTarget { Home, Office, Search }

@Composable
private fun MapsCard(onMaps: (MapsTarget) -> Unit) {
    Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Maps", color = Palette.Muted, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MapsTarget.Home to stringResource(R.string.maps_home), MapsTarget.Office to stringResource(R.string.maps_office), MapsTarget.Search to stringResource(R.string.maps_search)).forEach { (target, label) ->
                    Surface(
                        onClick = { onMaps(target) },
                        color = Color(0xFF22302A),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Palette.Border),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(label, color = Palette.Text, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Ringkasan pemakaian sosmed hari ini untuk kartu di Home. [activeToday] false = hari bebas. */
data class SosmedSummary(
    val usedMin: Int,
    val limitMin: Int,
    val activeToday: Boolean,
    /** App sosmed yang sudah dibuka hari ini, terlama dipakai di atas. */
    val apps: List<SosmedAppUsage> = emptyList(),
)

data class SosmedAppUsage(val label: String, val icon: ImageBitmap, val usedMs: Long)

@Composable
private fun SosmedCard(s: SosmedSummary) {
    var expanded by remember { mutableStateOf(false) }
    val fraction = (s.usedMin.toFloat() / s.limitMin).coerceIn(0f, 1f)
    val barColor = when {
        !s.activeToday -> Palette.Muted
        s.usedMin >= s.limitMin -> Color(0xFFF27C6B)
        fraction >= 0.8f -> Palette.Amber
        else -> Palette.Mint
    }
    Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickableNoRipple { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.social_today), color = Palette.Muted, fontSize = 14.sp)
                Text(
                    text = (if (s.activeToday) stringResource(R.string.social_usage, s.usedMin, s.limitMin) else stringResource(R.string.social_free_day, s.usedMin)) +
                        if (expanded) "  ▴" else "  ▾",
                    color = Palette.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.Border)) {
                Box(Modifier.fillMaxWidth(if (s.activeToday) fraction else 0f).fillMaxHeight().background(barColor))
            }
            if (expanded) {
                if (s.apps.isEmpty()) {
                    Text(stringResource(R.string.social_none_today), color = Palette.Muted, fontSize = 14.sp)
                }
                s.apps.forEach { app ->
                    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)),
                        )
                        Text(app.label, color = Palette.Text, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        val min = app.usedMs / 60_000
                        Text(if (min < 1) stringResource(R.string.lt_one_min) else stringResource(R.string.min_short, min), color = Palette.Muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    todayExpenses: List<Expense>,
    bankIcons: Map<String, ImageBitmap>,
    sosmed: SosmedSummary?,
    gateWarning: String?,
    onFixGate: () -> Unit,
    onMaps: (MapsTarget) -> Unit,
    weather: WeatherUi,
    onWeatherClick: () -> Unit,
    onSearch: () -> Unit,
    pinned: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onReorderPinned: (List<String>) -> Unit,
    onOpenDrawer: () -> Unit,
    onQuickAdd: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = appLocale()
    val dateFormat = rememberDateFormatter("EEEEdMMMM")
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000)
        }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        // Semua di atas tombol bawah bisa di-scroll, supaya kartu yang membesar tidak mendorong tombol keluar layar.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            WeatherBlock(weather, onWeatherClick, Modifier.weight(1f))
            SearchButton(onSearch)
        }
        Text(
            text = now.format(dateFormat),
            color = Palette.Muted,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))

        if (sosmed != null) {
            SosmedCard(sosmed)
            Spacer(Modifier.height(12.dp))
        }

        var expanded by remember { mutableStateOf(false) }
        val total = todayExpenses.sumOf { it.amount }
        Surface(
            color = Palette.Surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Palette.Border),
        ) {
            Column(Modifier.animateContentSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded = !expanded }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.spent_today), color = Palette.Muted, fontSize = 14.sp)
                        Text(formatRupiah(total, locale), color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (expanded) "▴" else "▾", color = Palette.Mint, fontSize = 18.sp)
                }
                if (expanded) {
                    Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 14.dp)) {
                        if (todayExpenses.isEmpty()) {
                            Text(stringResource(R.string.no_notes_today), color = Palette.Muted, fontSize = 14.sp)
                        }
                        todayExpenses.take(MAX_ROWS).forEach { e ->
                            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BankBadge(e.bankPackage?.let(bankIcons::get), modifier = Modifier.padding(end = 8.dp), size = 18.dp)
                                        Text(categoryLabel(e.category), color = Palette.Text, fontSize = 15.sp)
                                    }
                                    val sub = listOf(timeLabel(e), e.note).filter { it.isNotBlank() }.joinToString(" · ")
                                    Text(sub, color = Palette.Muted, fontSize = 12.sp)
                                }
                                Text(formatRupiah(e.amount, locale), color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (todayExpenses.size > MAX_ROWS) stringResource(R.string.all_history_n, todayExpenses.size) else stringResource(R.string.all_history),
                            color = Palette.Mint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickableNoRipple(onOpenHistory),
                        )
                    }
                }
            }
        }

        if (gateWarning != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onFixGate,
                color = Color(0xFF2A2210),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF4A3B14)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.gate_inactive), color = Color(0xFFF2D58F), fontSize = 14.sp)
                        Text(stringResource(R.string.needs_permission, gateWarning), color = Palette.Muted, fontSize = 12.sp)
                    }
                    Text(stringResource(R.string.enable_arrow), color = Palette.Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MapsCard(onMaps)
        Spacer(Modifier.height(12.dp))
        }

        if (pinned.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            QuickAppsRow(pinned, onLaunchApp, onReorderPinned)
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onQuickAdd,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Color(0xFF1A1405)),
            ) {
                Text(stringResource(R.string.log_expense_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Palette.Border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Surface, contentColor = Palette.Text),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
                    val r = size.minDimension / 12f
                    for (i in 0..2) for (j in 0..2) {
                        drawCircle(Palette.Text, r, androidx.compose.ui.geometry.Offset(size.width * (0.17f + 0.33f * j), size.height * (0.17f + 0.33f * i)))
                    }
                }
            }
        }
    }
}
