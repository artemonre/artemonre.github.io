package presentation

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Reminder {

    fun setReminder(message: String) {
        val dateTime = countDate(System.currentTimeMillis(), message)
        val isBefore = dateTime.isBefore(LocalDateTime.now())
        val dateTimeFormatted = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        println(
            if (isBefore) {
                "I can't do anything with this."
            } else {
                "Date and time: $dateTimeFormatted"
            },
        )
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
}