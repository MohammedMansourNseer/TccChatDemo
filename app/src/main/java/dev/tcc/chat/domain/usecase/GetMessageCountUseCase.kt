package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import javax.inject.Inject

class GetMessageCountUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(): Int {
        return repository.getMessageCount()
    }
}

