package dev.tcc.chat.data.respository

import dev.tcc.chat.data.local.dao.MessageDao
import dev.tcc.chat.data.local.entity.MessageEntity
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.utililty.Base64Util
import dev.tcc.chat.utililty.CryptoManager
import dev.tcc.chat.utililty.EncryptedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val cryptoManager: CryptoManager
) {

    /**
     * Inserts a new message into the database.
     * The message content is encrypted before storage.
     */
    suspend fun insertMessage(message: Message): Long {
        val encrypted = cryptoManager.encrypt(message.content)

        val entity = MessageEntity(
            id = message.id,
            encryptedContent = Base64Util.encode(encrypted.ciphertext),
            iv = Base64Util.encode(encrypted.iv),
            timestamp = message.timestamp,
            isSent = message.isSent
        )

        return messageDao.insertMessage(entity)
    }

    /**
     * Gets all messages as a Flow.
     * Messages are decrypted on-the-fly as they're emitted.
     */
    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                decryptEntity(entity)
            }
        }
    }

    /**
     * Gets the total message count.
     */
    suspend fun getMessageCount(): Int {
        return messageDao.getMessageCount()
    }

    /**
     * Deletes all messages.
     */
    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    /**
     * Inserts multiple messages in bulk.
     * Each message is encrypted before storage.
     */
    suspend fun insertMessages(messages: List<Message>) {
        val entities = messages.map { message ->
            val encrypted = cryptoManager.encrypt(message.content)
            MessageEntity(
                id = message.id,
                encryptedContent = Base64Util.encode(encrypted.ciphertext),
                iv = Base64Util.encode(encrypted.iv),
                timestamp = message.timestamp,
                isSent = message.isSent
            )
        }
        messageDao.insertMessages(entities)
    }

    /**
     * Decrypts a MessageEntity to a Message.
     */
    private fun decryptEntity(entity: MessageEntity): Message {
        val encryptedData = EncryptedData(
            ciphertext = Base64Util.decode(entity.encryptedContent),
            iv = Base64Util.decode(entity.iv)
        )

        val decryptedContent = cryptoManager.decrypt(encryptedData)

        return Message(
            id = entity.id,
            content = decryptedContent,
            timestamp = entity.timestamp,
            isSent = entity.isSent
        )
    }
}

