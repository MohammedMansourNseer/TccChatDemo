package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import kotlinx.coroutines.delay
import javax.inject.Inject

class SimulateReplyUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(delayMillis: Long = 1500L): Result<Long> {
        return try {
            delay(delayMillis)
            
            val message = Message(
                content = "ok",
                timestamp = System.currentTimeMillis(),
                isSent = false
            )
            
            val id = repository.insertMessage(message)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

