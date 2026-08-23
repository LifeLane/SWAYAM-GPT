package com.example.edgeaicore.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

data class NotificationPayload(
    val id: Int,
    val title: String,
    val message: String,
    val channelId: String = "edge_ai_alerts"
)

interface NotificationProvider {
    fun sendNotification(payload: NotificationPayload)
}

class LocalNotificationProvider(private val context: Context) : NotificationProvider {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "edge_ai_alerts",
                "EdgeAI System Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "On-Device AI and Memory Notifications"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun sendNotification(payload: NotificationPayload) {
        val builder = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            notificationManager?.notify(payload.id, builder.build())
        } catch (e: SecurityException) {
            // Contextually handled if POST_NOTIFICATIONS is not yet granted
        }
    }
}
