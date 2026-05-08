
package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import java.util.regex.Pattern
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.puz.io.RaetselZentraleSchwedenJSONIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Downloader for RaetselZentrale Swedish Crosswords
 *
 * Three levels:
 *
 * 1) Look for embedded RaetselZentrale iframe in page
 * 2) Get ID from embedded src page
 * 3) Get puzzle data from ID
 *
 * Embedded URL is
 * https://raetsel.raetselzentrale.de/l/<shortname>/<setname>/
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 *
 * Try first or second stage as starting point.
 */
class RaetselZentraleSchwedenStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        // so we can reuse
        val secondStage = SecondStageParser()

        var puz : Puzzle? = null

        val secondStageURL = getSecondStageURL(input.peek())
        if (secondStageURL != null) {
            try {
                getURL(secondStageURL).use { ss ->
                    puz = secondStage.parseInput(ss)
                }
            } catch (e : Exception) {
                // fall through
            }
        }

        if (puz == null) {
            puz = secondStage.parseInput(input)
        }

        puz?.source = DEFAULT_SOURCE

        return puz
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    fun getSecondStageURL(input : Source) : String? {
        return regexScrape(input, SECOND_STAGE_URL_MATCH)
    }

    /**
     * Parse the page containing the puzzle ID and get puzzle
     *
     * Finds ID, then gets puzzle from API URL. Does not close input
     * stream.
     */
    private suspend fun parseSecondStage(input : Source) : Puzzle? {
        val apiUrl = getAPIURL(input)
        if (apiUrl == null)
            return null

        try {
            getURL(apiUrl).use { apiIn ->
                return RaetselZentraleSchwedenJSONIO.readPuzzle(apiIn)
            }
        } catch (e : Exception) {
            return null
        }
    }

    private inner class SecondStageParser : StreamScraper {
        @Throws(Exception::class)
        override suspend fun parseInput(
            input : Source,
            url : String?,
        ) : Puzzle? {
            return parseSecondStage(input)
        }
    }

    companion object {
        // Note <iframe> may be generated in javascript. Hamburger uses
        // pagespeed_iframe... So look for something that looks like it's
        // creating it.
        private val SECOND_STAGE_URL_MATCH = RegexScrape(
            Pattern.compile(
                "https://raetsel.raetselzentrale.de/.*schwede"
            ),
            0,
        )

        private val PUZZLE_ID_MATCH
            = RegexScrape(Pattern.compile("window.__riddleId = (\\d*);"), 1)

        private const val DEFAULT_SOURCE = "Raetsel Zentrale"

        private const val JSON_URL_FORMAT
            = "https://raetsel.raetselzentrale.de/api/r/%s"

        /**
         * Gets API URL from input stream, does not close, or null
         */
        fun getAPIURL(input : Source) : String? {
            val id : String? = regexScrape(input, PUZZLE_ID_MATCH)
            if (id != null)
                return String.format(Locale.US, JSON_URL_FORMAT, id)
            else
                return null
        }
    }
}

