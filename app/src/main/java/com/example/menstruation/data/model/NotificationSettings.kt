package com.example.menstruation.data.model

import java.util.Locale

data class NotificationSettings(
    val enabled: Boolean = true,
    val periodStartReminder: Boolean = true,
    val periodEndReminder: Boolean = true,
    val predictedPeriodReminder: Boolean = true,
    val reminderTime: ReminderTime = ReminderTime.DEFAULT
)

data class ReminderTime(
    val hour: Int,
    val minute: Int
) {
    companion object {
        val DEFAULT = ReminderTime(9, 0) // 默认早上9点

        fun fromString(timeStr: String): ReminderTime {
            val parts = timeStr.split(":")
            return ReminderTime(
                hour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
                minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            )
        }
    }

    override fun toString(): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun toMinutes(): Int = hour * 60 + minute
}
