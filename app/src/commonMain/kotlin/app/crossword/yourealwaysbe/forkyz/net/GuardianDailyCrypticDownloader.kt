package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.Period

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO

/**
 * Guardian Daily Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/cryptic/
 * Date = Daily
 */
class GuardianDailyCrypticDownloader(
    internalName : String,
    name : String,
) : AbstractDateDownloader(
    internalName,
    name,
    DAYS_WEEKDAY,
    Duration.ZERO,  // TODO: availability time
    SUPPORT_URL,
    GuardianJSONIO(),
) {
    override fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val sourceUrl = getSourceUrl(date)
        if (sourceUrl == null)
            return null

        try {
            getInputStream(
                (URI(sourceUrl)).toURL(), headers
            ).use { inputStream ->
                return GuardianJSONIO.readFromHTML(inputStream)?.apply {
                    setCopyright("Guardian / " + getAuthor())
                    setSource(name)
                    setSupportUrl(supportUrl)
                    setSourceUrl(sourceUrl)
                    setShareUrl(sourceUrl)
                }
            }
        } catch (e : IOException) {
            e.printStackTrace()
        } catch (e : URISyntaxException) {
            e.printStackTrace()
        }

        return null
    }

    override fun getSourceUrl(date : LocalDate) : String? {
        return BASE_SOURCE_URL + createUrlSuffix(date)
    }

    override fun getShareUrl(date : LocalDate) : String? {
        return getSourceUrl(date)
    }

    protected fun createUrlSuffix(date : LocalDate) : String {
        var lower : LocalDate = BASE_CW_DATE
        var upper = date
        var direction = 1

        if (lower.isAfter(upper)) {
            lower = date
            upper = BASE_CW_DATE
            direction = -1
        }

        val diff = Duration.between(
            lower.atStartOfDay(),
            upper.atStartOfDay()
        )

        val daysDiff = diff.toDays()
        val yearsDiff = Period.between(lower, upper).getYears()

        var cwNumOffset = daysDiff
        // no Sundays (base day is Monday so negative gets one more)
        cwNumOffset -= (daysDiff / 7)
        if (direction < 0 && daysDiff % 7 != 0L)
            cwNumOffset -= 1
        // no Christmas
        cwNumOffset -= countNonSundayChristmas(lower, upper).toLong()
        // no Boxing day pre 2010
        cwNumOffset -= countNonSundayBoxing(
            lower,
            LocalDate.of(2009, 12, 26),
        ).toLong()

        val cwNum : Long = BASE_CW_NUMBER + direction * cwNumOffset

        return cwNum.toString()
    }

    companion object {
        private const val INTERNAL_NAME = "guardian"
        private const val SUPPORT_URL = "https://support.theguardian.com"
        private const val BASE_SOURCE_URL
            = "https://www.theguardian.com/crosswords/cryptic/"
        private const val BASE_CW_NUMBER = 28112
        private val BASE_CW_DATE : LocalDate = LocalDate.of(2020, 4, 20)

        /**
         * Counts number of Christmasses that aren't Sunday between dates
         * (inclusive).
         *
         * Returns 0 if upper below lower
         */
        private fun countNonSundayChristmas(
            lower : LocalDate,
            upper : LocalDate,
        ) : Int {
            return countNonSundaySpecial(lower, upper, 12, 25)
        }

        /**
         * Counts number of Boxing Days that aren't Sunday between dates
         * (inclusive)
         *
         * Returns 0 if upper below lower
         */
        private fun countNonSundayBoxing(
            lower : LocalDate,
            upper : LocalDate,
        ) : Int {
            return countNonSundaySpecial(lower, upper, 12, 26)
        }

        /**
         * Counts number of special days that aren't Sunday between dates
         * (inclusive)
         *
         * @param lower start date inclusive
         * @param upper end date inclusive
         * @param month month of special date (1-12)
         * @param day day of special date (1-31)
         * @return number of non-sunday occurences, or 0 if upper &lt; lower
         */
        private fun countNonSundaySpecial(
            lower : LocalDate,
            upper : LocalDate,
            month : Int,
            day : Int,
        ) : Int {
            if (upper.isBefore(lower))
                return 0

            var special = LocalDate.of(lower.getYear(), month, day)
            if (lower.isAfter(special))
                special = special.plusYears(1)

            var count = 0
            while (!special.isAfter(upper)) {
                if (DayOfWeek.from(special) != DayOfWeek.SUNDAY) count += 1
                special = special.plusYears(1)
            }

            return count
        }
    }
}

