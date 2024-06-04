package domain.model

data class TelegramMessage(
    val chatId: ChatId,
    val userId: UserId,
    val text: String
)