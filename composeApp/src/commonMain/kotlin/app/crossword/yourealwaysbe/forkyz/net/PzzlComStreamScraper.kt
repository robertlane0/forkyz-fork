
package app.crossword.yourealwaysbe.forkyz.net

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils

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
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        var bis = StreamUtils.makeByteArrayInputStream(inputStream)

        var embedURL : String? = regexScrape(bis, IFRAME_MATCH)
        if (embedURL != null) {
            try {
                getInputStream(embedURL).use { embedIS ->
                    bis = StreamUtils.makeByteArrayInputStream(embedIS)
                }
            } catch (e : IOException) {
                // fall through, try old bis as embedded
            }
        } else {
            // fallback to the provided URL
            embedURL = url
        }

        bis.reset()

        if (embedURL == null)
            return null

        val mainInfo = getMainInfo(embedURL, bis)
        if (mainInfo == null)
            return null

        val date = getDateString(url, mainInfo)
        if (date == null)
            return null

        try {
            getInputStream(makePuzzleURL(mainInfo, date)).use { puzIS ->
                val puz = BrainsOnlyIO.parse(puzIS)
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
        inputStream : InputStream,
    ) : MainInfo? {
        val mains = regexScrape(
            inputStream,
            arrayOf(PZZL_XWORD_ID, MAIN_JS_PATH),
        )
        val relPath = mains[1]
        if (relPath == null)
            return null

        val mainJSPath = makeMainJSPath(embedURL, relPath)
        try {
            getInputStream(mainJSPath).use { mainIS ->
                val results = regexScrape(
                    mainIS,
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
        } catch (e : URISyntaxException) {
            return null
        }
    }

    private fun makeMainJSPath(
        embedURL : String,
        relPath : String,
    ) : String {
        return URI.create(embedURL).resolve(relPath).toString()
    }

    /**
     * Get puzzle date from URL or use latest from pzzl archive
     */
    private suspend fun getDateString(
        originalURL : String?,
        mainInfo : MainInfo,
    ) : String? {
        if (originalURL != null) {
            val matcher = DATE_URL.regex.matcher(originalURL)
            if (matcher.matches())
                return matcher.group(DATE_URL.group)
        }

        try {
            BufferedReader(
                InputStreamReader(getInputStream(makeArchiveURL(mainInfo)))
            ).use { reader ->
                val first = reader.readLine()
                if (first != null)
                    return first.trim()
            }
        } catch (e : IOException) {
            /* fall through */
        } catch (e : URISyntaxException) {
            /* fall through */
        }

        return null
    }

    /**
     * Get URL for list of available puzzles from mainInfo
     */
    private fun makeArchiveURL(mainInfo : MainInfo) : String {
        return String.format(
            Locale.US,
            ARCHIVE_URL_FMT,
            mainInfo.baseURL,
            mainInfo.setName,
        )
    }

    /**
     * Make puzzle URL from mainInfo and date
     */
    private fun makePuzzleURL(mainInfo : MainInfo, date : String) : String {
        return String.format(
            Locale.US,
            PUZZLE_URL_FMT,
            mainInfo.baseURL,
            mainInfo.setName,
            date
        )
    }

    private inner class MainInfo(
        val setName : String,
        val baseURL : String,
    )

    companion object {
        private val IFRAME_MATCH = RegexScrape(
            Pattern.compile("src=\"([^\"]*pzzl.com/[^\"]*)\""),
            1,
        )
        private val PZZL_XWORD_ID = RegexScrape(
            Pattern.compile("id=\"pzzl-xword\""),
            0,
        )
        private val MAIN_JS_PATH = RegexScrape(
            Pattern.compile("src=\"([^\"]*main.js)\""),
            1,
        )

        private val MAIN_INFO = Pattern.compile(
            "var n=\"([^\"]*)\";n\\|\\|"
                + "\\(n=\"[^\"]*\"\\);"
                + "this.base_url=\"([^\"]*)\"",
        )
        private val MAIN_SET_NAME = RegexScrape(MAIN_INFO, 1)
        private val MAIN_BASE_URL = RegexScrape(MAIN_INFO, 2)

        private val DATE_URL = RegexScrape(
            Pattern.compile(".*\\/(\\d{6})"),
            1,
        )
        private val DATE_FORMATTER = LocalDate.Format {
            yearTwoDigits(2000)
            monthNumber()
            day()
        }

        // needs baseURL then setName
        private const val ARCHIVE_URL_FMT = "%s/%s?date=list&get=archivecurrent"

        // needs baseURL then setName then date in yymmdd format
        private const val PUZZLE_URL_FMT = "%s/%s?date=%s"

        private const val DEFAULT_SOURCE = "Pzzl.com"
    }
}

