package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.KeesingXMLIO
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

/**
 * Downloader for Keesing Crosswords
 *
 * Assuming that archives are not available.
 */
class KeesingDownloader(
    downloaderID : DownloaderID,
    name : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String?,
    shareUrlPattern : String?,
    private val clientID : String,
    private val puzzleSetID : String,
) : AbstractDateDownloader(
    downloaderID,
    name,
    days,
    utcAvailabilityOffset,
    supportUrl,
    KeesingXMLIO(),
    null,
    shareUrlPattern,
    // website has "yesterday's" puzzle up until the
    // utcAvailabilityOffset, so either return today or yesterday
    // depending on what's available
    TimeUtils.offsetTodayUTC(-utcAvailabilityOffset),
) {
    override suspend fun getSourceUrl(date : LocalDate) : String? {
        val scraper = KeesingStreamScraper()
        scraper.timeout = timeout
        return scraper.getPuzzleURL(clientID, puzzleSetID)
    }

    override suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        return super.download(date, headers)?.apply {
            this.copyright = name
            this.date = date
        }
    }
}

