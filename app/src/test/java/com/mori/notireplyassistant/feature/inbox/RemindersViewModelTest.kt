package com.mori.notireplyassistant.feature.inbox

import com.mori.notireplyassistant.core.domain.model.ReminderUiModel
import com.mori.notireplyassistant.core.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
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
class RemindersViewModelTest {

    @Mock
    private lateinit var repository: NotificationRepository

    private lateinit var viewModel: RemindersViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        Mockito.doReturn(flowOf(emptyList<ReminderUiModel>()))
            .`when`(repository).observeActiveReminders()

        viewModel = RemindersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onDismiss_callsRepository() = runTest(testDispatcher) {
        viewModel.onDismiss(1L)
        advanceUntilIdle()
        verify(repository).dismissReminder(1L)
    }

    @Test
    fun onSnooze_callsRepository() = runTest(testDispatcher) {
        viewModel.onSnooze(2L)
        advanceUntilIdle()
        verify(repository).snoozeReminder(2L)
    }
}
