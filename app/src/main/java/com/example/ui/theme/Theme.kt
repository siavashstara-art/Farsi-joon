package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
  primary = DarkTurquoisePrimary,
  onPrimary = DarkTurquoiseOnPrimary,
  primaryContainer = DarkTurquoiseContainer,
  onPrimaryContainer = DarkTurquoiseOnContainer,
  secondary = DarkSaffronSecondary,
  onSecondary = DarkSaffronOnSecondary,
  secondaryContainer = DarkSaffronContainer,
  onSecondaryContainer = DarkSaffronOnContainer,
  tertiary = DarkPomegranateTertiary,
  onTertiary = DarkPomegranateOnTertiary,
  tertiaryContainer = DarkPomegranateContainer,
  onTertiaryContainer = DarkPomegranateOnContainer,
  background = MidnightBackground,
  onBackground = SandLight,
  surface = MidnightSurface,
  onSurface = SandLight,
  surfaceVariant = MidnightSurfaceVariant,
  onSurfaceVariant = SandMuted,
  outline = MidnightOutline
)

private val LightColorScheme = lightColorScheme(
  primary = TurquoisePrimary,
  onPrimary = TurquoiseOnPrimary,
  primaryContainer = TurquoiseContainer,
  onPrimaryContainer = TurquoiseOnContainer,
  secondary = SaffronSecondary,
  onSecondary = SaffronOnSecondary,
  secondaryContainer = SaffronContainer,
  onSecondaryContainer = SaffronOnContainer,
  tertiary = PomegranateTertiary,
  onTertiary = PomegranateOnTertiary,
  tertiaryContainer = PomegranateContainer,
  onTertiaryContainer = PomegranateOnContainer,
  background = IvoryBackground,
  onBackground = InkDark,
  surface = WarmSurface,
  onSurface = InkDark,
  surfaceVariant = WarmSurfaceVariant,
  onSurfaceVariant = InkMuted,
  outline = WarmOutline
)

@Composable
fun FarsiJoonTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    content = content
  )
}
