package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.data.PinnedPrefs
import com.hizkia.homescreen.launcher.AppInfo
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

/** Atur Home quick apps: pilih (maks [PinnedPrefs.MAX]) dan susun urutan kiri ke kanan. */
@Composable
fun QuickAppsScreen(
    apps: List<AppInfo>,
    selected: List<String>,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var order by remember { mutableStateOf(selected.filter { pkg -> apps.any { it.packageName == pkg } }) }
    var query by remember { mutableStateOf("") }
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val candidates = remember(apps, order, query) {
        apps.filter { it.packageName !in order && (query.isBlank() || it.label.contains(query.trim(), ignoreCase = true)) }
    }
    val full = order.size >= PinnedPrefs.MAX

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_quick_apps), color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.quick_apps_info, order.size, PinnedPrefs.MAX), color = Palette.Muted, fontSize = 14.sp)
        }

        if (order.isNotEmpty()) {
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    order.forEachIndexed { i, pkg ->
                        val app = byPkg[pkg] ?: return@forEachIndexed
                        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                            Text(app.label, color = Palette.Text, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 1)
                            RoundButton("▲", enabled = i > 0) { order = order.moved(i, -1) }
                            RoundButton("▼", enabled = i < order.lastIndex) { order = order.moved(i, 1) }
                            RoundButton("✕", enabled = true, tint = Color(0xFFF27C6B)) { order = order - pkg }
                        }
                        if (i < order.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Border))
                    }
                }
            }
        } else {
            Text(stringResource(R.string.quick_apps_empty), color = Palette.Muted, fontSize = 14.sp)
        }

        Text(if (full) stringResource(R.string.quick_apps_full) else stringResource(R.string.add_app), color = Palette.Muted, fontSize = 13.sp)
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_app), color = Palette.Muted) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                cursorColor = Palette.Mint,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )

        LazyColumn(Modifier.weight(1f)) {
            items(candidates, key = { it.packageName }) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable(enabled = !full) { order = order + app.packageName },
                ) {
                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)))
                    Text(
                        app.label,
                        color = if (full) Palette.Muted else Palette.Text,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f).padding(start = 14.dp),
                    )
                    if (!full) Text(stringResource(R.string.add_plus), color = Palette.Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { onSave(order) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
        ) {
            Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RoundButton(label: String, enabled: Boolean, tint: Color = Palette.Text, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color(0xFF22302A),
        modifier = Modifier.padding(start = 6.dp).size(36.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = if (enabled) tint else Palette.Border, fontSize = 13.sp)
        }
    }
}
