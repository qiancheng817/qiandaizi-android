package com.qiandaizi.app.core

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

// 配色复刻参考设计 qiandaizi-app（鲨鱼记账风格：暖黄 + 卡片白）
val Yellow = Color(0xFFFFD23F)
val YellowDark = Color(0xFFF7B500)
val YellowSoft = Color(0xFFFFF3C4)
val AppBg = Color(0xFFF6F7F9)
val CardWhite = Color(0xFFFFFFFF)
val DividerGray = Color(0xFFEEEEEE)
val TextMain = Color(0xFF222222)
val TextSub = Color(0xFF888888)
val ExpenseRed = Color(0xFFF04438)
val IncomeGreen = Color(0xFF2BA471)
val BrandBlue = Color(0xFF3A7BFF)

private val LightColors = lightColorScheme(
    primary = Yellow,
    onPrimary = TextMain,
    primaryContainer = YellowSoft,
    onPrimaryContainer = TextMain,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    background = AppBg,
    onBackground = TextMain,
    surface = CardWhite,
    onSurface = TextMain,
    surfaceVariant = Color(0xFFF2F3F5),
    onSurfaceVariant = TextSub,
    outline = DividerGray,
    error = ExpenseRed
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelSmall = TextStyle(fontSize = 11.sp)
)

@Composable
fun QianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
