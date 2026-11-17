package com.golagrocery.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GolaColors {
    val Green = Color(0xFF0C831F)
    val GreenDark = Color(0xFF0B3D16)
    val GreenLight = Color(0xFFE9F7EC)
    val Yellow = Color(0xFFF9C80E)
    val YellowLight = Color(0xFFFFF8DB)
    val Red = Color(0xFFD93025)
    val Blue = Color(0xFF1A73E8)
    val Grey = Color(0xFF6B7280)
    val Surface = Color(0xFFF6F8F6)
    val Discount = Color(0xFF256FEF)
}

private val scheme = lightColorScheme(
    primary = GolaColors.Green,
    onPrimary = Color.White,
    primaryContainer = GolaColors.GreenLight,
    onPrimaryContainer = GolaColors.GreenDark,
    secondary = GolaColors.Yellow,
    onSecondary = GolaColors.GreenDark,
    secondaryContainer = GolaColors.YellowLight,
    onSecondaryContainer = Color(0xFF5C4600),
    tertiary = GolaColors.Blue,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = GolaColors.Surface,
    error = GolaColors.Red,
    outline = Color(0xFFE0E3E7),
)

private val typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
)

@Composable
fun GolaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
