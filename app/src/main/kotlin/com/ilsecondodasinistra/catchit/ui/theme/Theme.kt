package com.ilsecondodasinistra.catchit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Lagoon blue with a green "live" accent; only used before Android 12, where Material You doesn't exist.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00629E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    tertiary = Color(0xFF1B6C3A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA6F4B5),
    onTertiaryContainer = Color(0xFF00210D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99CBFF),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFCFE5FF),
    tertiary = Color(0xFF8BD79B),
    onTertiary = Color(0xFF00391B),
    tertiaryContainer = Color(0xFF00522A),
    onTertiaryContainer = Color(0xFFA6F4B5),
)

/** Material 3 Expressive; colors follow the wallpaper on Android 12+, light/dark follow the system. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CatchitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
