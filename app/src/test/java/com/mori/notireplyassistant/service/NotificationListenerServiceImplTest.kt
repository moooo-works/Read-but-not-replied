package com.mori.notireplyassistant.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import com.mori.notireplyassistant.core.repository.SettingsRepository
import com.mori.notireplyassistant.service.processor.NotificationProcessor
import com.mori.notireplyassistant.service.receiver.ActiveReplyMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.mockito.ArgumentMatchers.any

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
    private lateinit var isReadyFlow: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = NotificationListenerServiceImpl()
        service.notificationProcessor = notificationProcessor
        service.settingsRepository = settingsRepository
        service.activeReplyMap = activeReplyMap

        isReadyFlow = MutableStateFlow(false)
        `when`(settingsRepository.isReadyFlow).thenReturn(isReadyFlow)
        `when`(settingsRepository.isReady).thenAnswer { isReadyFlow.value }
    }

    private fun createMockSbn(
        sbnKey: String,
        pkgName: String,
        idValue: Int = 123,
        tagValue: String? = "test_tag",
        time: Long = 1000L,
        title: String? = "Test Title",
        text: String? = "Test Text"
    ): StatusBarNotification {
        val sbn = mock(StatusBarNotification::class.java)
        `when`(sbn.key).thenReturn(sbnKey)
        `when`(sbn.packageName).thenReturn(pkgName)
        `when`(sbn.id).thenReturn(idValue)
        `when`(sbn.tag).thenReturn(tagValue)
        `when`(sbn.postTime).thenReturn(time)

        val notification = mock(Notification::class.java)
        val extras = Bundle()
        if (title != null) extras.putString(Notification.EXTRA_TITLE, title)
        if (text != null) extras.putCharSequence(Notification.EXTRA_TEXT, text)
        extras.putString(Notification.EXTRA_TEMPLATE, "androidx.core.app.NotificationCompat.Builder")

        notification.extras = extras
        `when`(sbn.notification).thenReturn(notification)
        return sbn
    }

    @Test
    fun onNotificationRemoved_whenNotExcluded_processesRemoval() = runTest {
        service.serviceScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        isReadyFlow.value = true

        val sbn = createMockSbn("test_key", "com.test.app")
        `when`(settingsRepository.isExcluded("com.test.app")).thenReturn(false)

        service.onNotificationRemoved(sbn)
        advanceUntilIdle()

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
        service.serviceScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        isReadyFlow.value = true

        val sbn = createMockSbn("test_key", "com.test.app")
        `when`(settingsRepository.isExcluded("com.test.app")).thenReturn(true)

        service.onNotificationRemoved(sbn)
        advanceUntilIdle()

        verify(activeReplyMap).remove("test_key")
        verifyNoInteractions(notificationProcessor)
    }

    @Test
    fun onNotificationPosted_whenNotReady_buffersAndProcessesLater_notExcluded() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        service.serviceScope = TestScope(testDispatcher)
        isReadyFlow.value = false // Not ready initially

        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            settingsRepository.isReadyFlow.collect { isReady ->
                if (isReady) {
                    val method = NotificationListenerServiceImpl::class.java.getDeclaredMethod("flushBuffer")
                    method.isAccessible = true
                    method.invoke(service)
                }
            }
        }

        val sbn = createMockSbn("buffer_key", "com.test.app")
        `when`(settingsRepository.isExcluded("com.test.app")).thenReturn(false)

        service.onNotificationPosted(sbn)
        advanceUntilIdle()

        verifyNoInteractions(notificationProcessor)

        isReadyFlow.value = true
        advanceUntilIdle()

        verify(notificationProcessor).processNotification((any() ?: NotificationEvent("", "", 0, null, 0L, "", "", null, null, false, false, null, "", emptyList(), false)))

        job.cancel()
    }

    @Test
    fun onNotificationPosted_whenNotReady_buffersAndDiscardsLater_excluded() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        service.serviceScope = TestScope(testDispatcher)
        isReadyFlow.value = false

        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            settingsRepository.isReadyFlow.collect { isReady ->
                if (isReady) {
                    val method = NotificationListenerServiceImpl::class.java.getDeclaredMethod("flushBuffer")
                    method.isAccessible = true
                    method.invoke(service)
                }
            }
        }

        val sbn = createMockSbn("buffer_key", "com.excluded.app")
        `when`(settingsRepository.isExcluded("com.excluded.app")).thenReturn(true)

        service.onNotificationPosted(sbn)
        advanceUntilIdle()

        isReadyFlow.value = true
        advanceUntilIdle()

        verifyNoInteractions(notificationProcessor)
        job.cancel()
    }
}
