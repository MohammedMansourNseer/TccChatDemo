package dev.tcc.chat.domain.repository

import androidx.paging.PagingData
import dev.tcc.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getMaxTimestamp(): Long
    suspend fun insertMessage(message: Message): Long
    fun getAllMessages(): Flow<List<Message>>
    fun getMessagesPaged(pageSize: Int = 20): Flow<PagingData<Message>>
    suspend fun getMessageCount(): Int
    suspend fun deleteAllMessages()
    suspend fun insertMessages(messages: List<Message>, onProgress: ((Int) -> Unit)? = null)
}