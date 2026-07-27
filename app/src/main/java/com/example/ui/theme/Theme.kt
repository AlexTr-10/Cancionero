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
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = Color.White,
    secondary = DarkAccent,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    outline = Color.Gray,
    outlineVariant = Color.DarkGray
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF), // very light blue container
    onPrimaryContainer = RoyalBlue,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7), // very light amber container
    onSecondaryContainer = GoldAccent,
    background = BackgroundSlate,
    onBackground = OnBackgroundDark,
    surface = SurfaceWhite,
    onSurface = OnBackgroundDark,
    outline = BorderLight,
    outlineVariant = Color.LightGray
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
