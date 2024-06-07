package domain.usecase

import domain.repository.MessagesRepository
import domain.model.TelegramScheduledMessage

@JvmInline
value class ScheduledMessagesDeleteUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(messagesIds: List<Long>) {
        repository.deleteScheduledMessages(messagesIds)
    }
}