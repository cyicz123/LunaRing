package com.example.menstruation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.menstruation.R
import kotlinx.coroutines.flow.first

class PeriodNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationType = inputData.getString(KEY_NOTIFICATION_TYPE) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()

        showNotification(notificationType, title, message)

        return Result.success()
    }

    private fun showNotification(type: String, title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "生理期提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "经期开始、结束及预测提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(type.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "period_notifications"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"

        // Notification types
        const val TYPE_PERIOD_START = "period_start"
        const val TYPE_PERIOD_END = "period_end"
        const val TYPE_PREDICTED_PERIOD = "predicted_period"
    }
}
