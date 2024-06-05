import config.Config
import data.MessagesDefaultRepository
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import domain.MessageSaveUseCase
import kotlinx.serialization.json.Json
import presentation.BotDelegate
import java.io.File

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {
    args.forEach {
        println("arg = $it")
    }
    val json = Json { ignoreUnknownKeys = true }
    val config: Config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val bot = telegramBot(config.token)/*  {
        client = HttpClient(OkHttp) {
            config.client?.apply {
                setupConfig()
            }
        }
    } */

    BotDelegate(bot, MessageSaveUseCase(MessagesDefaultRepository())).doSomeBotStuff()
}