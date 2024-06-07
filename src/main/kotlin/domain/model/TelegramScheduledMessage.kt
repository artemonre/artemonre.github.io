package domain.model

import java.time.LocalDateTime

data class TelegramScheduledMessage(
    val messageId: Long,
    val chatId: ChatId,
    val usersToTag: List<TelegramUser>,
    val text: String,
    val scheduledDateTime: LocalDateTime
)