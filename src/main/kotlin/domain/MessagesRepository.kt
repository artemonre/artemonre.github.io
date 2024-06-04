package domain

import domain.model.TelegramMessage

interface MessagesRepository {
    suspend fun saveMessage(message: TelegramMessage)
}