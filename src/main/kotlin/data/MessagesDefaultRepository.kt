package data

import domain.repository.MessagesRepository
import domain.model.TelegramMessage
import domain.model.TelegramScheduledMessage
import domain.usecase.ScheduledMessagesFilterTypes
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.LinkedList

class MessagesDefaultRepository : MessagesRepository {

    private val messages = mutableListOf<TelegramMessage>()
    private val scheduledMessages = LinkedList<TelegramScheduledMessage>()

    override suspend fun saveMessage(message: TelegramMessage) {
        messages.add(message)
    }

    override suspend fun saveScheduledMessage(message: TelegramScheduledMessage): Boolean {
        val copiedMessage = message.copy(messageId = scheduledMessages.size.toLong())
        scheduledMessages.forEachIndexed { index, telegramScheduledMessage ->
            println(
                "current message time = ${copiedMessage.scheduledDateTime}, saved message time = ${telegramScheduledMessage.scheduledDateTime}"
            )
            if (copiedMessage.scheduledDateTime.isBefore(telegramScheduledMessage.scheduledDateTime)) {
                scheduledMessages.add(index, copiedMessage)
                return true
            }

            if (index == scheduledMessages.lastIndex) {
                return scheduledMessages.add(copiedMessage)
            }
        }

        return if (scheduledMessages.isEmpty()) {
            scheduledMessages.add(copiedMessage)
        } else {
            false
        }
    }

    override suspend fun getMessages(count: Int) = messages.subList(0, count)

    override suspend fun getScheduledMessages(
        filterType: ScheduledMessagesFilterTypes
    ): List<TelegramScheduledMessage> {
        val currentHour = OffsetDateTime.now(ZoneOffset.UTC).hour

        return when (filterType) {
            ScheduledMessagesFilterTypes.ALL -> scheduledMessages
            ScheduledMessagesFilterTypes.MONTH -> emptyList()
            ScheduledMessagesFilterTypes.WEEK -> emptyList()
            ScheduledMessagesFilterTypes.DAY -> emptyList()
//            TODO need to found better solution with early stop
            ScheduledMessagesFilterTypes.CURRENT_HOUR -> {
                scheduledMessages.filter {
                    it.scheduledDateTime.hour == currentHour
                }
            }
            ScheduledMessagesFilterTypes.NEXT_HOUR -> {
                scheduledMessages.filter {
                    it.scheduledDateTime.hour == currentHour + 1
                }
            }
            ScheduledMessagesFilterTypes.CLOSEST -> listOf(scheduledMessages.first)
        }
    }

    override suspend fun deleteScheduledMessages(messagesIds: List<Long>) {
        messagesIds.forEach { messageId ->
            scheduledMessages.removeIf { message ->
                messageId == message.messageId
            }
        }

        println("deleteScheduledMessages, scheduledMessages = $scheduledMessages")
    }
}