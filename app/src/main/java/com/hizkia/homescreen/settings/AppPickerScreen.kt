package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.hizkia.homescreen.launcher.AppInfo
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

@Composable
fun AppPickerScreen(
    title: String,
    hint: String,
    maxSelect: Int? = null,
    apps: List<AppInfo>,
    selected: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picked by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.back), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("${if (maxSelect != null) stringResource(R.string.selected_of_max, picked.size, maxSelect) else stringResource(R.string.selected_count, picked.size)}. $hint", color = Palette.Muted, fontSize = 14.sp)
        }

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
            items(shown, key = { it.packageName }) { app ->
                val on = app.packageName in picked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable {
                            picked = when {
                                on -> picked - app.packageName
                                maxSelect == null || picked.size < maxSelect -> picked + app.packageName
                                else -> picked
                            }
                        },
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Text(app.label, color = Palette.Text, fontSize = 17.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
                    Checkbox(
                        checked = on,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Palette.Mint, checkmarkColor = Color(0xFF0E1311)),
                    )
                }
            }
        }

        Button(
            onClick = { onSave(picked) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
        ) {
            Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
