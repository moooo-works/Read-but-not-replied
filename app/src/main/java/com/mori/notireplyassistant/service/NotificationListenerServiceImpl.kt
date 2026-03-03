package com.mori.notireplyassistant.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mori.notireplyassistant.core.domain.model.MessageData
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import com.mori.notireplyassistant.core.repository.SettingsRepository
import com.mori.notireplyassistant.service.processor.BurstFilter
import com.mori.notireplyassistant.service.processor.NotificationProcessor
import com.mori.notireplyassistant.service.receiver.ActiveReplyMap
import com.mori.notireplyassistant.service.util.ConversationIdGenerator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerServiceImpl : NotificationListenerService() {

    @Inject
    lateinit var notificationProcessor: NotificationProcessor

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var activeReplyMap: ActiveReplyMap

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val burstFilter = BurstFilter()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (settingsRepository.isExcluded(sbn.packageName)) return

        if (!burstFilter.shouldProcess(sbn.key, sbn.id, sbn.tag, sbn.postTime)) {
            Log.d("NotiReply", "Burst suppressed for ${sbn.packageName}")
            return
        }

        val event = mapToEvent(sbn) ?: return

        // Resolve conversationId to cache reply action
        val conversationId = ConversationIdGenerator.generate(
            event.packageName,
            event.groupKey,
            event.title,
            if (event.messages.isNotEmpty()) event.messages.last().sender else event.title,
            event.sbnKey
        )

        // Cache Reply Action
        val remoteInputInfo = extractRemoteInput(sbn.notification)
        activeReplyMap.add(
            conversationId = conversationId,
            sbnKey = sbn.key,
            pendingIntent = remoteInputInfo?.first,
            remoteInput = remoteInputInfo?.second,
            contentIntent = sbn.notification.contentIntent
        )

        serviceScope.launch {
            try {
                notificationProcessor.processNotification(event)
            } catch (e: Exception) {
                Log.e("NotiReply", "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeReplyMap.remove(sbn.key)

        serviceScope.launch {
            try {
                notificationProcessor.processRemoval(
                    sbn.key,
                    sbn.packageName,
                    sbn.id,
                    sbn.tag,
                    sbn.postTime
                )
            } catch (e: Exception) {
                Log.e("NotiReply", "Error processing removal", e)
            }
        }
    }

    private fun mapToEvent(sbn: StatusBarNotification): NotificationEvent? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getString(Notification.EXTRA_TITLE)
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isNullOrEmpty()) return null

        val remoteInputInfo = extractRemoteInput(sbn.notification)

        // Extract MessagingStyle messages
        val messages = mutableListOf<MessageData>()
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
        if (messagingStyle != null) {
            val styleMessages: List<NotificationCompat.MessagingStyle.Message> = messagingStyle.messages
            for (msg in styleMessages) {
                val sender = msg.person?.name?.toString() ?: title
                val text = msg.text?.toString()
                if (text != null) {
                    messages.add(
                        MessageData(
                            sender = sender,
                            text = text,
                            timestamp = msg.timestamp
                        )
                    )
                }
            }
        }

        return NotificationEvent(
            sbnKey = sbn.key,
            packageName = sbn.packageName,
            notificationId = sbn.id,
            tag = sbn.tag,
            postTime = sbn.postTime,
            title = title.toString(),
            content = content,
            groupKey = sbn.groupKey,
            category = sbn.notification.category,
            isGroup = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            styleType = extras.getString(Notification.EXTRA_TEMPLATE),
            styleMetadata = "{\"type\": \"${extras.getString(Notification.EXTRA_TEMPLATE)}\"}",
            messages = messages,
            hasRemoteInput = remoteInputInfo != null
        )
    }

    private fun extractRemoteInput(notification: Notification): Pair<android.app.PendingIntent, android.app.RemoteInput>? {
        notification.actions?.forEach { action ->
            val remoteInputs = action.remoteInputs
            if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                for (input in remoteInputs) {
                    if (input.allowFreeFormInput) {
                        return Pair(action.actionIntent, input)
                    }
                }
            }
        }
        return null
    }
}
