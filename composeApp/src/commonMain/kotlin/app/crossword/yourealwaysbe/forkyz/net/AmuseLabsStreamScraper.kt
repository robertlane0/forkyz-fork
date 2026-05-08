
package app.crossword.yourealwaysbe.forkyz.net

import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Downloader for embedded AmuseLabs crosswords
 */
class AmuseLabsStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        // so we can reuse
        val bis = StreamUtils.makeCopy(input)
        val secondStage = SecondStageParser()

        var puz : Puzzle? = null

        val secondStageURL = getSecondStageURL(bis.peek())
        if (secondStageURL != null) {
            try {
                getURL(secondStageURL).use { ssIn ->
                    puz = secondStage.parseInput(ssIn)
                }
            } catch (e : Exception) {
                // fall through
            }
        }
        if (puz == null) {
            puz = secondStage.parseInput(bis)
        }

        if (puz != null) {
            puz.source = DEFAULT_SOURCE
        }

        return puz
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    fun getSecondStageURL(input : Source) : String? {
        try {
            var line : String?
            while ((input.readLine().also { line = it }) != null) {
                line?.let { line ->
                    val matcher : Matcher = SECOND_STAGE_URL_RE.matcher(line)
                    if (matcher.find()) {
                        return matcher.group(SECOND_STAGE_URL_GRP)
                    }
                }
            }
        } catch (e : IOException) {
            // fall through
        }
        return null
    }

    /**
     * Parse the page containing the puzzle ID and get puzzle
     *
     * Finds ID, then gets puzzle from API URL. Does not close input
     * stream.
     */
    fun parseSecondStage(input : Source) : Puzzle? {
        try {
            var line : String?
            while ((input.readLine().also { line = it }) != null) {
                //Matcher matcher = PUZZLE_ID_PAT.matcher(line);
                //if (matcher.find()) {
                //    String id = matcher.group(PUZZLE_ID_GROUP);
                //    return String.format(Locale.US, JSON_URL_FORMAT, id);
                //}
            }
        } catch (e : IOException) {
            // fall through
        }
        return null
    }

    private inner class SecondStageParser : PuzzleParser {
        @Throws(Exception::class)
        override fun parseInput(input : Source) : Puzzle? {
            return parseSecondStage(input)
        }
    }

    companion object {
        // Note <iframe> may be generated in javascript. Hamburger uses
        // pagespeed_iframe... So look for something that looks like it's
        // creating it.
        private val SECOND_STAGE_URL_RE : Pattern = Pattern.compile(
            "iframe[^>]*src=\"([^\"]*amuselabs\\.com[^\"]*crossword[^\"]*)\""
        )
        private const val SECOND_STAGE_URL_GRP = 1

        private const val DEFAULT_SOURCE = "AmuseLabs"
    }
}
