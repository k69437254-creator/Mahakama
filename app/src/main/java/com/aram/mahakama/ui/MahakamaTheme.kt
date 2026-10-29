package com.aram.mahakama.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val VioletScheme = darkColorScheme(
    primary = Color(0xFFB9A1FF),
    onPrimary = Color(0xFF211536),
    primaryContainer = Color(0xFF312147),
    onPrimaryContainer = Color(0xFFE9DEFF),
    secondary = Color(0xFFB6A7CC),
    onSecondary = Color(0xFF261E31),
    background = Color(0xFF0C0A12),
    onBackground = Color(0xFFF0EAF8),
    surface = Color(0xFF15111D),
    onSurface = Color(0xFFF0EAF8),
    surfaceVariant = Color(0xFF231C2D),
    onSurfaceVariant = Color(0xFFC8BED7),
    outline = Color(0xFF5A4D6B),
    error = Color(0xFFFFB4AB)
)

private val MahakamaTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 27.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun MahakamaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VioletScheme,
        typography = MahakamaTypography,
        content = content
    )
}
