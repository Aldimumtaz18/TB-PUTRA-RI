package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = Sky400,
    onPrimary = Navy950,
    primaryContainer = Navy800,
    onPrimaryContainer = Navy100,
    secondary = Orange500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Orange100,
    tertiary = Orange400,
    onTertiary = Navy950,
    background = Navy950,
    onBackground = Slate50,
    surface = Color(0xFF10223D),
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Color(0xFF1E3454),
    error = Red400,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Red100,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Navy900,
    onPrimary = Color.White,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy900,
    secondary = Orange600,
    onSecondary = Color.White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Orange600,
    onTertiary = Color.White,
    background = Color(0xFFF4F6FA),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Red600,
    errorContainer = Red100,
    onErrorContainer = Color(0xFF7F1D1D),
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
  )

@Composable
fun TbPutraRiTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
