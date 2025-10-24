package dev.tcc.chat.domain.usecase

import dev.tcc.chat.domain.repository.MessageRepository
import dev.tcc.chat.domain.model.Message
import javax.inject.Inject


class InsertLargeDatasetUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(
        count: Int = 1000,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Unit> {
        return try {
            val messages = mutableListOf<Message>()

            val existingCount = repository.getMessageCount()
            val startNumber = (existingCount / 2) + 1

            val now = System.currentTimeMillis()
            val maxTs = repository.getMaxTimestamp()
            var baseTs = maxOf(now, maxTs)
            val step = 1L

            for (i in 1..count) {
                val isSent = i % 2 == 1
                val messageNumber = startNumber + ((i - 1) / 2)
                val content = if (isSent) "Test message $messageNumber" else "ok"
                baseTs += step
                messages.add(
                    Message(
                        content = content,
                        timestamp = baseTs,
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

