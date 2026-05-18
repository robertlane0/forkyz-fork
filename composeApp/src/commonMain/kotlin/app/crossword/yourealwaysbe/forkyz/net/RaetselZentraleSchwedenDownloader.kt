package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.RaetselZentraleSchwedenJSONIO
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

/**
 * Downloader for RaetselZentrale Swedish Crosswords
 *
 * This only really captures the Hamburg Abendsblatt pattern of
 * interation, so will need work later.
 *
 * Assuming archives are not available (paywall for Hamburg Abendsblatt,
 * just not available for any other i see with the same puzzle). Good
 * form is current day only.
 *
 * First URL is https://raetsel.raetselzentrale.de/l/<shortname>/schwede/
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 */
class RaetselZentraleSchwedenDownloader(
    downloaderID : DownloaderID,
    name : String,
    shortName : String,
    days : List<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String?,
    shareUrlPattern : String?
) : AbstractDateDownloader(
    downloaderID,
    name,
    days,
    utcAvailabilityOffset,
    supportUrl,
    RaetselZentraleSchwedenJSONIO(),
    null,
    shareUrlPattern,
    // website has "yesterday's" puzzle up until the
    // utcAvailabilityOffset, so either return today or yesterday
    // depending on what's available
    TimeUtils.offsetTodayUTC(-utcAvailabilityOffset),
) {
    private val idUrl : String
        = "https://raetsel.raetselzentrale.de/l/$shortName/schwede"

    override suspend fun getSourceUrl(date : LocalDate) : String? {
        try {
            getURL(idUrl, null).use { input ->
                return RaetselZentraleSchwedenStreamScraper
                    .getAPIURL(input)
            }
        } catch (e : IOException) {
            // fall through
        }
        return null
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

