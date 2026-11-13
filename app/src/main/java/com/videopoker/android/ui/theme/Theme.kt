package com.videopoker.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrassGold,
    secondary = BrassGoldDark,
    background = BrassBackground,
    surface = BrassPanel,
    onPrimary = BrassBackground,
    onSecondary = BrassText,
    onBackground = BrassText,
    onSurface = BrassText
)

@Composable
fun VideoPokerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
