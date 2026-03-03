package com.mori.notireplyassistant.service.processor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BurstFilterTest {

    @Test
    fun allowsFirstEvent() {
        val filter = BurstFilter()
        assertTrue(filter.shouldProcess("key1", 1, null, 1000))
    }

    @Test
    fun suppressesBurstWithinBucket() {
        val filter = BurstFilter()
        // First event
        filter.shouldProcess("key1", 1, null, 1000)

        // Same event slightly later (same 500ms bucket)
        assertFalse(filter.shouldProcess("key1", 1, null, 1200))
    }

    @Test
    fun allowsEventInNextBucket() {
        val filter = BurstFilter()
        filter.shouldProcess("key1", 1, null, 1000)

        // Next bucket ( > 1500ms)
        assertTrue(filter.shouldProcess("key1", 1, null, 1600))
    }

    @Test
    fun differentiatesByNotificationId() {
        val filter = BurstFilter()
        filter.shouldProcess("key1", 1, null, 1000)

        // Different ID, same bucket -> should process
        assertTrue(filter.shouldProcess("key1", 2, null, 1200))
    }
}
