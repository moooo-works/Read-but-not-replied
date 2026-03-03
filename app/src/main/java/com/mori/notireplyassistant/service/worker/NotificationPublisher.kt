package com.mori.notireplyassistant.service.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.mori.notireplyassistant.NotiReplyApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPublisher {
    fun publishReminder(notificationId: Int, title: String, text: String)
}

@Singleton
class AndroidNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPublisher {

    override fun publishReminder(notificationId: Int, title: String, text: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotiReplyApp.CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
