package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(content: String): Result<Long> {
        return try {
            val message = Message(
                content = content,
                timestamp = System.currentTimeMillis(),
                isSent = true
            )
            val id = repository.insertMessage(message)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

