package domain

import domain.model.TelegramMessage

@JvmInline
value class MessageSaveUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(message: TelegramMessage) {
        repository.saveMessage(message)
    }
}