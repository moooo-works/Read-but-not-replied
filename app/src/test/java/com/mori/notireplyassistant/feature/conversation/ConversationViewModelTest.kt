package com.mori.notireplyassistant.feature.conversation

import androidx.lifecycle.SavedStateHandle
import com.mori.notireplyassistant.core.domain.handler.ReplyHandler
import com.mori.notireplyassistant.core.domain.handler.ReplyResult
import com.mori.notireplyassistant.core.domain.model.MessageUiModel
import com.mori.notireplyassistant.core.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.net.URLEncoder

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ConversationViewModelTest {

    @Mock
    private lateinit var repository: NotificationRepository
    @Mock
    private lateinit var replyHandler: ReplyHandler

    private lateinit var viewModel: ConversationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val encodedId = URLEncoder.encode("com.pkg|key", "UTF-8")
        val savedState = SavedStateHandle(mapOf("conversationId" to encodedId))

        Mockito.doReturn(flowOf(emptyList<MessageUiModel>()))
            .`when`(repository).observeMessages("com.pkg|key")

        viewModel = ConversationViewModel(savedState, repository, replyHandler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendReply_emptyText_doesNothing() = runTest(testDispatcher) {
        viewModel.onReplyTextChanged("   ")
        viewModel.onSendReply()
        advanceUntilIdle()

        Mockito.verifyNoInteractions(replyHandler)
    }

    @Test
    fun sendReply_success_clearsTextAndEmitsEvent() = runTest(testDispatcher) {
        Mockito.doReturn(ReplyResult.SUCCESS)
            .`when`(replyHandler).sendReply("com.pkg|key", "com.pkg", "Hello")

        val events = mutableListOf<String>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvent.collect { events.add(it) }
        }

        viewModel.onReplyTextChanged("Hello")
        viewModel.onSendReply()
        advanceUntilIdle()

        verify(replyHandler).sendReply("com.pkg|key", "com.pkg", "Hello")
        assertEquals("", viewModel.replyText.value)
        assertTrue(events.contains("Reply sent"))

        job.cancel()
    }
}
