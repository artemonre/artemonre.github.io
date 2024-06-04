import config.Config
import data.MessagesDefaultRepository
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import domain.MessageSaveUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import presentation.BotDelegate
import java.io.File

const val nonWordCharOrStart = "([^а-яА-Я0-9a-zA-Z]|^)"
const val nonWordCharOrEnd = "([^а-яА-Я0-9a-zA-Z]|$)"

/**
 * This method by default expects one argument in [args] field: telegram bot configuration
 */
suspend fun main(args: Array<String>) {
    val json = Json { ignoreUnknownKeys = true }
    val config: Config = json.decodeFromString(Config.serializer(), File(args.first()).readText())
    val bot = telegramBot(config.token) {
        client = HttpClient(OkHttp) {
            config.client?.apply {
                setupConfig()
            }
        }
    }

    BotDelegate(bot, MessageSaveUseCase(MessagesDefaultRepository())).doSomeBotStuff()
}