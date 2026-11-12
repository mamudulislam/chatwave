package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = ChatWaveDarkPrimary,
  onPrimary = ChatWaveDarkOnPrimary,
  primaryContainer = WhatsAppTealDark,
  onPrimaryContainer = Color.White,
  secondary = WhatsAppGreenDarkTheme,
  onSecondary = Color.Black,
  background = ChatWaveDarkBackground,
  onBackground = ChatWaveDarkTextPrimary,
  surface = ChatWaveDarkSurface,
  onSurface = ChatWaveDarkTextPrimary,
  surfaceVariant = ChatWaveDarkHeader,
  onSurfaceVariant = ChatWaveDarkTextSecondary,
  outline = ChatWaveDarkDivider
)

private val LightColorScheme = lightColorScheme(
  primary = ChatWaveLightPrimary,
  onPrimary = ChatWaveLightOnPrimary,
  primaryContainer = WhatsAppTeal,
  onPrimaryContainer = Color.White,
  secondary = WhatsAppGreen,
  onSecondary = Color.White,
  background = ChatWaveLightBackground,
  onBackground = ChatWaveLightTextPrimary,
  surface = ChatWaveLightSurface,
  onSurface = ChatWaveLightTextPrimary,
  surfaceVariant = Color(0xFFF0F2F5),
  onSurfaceVariant = ChatWaveLightTextSecondary,
  outline = ChatWaveLightDivider
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep WhatsApp signature branding consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
