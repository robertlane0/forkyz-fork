
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.convertMillisecondsToTime
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.convertUrlFormatPattern
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.durationBetween
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.durationUntil
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.fromEpochMilliseconds
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.millisecondsUntilNext
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.offsetTodayUTC
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.plusDays
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.plusYears
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.today
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils.Companion.todayIn

class TimeUtilsTest {
    @Test
    fun testConvertZero() {
        assertEquals("00:00.00", convertMillisecondsToTime(0))
    }

    @Test
    fun testConvertSeconds() {
        assertEquals("00:01.50", convertMillisecondsToTime(1500))
    }

    @Test
    fun testConvertMinutes() {
        assertEquals("01:05.00", convertMillisecondsToTime(65_000))
    }

    @Test
    fun testConvertSubMinute() {
        assertEquals("00:59.99", convertMillisecondsToTime(59_990))
    }

    @Test
    fun testConvertExactlyOneHour() {
        assertEquals("01:00:00.00", convertMillisecondsToTime(3_600_000))
    }

    @Test
    fun testConvertHoursMinutesSeconds() {
        assertEquals("01:01:01.00", convertMillisecondsToTime(3_661_000))
    }

    @Test
    fun testConvertMultipleHours() {
        assertEquals("10:30:15.50", convertMillisecondsToTime(37_815_500))
    }

    @Test
    fun testDurationBetweenSameDay() {
        val date = LocalDate(2024, 3, 15)
        assertEquals(0.hours, durationBetween(date, date))
    }

    @Test
    fun testDurationBetweenOneDay() {
        val start = LocalDate(2024, 3, 15)
        val end = LocalDate(2024, 3, 16)
        assertEquals(24.hours, durationBetween(start, end))
    }

    @Test
    fun testDurationBetweenMultipleDays() {
        val start = LocalDate(2024, 3, 1)
        val end = LocalDate(2024, 3, 8)
        assertEquals((24 * 7).hours, durationBetween(start, end))
    }

    @Test
    fun testDurationBetweenAcrossMonths() {
        val start = LocalDate(2024, 1, 30)
        val end = LocalDate(2024, 2, 2)
        assertEquals((24 * 3).hours, durationBetween(start, end))
    }

    @Test
    fun testDurationBetweenAcrossYears() {
        val start = LocalDate(2023, 12, 31)
        val end = LocalDate(2024, 1, 2)
        assertEquals((24 * 2).hours, durationBetween(start, end))
    }

    @Test
    fun testPlusDaysInt() {
        val date = LocalDate(2024, 3, 15)
        assertEquals(LocalDate(2024, 3, 20), plusDays(date, 5))
    }

    @Test
    fun testPlusDaysLong() {
        val date = LocalDate(2024, 3, 15)
        assertEquals(LocalDate(2024, 3, 20), plusDays(date, 5L))
    }

    @Test
    fun testPlusDaysAcrossMonth() {
        val date = LocalDate(2024, 1, 30)
        assertEquals(LocalDate(2024, 2, 2), plusDays(date, 3))
    }

    @Test
    fun testPlusDaysAcrossYear() {
        val date = LocalDate(2023, 12, 30)
        assertEquals(LocalDate(2024, 1, 3), plusDays(date, 4))
    }

    @Test
    fun testPlusDaysLeapDay() {
        val date = LocalDate(2024, 2, 28)
        assertEquals(LocalDate(2024, 2, 29), plusDays(date, 1))
    }

    @Test
    fun testPlusYears() {
        val date = LocalDate(2024, 3, 15)
        assertEquals(LocalDate(2025, 3, 15), plusYears(date, 1))
    }

    @Test
    fun testPlusYearsMultiple() {
        val date = LocalDate(2020, 6, 1)
        assertEquals(LocalDate(2025, 6, 1), plusYears(date, 5))
    }

    @Test
    fun testConvertUrlFormatPatternNull() {
        assertNull(convertUrlFormatPattern(null))
    }

    @Test
    fun testConvertUrlFormatPatternValid() {
        val format = convertUrlFormatPattern("yyyyMMdd")
        assertNotNull(format)
        assertEquals("20240315", format!!.format(LocalDate(2024, 3, 15)))
    }

    @Test
    fun testConvertUrlFormatPatternYearMonthDay() {
        val format = convertUrlFormatPattern("yyyy-MM-dd")
        assertNotNull(format)
        assertEquals("2024-03-15", format!!.format(LocalDate(2024, 3, 15)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConvertUrlFormatPatternInvalid() {
        convertUrlFormatPattern("not-a-valid-pattern-QQQ")
    }

    @Test
    fun testTodayReturnsDate() {
        assertNotNull(today())
    }

    @Test
    fun testTodayInUTCMatchesSystemUTC() {
        val utcDate = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .date
        assertEquals(utcDate, todayIn("UTC"))
    }

    @Test
    fun testTodayInDifferentTimezonesWithinOneDay() {
        // Tokyo and New York can differ by at most 1 calendar day
        val tokyo = todayIn("Asia/Tokyo")
        val newYork = todayIn("America/New_York")
        val diff = durationBetween(newYork, tokyo).absoluteValue
        assertTrue(diff <= 24.hours)
    }

    @Test
    fun testFromEpochMillisecondsKnownNoonUTC() {
        // 2024-03-15T12:00:00Z — noon UTC, so the local date is 2024-03-15
        // in any timezone between UTC-12 and UTC+12
        val noonUtcMillis = 1710504000000L
        val date = fromEpochMilliseconds(noonUtcMillis)
        val diff = durationBetween(LocalDate(2024, 3, 15), date).absoluteValue
        assertTrue(diff <= 24.hours)
    }

    @Test
    fun testFromEpochMillisecondsEpochOrigin() {
        // Epoch 0 is 1970-01-01T00:00:00Z; any timezone gives that date
        // or the day before (UTC-1 and earlier). Just check the year is
        // 1969/1970.
        val date = fromEpochMilliseconds(0L)
        assertTrue(date.year == 1969 || date.year == 1970)
    }

    // durationUntil

    @Test
    fun testDurationUntilFarPastIsNegative() {
        val past = plusYears(today(), -10)
        assertTrue(durationUntil(past, 0.hours).isNegative())
    }

    @Test
    fun testDurationUntilFarFutureIsPositive() {
        val future = plusYears(today(), 10)
        assertTrue(!durationUntil(future, 0.hours).isNegative())
    }

    @Test
    fun testDurationUntilPositiveOffsetIncreasesWait() {
        // A positive UTC offset shifts availableFrom later, so the wait
        // is longer
        val future = plusYears(today(), 10)
        val noOffset = durationUntil(future, 0.hours)
        val withOffset = durationUntil(future, 6.hours)
        assertTrue(noOffset < withOffset)
    }

    @Test
    fun testOffsetTodayUTCZeroOffset() {
        val utcNow = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        assertEquals(utcNow, offsetTodayUTC(0.hours))
    }

    @Test
    fun testOffsetTodayUTCPlusOneDayGivesTomorrow() {
        val utcToday = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        assertEquals(plusDays(utcToday, 1), offsetTodayUTC(24.hours))
    }

    @Test
    fun testOffsetTodayUTCMinusOneDayGivesYesterday() {
        val utcToday = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        assertEquals(plusDays(utcToday, -1), offsetTodayUTC((-24).hours))
    }

    @Test
    fun testMillisecondsUntilNextTomorrowIsPositive() {
        // Targeting tomorrow's day number guarantees a strictly future
        // target
        val tomorrowDayNum = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .dayOfWeek
            .isoDayNumber % 7 + 1
        val ms = millisecondsUntilNext(tomorrowDayNum, 0)
        assertTrue(ms > 0)
    }

    @Test
    fun testMillisecondsUntilNextWithinOneWeek() {
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val ms = millisecondsUntilNext(1, 0)
        assertTrue(ms <= sevenDaysMs)
    }
}

