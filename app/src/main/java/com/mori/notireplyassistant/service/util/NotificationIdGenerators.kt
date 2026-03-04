package com.mori.notireplyassistant.service.util

import java.security.MessageDigest

object ConversationIdGenerator {
    fun generate(packageName: String, groupKey: String?, title: String?, sender: String?, sbnKey: String): String {
        // Preference: groupKey (if present) -> title/sender -> sbnKey
        val normalizedKey = when {
            !groupKey.isNullOrEmpty() -> groupKey
            !sender.isNullOrEmpty() -> "sender:$sender"
            !title.isNullOrEmpty() -> "title:$title"
            else -> sbnKey
        }
        return "$packageName|$normalizedKey"
    }
}

object MessageIdGenerator {
    fun generate(
        packageName: String,
        conversationId: String,
        sender: String,
        text: String?,
        timestamp: Long,
        sbnKey: String = "",
        messageIndex: Int = 0
    ): String {
        val input = if (timestamp > 0) {
            "$packageName:$conversationId:$sender:${text ?: ""}:$timestamp"
        } else {
            "$packageName:$conversationId:$sender:${text ?: ""}:$sbnKey:$messageIndex"
        }
        return sha256(input)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
