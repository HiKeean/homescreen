package com.hizkia.homescreen.apps

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.hizkia.homescreen.R
import com.hizkia.homescreen.ui.appLocale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

/** Satu baris di daftar: data pemakaian digabung dengan nama dan ikon app. */
data class RareRow(val usage: AppUsage, val label: String, val icon: ImageBitmap?)

@Composable
fun RareAppsScreen(
    rows: List<RareRow>,
    sort: RareSort,
    onSort: (RareSort) -> Unit,
    nowMs: Long,
    loading: Boolean,
    permissionMissing: Boolean,
    onFixPermission: () -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = appLocale()
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.back_all_apps), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
            Text(stringResource(R.string.rare_title), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.rare_subtitle, RareApps.MIN_DAYS), color = Palette.Muted, fontSize = 14.sp)
        }

        if (permissionMissing) {
            Surface(
                onClick = onFixPermission,
                color = Color(0xFF2A2210),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF4A3B14)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.rare_need_permission), color = Color(0xFFF2D58F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.rare_need_permission_body), color = Palette.Muted, fontSize = 13.sp)
                }
            }
        } else {
            Surface(color = Palette.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Palette.Border)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.can_free), color = Palette.Muted, fontSize = 13.sp)
                        Text(
                            if (loading) stringResource(R.string.calculating) else RareApps.formatSize(RareApps.totalBytes(rows.map { it.usage }), locale),
                            color = Palette.Mint,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(if (loading) "" else pluralStringResource(R.plurals.n_apps, rows.size, rows.size), color = Palette.Muted, fontSize = 13.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RareSort.entries.forEach { s ->
                    val selected = s == sort
                    Surface(
                        onClick = { onSort(s) },
                        color = if (selected) Color(0xFF2A2210) else Palette.Surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Palette.Amber else Palette.Border),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(s.labelRes), color = if (selected) Palette.Amber else Palette.Text, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (!permissionMissing && !loading && rows.isEmpty()) {
            Text(stringResource(R.string.no_rare_apps), color = Palette.Muted, fontSize = 15.sp)
        }

        LazyColumn(Modifier.weight(1f)) {
            items(rows, key = { it.usage.pkg }) { row ->
                val days = RareApps.daysSince(row.usage.lastUsedMs, nowMs)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(66.dp),
                ) {
                    row.icon?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(row.label, color = Palette.Text, fontSize = 16.sp)
                        val age = if (row.usage.everOpened) pluralStringResource(R.plurals.n_days_ago, days, days) else pluralStringResource(R.plurals.n_installed_never_opened, days, days)
                        Text("$age · ${RareApps.formatSize(row.usage.sizeBytes, locale)}", color = Palette.Muted, fontSize = 13.sp)
                    }
                    Surface(
                        onClick = { onDelete(row.usage.pkg) },
                        color = Color(0xFF2B1815),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF4A2A25)),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.delete), color = Color(0xFFF27C6B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Text(
            stringResource(R.string.rare_footer),
            color = Palette.Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}
