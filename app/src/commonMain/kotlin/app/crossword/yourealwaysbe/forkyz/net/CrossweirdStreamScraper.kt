
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader

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
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        val puzURL = getPuzURL(inputStream) ?: getPuzURL(url)
        if (puzURL == null)
            return null

        try {
            getInputStream(puzURL).use { puzIS ->
                val puz = PuzzleStreamReader.parseInputStatic(puzIS)
                puz?.setSource(DEFAULT_SOURCE)
                return puz
            }
        } catch (e : Exception) {
            // fall through
        }

        return null
    }

    fun getPuzURL(inputStream : InputStream) : String? {
        val path = regexScrape(inputStream, PUZ_PATH_MATCH)
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

        val matcher = PUZ_PATH_MATCH.regex.matcher(url)
        if (matcher.find()) {
            val path = matcher.group(PUZ_PATH_MATCH.group)
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
            Pattern.compile("crossweirdpuzzles.com[^\" ]*file=([^&\" ]*)"),
            1,
        )

        private const val DEFAULT_SOURCE = "Crossweird Puzzles"

        // args: path
        private const val PUZ_URL_FORMAT = "https://solver.crossweirdpuzzles.com/%s"
    }
}

