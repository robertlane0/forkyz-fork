
package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.PuzzleStreamReader

/**
 * Downloader for Crossweird embedded Crosswords
 *
 * Look for
 *
 * https://solver.crossweirdpuzzles.com/?file=puzzles/uploads/Get-The-Hell-Out-Of-Coachella-fced4b71165794b7dc755203ed3c4742.puz
 *
 * then get
 *
 * https://solver.crossweirdpuzzles.com/puzzles/uploads/Get-The-Hell-Out-Of-Coachella-fced4b71165794b7dc755203ed3c4742.puz
 */
class CrossweirdStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        val puzURL = getPuzURL(input) ?: getPuzURL(url)
        if (puzURL == null)
            return null

        try {
            getURL(puzURL).use { puzIn ->
                val puz = PuzzleStreamReader.parseInputStatic(puzIn)
                puz?.source = DEFAULT_SOURCE
                return puz
            }
        } catch (e : Exception) {
            // fall through
        }

        return null
    }

    fun getPuzURL(input : Source) : String? {
        val path = regexScrape(input, PUZ_PATH_MATCH)
        if (path != null) {
            return String.format(
                Locale.US,
                PUZ_URL_FORMAT,
                path,
            )
        } else {
            return null
        }
    }

    fun getPuzURL(url : String?) : String? {
        if (url == null)
            return null

        val match = PUZ_PATH_MATCH.regex.find(url)
        if (match != null) {
            val path = match.groupValues[PUZ_PATH_MATCH.group]
            return String.format(
                Locale.US,
                PUZ_URL_FORMAT,
                path,
            )
        }

        return null
    }

    companion object {
        private val PUZ_PATH_MATCH = RegexScrape(
            Regex("crossweirdpuzzles.com[^\" ]*file=([^&\" ]*)"),
            1,
        )

        private const val DEFAULT_SOURCE = "Crossweird Puzzles"

        // args: path
        private const val PUZ_URL_FORMAT = "https://solver.crossweirdpuzzles.com/%s"
    }
}

