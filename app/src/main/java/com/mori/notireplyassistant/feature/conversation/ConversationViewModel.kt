package com.mori.notireplyassistant.feature.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.notireplyassistant.core.domain.handler.ReplyHandler
import com.mori.notireplyassistant.core.domain.handler.ReplyResult
import com.mori.notireplyassistant.core.domain.model.MessageUiModel
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

sealed interface ConversationUiState {
    data object Loading : ConversationUiState
    data class Success(val messages: List<MessageUiModel>) : ConversationUiState
    data object Empty : ConversationUiState
}

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
    private val replyHandler: ReplyHandler
) : ViewModel() {

    private val conversationId: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["conversationId"]),
        StandardCharsets.UTF_8.toString()
    )

    private val packageName: String = conversationId.substringBefore("|")

    val uiState: StateFlow<ConversationUiState> = repository.observeMessages(conversationId)
        .map { messages ->
            if (messages.isEmpty()) ConversationUiState.Empty else ConversationUiState.Success(messages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConversationUiState.Loading
        )

    private val _replyText = MutableStateFlow("")
    val replyText: StateFlow<String> = _replyText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun onReplyTextChanged(text: String) {
        _replyText.value = text
    }

    fun onMarkHandled() {
        viewModelScope.launch {
            repository.markConversationHandled(conversationId)
        }
    }

    fun onScheduleReminder(delayMillis: Long) {
        viewModelScope.launch {
            val scheduledTime = System.currentTimeMillis() + delayMillis
            repository.createReminder(conversationId, scheduledTime, null)
            _snackbarEvent.emit("Reminder scheduled")
        }
    }

    fun onSendReply() {
        val text = _replyText.value
        if (text.isBlank() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true

            val result = replyHandler.sendReply(conversationId, packageName, text)

            _isSending.value = false

            when (result) {
                ReplyResult.SUCCESS -> {
                    _replyText.value = "" // Clear on success
                    _snackbarEvent.emit("Reply sent")
                }
                ReplyResult.FALLBACK_OPENED -> {
                    _replyText.value = "" // Clear if fallback app opened
                    _snackbarEvent.emit("Opened app to reply")
                }
                ReplyResult.NOT_AVAILABLE -> {
                    _snackbarEvent.emit("Reply not available")
                }
                ReplyResult.FAILED -> {
                    _snackbarEvent.emit("Failed to send reply")
                }
            }
        }
    }
}
