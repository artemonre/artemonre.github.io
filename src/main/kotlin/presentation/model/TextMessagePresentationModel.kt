package presentation.model

import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.UserId

data class TextMessagePresentationModel(
    val chatId: ChatIdentifier,
    val userId: UserId?,
    val text: String
)