package com.mori.notireplyassistant.service.handler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PendingIntentSender {
    fun send(pendingIntent: PendingIntent, intent: Intent)
}

@Singleton
class FrameworkPendingIntentSender @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingIntentSender {
    override fun send(pendingIntent: PendingIntent, intent: Intent) {
        pendingIntent.send(context, 0, intent)
    }
}
