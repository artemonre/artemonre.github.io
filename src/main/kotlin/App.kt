import config.Config
import data.MessagesDefaultRepository
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import domain.usecase.MessageSaveUseCase
import kotlinx.serialization.json.Json
import presentation.MainBotClass
import java.io.File

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {
    val json = Json { ignoreUnknownKeys = true }
    val config: Config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val bot = telegramBot(config.token)/*  {
        client = HttpClient(OkHttp) {
            config.client?.apply {
                setupConfig()
            }
        }
    } */

    MainBotClass(bot, MessageSaveUseCase(MessagesDefaultRepository())).doSomeBotStuff()
}

const val version = "0.1"