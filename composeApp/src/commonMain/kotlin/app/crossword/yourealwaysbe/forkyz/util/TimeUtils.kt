
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_friday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_monday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_saturday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_sunday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_thursday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_tuesday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.day_wednesday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_april
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_august
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_december
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_february
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_january
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_july
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_june
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_march
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_may
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_november
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_october
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_september

class TimeUtils {
    companion object {
        fun convertMillisecondsToTime(milliseconds : Long) : String {
            var remaining = milliseconds.milliseconds
            val hours = remaining.inWholeHours
            remaining -= hours.hours
            val minutes = remaining.inWholeMinutes
            remaining -= minutes.minutes
            val seconds = remaining.inWholeSeconds
            remaining -= seconds.seconds
            val milliseconds = remaining.inWholeMilliseconds / 10

            val minutesStr = minutes.toString().padStart(2, '0')
            val secondsStr = seconds.toString().padStart(2, '0')
            val millisecondsStr = milliseconds.toString().padStart(2, '0')

            if (hours == 0L) {
                return "$minutesStr:$secondsStr.$millisecondsStr"
            } else {
                val hoursStr = hours.toString().padStart(2, '0')
                return "$hoursStr:$minutesStr:$secondsStr.$millisecondsStr"
            }
        }

        fun currentTimeMillis() : Long {
            return Clock.System.now().toEpochMilliseconds()
        }

        /**
         * Today in local timezone
         */
        fun today() : LocalDate {
            return Clock.System.todayIn(TimeZone.currentSystemDefault())
        }

        /**
         * Today in given timezone
         */
        fun todayIn(timezone : String) : LocalDate {
            return Clock.System.todayIn(TimeZone.of(timezone))
        }

        /**
         * Get date in current timezone from epoch millis
         */
        fun fromEpochMilliseconds(milliseconds : Long) : LocalDate {
            return Instant.fromEpochMilliseconds(milliseconds)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }

        /**
         * How long until midnight of date in UTC plus offset
         */
        fun durationUntil(date : LocalDate, utcOffset : Duration) : Duration {
            val now = Clock.System.now()
            val availableFrom = date.atStartOfDayIn(TimeZone.UTC)
                .plus(utcOffset)
            return availableFrom - now
        }

        /**
         * Duration between midnight of both dates
         */
        fun durationBetween(
            startInc : LocalDate,
            endExc : LocalDate,
        ) : Duration {
            return (
                endExc.atStartOfDayIn(TimeZone.UTC)
                - startInc.atStartOfDayIn(TimeZone.UTC)
            )
        }

        /**
         * The LocalDate of now, plus the utcOffset
         */
        fun offsetTodayUTC(utcOffset : Duration) : LocalDate {
            return Clock.System.now()
                .plus(utcOffset)
                .toLocalDateTime(TimeZone.UTC)
                .date
        }

        fun plusDays(date : LocalDate, days : Int) : LocalDate {
            return plusDays(date, days.toLong())
        }

        fun plusDays(date : LocalDate, days : Long) : LocalDate {
            return date.plus(days, DateTimeUnit.DAY)
        }

        fun plusYears(date : LocalDate, years : Int) : LocalDate {
            return date.plus(years, DateTimeUnit.YEAR)
        }

        /**
         * Number of milliseconds from now until next day/hour
         */
        fun millisecondsUntilNext(dayNum : Int, hour : Int) : Long {
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val nowDayNum = now.date.dayOfWeek.isoDayNumber
            val daysUntil = (dayNum - nowDayNum + 7) % 7
            val then = plusDays(now.date, daysUntil)
                .atTime(LocalTime(hour, 0))
            val gap = (
                then.toInstant(TimeZone.currentSystemDefault())
                - now.toInstant(TimeZone.currentSystemDefault())
            )
            return gap.inWholeMilliseconds
        }

        /**
         * Convenience for converting string patters to date formats
         *
         * @throws IllegalArgumentException if bad
         */
        @OptIn(FormatStringsInDatetimeFormats::class)
        fun convertUrlFormatPattern(
            pattern : String?,
        ) : DateTimeFormat<LocalDate>? {
            return pattern?.let { LocalDate.Format { byUnicodePattern(it) } }
        }

        fun getMonthNameID(date : LocalDate) : StringResource {
            return when (date.month) {
                Month.JANUARY -> Res.string.month_january
                Month.FEBRUARY -> Res.string.month_february
                Month.MARCH -> Res.string.month_march
                Month.APRIL -> Res.string.month_april
                Month.MAY -> Res.string.month_may
                Month.JUNE -> Res.string.month_june
                Month.JULY -> Res.string.month_july
                Month.AUGUST -> Res.string.month_august
                Month.SEPTEMBER -> Res.string.month_september
                Month.OCTOBER -> Res.string.month_october
                Month.NOVEMBER -> Res.string.month_november
                Month.DECEMBER -> Res.string.month_december
            }
        }

        fun getDayNameID(date : LocalDate) : StringResource {
            return when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> Res.string.day_monday
                DayOfWeek.TUESDAY -> Res.string.day_tuesday
                DayOfWeek.WEDNESDAY -> Res.string.day_wednesday
                DayOfWeek.THURSDAY -> Res.string.day_thursday
                DayOfWeek.FRIDAY -> Res.string.day_friday
                DayOfWeek.SATURDAY -> Res.string.day_saturday
                DayOfWeek.SUNDAY -> Res.string.day_sunday
            }
        }
    }
}

