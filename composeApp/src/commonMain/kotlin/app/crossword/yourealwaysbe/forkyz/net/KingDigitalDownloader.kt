
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek

import app.crossword.yourealwaysbe.forkyz.puz.io.JPZIO

/**
 * King Digital Downloader
 * https://puzzles.kingdigital.com/jpz/<crosswordSet>/YYYYMMDD.jpz
 *
 * @param crosswordSet used to fill in URL as above
 */
class KingDigitalDownloader(
    crosswordSet : String,
    downloaderID : DownloaderID,
    downloaderName : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String?,
    shareUrlPattern : String?
) : AbstractDateDownloader(
    downloaderID,
    downloaderName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    JPZIO(),
    String.format(SOURCE_URL_FMT_PATTERN, crosswordSet),
    shareUrlPattern
) {
    companion object {
        private const val SOURCE_URL_FMT_PATTERN
            = "'https://puzzles.kingdigital.com/jpz/%s/'yyyyMMdd'.jpz'"
    }
}

