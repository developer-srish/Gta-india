package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameDarkColorScheme =
  darkColorScheme(
    primary = SaffronGold,
    onPrimary = ObsidianBlack,
    primaryContainer = ElevatedCard,
    onPrimaryContainer = AmberGlow,
    secondary = IndiaEmerald,
    onSecondary = WarmIvory,
    tertiary = PoliceBlue,
    onTertiary = WarmIvory,
    background = ObsidianBlack,
    onBackground = WarmIvory,
    surface = AsphaltSurface,
    onSurface = WarmIvory,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = MutedSlate,
    error = CrimsonSiren,
    onError = WarmIvory,
    outline = GlassStroke,
  )

@Composable
fun GrandTheftIndiaTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = GameDarkColorScheme,
    typography = GrandTheftTypography,
    content = content,
  )
}
