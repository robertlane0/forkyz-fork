package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek

import app.crossword.yourealwaysbe.forkyz.puz.io.AMUniversalJSONIO

/**
 * AM Universal JSON Puzzles
 * URL: https://gamedata.services.amuniversal.com/c/[slug]/l/[token]/g/[puzzle]/d/YYYY-MM-DD/data.json
 */
class AMUniversalDownloader(
    downloaderID : DownloaderID,
    shortName : String,
    slug : String,
    token : String,
    fullName : String,
    supportUrl : String?,
    days : List<DayOfWeek>,
    utcAvailabilityOffset : Duration,
) : AbstractDateDownloader(
    downloaderID,
    fullName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    AMUniversalJSONIO(),
    "'https://gamedata.services.amuniversal.com/c/$slug/l/"
        + "$token/g/$shortName/d/'yyyy-MM-dd'/data.json'",
)
