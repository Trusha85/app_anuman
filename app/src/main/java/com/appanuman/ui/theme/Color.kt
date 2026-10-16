package com.appanuman.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Band Colors
val BandLow = Color(0xFF2E7D32)       // Green
val BandMedium = Color(0xFFE65100)    // Amber
val BandHigh = Color(0xFFC62828)      // Red
val BandCritical = Color(0xFF4A148C)  // Deep Purple
val PanicRed = Color(0xFFB71C1C)      // Panic Red

// Grandparent Mode High-Contrast Colors (3 colors only)
val GpSafeGreen = Color(0xFF2E7D32)
val GpWarningAmber = Color(0xFFE65100)
val GpAlertRed = Color(0xFFC62828)

// Dark Palette
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkPrimary = Color(0xFF38BDF8)
val DarkOnPrimary = Color(0xFF0F172A)
val DarkPrimaryContainer = Color(0xFF0369A1)
val DarkOnPrimaryContainer = Color(0xFFE0F2FE)

// Light Palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF1E293B)
val LightPrimary = Color(0xFF0284C7)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0F2FE)
val LightOnPrimaryContainer = Color(0xFF0369A1)

@Immutable
data class ExtendedRiskColors(
    val low: Color = BandLow,
    val medium: Color = BandMedium,
    val high: Color = BandHigh,
    val critical: Color = BandCritical,
    val panic: Color = PanicRed
)

val LocalRiskColors = staticCompositionLocalOf { ExtendedRiskColors() }
