package com.zhiqi.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ZhiQiTokens {
    val Primary = Color(0xFFD85B95)
    val PrimaryStrong = Color(0xFFC64582)
    val PrimarySoft = Color(0xFFFFE4EF)
    val AccentSoft = Color(0xFFFFF0F6)
    val AccentStrongerSoft = Color(0xFFFFD8E9)

    val TextPrimary = Color(0xFF1F2532)
    val TextSecondary = Color(0xFF6C7382)
    val TextMuted = Color(0xFF8C94A3)

    val BackgroundTop = Color(0xFFFFFCFE)
    val BackgroundBottom = Color(0xFFFDF2F7)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFFBF5F8)
    val Border = Color(0xFFEEDFE8)
    val BorderStrong = Color(0xFFE6CDDB)

    val Danger = Color(0xFFCC4A69)
}

private val LightColors = lightColorScheme(
    primary = ZhiQiTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = ZhiQiTokens.PrimarySoft,
    onPrimaryContainer = ZhiQiTokens.PrimaryStrong,
    secondary = ZhiQiTokens.PrimaryStrong,
    onSecondary = Color.White,
    secondaryContainer = ZhiQiTokens.AccentSoft,
    onSecondaryContainer = ZhiQiTokens.TextPrimary,
    tertiary = Color(0xFFA06ECF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E7FB),
    onTertiaryContainer = Color(0xFF4D3A66),
    error = ZhiQiTokens.Danger,
    onError = Color.White,
    background = ZhiQiTokens.BackgroundTop,
    onBackground = ZhiQiTokens.TextPrimary,
    surface = ZhiQiTokens.Surface,
    onSurface = ZhiQiTokens.TextPrimary,
    surfaceVariant = ZhiQiTokens.SurfaceSoft,
    onSurfaceVariant = ZhiQiTokens.TextSecondary,
    outline = ZhiQiTokens.BorderStrong,
    outlineVariant = ZhiQiTokens.Border
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
)

@Composable
fun ZhiQiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
