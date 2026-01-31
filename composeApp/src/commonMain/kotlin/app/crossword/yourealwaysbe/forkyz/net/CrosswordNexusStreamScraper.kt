
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream
import java.util.regex.Pattern

import org.jsoup.nodes.Document

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader

/**
 * Scraper for embedded Crossword Nexus puzzles
 */
class CrosswordNexusStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        return getDocument(inputStream)?.let { doc ->
            htmlElementScrape(
                doc,
                "iframe", "src",
                PUZZLE_URL_RE, PUZZLE_URL_EXTRACT,
                PuzzleStreamReader(),
            )?.also { puz ->
                if (!isThere(puz.getTitle())) {
                    doc.selectFirst("cw-title")?.let {
                        puz.setTitle(it.text().trim())
                    }
                }

                if (!isThere(puz.getAuthor())) {
                    doc.selectFirst("cw-author")?.let {
                        puz.setAuthor(it.text().trim())
                    }
                }

                if (!isThere(puz.getSource())) {
                    if (isThere(puz.getAuthor()))
                        puz.setSource(puz.getAuthor())
                    else
                        puz.setSource(DEFAULT_SOURCE)
                }

                if (!isThere(puz.getCopyright())) {
                    doc.selectFirst("cw-copyright")?.let {
                        puz.setCopyright(it.text().trim())
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

