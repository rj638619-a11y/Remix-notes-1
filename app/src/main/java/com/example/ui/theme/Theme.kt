package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentLavender,
    tertiary = AccentMint,
    background = BgDark,
    surface = CardDark,
    onPrimary = TxDark,
    onSecondary = CardDark,
    onBackground = TxDark,
    onSurface = TxDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentLavender,
    tertiary = ChipOnTxLight,
    background = BgLight,
    surface = CardLight,
    onPrimary = CardLight,
    onSecondary = TxLight,
    onBackground = TxLight,
    onSurface = TxLight
)

@Composable
fun GlassNotesTheme(
    themeSetting: String = "auto", // "auto", "light", "dark", "matcha", "lavender", "sepia", "ocean"
    reduceTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSysDark = isSystemInDarkTheme()
    val baseColors = when (themeSetting.lowercase()) {
        "dark" -> DarkGlassColors
        "light" -> LightGlassColors
        "matcha" -> MatchaGlassColors
        "lavender" -> LavenderGlassColors
        "sepia" -> SepiaGlassColors
        "ocean" -> OceanGlassColors
        else -> if (isSysDark) DarkGlassColors else LightGlassColors
    }

    val isDark = baseColors.isDark

    val targetColors = baseColors.let { base ->
        base.copy(
            isReduced = reduceTransparency,
            glass = if (reduceTransparency) base.card else base.glass
        )
    }

    val colorScheme = if (isDark) {
        DarkColorScheme.copy(
            primary = targetColors.accent,
            background = targetColors.bg,
            surface = targetColors.card,
            onBackground = targetColors.text,
            onSurface = targetColors.text
        )
    } else {
        LightColorScheme.copy(
            primary = targetColors.accent,
            background = targetColors.bg,
            surface = targetColors.card,
            onBackground = targetColors.text,
            onSurface = targetColors.text
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalGlassColors provides targetColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object GlassTheme {
    val colors: GlassCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current
}
