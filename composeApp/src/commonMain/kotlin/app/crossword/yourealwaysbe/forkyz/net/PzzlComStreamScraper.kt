
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.datetime.LocalDate
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.BrainsOnlyIO
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils
import app.crossword.yourealwaysbe.forkyz.util.resolveUrl

/**
 * Downloader for Pzzl.com embedded Crosswords
 *
 * Look for iframe with src https://blah.pzzl.com/blah
 * Then at that URL, find the path to main.js
 * From main.js, figure out the puzzle set name
 * Then get puzzle date either from original URL or take most recent
 * from puzzle archive.
 * Then download puzzle, which is BrainsOnly format
 */
class PzzlComStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        var bis = StreamUtils.makeCopy(input)

        var embedURL = regexScrape(bis.peek(), IFRAME_MATCH)
        if (embedURL != null) {
            try {
                getURL(embedURL).use { embedIn ->
                    bis = StreamUtils.makeCopy(embedIn)
                }
            } catch (e : IOException) {
                // fall through, try old bis as embedded
            }
        } else {
            // fallback to the provided URL
            embedURL = url
        }

        if (embedURL == null)
            return null

        val mainInfo = getMainInfo(embedURL, bis)
        if (mainInfo == null)
            return null

        val date = getDateString(url, mainInfo)
        if (date == null)
            return null

        try {
            getURL(makePuzzleURL(mainInfo, date)).use { puzIn ->
                val puz = BrainsOnlyIO.parse(puzIn)
                if (puz != null) {
                    if (puz.source == null)
                        puz.source = DEFAULT_SOURCE
                    if (puz.date == null) {
                        try {
                            puz.date = DATE_FORMATTER.parse(date)
                        } catch (e : IllegalArgumentException) {
                            // oh well, we tried
                        }
                    }

                    return puz
                }
            }
        } catch (e : IOException) {
            /* fall through */
        }

        return null
    }

    private suspend fun getMainInfo(
        embedURL : String,
        input : Source,
    ) : MainInfo? {
        val mains = regexScrape(
            input,
            arrayOf(PZZL_XWORD_ID, MAIN_JS_PATH),
        )
        val relPath = mains[1]
        if (relPath == null)
            return null

        val mainJSPath = makeMainJSPath(embedURL, relPath)
        if (mainJSPath == null)
            return null

        try {
            getURL(mainJSPath).use { mainIn ->
                val results = regexScrape(
                    mainIn,
                    arrayOf(MAIN_SET_NAME, MAIN_BASE_URL),
                )
                val setName = results[0]
                val baseURL = results[1]
                if (setName == null || baseURL == null)
                    return null
                else return MainInfo(setName, baseURL)
            }
        } catch (e : IOException) {
            return null
        }
    }

    private fun makeMainJSPath(
        embedURL : String,
        relPath : String,
    ) : String? {
        return try {
            resolveUrl(embedURL, relPath)
        } catch (e : IllegalArgumentException) {
            null
        }
    }

    /**
     * Get puzzle date from URL or use latest from pzzl archive
     */
    private suspend fun getDateString(
        originalURL : String?,
        mainInfo : MainInfo,
    ) : String? {
        if (originalURL != null) {
            val match = DATE_URL.regex.matchEntire(originalURL)
            if (match != null)
                return match.groupValues[DATE_URL.group]
        }

        try {
            getURL(makeArchiveURL(mainInfo)).use { archiveIn ->
                val first = archiveIn.readLine()
                if (first != null)
                    return first.trim()
            }
        } catch (e : IOException) {
            /* fall through */
        }

        return null
    }

    /**
     * Get URL for list of available puzzles from mainInfo
     */
    private fun makeArchiveURL(mainInfo : MainInfo) : String {
        return (
            "${mainInfo.baseURL}/${mainInfo.setName}"
            + "?date=list&get=archivecurrent"
        )
    }

    /**
     * Make puzzle URL from mainInfo and date
     */
    private fun makePuzzleURL(mainInfo : MainInfo, date : String) : String {
        return "${mainInfo.baseURL}/${mainInfo.setName}?date=${date}"
    }

    private inner class MainInfo(
        val setName : String,
        val baseURL : String,
    )

    companion object {
        private val IFRAME_MATCH = RegexScrape(
            Regex("src=\"([^\"]*pzzl.com/[^\"]*)\""),
            1,
        )
        private val PZZL_XWORD_ID = RegexScrape(
            Regex("id=\"pzzl-xword\""),
            0,
        )
        private val MAIN_JS_PATH = RegexScrape(
            Regex("src=\"([^\"]*main.js[^\"]*)\""),
            1,
        )

        private val MAIN_INFO = Regex(
            "var n=\"([^\"]*)\";n\\|\\|"
                + "\\(n=\"[^\"]*\"\\);"
                + "this.base_url=\"([^\"]*)\"",
        )
        private val MAIN_SET_NAME = RegexScrape(MAIN_INFO, 1)
        private val MAIN_BASE_URL = RegexScrape(MAIN_INFO, 2)

        private val DATE_URL = RegexScrape(
            Regex(".*\\/(\\d{6})"),
            1,
        )
        private val DATE_FORMATTER = LocalDate.Format {
            yearTwoDigits(2000)
            monthNumber()
            day()
        }

        private const val DEFAULT_SOURCE = "Pzzl.com"
    }
}

