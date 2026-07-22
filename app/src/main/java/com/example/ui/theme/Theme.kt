package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalPrimary,
    secondary = NaturalSecondary,
    tertiary = NaturalTertiary,
    background = Color(0xFF131411), // dark natural background
    surface = Color(0xFF131411),
    onPrimary = NaturalOnPrimary,
    onSecondary = NaturalOnSecondary,
    onTertiary = Color.White,
    onBackground = Color(0xFFE3E3DC),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF232520),
    onSurfaceVariant = Color(0xFFC4C8BA),
    outline = Color(0xFF43493E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalPrimary,
    secondary = NaturalSecondary,
    tertiary = NaturalTertiary,
    background = NaturalBackground,
    surface = NaturalSurface,
    onPrimary = NaturalOnPrimary,
    onSecondary = NaturalOnSecondary,
    onTertiary = Color.White,
    onBackground = NaturalOnBackground,
    onSurface = NaturalOnSurface,
    surfaceVariant = NaturalSurfaceVariant,
    onSurfaceVariant = NaturalOnSurfaceVariant,
    outline = NaturalOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disabled to strictly enforce the "Natural Tones" branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
