package domain.repository

import domain.model.TelegramMessage
import domain.model.TelegramScheduledMessage
import domain.usecase.ScheduledMessagesFilterTypes

interface MessagesRepository {
    suspend fun saveMessage(message: TelegramMessage)
    suspend fun saveScheduledMessage(message: TelegramScheduledMessage): Boolean
    suspend fun getMessages(count: Int): List<TelegramMessage>
    suspend fun getScheduledMessages(filterType: ScheduledMessagesFilterTypes): List<TelegramScheduledMessage>
    suspend fun deleteScheduledMessages(messagesIds: List<Long>)
}