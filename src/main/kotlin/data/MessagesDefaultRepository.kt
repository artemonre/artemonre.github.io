package data

import domain.MessagesRepository
import domain.model.TelegramMessage

class MessagesDefaultRepository : MessagesRepository {

    private val messages = mutableMapOf<Long, TelegramMessage>()

    override suspend fun saveMessage(message: TelegramMessage) {
        messages[System.currentTimeMillis()] = message
    }
}