
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
    days : List<DayOfWeek>,
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
    "'https://puzzles.kingdigital.com/jpz/$crosswordSet/'yyyyMMdd'.jpz'",
    shareUrlPattern
)

