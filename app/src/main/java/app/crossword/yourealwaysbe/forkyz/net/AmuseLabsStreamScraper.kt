
package app.crossword.yourealwaysbe.forkyz.net

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Downloader for embedded AmuseLabs crosswords
 */
class AmuseLabsStreamScraper : AbstractStreamScraper() {
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

        println("FORKYZ: second stage " + secondStageURL)

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

        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE)
        }

        return puz
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    fun getSecondStageURL(inputStream : InputStream) : String? {
        try {
            val reader = BufferedReader(
                InputStreamReader(inputStream)
            )

            var line : String?
            while ((reader.readLine().also { line = it }) != null) {
                println("FORKYZ: " + line)
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
    fun parseSecondStage(inputStream : InputStream) : Puzzle? {
        try {
            val reader = BufferedReader(
                InputStreamReader(inputStream)
            )

            var line : String?
            while ((reader.readLine().also { line = it }) != null) {
                println("FORKYZ: " + line)
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
        override fun parseInput(inputStream : InputStream) : Puzzle? {
            return parseSecondStage(inputStream)
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
