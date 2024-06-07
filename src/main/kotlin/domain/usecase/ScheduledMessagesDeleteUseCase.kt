package domain.usecase

import domain.repository.MessagesRepository

@JvmInline
value class ScheduledMessagesDeleteUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(messagesIds: List<Long>) {
        repository.deleteScheduledMessages(messagesIds)
    }
}