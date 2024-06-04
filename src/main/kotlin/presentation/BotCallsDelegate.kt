package presentation

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import nonWordCharOrEnd
import nonWordCharOrStart

class BotCallsDelegate(private val bot: TelegramBot) {

    suspend fun doWhenCalled(message: CommonMessage<MessageContent>, messageText: String) {
        if (containsKeywords(messageText, reminderAskedRegexes)) {
            println("contains reminder asking")

            Reminder().setReminder(messageText)
            bot.reply(
                message,
                BotDelegate.chooseRandomAnswer(BotDelegate.confirmationTextes),
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
        val botCalledRegexes = listOf(
            "${nonWordCharOrStart}бот$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}ботя$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}ботик$nonWordCharOrEnd".toRegex()
        )
        val reminderAskedRegexes = listOf(
            "${nonWordCharOrStart}напоминалка$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}напоминалку$nonWordCharOrEnd".toRegex(),
            "${nonWordCharOrStart}напомни$nonWordCharOrEnd".toRegex(),
        )
    }
}