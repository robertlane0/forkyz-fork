
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern

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
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        // so we can reuse
        val bis = StreamUtils.makeByteArrayInputStream(inputStream)
        val secondStage = SecondStageParser()

        var puz : Puzzle? = null

        val secondStageURL = getSecondStageURL(bis)
        if (secondStageURL != null) {
            try {
                getInputStream(secondStageURL).use { ssIS ->
                    puz = secondStage.parseInput(ssIS)
                }
            } catch (e : Exception) {
                // fall through
            }
        }
        if (puz == null) {
            bis.reset()
            puz = secondStage.parseInput(bis)
        }

        puz?.setSource(DEFAULT_SOURCE)

        return puz
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    fun getSecondStageURL(inputStream : InputStream) : String? {
        return regexScrape(inputStream, SECOND_STAGE_URL_MATCH)
    }

    /**
     * Parse the page containing the puzzle ID and get puzzle
     *
     * Finds ID, then gets puzzle from API URL. Does not close input
     * stream.
     */
    fun parseSecondStage(inputStream : InputStream) : Puzzle? {
        val apiUrl = getAPIURL(inputStream)
        if (apiUrl == null)
            return null

        try {
            getInputStream(apiUrl).use { apiIS ->
                return RaetselZentraleSchwedenJSONIO.readPuzzle(apiIS)
            }
        } catch (e : Exception) {
            return null
        }
    }

    private inner class SecondStageParser : PuzzleParser {
        @Throws(Exception::class)
        override fun parseInput(inputStream : InputStream) : Puzzle? {
            return parseSecondStage(inputStream)
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
        fun getAPIURL(inputStream : InputStream) : String? {
            val id : String? = regexScrape(inputStream, PUZZLE_ID_MATCH)
            if (id != null)
                return String.format(Locale.US, JSON_URL_FORMAT, id)
            else
                return null
        }
    }
}

