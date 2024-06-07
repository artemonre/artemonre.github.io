package domain.model

import java.time.LocalDateTime

data class TelegramMessage(
    val chatId: ChatId,
    val userId: UserId,
    val text: String,
    val messageDateTimeMillis: LocalDateTime
)