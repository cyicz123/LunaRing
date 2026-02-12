package com.example.menstruation.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 深色主题配色（参考小米健康） =====

// 主色调 - 鲜艳的粉色
val PinkPrimary = Color(0xFFFF6B8A)      // 主粉色，用于经期标记
val PinkDark = Color(0xFFFF477E)         // 深粉色，用于强调
val PinkLight = Color(0xFFFF8FA3)        // 浅粉色，用于悬停/高亮
val PinkTransparent = Color(0x33FF6B8A)  // 半透明粉色，用于背景

// 深色背景
val DarkBackground = Color(0xFF000000)       // 纯黑背景
val DarkSurface = Color(0xFF1C1C1E)          // 卡片背景
val DarkSurfaceElevated = Color(0xFF2C2C2E)  //  elevated 卡片

// 文字颜色
val TextPrimary = Color(0xFFFFFFFF)      // 主文字 - 白色
val TextSecondary = Color(0xFF8E8E93)    // 次要文字 - 灰色
val TextTertiary = Color(0xFF636366)     // 第三级文字 - 深灰

// 边框和分隔线
val BorderGray = Color(0xFF3A3A3C)
val DividerGray = Color(0xFF2C2C2E)

// 辅助色
val AccentBlue = Color(0xFF5AC8FA)
val AccentGreen = Color(0xFF34C759)
val AccentYellow = Color(0xFFFFCC00)
val AccentPurple = Color(0xFFAF52DE)
val AccentOrange = Color(0xFFFF9500)

// 状态色
val Success = Color(0xFF34C759)
val Warning = Color(0xFFFF9500)
val Error = Color(0xFFFF3B30)

// 保留原有颜色（兼容）
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = PinkLight

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = PinkDark