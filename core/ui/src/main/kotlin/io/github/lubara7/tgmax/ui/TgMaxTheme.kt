package io.github.lubara7.tgmax.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TgMaxTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) darkColorScheme(
        primary = NightBlue, background = NightBackground, surface = NightBackground,
        surfaceVariant = NightSurface,
    ) else lightColorScheme(primary = SkyBlue, background = Color.White, surface = Color.White)
    MaterialTheme(colorScheme = colors, typography = TgMaxTypography, content = content)
}
