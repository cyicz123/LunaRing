package com.example.menstruation.notification

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.menstruation.data.model.Period
import com.example.menstruation.domain.usecase.PredictNextPeriodUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val predictNextPeriodUseCase: PredictNextPeriodUseCase
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleAllNotifications(
        periods: List<Period>,
        cycleLength: Int,
        periodLength: Int,
        reminderTime: java.time.LocalTime,
        enabled: Boolean,
        periodStartReminder: Boolean,
        periodEndReminder: Boolean,
        predictedPeriodReminder: Boolean
    ) {
        if (!enabled) {
            cancelAllNotifications()
            return
        }

        val lastPeriod = periods.maxByOrNull { it.startDate }

        // Schedule period end reminder
        if (periodEndReminder && lastPeriod != null && lastPeriod.endDate == null) {
            val expectedEndDate = lastPeriod.startDate.plusDays(periodLength.toLong() - 1)
            if (expectedEndDate.isAfter(LocalDate.now())) {
                scheduleNotification(
                    type = PeriodNotificationWorker.TYPE_PERIOD_END,
                    title = "经期即将结束",
                    message = "您的经期预计今天结束",
                    targetDate = expectedEndDate,
                    reminderTime = reminderTime
                )
            }
        }

        // Schedule predicted period reminder
        if (predictedPeriodReminder) {
            val nextPeriod = predictNextPeriodUseCase(periods, cycleLength)
            nextPeriod?.let { next ->
                val reminderDate = next.minusDays(1) // Remind 1 day before

                if (reminderDate.isAfter(LocalDate.now()) ||
                    (reminderDate.isEqual(LocalDate.now()) && reminderTime.isAfter(LocalTime.now()))
                ) {
                    scheduleNotification(
                        type = PeriodNotificationWorker.TYPE_PREDICTED_PERIOD,
                        title = "经期即将到来",
                        message = "预测您的经期将在明天开始，请做好准备",
                        targetDate = reminderDate,
                        reminderTime = reminderTime
                    )
                }
            }
        }
    }

    fun schedulePeriodStartNotification(
        startDate: LocalDate,
        reminderTime: java.time.LocalTime
    ) {
        scheduleNotification(
            type = PeriodNotificationWorker.TYPE_PERIOD_START,
            title = "经期开始",
            message = "记录您的经期已开始",
            targetDate = startDate,
            reminderTime = reminderTime
        )
    }

    private fun scheduleNotification(
        type: String,
        title: String,
        message: String,
        targetDate: LocalDate,
        reminderTime: java.time.LocalTime
    ) {
        val targetDateTime = LocalDateTime.of(targetDate, reminderTime)
        val delay = Duration.between(LocalDateTime.now(), targetDateTime)

        if (delay.isNegative) return

        val inputData = Data.Builder()
            .putString(PeriodNotificationWorker.KEY_NOTIFICATION_TYPE, type)
            .putString(PeriodNotificationWorker.KEY_TITLE, title)
            .putString(PeriodNotificationWorker.KEY_MESSAGE, message)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PeriodNotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "${type}_${targetDate}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAllNotifications() {
        workManager.cancelAllWork()
    }

    fun cancelNotification(type: String) {
        workManager.cancelUniqueWork(type)
    }
}

object NotificationPermissionHelper {
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
