package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple
import com.hizkia.homescreen.weather.City
import com.hizkia.homescreen.weather.WeatherRepository
import kotlinx.coroutines.launch

@Composable
fun WeatherSettingsScreen(
    current: City?,
    auto: Boolean,
    onUseLocation: () -> Unit,
    onPick: (City) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    fun search() {
        val q = query.trim()
        if (q.length < 2) return
        status = context.getString(R.string.searching)
        scope.launch {
            val found = WeatherRepository.searchCities(q)
            results = found.orEmpty()
            status = when {
                found == null -> context.getString(R.string.no_connection)
                found.isEmpty() -> context.getString(R.string.city_not_found)
                else -> null
            }
        }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.weather), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.weather_settings_intro),
            color = Palette.Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        if (current != null) {
            val mode = if (auto) stringResource(R.string.auto_suffix) else ""
            Text(stringResource(R.string.now_city, current.name + (if (current.subtitle.isNotBlank()) ", ${current.subtitle}" else "")) + mode, color = Palette.Mint, fontSize = 15.sp)
        }

        Surface(
            onClick = onUseLocation,
            color = Palette.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(if (auto) 2.dp else 1.dp, if (auto) Palette.Mint else Palette.Border),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.use_current_location), color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.use_current_location_note),
                    color = Palette.Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
        Text(stringResource(R.string.or_choose_city), color = Palette.Muted, fontSize = 13.sp)

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.city_search_hint), color = Palette.Muted) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
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
        status?.let { Text(it, color = Palette.Muted, fontSize = 14.sp) }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { "${it.name}${it.lat}${it.lon}" }) { city ->
                Surface(
                    onClick = { onPick(city) },
                    color = Palette.Surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Palette.Border),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(city.name, color = Palette.Text, fontSize = 16.sp)
                        if (city.subtitle.isNotBlank()) Text(city.subtitle, color = Palette.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
