package com.mori.notireplyassistant.service.handler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface IntentOpener {
    fun openContentIntent(pendingIntent: PendingIntent): Boolean
    fun openAppLaunchIntent(packageName: String): Boolean
}

@Singleton
class FrameworkIntentOpener @Inject constructor(
    @ApplicationContext private val context: Context
) : IntentOpener {

    override fun openContentIntent(pendingIntent: PendingIntent): Boolean {
        return try {
            pendingIntent.send()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun openAppLaunchIntent(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
