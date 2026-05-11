
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PAPuzzlesIO

/**
 * Scraper for embedded PA Puzzles pages
 */
class PAPuzzlesStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        val scrapes = regexScrape(
            input,
            arrayOf(PA_PUZZLES_URL_RE, TITLE_RE),
        )

        val srcUrl = scrapes[0]
        val title = scrapes[1]

        var puz : Puzzle? = null
        if (srcUrl != null) {
            try {
                getURL(srcUrl).use { urlIn ->
                    puz = PAPuzzlesIO.readPuzzle(urlIn)
                }
            } catch (e : Exception) {
                // we tried
            }
        }

        if (puz != null) {
            puz.source = DEFAULT_SOURCE
            if (title != null)
                puz.title = title.trim()
        }

        return puz
    }

    companion object {
        private val PA_PUZZLES_URL_RE = RegexScrape(
            Regex(
                "src=\\\\\"([^\"]*pa-puzzles.com[^\"]*)\\\\\"",
                RegexOption.IGNORE_CASE,
            ),
            1,
        )
        private val TITLE_RE = RegexScrape(
            Regex(
                "<title>([^<]*)<\\/title>",
                RegexOption.IGNORE_CASE,
            ),
            1,
        )

        private const val DEFAULT_SOURCE = "PA Puzzles"
    }
}

