
package app.crossword.yourealwaysbe.forkyz.net

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.jsoup.nodes.Document

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PMLIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Scraper for embedded PML Puzzles pages
 */
class PMLStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        var bis = StreamUtils.makeByteArrayInputStream(inputStream)

        val embedURL : String? = getPMLURL(bis)
        if (embedURL != null) {
            try {
                getInputStream(embedURL).use { embedIS ->
                    bis = StreamUtils.makeByteArrayInputStream(embedIS)
                }
            } catch (e : IOException) {
                // fall through, try old bis as embedded
                bis.reset()
            }
        } else {
            bis.reset()
        }

        var puz : Puzzle? = null

        val embedJSON : String? = getPMLJSON(bis)
        if (embedJSON != null)
            puz = PMLIO.readFromJSON(embedJSON)

        if (puz == null) {
            bis.reset()
            puz = PMLIO.readPuzzle(bis)
        }

        if (puz != null)
            puz.source = DEFAULT_SOURCE

        return puz
    }

    companion object {
        private val PML_URL_RE : Pattern = Pattern.compile(
            ".*/puzzles/[a-z-]*crossword/(\\d{6}|latest).html?",
            Pattern.CASE_INSENSITIVE,
        )
        private const val PML_URL_EXTRACT = "$0"

        private val IS_LATEST_RE : Pattern = Pattern.compile(
            "(.*)latest(.html?)",
            Pattern.CASE_INSENSITIVE,
        )
        private const val IS_LATEST_PRE_URL_GRP = 1
        private const val IS_LATEST_POST_URL_GRP = 2

        private val EMBED_JSON_MATCH = RegexScrape(
            Pattern.compile("\\{\"pml_id\".*\\}"),
            0,
        )

        private val DATE_FORMATTER
            = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

        private const val DEFAULT_SOURCE = "PML Puzzles"

        /**
         * Find url
         *
         * Replace latest.html with yyyyMMdd.html. Return null if nothing
         * that looks like a PML URL found.
         */
        private fun getPMLURL(inputStream : InputStream) : String? {
            val doc = getDocument(inputStream)
            if (doc == null)
                return null

            val url = htmlElementFindURL(
                doc,
                "iframe",
                "src",
                PML_URL_RE,
                PML_URL_EXTRACT,
            )

            if (url == null)
                return null

            val m = IS_LATEST_RE.matcher(url)
            if (m.matches()) {
                val today = DATE_FORMATTER.format(LocalDate.now())
                return (
                    (m.group(IS_LATEST_PRE_URL_GRP) ?: "")
                    + today
                    + (m.group(IS_LATEST_POST_URL_GRP) ?: "")
                )
            } else {
                return url
            }
        }

        /**
         * Find directly embedded JSON
         */
        private fun getPMLJSON(inputStream : InputStream) : String? {
            return regexScrape(inputStream, EMBED_JSON_MATCH)
        }
    }
}

