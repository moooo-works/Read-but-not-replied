package com.mori.notireplyassistant.service.util

import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationKeyResolverTest {

    @Test
    fun normalize_handlesSpacesAndCounts() {
        assertEquals("test group", ConversationKeyResolver.normalize("  Test   Group  "))
        assertEquals("line chat", ConversationKeyResolver.normalize("LINE CHAT (3)"))
        assertEquals("some chat", ConversationKeyResolver.normalize("Some Chat （12）"))
        assertEquals("no count (but bracket)", ConversationKeyResolver.normalize("No count (but bracket)"))
        assertEquals("no count", ConversationKeyResolver.normalize("No count "))
    }

    @Test
    fun resolve_usesMessagingConversationTitle() {
        val event = NotificationEvent(
            sbnKey = "key1", packageName = "pkg", notificationId = 1, tag = null, postTime = 1L,
            title = "Fallback Title", content = "text", groupKey = null, category = null, isGroup = false,
            styleType = "android.app.Notification\$MessagingStyle", styleMetadata = "{}", hasRemoteInput = false,
            conversationTitle = "LINE Group (5)", isGroupConversation = true
        )

        val resolved = ConversationKeyResolver.resolve(event)
        assertEquals("mt:line group", resolved.threadKey)
        assertEquals(ThreadType.GROUP, resolved.threadType)
        assertEquals("MESSAGING_CONVERSATION_TITLE", resolved.keySource)
    }

    @Test
    fun resolve_fallbackToTitle() {
        val event = NotificationEvent(
            sbnKey = "key1", packageName = "pkg", notificationId = 1, tag = null, postTime = 1L,
            title = "John Doe", content = "text", groupKey = null, category = null, isGroup = false,
            styleType = null, styleMetadata = "{}", hasRemoteInput = false,
            conversationTitle = null, isGroupConversation = null
        )

        val resolved = ConversationKeyResolver.resolve(event)
        assertEquals("t:john doe", resolved.threadKey)
        assertEquals(ThreadType.UNKNOWN, resolved.threadType)
        assertEquals("TITLE", resolved.keySource)
    }

    @Test
    fun resolve_fallbackToSbnKey() {
        val event = NotificationEvent(
            sbnKey = "0|pkg|1|tag|1000", packageName = "pkg", notificationId = 1, tag = "tag", postTime = 1000L,
            title = " ", content = "text", groupKey = null, category = null, isGroup = false,
            styleType = null, styleMetadata = "{}", hasRemoteInput = false,
            conversationTitle = null, isGroupConversation = null
        )

        val resolved = ConversationKeyResolver.resolve(event)
        assertEquals("k:0|pkg|1|tag|1000", resolved.threadKey)
        assertEquals(ThreadType.UNKNOWN, resolved.threadType)
        assertEquals("SBN_KEY", resolved.keySource)
    }
}
