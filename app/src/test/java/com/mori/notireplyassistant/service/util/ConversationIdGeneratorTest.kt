package com.mori.notireplyassistant.service.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationIdGeneratorTest {

    @Test
    fun generatesCorrectIdWithGroupKey() {
        val id = ConversationIdGenerator.generate("com.pkg", "group1", "Title", "Sender", "key1")
        assertEquals("com.pkg|group1", id)
    }

    @Test
    fun generatesCorrectIdWithoutGroupKey_UsesSender() {
        val id = ConversationIdGenerator.generate("com.pkg", null, "Title", "Sender", "key1")
        assertEquals("com.pkg|sender:Sender", id)
    }

    @Test
    fun generatesCorrectIdWithoutGroupOrSender_UsesTitle() {
        val id = ConversationIdGenerator.generate("com.pkg", null, "Title", null, "key1")
        assertEquals("com.pkg|title:Title", id)
    }

    @Test
    fun generatesCorrectIdFallbackToKey() {
        val id = ConversationIdGenerator.generate("com.pkg", null, null, null, "key1")
        assertEquals("com.pkg|key1", id)
    }
}
