package com.mori.notireplyassistant.service.processor

import android.util.LruCache

/**
 * Filter to suppress rapid updates (bursts) from the same notification source.
 *
 * Key: packageName|sbnKey|id|tag|bucket(500ms)
 */
class BurstFilter {

    private val cache = LruCache<String, Boolean>(100) // Keep last 100 recent events

    fun shouldProcess(sbnKey: String, notificationId: Int, tag: String?, postTime: Long): Boolean {
        val bucket = postTime / 500 // 500ms bucket
        val key = "$sbnKey|$notificationId|$tag|$bucket"

        synchronized(cache) {
            if (cache.get(key) != null) {
                return false
            }
            cache.put(key, true)
            return true
        }
    }
}
