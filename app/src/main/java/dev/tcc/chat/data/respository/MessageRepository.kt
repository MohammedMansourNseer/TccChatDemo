package dev.tcc.chat.data.respository

import androidx.collection.LruCache
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.tcc.chat.data.local.dao.MessageDao
import dev.tcc.chat.data.local.entity.MessageEntity
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.utililty.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val cryptoEngine: CryptoEngine
) {

    private val decryptCache = LruCache<Long, String>(500)

    suspend fun getMaxTimestamp(): Long = withContext(Dispatchers.IO) {
        messageDao.getMaxTimestamp()
    }

    suspend fun insertMessage(message: Message): Long {
        val (ctB64, ivB64) = cryptoEngine.encryptToBase64(message.content)

        val entity = MessageEntity(
            id = message.id,
            encryptedContent = ctB64,
            iv = ivB64,
            timestamp = message.timestamp,
            isSent = message.isSent
        )

        return withContext(Dispatchers.IO) {
            messageDao.insertMessage(entity)
        }
    }

    fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages()
            .map { entities -> entities.map { decryptEntityCached(it) } }
            .flowOn(Dispatchers.Default)
    }

    fun getMessagesPaged(pageSize: Int = 20): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2,
                prefetchDistance = pageSize / 2,
                maxSize = pageSize * 10
            ),
            pagingSourceFactory = { messageDao.getMessagesPaged() }
        ).flow
            .map { paging -> paging.map { entity -> decryptEntityCached(entity) } }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getMessageCount()
    }

    suspend fun deleteAllMessages() = withContext(Dispatchers.IO) {
        messageDao.deleteAllMessages()
        decryptCache.evictAll()
    }

    suspend fun insertMessages(
        messages: List<Message>,
        onProgress: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val totalCount = messages.size
        var completedCount = 0

        val batchSize = 50
        val limited = Dispatchers.Default.limitedParallelism(2)

        messages.chunked(batchSize).forEach { batch ->
            val entities = coroutineScope {
                batch.map { msg ->
                    async(limited) {
                        val (ctB64, ivB64) = cryptoEngine.encryptToBase64(msg.content)
                        MessageEntity(
                            id = msg.id,
                            encryptedContent = ctB64,
                            iv = ivB64,
                            timestamp = msg.timestamp,
                            isSent = msg.isSent
                        )
                    }
                }.awaitAll()
            }

            messageDao.insertMessages(entities)

            completedCount += batch.size
            onProgress?.invoke((completedCount * 100) / totalCount)
        }
    }

    private fun decryptEntityCached(entity: MessageEntity): Message {
        decryptCache[entity.id]?.let { cached ->
            return Message(
                id = entity.id,
                content = cached,
                timestamp = entity.timestamp,
                isSent = entity.isSent
            )
        }

        val encryptedData = CryptoEngine.EncryptedData(
            ciphertext = android.util.Base64.decode(entity.encryptedContent, android.util.Base64.NO_WRAP),
            iv = android.util.Base64.decode(entity.iv, android.util.Base64.NO_WRAP)
        )

        val decryptedContent = cryptoEngine.decrypt(encryptedData)
        decryptCache.put(entity.id, decryptedContent)

        return Message(
            id = entity.id,
            content = decryptedContent,
            timestamp = entity.timestamp,
            isSent = entity.isSent
        )
    }
}
