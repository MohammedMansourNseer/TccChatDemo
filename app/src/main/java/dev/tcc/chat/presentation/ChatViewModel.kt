package dev.tcc.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.chat.domain.usecase.ObserveMessagesPagedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.domain.usecase.*
import dev.tcc.chat.presentation.chat.contract.ChatIntent
import dev.tcc.chat.presentation.chat.contract.ChatState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val observeMessagesPagedUseCase: ObserveMessagesPagedUseCase,
    private val simulateReplyUseCase: SimulateReplyUseCase,
    private val insertLargeDatasetUseCase: InsertLargeDatasetUseCase,
    private val getMessageCountUseCase: GetMessageCountUseCase,
    private val deleteAllMessagesUseCase: DeleteAllMessagesUseCase,
    private val repository: MessageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()


    val messagesPaged: Flow<PagingData<Message>> = observeMessagesPagedUseCase(pageSize = 100)
        .cachedIn(viewModelScope)

    init {
        handleIntent(ChatIntent.LoadMessages)
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.LoadMessages -> loadMessages()
            is ChatIntent.InsertLargeDataset -> insertLargeDataset(intent.count)
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
            is ChatIntent.ClearAllMessages -> clearAllMessages()
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }

            sendMessageUseCase(text)
                .onSuccess {
                    _state.update { it.copy(inputText = "", isSending = false) }
                    simulateAutoReply()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = "Failed to send message: ${error.message}"
                        )
                    }
                }
        }
    }


    private fun simulateAutoReply() {
        viewModelScope.launch {
            _state.update { it.copy(isOtherPersonTyping = true) }

            simulateReplyUseCase()
                .onSuccess {
                    _state.update { it.copy(isOtherPersonTyping = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isOtherPersonTyping = false,
                            error = "Failed to simulate reply: ${error.message}"
                        )
                    }
                }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            observeMessagesUseCase()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load messages: ${error.message}"
                        )
                    }
                }
                .collect { messages ->
                    val count = getMessageCountUseCase()
                    _state.update {
                        it.copy(
                            messages = messages,
                            messageCount = count,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun insertLargeDataset(count: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, insertProgress = 0, error = null) }

            withContext(Dispatchers.IO) {
                insertLargeDatasetUseCase(count) { progress ->
                    _state.update { it.copy(insertProgress = progress) }
                }
            }
                .onSuccess {
                    _state.update { it.copy(isLoading = false, insertProgress = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            insertProgress = null,
                            error = "Failed to insert dataset: ${error.message}"
                        )
                    }
                }
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    private fun clearAllMessages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                deleteAllMessagesUseCase()
            }.onSuccess {
                _state.update {
                    it.copy(
                        isLoading = false,
                        inputText = "",
                        messageCount = 0,
                        messages = emptyList(),
                        insertProgress = null,
                        error = null
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to clear") }
            }
        }
    }
}