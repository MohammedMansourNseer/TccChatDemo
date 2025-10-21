package dev.tcc.chat.presentation.chat.contract

sealed interface ChatIntent {
    data class SendMessage(val text: String) : ChatIntent
    data object LoadMessages : ChatIntent
    data class InsertLargeDataset(val count: Int = 1000) : ChatIntent
    data class UpdateInputText(val text: String) : ChatIntent
}