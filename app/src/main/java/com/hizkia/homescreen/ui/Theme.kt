package com.hizkia.homescreen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val Bg = Color(0xFF0E1311)
    val Surface = Color(0xFF19211D)
    val Border = Color(0xFF26312C)
    val Text = Color(0xFFEEF3EF)
    val Muted = Color(0xFF9AA8A0)
    val Amber = Color(0xFFF2B84B)
    val Mint = Color(0xFF7BE0A6)
}

private val Scheme = darkColorScheme(
    background = Palette.Bg,
    surface = Palette.Surface,
    onBackground = Palette.Text,
    onSurface = Palette.Text,
    primary = Palette.Amber,
    onPrimary = Color(0xFF1A1405),
)

@Composable
fun HomeTheme(content: @Composable () -> Unit) {
    // Selalu gelap: launcher ini punya satu tampilan.
    MaterialTheme(colorScheme = Scheme, content = content)
}
