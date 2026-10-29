package com.aram.mahakama.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 27.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun MahakamaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VioletScheme,
        typography = MahakamaTypography,
        content = content
    )
}
