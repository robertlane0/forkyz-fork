
package app.crossword.yourealwaysbe.forkyz.net

import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

import app.crossword.yourealwaysbe.forkyz.util.JSONUtils
import app.crossword.yourealwaysbe.puz.io.TCAJSONIO

private val PUZZLES_FEED_FORMAT = DateTimeFormatter.ofPattern(
    "'https://crosswords-sgweb.gamedistribution.com/feed/"
        + "tca_la-times-daily-crossword/year/'yyyy'/month/'M"
)
private val PUZZLES_FEED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Get embedded PuzzleMe puzzles
 *
 * Stage 1: get available puzzles from
 * https://crosswords-sgweb.gamedistribution.com/feed/tca_la-times-daily-crossword/year/2025/month/7
 *
 * {
 *      "author": ...
 *      "date": "YYYY-MM-DD"
 *      "feedUrl": url to json
 * }
 *
 * Stage 2: get json from feedUrl and parse as TCA JSON
 */
class LATimesDownloader(
    internalName : String,
    downloaderName : String,
) : AbstractDateDownloader(
    internalName,
    downloaderName,
    DAYS_DAILY,
    Duration.ofDays(-5), // was 7 days ahead when checked
    "https://subscription.chicagotribune.com/",
    TCAJSONIO(),
    null,
    "https://fun.chicagotribune.com/game/la-times-daily-crossword",
) {
    override fun getSourceUrl(date : LocalDate) : String? {
        val url = date.format(PUZZLES_FEED_FORMAT)
        return getInputStream(URL(url), null)?.use { input ->
            val puzzles = JSONUtils.streamToJSONArray(input)
            (0 until puzzles.length()).forEach {
                val puzzle = puzzles.getJSONObject(it)
                puzzle.optString("date")?.let { dateStr ->
                    try {
                        val puzzleDate = LocalDate.parse(
                            dateStr,
                            PUZZLES_FEED_DATE_FORMAT,
                        )
                        if (puzzleDate == date) {
                            puzzle.optString("feedUrl")?.let { puzURL ->
                                return puzURL
                            }
                        }
                    } catch (e : DateTimeParseException) {
                        // ignore
                    }
                }
            }
            return null
        }
    }
}
