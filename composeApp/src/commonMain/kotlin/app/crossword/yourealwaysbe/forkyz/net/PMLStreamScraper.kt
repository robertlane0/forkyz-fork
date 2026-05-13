
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.io.IOException
import kotlinx.io.Source

import com.fleeksoft.ksoup.nodes.Document

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.PMLIO
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

/**
 * Scraper for embedded PML Puzzles pages
 */
class PMLStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        var bis = StreamUtils.makeCopy(input)

        val embedURL : String? = getPMLURL(bis.peek())
        if (embedURL != null) {
            try {
                getURL(embedURL).use { embedIn ->
                    bis = StreamUtils.makeCopy(embedIn)
                }
            } catch (e : IOException) {
                // fall through, try old bis as embedded
            }
        }

        var puz : Puzzle? = null

        val embedJSON = getPMLJSON(bis.peek())
        if (embedJSON != null)
            puz = PMLIO.readFromJSON(embedJSON)

        if (puz == null) {
            puz = PMLIO.readPuzzle(bis)
        }

        if (puz != null)
            puz.source = DEFAULT_SOURCE

        return puz
    }

    companion object {
        private val PML_URL_RE = Regex(
            ".*/puzzles/[a-z-]*crossword/(\\d{6}|latest).html?",
            RegexOption.IGNORE_CASE,
        )
        private const val PML_URL_EXTRACT = "$0"

        private val IS_LATEST_RE = Regex(
            "(.*)latest(.html?)",
            RegexOption.IGNORE_CASE,
        )
        private const val IS_LATEST_PRE_URL_GRP = 1
        private const val IS_LATEST_POST_URL_GRP = 2

        private val EMBED_JSON_MATCH = RegexScrape(
            Regex("\\{\"pml_id\".*\\}"),
            0,
        )

        private val DATE_FORMATTER = LocalDate.Format {
            year()
            monthNumber()
            day()
        }

        private const val DEFAULT_SOURCE = "PML Puzzles"

        /**
         * Find url
         *
         * Replace latest.html with yyyyMMdd.html. Return null if nothing
         * that looks like a PML URL found.
         */
        private fun getPMLURL(input : Source) : String? {
            val doc = getDocument(input)
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

            val m = IS_LATEST_RE.matchEntire(url)
            if (m != null) {
                val today = TimeUtils.today().format(DATE_FORMATTER)
                return (
                    m.groupValues[IS_LATEST_PRE_URL_GRP]
                    + today
                    + m.groupValues[IS_LATEST_POST_URL_GRP]
                )
            } else {
                return url
            }
        }

        /**
         * Find directly embedded JSON
         */
        private fun getPMLJSON(input : Source) : String? {
            return regexScrape(input, EMBED_JSON_MATCH)
        }
    }
}

