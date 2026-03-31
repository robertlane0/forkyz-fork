package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.RaetselZentraleSchwedenJSONIO

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
    days : Array<DayOfWeek>,
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
        = String.format(Locale.US, ID_URL_FORMAT, shortName)

    override fun getSourceUrl(date : LocalDate) : String? {
        try {
            getInputStream((URI(idUrl)).toURL(), null).use { inputStream ->
                return RaetselZentraleSchwedenStreamScraper
                    .getAPIURL(inputStream)
            }
        } catch (e : IOException) {
            // fall through
        } catch (e : URISyntaxException) {
        }
        return null
    }

    override fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        return super.download(date, headers)?.apply {
            this.copyright = name
            this.date = date
        }
    }

    companion object {
        private const val ID_URL_FORMAT
            = "https://raetsel.raetselzentrale.de/l/%s/schwede"
    }
}

