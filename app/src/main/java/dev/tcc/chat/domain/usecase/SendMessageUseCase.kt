package dev.tcc.chat.domain.usecase

import dev.tcc.chat.domain.repository.MessageRepository
import dev.tcc.chat.domain.model.Message
import javax.inject.Inject


class SendMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(text: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val maxTimestamp = repository.getMaxTimestamp()
        val ts = maxOf(now, maxTimestamp + 1)

        val message = Message(
            content = text.trim(),
            timestamp = ts,
            isSent = true
        )
        repository.insertMessage(message)
    }
}


