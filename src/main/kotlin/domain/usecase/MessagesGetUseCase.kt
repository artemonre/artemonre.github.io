package domain.usecase

import domain.repository.MessagesRepository
import domain.model.TelegramMessage

@JvmInline
value class MessagesGetUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(count: Int): List<TelegramMessage> = repository.getMessages(count)
}