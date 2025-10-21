package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import javax.inject.Inject
import kotlin.random.Random


class InsertLargeDatasetUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(count: Int = 1000): Result<Unit> {
        return try {
            val messages = mutableListOf<Message>()
            val currentTime = System.currentTimeMillis()
            
            // Generate messages with varied content and timestamps
            for (i in 1..count) {
                val isSent = Random.nextBoolean()
                val content = if (isSent) {
                    "Test message $i - ${Random.nextInt(0, 100)}"
                } else {
                    "ok"
                }
                
                messages.add(
                    Message(
                        content = content,
                        timestamp = currentTime - (count - i) * 60000L, // Space messages by 1 minute
                        isSent = isSent
                    )
                )
            }
            
            repository.insertMessages(messages)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

