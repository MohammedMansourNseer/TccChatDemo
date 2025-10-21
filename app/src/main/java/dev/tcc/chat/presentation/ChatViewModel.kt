package dev.tcc.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tcc.chat.domain.usecase.GetMessageCountUseCase
import dev.tcc.chat.domain.usecase.InsertLargeDatasetUseCase
import dev.tcc.chat.domain.usecase.ObserveMessagesUseCase
import dev.tcc.chat.domain.usecase.SendMessageUseCase
import dev.tcc.chat.domain.usecase.SimulateReplyUseCase
import dev.tcc.chat.presentation.chat.contract.ChatIntent
import dev.tcc.chat.presentation.chat.contract.ChatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val simulateReplyUseCase: SimulateReplyUseCase,
    private val insertLargeDatasetUseCase: InsertLargeDatasetUseCase,
    private val getMessageCountUseCase: GetMessageCountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        handleIntent(ChatIntent.LoadMessages)
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.LoadMessages -> loadMessages()
            is ChatIntent.InsertLargeDataset -> insertLargeDataset(intent.count)
            is ChatIntent.UpdateInputText -> updateInputText(intent.text)
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
            simulateReplyUseCase()
                .onFailure { error ->
                    _state.update {
                        it.copy(error = "Failed to simulate reply: ${error.message}")
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
            _state.update { it.copy(isLoading = true, error = null) }

            insertLargeDatasetUseCase(count)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to insert dataset: ${error.message}"
                        )
                    }
                }
        }
    }

    private fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }
}