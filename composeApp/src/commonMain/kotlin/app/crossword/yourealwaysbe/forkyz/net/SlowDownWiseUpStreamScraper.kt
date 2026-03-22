
package app.crossword.yourealwaysbe.forkyz.net

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.SlowDownWiseUpJSONIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils

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
        private val STAGE2_URL_FORMAT = (
            "https://content-api.slowdownwiseup.co.uk/api/mobile/v1/"
            + "puzzle-data/%s/file/data.json"
        )
        private val IS_SLOWDOWNWISEUP_RE = Pattern.compile(
            "slowdownwiseup",
            Pattern.CASE_INSENSITIVE
        )
        private val STAGE2_URL_RE = Pattern.compile(
            // \\\\\\\" = \"!
            "\\\\\\\"uuid\\\\\\\":\\s*\\\\\\\"([0-9a-f-]*)\\\\\\\"",
            Pattern.CASE_INSENSITIVE
        )
        private val IS_SLOWDOWNWISEUP_REGEX
            = RegexScrape(IS_SLOWDOWNWISEUP_RE, 0)
        private val STAGE2_REGEX = RegexScrape(STAGE2_URL_RE, 1)
    }

    override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        AppLogger.i(TAG, "Trying SlowDownWiseUp scraper on " + url)
        val is1 = StreamUtils.makeByteArrayInputStream(inputStream)
        val is2 = getStage2InputStream(is1)

        try {
            return SlowDownWiseUpJSONIO.readPuzzle(is2)?.apply {
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
    fun getStage2InputStream(
        bis : ByteArrayInputStream,
    ) : InputStream {
        val results = regexScrape(
            bis,
            arrayOf(IS_SLOWDOWNWISEUP_REGEX, STAGE2_REGEX),
        )
        val isSlowDownWiseUp = results[0]
        val uuid = results[1]
        if (isSlowDownWiseUp == null || uuid == null) {
            bis.reset()
            return bis
        } else {
            return getInputStream(STAGE2_URL_FORMAT.format(uuid))
        }
    }
}

