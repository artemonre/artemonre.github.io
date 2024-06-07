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
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    suspend fun scheduleMessage(message: CommonMessage<MessageContent>, requestedDateTimeWords: List<String>): Boolean {
        val dateTime = countDate(System.currentTimeMillis(), requestedDateTimeWords)
        println("scheduleMessage, dateTime = $dateTime")
        val dateTimeUtc = dateTime.toInstant().atOffset(ZoneOffset.UTC)
        println("scheduleMessage, dateTimeUtc = $dateTimeUtc")
        val isBefore = dateTimeUtc.isBefore(OffsetDateTime.now(ZoneOffset.UTC))
        val notificationMessage = requestedDateTimeWords.joinToString(" ")

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
                    notificationMessage,
                    dateTimeUtc
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

                    if (closestMessage.scheduledDateTime.minute == OffsetDateTime.now().minute) {
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

    private fun countDate(
        currentTimeMillis: Long,
        requestedDateTimeWords: List<String>
    ): OffsetDateTime {
        val dateString = requestedDateTimeWords.first { isDate(it) }
        var timeString = DEFAULT_REMINDER_TIME
        var zoneOffset = ZoneOffset.ofHours(TIME_ZONE_DEFAULT_OFFSET)

        var textLowercase: String

        for (text in requestedDateTimeWords) {
            textLowercase = text.lowercase()
            if (isTime(textLowercase)) {
                timePatternRegex.find(textLowercase, 0)?.let {
                    timeString = textLowercase.substring(it.range)
                }

                if (timeString != textLowercase) {
                    val timezoneString = text.replace(timeString, "")

                    zoneOffset = getTimezoneFromString(timezoneString.lowercase())
                }
            } else if (isTimeZone(text)) {
                zoneOffset = getTimezoneFromString(text.lowercase())
            }
        }

        val date = when {
            isLongDate(dateString) -> getDateFromDateString(dateString)
            isShortDate(dateString) -> {
                val yearString = if (isDateAfterToday(dateString)) {
                    LocalDate.now().year
                } else {
                    LocalDate.now().year + 1
                }
                getDateFromDateString("$dateString.$yearString")
            }

            else -> {
                LocalDate.now()
            }
        }

        val time = getTimeFromString(timeString)

        return OffsetDateTime.of(date, time, zoneOffset)
    }

    private fun getDateFromDateString(dateString: String): LocalDate {
        println("getDateFromString, before")
        val date = LocalDate.parse(
            dateString,
            DateTimeFormatter.ofPattern(DATE_PATTERN),
        )
        println("getDateFromString, $date")

        return date
    }

    private fun getTimeFromString(timeString: String): LocalTime {
        val time = LocalTime.parse(
            timeString,
            DateTimeFormatter.ofPattern(TIME_PATTERN),
        )
        println("getTimeFromString, $time")

        return time
    }

    private fun getTimezoneFromString(timeZoneString: String): ZoneOffset {
        val timeZone  = when {
            timeZoneString.contains(TIME_ZONE_KEY_URAL) ||
                    timeZoneString.contains(TIME_ZONE_KEY_URAL1) -> ZoneOffset.ofHours(TIME_ZONE_URAL_OFFSET)
            timeZoneString.contains(timeZonePatternRegex) -> {
                println("timezone = ${timeZoneString.toIntOrNull()}")
                ZoneOffset.ofHours(timeZoneString.toIntOrNull() ?: TIME_ZONE_DEFAULT_OFFSET)
            }
            else -> {
                ZoneOffset.ofHours(TIME_ZONE_DEFAULT_OFFSET)
            }
        }

        println("getTimezoneFromString, $timeZone")

        return timeZone
    }

    private fun isDate(maybeDate: String): Boolean {
        println("is date $maybeDate")
        return isLongDate(maybeDate) || isShortDate(maybeDate)
    }

    private fun isTime(maybeTime: String): Boolean {
        println("is time $maybeTime")
        return maybeTime.contains(timePatternRegex)
    }

    private fun isTimeZone(maybeTimeZone: String): Boolean {
        println("is time $maybeTimeZone")
        return maybeTimeZone.contains(TIME_ZONE_KEY_MOSCOW) ||
                maybeTimeZone.contains(TIME_ZONE_KEY_URAL) ||
                maybeTimeZone.contains(TIME_ZONE_KEY_URAL1) ||
                maybeTimeZone.contains(timeZonePatternRegex)
    }

    private fun isShortDate(date: String): Boolean = date.matches(dateShortPatternRegex)

    private fun isLongDate(date: String): Boolean = date.matches(dateLongPatternRegex)

    private fun isDateAfterToday(date: String): Boolean {
        val now = LocalDate.now()
        return getDateFromDateString("$date.${now.year}").isAfter(now)
    }

    companion object {
        const val TIME_ZONE_DEFAULT_OFFSET = 3
        const val TIME_ZONE_URAL_OFFSET = 5
        const val TIME_ZONE_KEY_MOSCOW = "мск"
        const val TIME_ZONE_KEY_URAL = "екб"
        const val TIME_ZONE_KEY_URAL1 = "сысерть"
        const val DATE_PATTERN = "dd.MM.yyyy"
        const val TIME_PATTERN = "HH:mm"
        const val DEFAULT_REMINDER_TIME = "15:00"
        val dateShortPatternRegex = "\\d{2}\\.\\d{2}".toRegex()
        val dateLongPatternRegex = "\\d{2}\\.\\d{2}\\.\\d{2,4}".toRegex()
        val timePatternRegex = "\\d{1,2}:\\d{2}".toRegex()
        val timeZonePatternRegex = "([+\\-])\\d{1,2}".toRegex()
    }

    interface ScheduledMessagesSender {
        suspend fun sendScheduledMessage(message: TelegramScheduledMessage)
    }
}