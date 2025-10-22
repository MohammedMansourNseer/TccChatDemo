package com.chat.domain.usecase

import androidx.paging.PagingData
import dev.tcc.chat.data.respository.MessageRepository
import dev.tcc.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class ObserveMessagesPagedUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(pageSize: Int = 20): Flow<PagingData<Message>> {
        return repository.getMessagesPaged(pageSize)
    }
}

