package dev.tcc.chat.domain.usecase

import dev.tcc.chat.data.respository.MessageRepository
import javax.inject.Inject

class DeleteAllMessagesUseCase @Inject constructor(
    private val repo: MessageRepository
) {
    suspend operator fun invoke() = repo.deleteAllMessages()
}