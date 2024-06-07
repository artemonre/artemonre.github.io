package domain.usecase

import domain.repository.MessagesRepository
import domain.model.TelegramScheduledMessage

@JvmInline
value class ScheduledMessageSaveUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(
        message: TelegramScheduledMessage
    ): Boolean = repository.saveScheduledMessage(message)
}