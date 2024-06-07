package domain.model

import java.time.OffsetDateTime

data class TelegramScheduledMessage(
    val messageId: Long,
    val chatId: ChatId,
    val usersToTag: List<TelegramUser>,
    val text: String,
    val scheduledDateTime: OffsetDateTime
)