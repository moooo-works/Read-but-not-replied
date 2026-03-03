package com.mori.notireplyassistant.feature.inbox

import com.mori.notireplyassistant.core.domain.model.ConversationUiModel
import com.mori.notireplyassistant.core.repository.ConversationFilter
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class InboxViewModelTest {

    @Mock
    private lateinit var repository: NotificationRepository

    private lateinit var viewModel: InboxViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        Mockito.doReturn(flowOf(emptyList<ConversationUiModel>()))
            .`when`(repository).observeConversations(ConversationFilter.ALL)

        Mockito.doReturn(flowOf(emptyList<ConversationUiModel>()))
            .`when`(repository).observeConversations(ConversationFilter.ARCHIVED)

        viewModel = InboxViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isAllFilter() = runTest(testDispatcher) {
        // Collect to trigger SharingStarted.WhileSubscribed
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        verify(repository).observeConversations(ConversationFilter.ALL)
        job.cancel()
    }

    @Test
    fun setTab_switchesFilter() = runTest(testDispatcher) {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onTabSelected(1) // Archived
        advanceUntilIdle()

        verify(repository).observeConversations(ConversationFilter.ARCHIVED)
        job.cancel()
    }

    @Test
    fun markHandled_callsRepository() = runTest(testDispatcher) {
        viewModel.onMarkHandled("id1")
        advanceUntilIdle()

        verify(repository).markConversationHandled("id1")
    }
}
