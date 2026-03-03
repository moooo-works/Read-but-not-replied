package com.mori.notireplyassistant.service.receiver

import android.app.PendingIntent
import android.app.RemoteInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveReplyMap @Inject constructor() {

    private val replyMap = mutableMapOf<String, ReplyActionInfo>()
    private val conversationToSbnMap = mutableMapOf<String, String>()

    fun add(conversationId: String, sbnKey: String, pendingIntent: PendingIntent?, remoteInput: RemoteInput?, contentIntent: PendingIntent?) {
        conversationToSbnMap[conversationId] = sbnKey
        replyMap[sbnKey] = ReplyActionInfo(pendingIntent, remoteInput, contentIntent)
    }

    fun get(sbnKey: String): ReplyActionInfo? {
        return replyMap[sbnKey]
    }

    fun getByConversation(conversationId: String): ReplyActionInfo? {
        val sbnKey = conversationToSbnMap[conversationId] ?: return null
        return replyMap[sbnKey]
    }

    fun remove(sbnKey: String) {
        replyMap.remove(sbnKey)
        // Find and remove from reverse map
        val entriesToRemove = conversationToSbnMap.filterValues { it == sbnKey }.keys
        entriesToRemove.forEach { conversationToSbnMap.remove(it) }
    }

    fun clear() {
        replyMap.clear()
        conversationToSbnMap.clear()
    }

    data class ReplyActionInfo(
        val pendingIntent: PendingIntent?, // Action intent for RemoteInput
        val remoteInput: RemoteInput?,
        val contentIntent: PendingIntent? // To open the app/conversation
    )
}
