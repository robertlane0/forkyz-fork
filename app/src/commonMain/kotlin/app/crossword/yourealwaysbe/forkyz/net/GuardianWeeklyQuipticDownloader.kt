
package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO

/**
 * Guardian Weekly Quiptic downloader
 * URL: https://www.theguardian.com/crosswords/quiptic/
 * Date = Weekly on Sundays
 */
class GuardianWeeklyQuipticDownloader(
    internalName : String,
    name : String,
) : AbstractDateDownloader(
    internalName,
    name,
    DAYS_SUNDAY,  // see isPublishedOnDay for logic over time
    Duration.ZERO,  // TODO: availability time
    SUPPORT_URL,
    GuardianJSONIO(),
) {
    override fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val sourceUrl = getSourceUrl(date)
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

        // change between tue/mon/sun release smoothed over by rounding
        var cwNumOffset = (daysDiff / 7)

        // 25 Dec 2023 was skipped, so adjust up until BASE_CW_DATE
        // Revisit in 2033 to see if that Christmas is skipped or not
        // (other Christmases weren't missed)
        if (
            date.isBefore(BASE_CW_DATE)
            && date.isAfter(LocalDate.of(2023, 12, 25))
        ) {
            cwNumOffset = cwNumOffset + 1
        }

        val cwNum : Long = BASE_CW_NUMBER + direction * cwNumOffset

        return cwNum.toString()
    }

    // override default isPublishedOnDay behaviour to follow changing release date
    override fun isPublishedOnDay(date : LocalDate) : Boolean {
        val dayOfWeek = date.getDayOfWeek()

        // #1272 onwards on Sundays
        if (date.isAfter(BASE_CW_DATE) || date.isEqual(BASE_CW_DATE))
            return (dayOfWeek == DayOfWeek.SUNDAY)
        // #1 on Tuesday
        else if (date.isEqual(LocalDate.of(1999, 11, 23)))
            return true
        // Skip 2024-12-25
        else if (date.isEqual(LocalDate.of(2023, 12, 25)))
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
        private val BASE_CW_DATE : LocalDate = LocalDate.of(2024, 4, 7)
    }
}

