package com.mori.notireplyassistant.service.handler

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteInputComposer {
    fun addResults(remoteInputs: Array<RemoteInput>, intent: Intent, results: Bundle)
}

@Singleton
class DefaultRemoteInputComposer @Inject constructor() : RemoteInputComposer {
    override fun addResults(remoteInputs: Array<RemoteInput>, intent: Intent, results: Bundle) {
        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
    }
}
