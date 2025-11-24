
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream
import java.util.regex.Pattern

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PAPuzzlesIO

/**
 * Scraper for embedded PA Puzzles pages
 */
class PAPuzzlesStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        val scrapes = regexScrape(
            inputStream,
            arrayOf(PA_PUZZLES_URL_RE, TITLE_RE),
        )

        val srcUrl = scrapes[0]
        val title = scrapes[1]

        var puz : Puzzle? = null
        if (srcUrl != null) {
            try {
                getInputStream(srcUrl).use { urlIS ->
                    puz = PAPuzzlesIO.readPuzzle(urlIS)
                }
            } catch (e : Exception) {
                // we tried
            }
        }

        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE)
            if (title != null)
                puz.setTitle(title.trim { it <= ' ' })
        }

        return puz
    }

    companion object {
        private val PA_PUZZLES_URL_RE = RegexScrape(
            Pattern.compile(
                "src=\\\\\"([^\"]*pa-puzzles.com[^\"]*)\\\\\"",
                Pattern.CASE_INSENSITIVE,
            ),
            1,
        )
        private val TITLE_RE = RegexScrape(
            Pattern.compile(
                "<title>([^<]*)<\\/title>",
                Pattern.CASE_INSENSITIVE,
            ),
            1,
        )

        private const val DEFAULT_SOURCE = "PA Puzzles"
    }
}

