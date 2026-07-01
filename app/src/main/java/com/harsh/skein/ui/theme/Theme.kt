package com.harsh.skein.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Skein's own theme tokens (things Material's colorScheme doesn't have a slot for).
data class SkeinColors(
    val bg: Color,
    val surface: Color,
    val header: Color,
    val accent: Color,
    val bubbleOut: Color,
    val onBubbleOut: Color,
    val text: Color,
    val sub: Color,
    val line: Color
)

private val PeachyLight = SkeinColors(
    bg = PeachBgLight,
    surface = PeachSurfaceLight,
    header = PeachHeaderLight,
    accent = PeachAccent,
    bubbleOut = PeachBubbleOutLight,
    onBubbleOut = PeachOnBubbleOutLight,
    text = PeachTextLight,
    sub = PeachSubLight,
    line = PeachLineLight
)

private val PeachyDark = SkeinColors(
    bg = PeachBgDark,
    surface = PeachSurfaceDark,
    header = PeachHeaderDark,
    accent = PeachAccent,
    bubbleOut = PeachBubbleOutDark,
    onBubbleOut = PeachOnBubbleOutDark,
    text = PeachTextDark,
    sub = PeachSubDark,
    line = PeachLineDark
)

// The channel that carries our tokens down the whole UI tree.
val LocalSkeinColors = staticCompositionLocalOf { PeachyLight }

@Composable
fun SkeinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val skein = if (darkTheme) PeachyDark else PeachyLight

    // Map our tokens onto Material's standard slots too, so built-in
    // components (Text default color, etc.) also follow the theme.
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = skein.accent,
            onPrimary = Color.White,
            background = skein.bg,
            onBackground = skein.text,
            surface = skein.surface,
            onSurface = skein.text,
            surfaceVariant = skein.header,
            onSurfaceVariant = skein.sub
        )
    } else {
        lightColorScheme(
            primary = skein.accent,
            onPrimary = Color.White,
            background = skein.bg,
            onBackground = skein.text,
            surface = skein.surface,
            onSurface = skein.text,
            surfaceVariant = skein.header,
            onSurfaceVariant = skein.sub
        )
    }

    CompositionLocalProvider(LocalSkeinColors provides skein) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
