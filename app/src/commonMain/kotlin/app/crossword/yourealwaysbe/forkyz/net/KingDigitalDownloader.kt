
package app.crossword.yourealwaysbe.forkyz.net

import java.time.DayOfWeek
import java.time.Duration

import app.crossword.yourealwaysbe.puz.io.JPZIO

/**
 * King Digital Downloader
 * https://puzzles.kingdigital.com/jpz/<crosswordSet>/YYYYMMDD.jpz
 *
 * @param crosswordSet used to fill in URL as above
 */
class KingDigitalDownloader(
    crosswordSet : String,
    internalName : String,
    downloaderName : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String?,
    shareUrlPattern : String?
) : AbstractDateDownloader(
    internalName,
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

