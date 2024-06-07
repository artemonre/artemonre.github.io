package presentation

import data.MessagesDefaultRepository
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import domain.model.TelegramScheduledMessage
import domain.usecase.ScheduledMessageSaveUseCase
import domain.usecase.ScheduledMessagesDeleteUseCase
import domain.usecase.ScheduledMessagesGetUseCase

class BotCallsDelegate(private val bot: TelegramBot) : Reminder.ScheduledMessagesSender {

    private val reminder by lazy {
        val messagesRepository = MessagesDefaultRepository()
        Reminder(
            scheduledMessagesSender = this,
            ScheduledMessageSaveUseCase(messagesRepository),
            ScheduledMessagesGetUseCase(messagesRepository),
            ScheduledMessagesDeleteUseCase(messagesRepository)
        )
    }

    suspend fun initialize() {
        if (!reminder.stillAlive) {
            reminder.startReminder()
        }
    }

    override suspend fun sendScheduledMessage(message: TelegramScheduledMessage) {
        val messageText = """
            ${message.usersToTag.map { it.nickName }.joinToString(" ")}
            ${message.text}
        """.trimIndent()
        bot.sendMessage(ChatId(message.chatId.id), messageText)
    }

    suspend fun doWhenCalled(message: CommonMessage<MessageContent>, messageText: String) {
        if (containsKeywords(messageText, reminderAskedRegexes)) {
            println("contains reminder asking")

            val patternWordsList = messageText
                .split(" ")
                .map {
                    println("check $it for replace")
                    val newWord = it.replace(redundantSymbolsPatternRegex, "")
                    newWord
                }
                .filterNot {
                    println("check $it for filter")
                    containsKeywords(it, botCalledRegexes) || containsKeywords(it, reminderAskedRegexes)
                }

            println("doWhenCalled, patternWordsList = $patternWordsList")

            val answer = if (reminder.scheduleMessage(message, patternWordsList)) {
                BotDelegate.chooseRandomAnswer(BotDelegate.confirmationTextes)
            } else {
                "Ничем не могу помочь совсем."
            }
            bot.reply(
                message,
                answer
            )
        } else {
            bot.reply(message, BotDelegate.chooseRandomAnswer(BotDelegate.meTextes))
        }
    }

    fun containsKeywords(message: String, keywords: List<Regex>): Boolean {
        val messageLowerCase = message.lowercase()
        var foundKeywords = false

        keywords.forEach {
            if (messageLowerCase.contains(it)) {
                foundKeywords = true
                return@forEach
            }
        }

        return foundKeywords
    }

    fun containsDate(maybeDateString: String): Boolean {
        return maybeDateString.contains(Reminder.dateLongPatternRegex) ||
                maybeDateString.contains(Reminder.dateShortPatternRegex)
    }

    companion object {
        val nonWordChar = "[^а-яА-Я0-9a-zA-Z]"
        val nonWordCharOrStart = "($nonWordChar|^)"
        val nonWordCharOrEnd = "($nonWordChar|$)"
        val redundantSymbolsPatternRegex = "(^$nonWordChar+)|($nonWordChar+$)".toRegex()

        val botCalledRegexes = listOf(
            "${nonWordCharOrStart}бот$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}ботя$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}ботик$nonWordCharOrEnd".toRegex(),
        )
        val reminderAskedRegexes = listOf(
            "${nonWordCharOrStart}напоминалка$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}напоминалку$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}напомни$nonWordCharOrEnd".toRegex(),
        )
    }
}