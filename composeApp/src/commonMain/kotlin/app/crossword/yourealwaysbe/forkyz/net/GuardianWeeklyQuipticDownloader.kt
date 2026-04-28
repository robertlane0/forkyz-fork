
package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO

/**
 * Guardian Weekly Quiptic downloader
 * URL: https://www.theguardian.com/crosswords/quiptic/
 * Date = Weekly on Sundays
 */
class GuardianWeeklyQuipticDownloader(
    downloaderID : DownloaderID,
    name : String,
) : AbstractDateDownloader(
    downloaderID,
    name,
    DAYS_SUNDAY,  // see isPublishedOnDay for logic over time
    Duration.ZERO,  // TODO: availability time
    SUPPORT_URL,
    GuardianJSONIO(),
) {
    override suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val sourceUrl = getSourceUrl(date)
        try {
            getInputStream(
                (URI(sourceUrl)).toURL(), headers
            ).use { inputStream ->
                return GuardianJSONIO.readFromHTML(inputStream)?.apply {
                    this.copyright = "Guardian / " + author
                    this.source = name
                    this.supportUrl = supportUrl
                    this.sourceUrl = sourceUrl
                    this.shareUrl = sourceUrl
                }
            }
        } catch (e : IOException) {
            e.printStackTrace()
        } catch (e : URISyntaxException) {
            e.printStackTrace()
        }

        return null
    }

    override suspend fun getSourceUrl(date : LocalDate) : String? {
        return BASE_SOURCE_URL + createUrlSuffix(date)
    }

    override suspend fun getShareUrl(date : LocalDate) : String? {
        return getSourceUrl(date)
    }

    protected fun createUrlSuffix(date : LocalDate) : String {
        var lower : LocalDate = BASE_CW_DATE
        var upper = date
        var direction = 1

        if (lower.compareTo(upper) > 0) {
            lower = date
            upper = BASE_CW_DATE
            direction = -1
        }

        val diff = TimeUtils.durationBetween(lower, upper)
        val daysDiff = diff.inWholeDays

        // change between tue/mon/sun release smoothed over by rounding
        var cwNumOffset = (daysDiff / 7)

        // 25 Dec 2023 was skipped, so adjust up until BASE_CW_DATE
        // Revisit in 2033 to see if that Christmas is skipped or not
        // (other Christmases weren't missed)
        if (
            date.compareTo(BASE_CW_DATE) < 0
            && date.compareTo(LocalDate(2023, 12, 25)) > 0
        ) {
            cwNumOffset = cwNumOffset + 1
        }

        val cwNum : Long = BASE_CW_NUMBER + direction * cwNumOffset

        return cwNum.toString()
    }

    // override default isPublishedOnDay behaviour to follow changing release date
    override fun isPublishedOnDay(date : LocalDate) : Boolean {
        val dayOfWeek = date.dayOfWeek

        // #1272 onwards on Sundays
        if (date.compareTo(BASE_CW_DATE) >= 0)
            return (dayOfWeek == DayOfWeek.SUNDAY)
        // #1 on Tuesday
        else if (date == LocalDate(1999, 11, 23))
            return true
        // Skip 2024-12-25
        else if (date == LocalDate(2023, 12, 25))
            return false
        // #2 to #1271 on Mondays
        else
            return (dayOfWeek == DayOfWeek.MONDAY)
    }

    companion object {
        private const val INTERNAL_NAME = "guardianQuiptic"
        private const val SUPPORT_URL = "https://support.theguardian.com"
        private const val BASE_SOURCE_URL
            = "https://www.theguardian.com/crosswords/quiptic/"
        private const val BASE_CW_NUMBER = 1272
        private val BASE_CW_DATE : LocalDate = LocalDate(2024, 4, 7)
    }
}

