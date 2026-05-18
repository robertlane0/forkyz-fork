
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.SlowDownWiseUpJSONIO
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils

/**
 * Get embedded SlowDownWiseUp puzzles
 *
 * Stage 1: look for slowdownwiseup somewhere then \"uuid\": \"theuuid\"
 *
 * Stage 2: get embedded puzzle from
 *  https://cdn2.amuselabs.com/puzzleme/crossword?id=<id>&set=<set>
 */
class SlowDownWiseUpStreamScraper() : AbstractStreamScraper() {
    companion object {
        private val TAG = "SlowDownWiseUpScrp"
        private val DEFAULT_SOURCE = "SlowDownWiseUp"
        private val IS_SLOWDOWNWISEUP_RE = Regex(
            "slowdownwiseup",
            RegexOption.IGNORE_CASE
        )
        private val STAGE2_URL_RE = Regex(
            // \\\\\\\" = \"!
            "\\\\\\\"uuid\\\\\\\":\\s*\\\\\\\"([0-9a-f-]*)\\\\\\\"",
            RegexOption.IGNORE_CASE
        )
        private val IS_SLOWDOWNWISEUP_REGEX
            = RegexScrape(IS_SLOWDOWNWISEUP_RE, 0)
        private val STAGE2_REGEX = RegexScrape(STAGE2_URL_RE, 1)
    }

    override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        AppLogger.i(TAG, "Trying SlowDownWiseUp scraper on " + url)
        val input2 = getStage2Input(input)
        try {
            return SlowDownWiseUpJSONIO.readPuzzle(input2)?.apply {
                if (source == null)
                    source = DEFAULT_SOURCE
            }
        } catch (e : IOException) {
            AppLogger.i(TAG, "Could not scrape PuzzleMe: " + e)
            return null
        }
    }

    /**
     * Look for SlowDownWiseUp or return reset inStream
     */
    private suspend fun getStage2Input(
        input : Source,
    ) : Source{
        val results = regexScrape(
            input.peek(),
            arrayOf(IS_SLOWDOWNWISEUP_REGEX, STAGE2_REGEX),
        )
        val isSlowDownWiseUp = results[0]
        val uuid = results[1]
        if (isSlowDownWiseUp == null || uuid == null) {
            return input
        } else {
            return getURL(
                "https://content-api.slowdownwiseup.co.uk/api/mobile/v1/"
                    + "puzzle-data/$uuid/file/data.json",
            )
        }
    }
}

