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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.ConcurrentLinkedQueue

@AndroidEntryPoint
class NotificationListenerServiceImpl : NotificationListenerService() {

    @Inject
    lateinit var notificationProcessor: NotificationProcessor

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var activeReplyMap: ActiveReplyMap

    // Make this var and internal for testing, defaults to real implementation
    internal var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val burstFilter = BurstFilter()

    private val pendingBuffer = ConcurrentLinkedQueue<PendingAction>()
    private val MAX_BUFFER_SIZE = 50
    private var isFlushed = false

    sealed class PendingAction {
        data class Post(val sbn: StatusBarNotification) : PendingAction()
        data class Remove(val sbn: StatusBarNotification) : PendingAction()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            settingsRepository.isReadyFlow.collectLatest { isReady ->
                if (isReady && !isFlushed) {
                    flushBuffer()
                }
            }
        }
    }

    private fun flushBuffer() {
        isFlushed = true
        while (pendingBuffer.isNotEmpty()) {
            val action = pendingBuffer.poll() ?: break
            when (action) {
                is PendingAction.Post -> processPost(action.sbn)
                is PendingAction.Remove -> processRemove(action.sbn)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!settingsRepository.isReady) {
            if (pendingBuffer.size < MAX_BUFFER_SIZE) {
                pendingBuffer.offer(PendingAction.Post(sbn))
            }
            return
        }
        processPost(sbn)
    }

    private fun processPost(sbn: StatusBarNotification) {
        if (settingsRepository.isExcluded(sbn.packageName)) return

        if (!burstFilter.shouldProcess(sbn.key, sbn.id, sbn.tag, sbn.postTime)) {
            // Burst suppressed; log removed to protect PII
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
        // Always remove from active map first, regardless of readiness
        activeReplyMap.remove(sbn.key)

        if (!settingsRepository.isReady) {
            if (pendingBuffer.size < MAX_BUFFER_SIZE) {
                pendingBuffer.offer(PendingAction.Remove(sbn))
            }
            return
        }
        processRemove(sbn)
    }

    private fun processRemove(sbn: StatusBarNotification) {
        if (settingsRepository.isExcluded(sbn.packageName)) return

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
            isGroup = true, // We don't really have a strict framework concept of isGroup beyond presence of groupKey, but we keep it true for backwards compatibility if needed.
            isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
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
