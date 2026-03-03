package com.mori.notireplyassistant.service.handler

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

interface IntentFactory {
    fun createReplyIntent(remoteInput: RemoteInput, message: String): Intent
}

@Singleton
class FrameworkIntentFactory @Inject constructor(
    private val remoteInputComposer: RemoteInputComposer
) : IntentFactory {
    override fun createReplyIntent(remoteInput: RemoteInput, message: String): Intent {
        val localIntent = Intent()
        val bundle = Bundle()
        bundle.putCharSequence(remoteInput.resultKey, message)
        remoteInputComposer.addResults(arrayOf(remoteInput), localIntent, bundle)
        return localIntent
    }
}
