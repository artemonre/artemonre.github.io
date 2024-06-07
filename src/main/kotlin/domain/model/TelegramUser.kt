package domain.model

data class TelegramUser(
    val id: UserId,
    val firstName: String?,
    val nickName: String?
)
