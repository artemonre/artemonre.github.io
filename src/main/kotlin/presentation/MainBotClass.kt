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
import domain.usecase.MessageSaveUseCase
import domain.model.ChatId
import domain.model.TelegramMessage
import domain.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime

class MainBotClass(
    private val bot: TelegramBot,
    private val saveMessage: MessageSaveUseCase
) {

    private var isStarted = false
    private val botCallsDelegate by lazy {
        BotCallsDelegate(bot)
    }

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
                reply(it, "Привет, меня зовут ${me.firstName}")
            }

            onCommand("history", requireOnlyCommandInMessage = false) {
                println("History command, text = $me")
                val stringBuilder = StringBuilder()
//                messages.forEach {
//                    stringBuilder.append("\n")
//                    stringBuilder.append(it)
//                }
                reply(it, "Вот список запрошенных сообщений:\n$stringBuilder")
            }

            onCommand("timezone_[0-9a-zA-Zа-яА-Я]+".toRegex(), requireOnlyCommandInMessage = true) {
                println("timezone command, text = $me")
                reply(it, "Поменял стандартный часовой пояс для напоминалок на ${it.content}")
            }

            onUnhandledCommand {
                println("Unknown command, text = $me")
                reply(it, "Ничем не могу помочь, сорян.")
            }
            println(me)
        }.join()
    }

    private suspend fun doSomeStuffWithMessage(message: CommonMessage<MessageContent>) {
        bot.run {
            botCallsDelegate.initialize()

            message.content.asTextContent()?.let { textContent ->
                save(message.chat.id, message.asFromUser(), textContent.textSources)

                val messageText = textContent.text

                if (
                    botCallsDelegate.containsKeywords(messageText, BotCallsDelegate.botCalledRegexes) ||
                    isPrivateChat(message)
                ) {
                    botCallsDelegate.doWhenCalled(message, messageText)
                }

                if (messageText.contains(Regex("блять"))) {
                    reply(message, chooseRandomAnswer(offenceTextes))
                }
            }
        }
    }

    private suspend fun save(chatId: ChatIdentifier, user: FromUser?, textSources: List<TextSource>) {
        val text = textSources.makeString()

        saveMessage(
            TelegramMessage(
                ChatId(chatId.chatIdOrNull()?.chatId ?: 0),
                UserId(user?.from?.id?.chatId ?: 0),
                text,
                LocalDateTime.now()
            ),
        )
        println("${user?.from?.firstName}: $text")
    }

    private fun isPrivateChat(
        message: CommonMessage<MessageContent>
    ) = message.chat.id.chatId == message.asFromUser()?.from?.id?.chatId

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