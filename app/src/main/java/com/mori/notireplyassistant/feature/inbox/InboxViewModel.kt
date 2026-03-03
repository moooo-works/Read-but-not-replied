package com.mori.notireplyassistant.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.domain.model.ConversationUiModel
import com.mori.notireplyassistant.core.repository.ConversationFilter
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InboxUiState {
    data object Loading : InboxUiState
    data class Success(val conversations: List<ConversationUiModel>) : InboxUiState
    data object Empty : InboxUiState
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InboxUiState> = _selectedTab
        .flatMapLatest { tabIndex ->
            val filter = when (tabIndex) {
                0 -> ConversationFilter.ALL
                1 -> ConversationFilter.ARCHIVED
                else -> ConversationFilter.ALL
            }
            repository.observeConversations(filter)
        }
        .map { conversations ->
            if (conversations.isEmpty()) {
                InboxUiState.Empty
            } else {
                InboxUiState.Success(conversations)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InboxUiState.Loading
        )

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun onMarkHandled(conversationId: String) {
        viewModelScope.launch {
            repository.markConversationHandled(conversationId)
        }
    }

    fun onArchive(conversationId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(conversationId, archived)
        }
    }

    fun onPin(conversationId: String, pinned: Boolean) {
        viewModelScope.launch {
            repository.setPinned(conversationId, pinned)
        }
    }
}
