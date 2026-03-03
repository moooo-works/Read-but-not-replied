package com.mori.notireplyassistant.core.common

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface Logger {
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

@Singleton
class AndroidLogger @Inject constructor() : Logger {
    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
