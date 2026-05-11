package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.util.getURL
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.SlowDownWiseUpJSONIO

/**
 * Scraper for puzzles embedded in Wall Street Journal
 *
 * Check is Wall Street Journal by looking for wsj.com.
 *
 * Get puzzle data URL by looking for puzzles/crossword/yyyymmdd/xxxxx
 * where xxxxx is a numeric puzzle ID. Data is then
 * wsj.com/puzzles/crossword/yyyymmdd/xxxxx/data.json.
 */
class WSJStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        val jsonURL = getJSONURL(input)
        if (jsonURL == null)
            return null

        try {
            getURL(jsonURL, timeout).use { jsonIS ->
                val puz = SlowDownWiseUpJSONIO.readPuzzle(jsonIS)
                puz?.source = DEFAULT_SOURCE
                return puz
            }
        } catch (e : Exception) {
            // fall through
        }

        return null
    }

    fun getJSONURL(input : Source) : String? {
        val matchers = arrayOf(WSJ_MATCH, PUZZLE_PATH_MATCH)
        val results = regexScrape(input, matchers)
        val isWSJ = results[0]
        val puzzlePath = results[1]

        if (isWSJ == null || puzzlePath == null)
            return null
        else
            return String.format(Locale.US, DATA_URL_FORMAT, puzzlePath)
    }

    companion object {
        private val WSJ_MATCH = RegexScrape(Regex("wsj.com"), 0)
        private val PUZZLE_PATH_MATCH = RegexScrape(
            Regex("puzzles/crossword/\\d{8}/\\d+"),
            0,
        )

        private const val DEFAULT_SOURCE = "Wall Street Journal"

        // args: puzzles/crossword/yyyymmdd/xxxxx
        private const val DATA_URL_FORMAT = "https://www.wsj.com/%s/data.json"
    }
}

