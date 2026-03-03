package com.mori.notireplyassistant.service.handler

import com.mori.notireplyassistant.core.common.Logger
import com.mori.notireplyassistant.core.domain.handler.ReplyHandler
import com.mori.notireplyassistant.core.domain.handler.ReplyResult
import com.mori.notireplyassistant.service.receiver.ActiveReplyMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReplyHandler @Inject constructor(
    private val activeReplyMap: ActiveReplyMap,
    private val pendingIntentSender: PendingIntentSender,
    private val intentOpener: IntentOpener,
    private val intentFactory: IntentFactory,
    private val logger: Logger
) : ReplyHandler {

    override suspend fun canReply(conversationId: String): Boolean {
        return activeReplyMap.getByConversation(conversationId) != null
    }

    override suspend fun sendReply(conversationId: String, packageName: String, message: String): ReplyResult {
        val info = activeReplyMap.getByConversation(conversationId)

        if (info == null) {
            return fallbackLaunch(packageName)
        }

        if (info.pendingIntent != null && info.remoteInput != null) {
            try {
                val localIntent = intentFactory.createReplyIntent(info.remoteInput, message)
                pendingIntentSender.send(info.pendingIntent, localIntent)
                return ReplyResult.SUCCESS
            } catch (e: Exception) {
                logger.e("NotiReply", "Failed to send RemoteInput", e)
                // Fallthrough to try content intent if remote input fails
            }
        }

        // Fallback: Content Intent
        if (info.contentIntent != null) {
            val opened = intentOpener.openContentIntent(info.contentIntent)
            if (opened) {
                return ReplyResult.FALLBACK_OPENED
            }
        }

        return fallbackLaunch(packageName)
    }

    private fun fallbackLaunch(packageName: String): ReplyResult {
        val opened = intentOpener.openAppLaunchIntent(packageName)
        return if (opened) ReplyResult.FALLBACK_OPENED else ReplyResult.NOT_AVAILABLE
    }
}
