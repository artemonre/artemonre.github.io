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
import java.time.*
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

    suspend fun scheduleMessage(
        message: CommonMessage<MessageContent>,
        requestedDateTimeWords: List<String>
    ): Boolean {
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

                    delay(TimeUnit.SECONDS.toMillis(30))
                    println("next half minute")
                }
            }

            delay(TimeUnit.MINUTES.toMillis(5))
            println("next 10 minutes")
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

        val date = getDateFromString(dateString)

        val time = getTimeFromString(timeString)

        return OffsetDateTime.of(date, time, zoneOffset)
    }

    private fun getDateFromString(dateString: String): LocalDate {
        println("getDateFromString, before")
        return when {
            isLongDate(dateString) -> getDateFromDateString(dateString)
            isShortDate(dateString) -> {
                val yearString = if (isDateAfterToday(dateString)) {
                    LocalDate.now().year
                } else {
                    LocalDate.now().year + 1
                }
                getDateFromDateString("$dateString.$yearString")
            }

            isDayDate(dateString) -> getDateFromDayString(dateString)
            isDayOfWeekDate(dateString) -> getDateFromDayOfWeekString(dateString)
            else -> {
                LocalDate.now()
            }
        }
    }

    private fun getDateFromDateString(date: String) = LocalDate.parse(
        date,
        DateTimeFormatter.ofPattern(DATE_PATTERN),
    )

    private fun getTimeFromString(timeString: String): LocalTime {
        val time = LocalTime.parse(
            timeString,
            DateTimeFormatter.ofPattern(TIME_PATTERN),
        )
        println("getTimeFromString, $time")

        return time
    }

    private fun getTimezoneFromString(timeZoneString: String): ZoneOffset {
        val timeZone = when {
            timeZoneUralRegex.matches(timeZoneString) ||
                    timeZoneUral1Regex.matches(timeZoneString) ||
                    timeZoneUral2Regex.matches(timeZoneString) ||
                    timeZoneUral3Regex.matches(timeZoneString) -> ZoneOffset.ofHours(TIME_ZONE_URAL_OFFSET)

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

    private fun getDateFromDayString(day: String): LocalDate = when {
        dateTomorrowPatternRegex.matches(day) -> LocalDate.now().plusDays(1)
        dateAfterTomorrowPatternRegex.matches(day) -> LocalDate.now().plusDays(2)
        else -> LocalDate.now()
    }

    private fun getDateFromDayOfWeekString(dayOfWeek: String): LocalDate {
        val today = LocalDate.now().dayOfWeek

        val amountOfDays = when {
            dayOfWeek.contains(dateMondayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.MONDAY.value)
            dayOfWeek.contains(dateTuesdayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.TUESDAY.value)
            dayOfWeek.contains(dateWednesdayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.WEDNESDAY.value)
            dayOfWeek.contains(dateThursdayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.THURSDAY.value)
            dayOfWeek.contains(dateFridayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.FRIDAY.value)
            dayOfWeek.contains(dateSaturdayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.SATURDAY.value)
            dayOfWeek.contains(dateSundayPatternRegex) -> getDaysUntil(today.value, DayOfWeek.SUNDAY.value)
            else -> 0
        }

        return LocalDate.now().plusDays(amountOfDays)
    }

    private fun getDaysUntil(today: Int, requestedDay: Int): Long {
        val amountOfDays = if (today >= requestedDay) {
            7 - today + requestedDay
        } else {
            requestedDay - today
        }

        return amountOfDays.toLong()
    }

    private fun isDate(maybeDate: String): Boolean {
        println("isDate $maybeDate")
        return isLongDate(maybeDate) ||
                isShortDate(maybeDate) ||
                isDayDate(maybeDate) ||
                isDayOfWeekDate(maybeDate)
    }

    private fun isTime(maybeTime: String): Boolean {
        println("isTime $maybeTime")
        return maybeTime.contains(timePatternRegex)
    }

    private fun isTimeZone(maybeTimeZone: String): Boolean {
        println("isTimeZone $maybeTimeZone")
        return maybeTimeZone.contains(timeZoneMoscowRegex) ||
                maybeTimeZone.contains(timeZoneUralRegex) ||
                maybeTimeZone.contains(timeZoneUral1Regex) ||
                maybeTimeZone.contains(timeZoneUral2Regex) ||
                maybeTimeZone.contains(timeZoneUral3Regex) ||
                maybeTimeZone.contains(timeZonePatternRegex)
    }

    private fun isShortDate(date: String): Boolean = dateShortPatternRegex.matches(date)

    private fun isLongDate(date: String): Boolean = dateLongPatternRegex.matches(date)

    private fun isDayDate(date: String): Boolean =
        dateTodayPatternRegex.matches(date) ||
                dateTomorrowPatternRegex.matches(date) ||
                dateAfterTomorrowPatternRegex.matches(date)

    private fun isDayOfWeekDate(date: String): Boolean =
        dateMondayPatternRegex.matches(date) ||
                dateTuesdayPatternRegex.matches(date) ||
                dateWednesdayPatternRegex.matches(date) ||
                dateThursdayPatternRegex.matches(date) ||
                dateFridayPatternRegex.matches(date) ||
                dateSaturdayPatternRegex.matches(date) ||
                dateSundayPatternRegex.matches(date)

    private fun isDateAfterToday(date: String): Boolean {
        val now = LocalDate.now()
        return getDateFromString("$date.${now.year}").isAfter(now)
    }

    companion object {
        const val TIME_ZONE_DEFAULT_OFFSET = 3
        const val TIME_ZONE_URAL_OFFSET = 5
        const val DATE_PATTERN = "dd.MM.yyyy"
        const val TIME_PATTERN = "HH:mm"
        const val DEFAULT_REMINDER_TIME = "15:00"
        val timeZoneMoscowRegex = "м.?ск.?.?".toRegex()
        val timeZoneUralRegex = "ек[а-яА-Я]{0,6}б[а-яА-Я]{0,3}".toRegex()
        val timeZoneUral1Regex = "екат.?".toRegex()
        val timeZoneUral2Regex = "с.?с.?рт.?".toRegex()
        val timeZoneUral3Regex = "урал.?".toRegex()
        val dateAfterPatternRegex = "ч.?р.?з".toRegex()
        val dateAfterMinutesPatternRegex = "м.?н.?т.?".toRegex()
        val dateAfterHoursPatternRegex = "ч.?с.?.?".toRegex()
        val dateAfterDaysPatternRegex = "дн.?.?".toRegex()
        val dateAfterWeeksPatternRegex = "н.?д.?л.?".toRegex()
        val dateAfterMonthsPatternRegex = "м.?с.?ц.?.?".toRegex()
        val dateShortPatternRegex = "\\d{2}\\.\\d{2}".toRegex()
        val dateLongPatternRegex = "\\d{2}\\.\\d{2}\\.\\d{2,4}".toRegex()
        val dateTodayPatternRegex = "с.?г.?дн.?".toRegex()
        val dateTomorrowPatternRegex = "з.?вт.?.?".toRegex()
        val dateAfterTomorrowPatternRegex = "п.?сл.?з.?вт.?.?".toRegex()
        val dateMondayPatternRegex = "п.?н.?д[а-яА-Я]{0,6}".toRegex()
        val dateTuesdayPatternRegex = "вт[а-яА-Я]{0,5}".toRegex()
        val dateWednesdayPatternRegex = "ср[а-яА-Я]{0,3}".toRegex()
        val dateThursdayPatternRegex = "ч.?т[а-яА-Я]{0,4}".toRegex()
        val dateFridayPatternRegex = "п.?т[а-яА-Я]{0,4}".toRegex()
        val dateSaturdayPatternRegex = "с.?б[а-яА-Я]{0,4}".toRegex()
        val dateSundayPatternRegex = "в.?с[а-яА-Я]{0,8}".toRegex()
        val timePatternRegex = "\\d{1,2}:\\d{2}".toRegex()
        val timeZonePatternRegex = "([+\\-])\\d{1,2}".toRegex()
    }

    interface ScheduledMessagesSender {
        suspend fun sendScheduledMessage(message: TelegramScheduledMessage)
    }
}