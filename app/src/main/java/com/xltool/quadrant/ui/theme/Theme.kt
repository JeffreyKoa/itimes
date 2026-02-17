package com.xltool.quadrant.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/**
 * 简洁·大方·得体 设计规范 Version 1.0
 */

// 主色调：深海蓝 - 专业、稳重、可信赖
private val PrimaryColor = Color(0xFF1E3A5F)
// 辅助色：灰蓝色 - 次要文字、图标
private val SecondaryColor = Color(0xFF64748B)
// 点缀色：浅灰色 - 占位文字、禁用状态
private val TertiaryColor = Color(0xFF94A3B8)

private val AppLightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF1E3A5F),
    
    secondary = SecondaryColor,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF475569),
    
    tertiary = TertiaryColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8FAFC),
    onTertiaryContainer = Color(0xFF64748B),
    
    background = Color(0xFFF8FAFC), // 极简背景色
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1E3A5F),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1)
)

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF90CDF4),
    onPrimary = Color(0xFF0D1D35),
    primaryContainer = Color(0xFF1A3A5F),
    onPrimaryContainer = Color(0xFFD1E9FF),
    
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun XlToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 默认关闭动态色，使用专业配色
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> AppDarkColors
        else -> AppLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
