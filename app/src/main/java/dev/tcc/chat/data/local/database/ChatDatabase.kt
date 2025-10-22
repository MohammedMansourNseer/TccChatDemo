package dev.tcc.chat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.tcc.chat.data.local.dao.MessageDao
import dev.tcc.chat.data.local.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "chat_database"
    }
}