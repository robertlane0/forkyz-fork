
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.USATodayJSONIO
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

private val TAG = "USATodayDownloader"
private val QUERY_URL = "https://play.usatoday.com/api/query"
private val HEADERS = mapOf("x-api-type" to "games")
private val SUPPORT_URL = "https://subscribe.usatoday.com"
private val SHARE_URL = "https://play.usatoday.com/crossword"

/**
 * USA Today downloader
 *
 * Daily downloads.
 * Look up puzzle ID via a query first.
 * Then download given ID.
 */
class USATodayDownloader (
    downloaderID : DownloaderID,
    name : String,
) : AbstractDateDownloader(
    downloaderID = downloaderID,
    name = name,
    days = DAYS_DAILY,
    utcAvailabilityOffset = 6.hours + 30.minutes, // guess
    supportUrl = SUPPORT_URL,
    puzzleParser = USATodayJSONIO(),
) {
    override suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val id = getPuzzleID(date)
        if (id == null) {
            AppLogger.e(TAG, "Could not get puzzle ID for $date")
            return null
        }

        val params = mapOf(
            "query" to
                "query CrosswordsSingleGame(\$id:String!)"
                    + "{__typename gameData(id:\$id){__typename "
                    + "...crosswordSingleGameData}}fragment "
                    + "crosswordSingleGameData on "
                    + "CrosswordData{__typename id date type title "
                    + "width author editor height layout downClue "
                    + "solution copyright acrossClue}",
            "variables" to buildJSONObject { put("id", id) }.toString(),
        )

        return getURL(QUERY_URL, HEADERS, params = params).use { input ->
            USATodayJSONIO.readPuzzle(input)?.apply {
                source = name
                supportUrl = SUPPORT_URL
                sourceUrl = SHARE_URL
                shareUrl = SHARE_URL
            }
        }
    }

    private suspend fun getPuzzleID(date : LocalDate) : String? {
        val now = TimeUtils.today()
        val diff = TimeUtils.durationBetween(date, now).inWholeDays
        if (diff < 0) {
            AppLogger.e(TAG, "Cannot get source URL from the future $date")
            return null
        }

        // get 7 puzzles at a time as a kind of fudge factor
        val page = diff / 7 + 1

        try {
            val params = mapOf(
                "query" to
                    "query anonymousCrosswordFindGameData("
                        + "\$type:String=\"crossword\"\$date:String\$pages:"
                        + "PagesInputType){__typename findGameData(type:\$type "
                        + "date:\$date pages:\$pages){__typename id date type "
                        + "...anonymousCrosswordDataPartsRecent}}"
                        + "fragment anonymousCrosswordDataPartsRecent on "
                        + "CrosswordData{__typename id date title author editor}",
                "variables" to
                    buildJSONObject {
                        put(
                            "pages",
                            buildJSONObject {
                                put("pageNum", page)
                                put("perPage", 7)
                            },
                        )
                        put("queryType", "crosswords_unfiltered_games")
                        put("type", "crossword")
                    }.toString(),
                "operationName" to "anonymousCrosswordFindGameData",
            )

            getURL(QUERY_URL, HEADERS, params = params).use { input ->
                val list = parseJSONObject(input)
                    .getJSONObject("data")
                    .getJSONArray("findGameData")

                for (i in 0 until list.size) {
                    val entry = list.getJSONObject(i)
                    try {
                        val entryDateStr = entry.getString("date")
                        val entryDate = LocalDate.Formats.ISO.parse(entryDateStr)
                        if (entryDate == date)
                            return entry.getString("id")
                    } catch (e : IllegalArgumentException) {
                        AppLogger.e(
                            TAG,
                            "Could not parse date, ignoring entry",
                        )
                    } catch (e : JSONException) {
                        AppLogger.e(
                            TAG,
                            "Ignoring entry due to JSON error $e",
                        )
                    }
                }
            }
        } catch (e : JSONException) {
            AppLogger.e(
                TAG,
                "Could not parse USA today crossword list response $e",
            )
        }

        return null
    }
}

