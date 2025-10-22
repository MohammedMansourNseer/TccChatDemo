package dev.tcc.chat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "messages", indices = [Index("timestamp")])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedContent: String,
    val iv: String,
    val timestamp: Long,
    val isSent: Boolean
)

