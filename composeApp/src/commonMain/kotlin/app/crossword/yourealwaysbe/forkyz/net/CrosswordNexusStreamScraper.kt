
package app.crossword.yourealwaysbe.forkyz.net

import java.util.regex.Pattern
import kotlinx.io.Source

import com.fleeksoft.ksoup.nodes.Document

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader

/**
 * Scraper for embedded Crossword Nexus puzzles
 */
class CrosswordNexusStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        return getDocument(input)?.let { doc ->
            htmlElementScrape(
                doc,
                "iframe", "src",
                PUZZLE_URL_RE, PUZZLE_URL_EXTRACT,
                PuzzleStreamReader(),
            )?.also { puz ->
                if (!isThere(puz.title)) {
                    doc.selectFirst("cw-title")?.let {
                        puz.title = it.text().trim()
                    }
                }

                if (!isThere(puz.author)) {
                    doc.selectFirst("cw-author")?.let {
                        puz.author = it.text().trim()
                    }
                }

                if (!isThere(puz.source)) {
                    if (isThere(puz.author))
                        puz.source = puz.author
                    else
                        puz.source = DEFAULT_SOURCE
                }

                if (!isThere(puz.copyright)) {
                    doc.selectFirst("cw-copyright")?.let {
                        puz.copyright = it.text().trim()
                    }
                }
            }
        }
    }

    /**
     * s is not null or empty
     */
    private fun isThere(s : String?) : Boolean {
        return s != null && !s.isEmpty()
    }

    companion object {
        private val PUZZLE_URL_RE = Pattern.compile(
            "(.*crosswordnexus.com).*puzzle=([^&]*).*",
        )
        private const val PUZZLE_URL_EXTRACT = "$1/$2"

        private const val DEFAULT_SOURCE = "Crossword Nexus"
    }
}

