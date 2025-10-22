package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import javax.inject.Inject


class InsertLargeDatasetUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(
        count: Int = 200,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Unit> {
        return try {
            val messages = mutableListOf<Message>()
            
            val existingCount = repository.getMessageCount()
            val startNumber = (existingCount / 2) + 1

            val now = System.currentTimeMillis()
            val maxTs = repository.getMaxTimestamp()
            var baseTimestamp = maxOf(now, maxTs)
            

            for (i in 1..count) {
                val isSent = i % 2 == 1
                val messageNumber = startNumber + ((i - 1) / 2)
                val content = if (isSent) {
                    "Test message $messageNumber"
                } else {
                    "ok"
                }
                
                messages.add(
                    Message(
                        content = content,
                        timestamp = baseTimestamp + (existingCount + i) * 1000L,
                        isSent = isSent
                    )
                )
            }
            
            repository.insertMessages(messages, onProgress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

