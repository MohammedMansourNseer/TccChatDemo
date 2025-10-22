package dev.tcc.chat.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.tcc.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC, id DESC")
    fun getMessagesPaged(): PagingSource<Int, MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM messages")
    suspend fun getMaxTimestamp(): Long
}

