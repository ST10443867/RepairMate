package za.co.quickfix.repairmate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import za.co.quickfix.repairmate.data.settings.ThemeMode

// Palette: workshop teal (trust, tech) with a burnt-orange accent (soldering-iron warmth).
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E79),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEFF5),
    onPrimaryContainer = Color(0xFF00363C),
    secondary = Color(0xFFB85C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC0),
    onSecondaryContainer = Color(0xFF3B1D00),
    background = Color(0xFFF6FAFA),
    surface = Color(0xFFF6FAFA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72D6E2),
    onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF004F57),
    onPrimaryContainer = Color(0xFFBDEFF5),
    secondary = Color(0xFFFFB77A),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF0E1415),
    surface = Color(0xFF0E1415)
)

@Composable
fun RepairMateTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
