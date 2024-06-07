package presentation

import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import domain.model.ChatId
import domain.model.TelegramScheduledMessage
import domain.model.TelegramUser
import domain.model.UserId
import domain.usecase.ScheduledMessageSaveUseCase
import domain.usecase.ScheduledMessagesDeleteUseCase
import domain.usecase.ScheduledMessagesGetUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class Reminder(
    val scheduledMessagesSender: ScheduledMessagesSender,
    val saveScheduledMessage: ScheduledMessageSaveUseCase,
    val getScheduledMessages: ScheduledMessagesGetUseCase,
    val deleteScheduledMessages: ScheduledMessagesDeleteUseCase
) {

    var stillAlive = false

    suspend fun startReminder() {
        val scope = CoroutineScope(Dispatchers.Default)
        stillAlive = true

        scope.launch { checkScheduledMessagesTimer() }
    }

    suspend fun scheduleMessage(message: CommonMessage<MessageContent>, messageText: String): Boolean {
        val dateTime = countDate(System.currentTimeMillis(), messageText)
        val isBefore = dateTime.isBefore(LocalDateTime.now())

        return if (!isBefore) {
            val users = message.asFromUser()?.from?.let {
                listOf(
                    TelegramUser(
                        UserId(it.id.chatId),
                        it.firstName,
                        it.username?.username
                    )
                )
            } ?: emptyList()
            println("scheduleMessage, user = ${users.firstOrNull()}")
            saveScheduledMessage(
                TelegramScheduledMessage(
                    0,
                    ChatId(message.chat.id.chatId),
                    users,
                    messageText,
                    dateTime
                )
            )
        } else {
            false
        }
    }

    private suspend fun checkScheduledMessagesTimer() {
        while (stillAlive) {
            val scheduledMessages = getScheduledMessages()

            if (scheduledMessages.isNotEmpty()) {
                var messageIndex = 0

                while (stillAlive) {
                    val closestMessage = scheduledMessages[messageIndex]

                    if (closestMessage.scheduledDateTime.minute == LocalDateTime.now().minute) {
                        scheduledMessagesSender.sendScheduledMessage(closestMessage)
                        deleteScheduledMessages(listOf(closestMessage.messageId))
                        messageIndex++
                    }

                    if (messageIndex == scheduledMessages.size) {
                        break
                    }

                    delay(TimeUnit.MINUTES.toMillis(1))
                    println("next minute")
                }
            }

            delay(TimeUnit.HOURS.toMillis(1))
            println("next hour")
        }
    }

    private fun countDate(currentTimeMillis: Long, requestedTimePattern: String): LocalDateTime {
        val patternWordsList = requestedTimePattern
            .split(" ")
            .map {
                val newWord = it.replace(redundantSymbolsPatternRegex, "")
                newWord
            }

        val dateString = patternWordsList.first { isDate(it) }
        val timeString = patternWordsList.firstOrNull { isTime(it) }
        println("dateString = $dateString")
        println("timeString = $timeString")

        val date = when {
            isLongDate(dateString) -> getDateFromDateString(dateString)
            isShortDate(dateString) -> {
                val yearString = if (isDateAfterToday(dateString)) {
                    LocalDate.now().year
                } else {
                    LocalDate.now().year + 1
                }
                println("yearString = $yearString")
                getDateFromDateString("$dateString.$yearString")
            }

            else -> {
                LocalDate.now()
            }
        }

        val time = getTimeFromString(timeString)

        return LocalDateTime.of(date, time)
    }

    private fun getDateFromDateString(dateString: String): LocalDate {
        println("getDateFromString, before")
        val date = LocalDate.parse(
            dateString,
            DateTimeFormatter.ofPattern(datePattern),
        )
        println("getDateFromString, $date")

        return date
    }

    private fun getTimeFromString(timeString: String?): LocalTime {
        val time = LocalTime.parse(
            timeString ?: "15:00",
            DateTimeFormatter.ofPattern(timePattern),
        )
        println("getTimeFromString, $time")

        return time
    }

    private fun isDate(maybeDate: String): Boolean {
        println("is date $maybeDate")
        return isLongDate(maybeDate) || isShortDate(maybeDate)
    }

    private fun isTime(maybeTime: String): Boolean {
        println("is time $maybeTime")
        return maybeTime.matches(timePatternRegex)
    }

    private fun isShortDate(date: String): Boolean = date.matches(dateShortPatternRegex)

    private fun isLongDate(date: String): Boolean = date.matches(dateLongPatternRegex)

    private fun isDateAfterToday(date: String): Boolean {
        val now = LocalDate.now()
        return getDateFromDateString("$date.${now.year}").isAfter(now)
    }

    companion object {
        const val datePattern = "dd.MM.yyyy"
        const val timePattern = "HH:mm"
        val dateShortPatternRegex = "\\d{2}\\.\\d{2}".toRegex()
        val dateLongPatternRegex = "\\d{2}\\.\\d{2}\\.\\d{2,4}".toRegex()
        val timePatternRegex = "\\d{1,2}:\\d{2}".toRegex()
        val redundantSymbolsPatternRegex = "(^\\W+)|(\\W+$)".toRegex()
    }

    interface ScheduledMessagesSender {
        suspend fun sendScheduledMessage(message: TelegramScheduledMessage)
    }
}