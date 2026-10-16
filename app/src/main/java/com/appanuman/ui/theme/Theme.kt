package com.appanuman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.appanuman.model.UserMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = PanicRed,
    onError = DarkOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    error = PanicRed,
    onError = LightOnBackground
)

@Composable
fun AppAnumanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    userMode: UserMode = UserMode.SIMPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = if (userMode == UserMode.GRANDPARENT) GrandparentTypography else AppTypography
    val riskColors = ExtendedRiskColors()

    CompositionLocalProvider(
        LocalRiskColors provides riskColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
