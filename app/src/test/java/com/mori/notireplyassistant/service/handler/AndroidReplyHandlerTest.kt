package com.mori.notireplyassistant.service.handler

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import com.mori.notireplyassistant.core.common.Logger
import com.mori.notireplyassistant.core.domain.handler.ReplyResult
import com.mori.notireplyassistant.service.receiver.ActiveReplyMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

class NoOpLogger : Logger {
    override fun e(tag: String, message: String, throwable: Throwable?) {}
}

class FakePendingIntentSender : PendingIntentSender {
    var shouldThrow = false
    var sendCount = 0

    override fun send(pendingIntent: PendingIntent, intent: Intent) {
        if (shouldThrow) {
            throw RuntimeException("Simulated send failure")
        }
        sendCount++
    }
}

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AndroidReplyHandlerTest {

    @Mock
    private lateinit var activeReplyMap: ActiveReplyMap
    @Mock
    private lateinit var intentOpener: IntentOpener
    @Mock
    private lateinit var intentFactory: IntentFactory

    @Mock
    private lateinit var mockPendingIntent: PendingIntent
    @Mock
    private lateinit var mockRemoteInput: RemoteInput
    @Mock
    private lateinit var mockContentIntent: PendingIntent
    @Mock
    private lateinit var mockIntent: Intent

    private lateinit var fakePendingIntentSender: FakePendingIntentSender
    private lateinit var handler: AndroidReplyHandler

    @Before
    fun setup() {
        fakePendingIntentSender = FakePendingIntentSender()
        handler = AndroidReplyHandler(
            activeReplyMap,
            fakePendingIntentSender,
            intentOpener,
            intentFactory,
            NoOpLogger()
        )
    }

    @Test
    fun sendReply_withRemoteInput_returnsSuccess() = runTest {
        val info = ActiveReplyMap.ReplyActionInfo(mockPendingIntent, mockRemoteInput, null)
        `when`(activeReplyMap.getByConversation("c1")).thenReturn(info)
        `when`(intentFactory.createReplyIntent(mockRemoteInput, "Hello")).thenReturn(mockIntent)

        val result = handler.sendReply("c1", "com.pkg", "Hello")

        assertEquals(ReplyResult.SUCCESS, result)
        assertEquals(1, fakePendingIntentSender.sendCount)
    }

    @Test
    fun sendReply_remoteInputThrows_fallsBackToContentIntent() = runTest {
        val info = ActiveReplyMap.ReplyActionInfo(mockPendingIntent, mockRemoteInput, mockContentIntent)
        `when`(activeReplyMap.getByConversation("c1")).thenReturn(info)
        `when`(intentFactory.createReplyIntent(mockRemoteInput, "Hello")).thenReturn(mockIntent)

        // Simulate failure in sending
        fakePendingIntentSender.shouldThrow = true

        `when`(intentOpener.openContentIntent(mockContentIntent)).thenReturn(true)

        val result = handler.sendReply("c1", "com.pkg", "Hello")

        assertEquals(ReplyResult.FALLBACK_OPENED, result)
        verify(intentOpener).openContentIntent(mockContentIntent)
    }

    @Test
    fun sendReply_noRemoteInput_usesContentIntent_returnsFallback() = runTest {
        val info = ActiveReplyMap.ReplyActionInfo(null, null, mockContentIntent)
        `when`(activeReplyMap.getByConversation("c1")).thenReturn(info)
        `when`(intentOpener.openContentIntent(mockContentIntent)).thenReturn(true)

        val result = handler.sendReply("c1", "com.pkg", "Hello")

        assertEquals(ReplyResult.FALLBACK_OPENED, result)
        verify(intentOpener).openContentIntent(mockContentIntent)
    }

    @Test
    fun sendReply_noInfo_usesAppLaunch_returnsFallback() = runTest {
        `when`(activeReplyMap.getByConversation("c1")).thenReturn(null)
        `when`(intentOpener.openAppLaunchIntent("com.pkg")).thenReturn(true)

        val result = handler.sendReply("c1", "com.pkg", "Hello")

        assertEquals(ReplyResult.FALLBACK_OPENED, result)
    }

    @Test
    fun sendReply_noInfo_noLaunchIntent_returnsNotAvailable() = runTest {
        `when`(activeReplyMap.getByConversation("c1")).thenReturn(null)
        `when`(intentOpener.openAppLaunchIntent("com.pkg")).thenReturn(false)

        val result = handler.sendReply("c1", "com.pkg", "Hello")

        assertEquals(ReplyResult.NOT_AVAILABLE, result)
    }
}
