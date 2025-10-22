package dev.tcc.chat.presentation.chat.contract

import dev.tcc.chat.domain.model.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isOtherPersonTyping: Boolean = false,
    val error: String? = null,
    val messageCount: Int = 0,
    val insertProgress: Int? = null  // Progress for bulk insert (0-100)
)
