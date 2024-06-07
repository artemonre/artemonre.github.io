package domain.usecase

import domain.repository.MessagesRepository
import domain.model.TelegramScheduledMessage

@JvmInline
value class ScheduledMessagesGetUseCase(val repository: MessagesRepository) {

    suspend inline operator fun invoke(
        filterType: ScheduledMessagesFilterTypes = ScheduledMessagesFilterTypes.CURRENT_HOUR
    ): List<TelegramScheduledMessage> = repository.getScheduledMessages(filterType)
}

enum class ScheduledMessagesFilterTypes {
    ALL, MONTH, WEEK, DAY, CURRENT_HOUR, NEXT_HOUR, CLOSEST
}