package dev.tcc.chat.domain.usecase

import dev.tcc.chat.domain.repository.MessageRepository
import dev.tcc.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class ObserveMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(): Flow<List<Message>> {
        return repository.getAllMessages()
    }
}

