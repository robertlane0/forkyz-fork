
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.GuardianJSONIO
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

/**
 * Guardian Weekly Quick Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/quick-cryptic/
 * Date = Weekly on Saturdays
 */
class GuardianWeeklyQuickCrypticDownloader(
    downloaderID : DownloaderID,
    name : String,
) : AbstractDateDownloader(
    downloaderID,
    name,
    DAYS_SATURDAY,
    Duration.ZERO,
    SUPPORT_URL,
    GuardianJSONIO(),
    goodFrom = BASE_CW_DATE,
) {
    override suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val sourceUrl = getSourceUrl(date)
        if (sourceUrl == null)
            return null

        try {
            getURL(sourceUrl, headers).use { input ->
                return GuardianJSONIO.readFromHTML(input)?.apply {
                    this.copyright = "Guardian / " + author
                    this.source = name
                    this.supportUrl = supportUrl
                    this.sourceUrl = sourceUrl
                    this.shareUrl = sourceUrl
                }
            }
        } catch (e : IOException) {
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
        val diff = TimeUtils.durationBetween(BASE_CW_DATE, date)
        val cwNum : Long = BASE_CW_NUMBER + (diff.inWholeDays / 7)
        return cwNum.toString()
    }

    companion object {
        private const val SUPPORT_URL = "https://support.theguardian.com"
        private const val BASE_SOURCE_URL
            = "https://www.theguardian.com/crosswords/quick-cryptic/"
        private const val BASE_CW_NUMBER = 1
        private val BASE_CW_DATE : LocalDate = LocalDate(2024, 4, 6)
    }
}

