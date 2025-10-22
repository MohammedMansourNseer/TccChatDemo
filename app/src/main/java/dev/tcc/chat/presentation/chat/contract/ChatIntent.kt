package dev.tcc.chat.presentation.chat.contract

import dev.tcc.chat.utililty.Constant.ADD_BULK_DATA

sealed interface ChatIntent {
    data class SendMessage(val text: String) : ChatIntent
    data object LoadMessages : ChatIntent
    data class InsertLargeDataset(val count: Int = ADD_BULK_DATA) : ChatIntent
    data class UpdateInputText(val text: String) : ChatIntent
    data object ClearAllMessages : ChatIntent

}