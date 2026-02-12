package com.example.menstruation.data.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserSettings(
    val periodLength: Int,
    val cycleLength: Int,
    val themeMode: ThemeMode = ThemeMode.DARK
)
