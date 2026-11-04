package com.routeleague.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Pleos Connect UI Design Guide 요약 반영:
 *  - 대비 4.5:1 이상, 주간/야간 모드 지원
 *  - 주요 텍스트 높이 5mm 이상, 보조 3mm 이상 → 차량 디스플레이 실측 후 sp 값 조정
 *  - 터치 응답 0.25초 이내, 한 손 조작
 * TODO: 공식 Figma(design assets) 의 색·타이포 토큰으로 교체.
 */

private val Accent = Color(0xFF2F7DFF)
private val Success = Color(0xFF1DB57A)
private val Warning = Color(0xFFFFB020)

private val NightColors = darkColorScheme(
    primary = Accent,
    secondary = Success,
    tertiary = Warning,
    background = Color(0xFF0E1116),
    surface = Color(0xFF181C23),
    onBackground = Color(0xFFF2F4F7),
    onSurface = Color(0xFFF2F4F7),
)

private val DayColors = lightColorScheme(
    primary = Color(0xFF0B5FE0),
    secondary = Color(0xFF0E8A5B),
    tertiary = Color(0xFFB36B00),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onBackground = Color(0xFF111418),
    onSurface = Color(0xFF111418),
)

private val CarTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 24.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    labelLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun RouteLeagueTheme(
    night: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (night) NightColors else DayColors,
        typography = CarTypography,
        content = content,
    )
}
