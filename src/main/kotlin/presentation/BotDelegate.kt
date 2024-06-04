package presentation

import dev.inmo.tgbotapi.abstracts.FromUser
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.chatIdOrNull
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.utils.extensions.makeString
import domain.MessageSaveUseCase
import domain.model.ChatId
import domain.model.TelegramMessage
import domain.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BotDelegate(
    val bot: TelegramBot,
    val saveMessage: MessageSaveUseCase,
) {

    private var isStarted = false

    suspend fun doSomeBotStuff() {
        val scope = CoroutineScope(Dispatchers.Default)
        bot.sendMessage(dev.inmo.tgbotapi.types.ChatId(183036749), "Hello World, I'm back")
        bot.buildBehaviourWithLongPolling(scope) {
            onContentMessage { message ->
                doSomeStuffWithMessage(message)
            }

            val me = getMe()

            onCommand("start", requireOnlyCommandInMessage = true) {
                println("Start command, text = $me")
                reply(it, "Hello, I am ${me.firstName}")
            }

            onCommand("history", requireOnlyCommandInMessage = false) {
                println("History command, text = $me")
                val stringBuilder = StringBuilder()
//                messages.forEach {
//                    stringBuilder.append("\n")
//                    stringBuilder.append(it)
//                }
                reply(it, "Here are all messages I saved yet:\n$stringBuilder")
            }

            onUnhandledCommand {
                println("Unknown command, text = $me")
                reply(it, "I can do nothing for you, sorry.")
            }
            println(me)
        }.join()
    }

    suspend fun doSomeStuffWithMessage(message: CommonMessage<MessageContent>) {
        bot.run {
            val botCallsDelegate = BotCallsDelegate(this)

            message.content.asTextContent()?.let { textContent ->
                message.asFromUser()?.from?.firstName
                save(message.chat.id, message.asFromUser(), textContent.textSources)

                val messageText = textContent.text

                if (botCallsDelegate.containsKeywords(messageText, BotCallsDelegate.botCalledRegexes)) {
                    botCallsDelegate.doWhenCalled(message, messageText)
                }

                if (messageText.contains(Regex("блять"))) {
                    reply(message, chooseRandomAnswer(offenceTextes))
                }
            }
        }
    }

    suspend fun save(chatId: ChatIdentifier, user: FromUser?, textSources: List<TextSource>) {
        val text = textSources.makeString()

        saveMessage(
            TelegramMessage(
                ChatId(chatId.chatIdOrNull()?.chatId ?: 0),
                UserId(user?.from?.id?.chatId ?: 0),
                text,
            ),
        )
        println("${user?.from?.firstName}: $text")
    }

    companion object {
        val offenceTextes = listOf(
            "Сам такой.",
            "Соси жопу.",
            "Вот ты хрен",
            "Собака сутулая.",
            "Пёс.",
            "Сам дурак.",
            "Вот ты чудовище, конечно",
            "Кончились идеи, пока.",
        )

        val confirmationTextes = listOf(
            "Понял.",
            "Окей.",
            "Океюшки.",
            "Сделаю.",
            "Ладно.",
            "Лады.",
            "Ладушки-оладушки.",
            "Будем.",
        )

        val meTextes = listOf(
            "Эт я.",
            "Это тоже я.",
            "И тут я.",
            "Я такой, да.",
            "Я, конечно.",
            "Ну, я.",
            "Шо надо.",
            "М?",
        )

        var nextAnswerNumber = 0

        fun chooseRandomAnswer(options: List<String>): String {
            return if (nextAnswerNumber < options.size) {
                options[nextAnswerNumber++]
            } else {
                nextAnswerNumber = 0
                options[nextAnswerNumber]
            }
        }
    }
}