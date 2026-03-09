package com.eterna.kee.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object EternaColors {
    val Background      = Color(0xFF0D0D1A)
    val Surface         = Color(0xFF1A1A2E)
    val SurfaceGlass    = Color(0xFF1E1E35)
    val SurfaceBright   = Color(0xFF252540)

    val Accent          = Color(0xFF8B7FFF)
    val AccentCyan      = Color(0xFF4FD1C5)

    val TextPrimary     = Color(0xFFE8E8F0)
    val TextSecondary   = Color(0xFF8888A0)
    val TextDim         = Color(0xFF555570)

    val BubbleUser      = Color(0xFF3D3566)
    val BubbleOther     = Color(0xFF1E1E35)

    val Divider         = Color(0xFF2A2A45)
    val Error           = Color(0xFFFF6B6B)
}

private val EternaDarkScheme = darkColorScheme(
    primary             = EternaColors.Accent,
    onPrimary           = Color.White,
    secondary           = EternaColors.AccentCyan,
    onSecondary         = Color.Black,
    background          = EternaColors.Background,
    onBackground        = EternaColors.TextPrimary,
    surface             = EternaColors.Surface,
    onSurface           = EternaColors.TextPrimary,
    surfaceVariant      = EternaColors.SurfaceBright,
    onSurfaceVariant    = EternaColors.TextSecondary,
    outline             = EternaColors.Divider,
    error               = EternaColors.Error,
    onError             = Color.White,
)

@Composable
fun EternaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EternaDarkScheme,
        typography = Typography(
            headlineLarge  = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            titleMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
            bodyLarge      = TextStyle(fontSize = 16.sp),
            bodyMedium     = TextStyle(fontSize = 14.sp),
            bodySmall      = TextStyle(fontSize = 12.sp),
            labelMedium    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
        ),
        shapes = Shapes(
            small  = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large  = RoundedCornerShape(24.dp),
        ),
        content = content
    )
}