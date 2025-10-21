package dev.tcc.chat.data.respository

import dev.tcc.chat.data.local.dao.MessageDao
import dev.tcc.chat.data.local.entity.MessageEntity
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.utililty.Base64Util
import dev.tcc.chat.utililty.CryptoManager
import dev.tcc.chat.utililty.EncryptedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val cryptoManager: CryptoManager
) {

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

    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                decryptEntity(entity)
            }
        }
    }

    suspend fun getMessageCount(): Int {
        return messageDao.getMessageCount()
    }

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
