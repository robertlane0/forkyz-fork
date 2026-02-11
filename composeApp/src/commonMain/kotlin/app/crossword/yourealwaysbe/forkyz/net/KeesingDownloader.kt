package app.crossword.yourealwaysbe.forkyz.net

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.KeesingXMLIO

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
    ZonedDateTime.now(ZoneId.of("UTC"))
        .minus(utcAvailabilityOffset)
        .toLocalDate(),
) {
    override fun getSourceUrl(date : LocalDate) : String? {
        val scraper = KeesingStreamScraper()
        scraper.timeout = timeout
        return scraper.getPuzzleURL(clientID, puzzleSetID)
    }

    override fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        return super.download(date, headers)?.apply {
            setCopyright(name)
            setDate(date)
        }
    }
}

