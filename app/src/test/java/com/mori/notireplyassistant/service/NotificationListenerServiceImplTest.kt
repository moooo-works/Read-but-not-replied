package com.mori.notireplyassistant.service

import android.service.notification.StatusBarNotification
import com.mori.notireplyassistant.core.repository.SettingsRepository
import com.mori.notireplyassistant.service.processor.NotificationProcessor
import com.mori.notireplyassistant.service.receiver.ActiveReplyMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class NotificationListenerServiceImplTest {

    @Mock
    lateinit var notificationProcessor: NotificationProcessor

    @Mock
    lateinit var settingsRepository: SettingsRepository

    @Mock
    lateinit var activeReplyMap: ActiveReplyMap

    private lateinit var service: NotificationListenerServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = NotificationListenerServiceImpl()
        service.notificationProcessor = notificationProcessor
        service.settingsRepository = settingsRepository
        service.activeReplyMap = activeReplyMap
    }

    @Test
    fun onNotificationRemoved_whenNotExcluded_processesRemoval() = runTest {
        val sbn = mock(StatusBarNotification::class.java)
        `when`(sbn.key).thenReturn("test_key")
        `when`(sbn.packageName).thenReturn("com.test.app")
        `when`(sbn.id).thenReturn(123)
        `when`(sbn.tag).thenReturn("test_tag")
        `when`(sbn.postTime).thenReturn(1000L)

        `when`(settingsRepository.isExcluded("com.test.app")).thenReturn(false)

        service.onNotificationRemoved(sbn)

        // Give coroutine time to launch
        Thread.sleep(100)

        verify(activeReplyMap).remove("test_key")
        verify(notificationProcessor).processRemoval(
            "test_key",
            "com.test.app",
            123,
            "test_tag",
            1000L
        )
    }

    @Test
    fun onNotificationRemoved_whenExcluded_doesNotProcessRemoval() = runTest {
        val sbn = mock(StatusBarNotification::class.java)
        `when`(sbn.key).thenReturn("test_key")
        `when`(sbn.packageName).thenReturn("com.test.app")

        `when`(settingsRepository.isExcluded("com.test.app")).thenReturn(true)

        service.onNotificationRemoved(sbn)

        // Give coroutine time to launch
        Thread.sleep(100)

        verify(activeReplyMap).remove("test_key")
        verifyNoInteractions(notificationProcessor)
    }
}
