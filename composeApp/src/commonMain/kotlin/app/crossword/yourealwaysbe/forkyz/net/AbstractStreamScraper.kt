
package app.crossword.yourealwaysbe.forkyz.net

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern
import kotlinx.io.asSource

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseSource

import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser

/**
 * Check each line against the regex return group
 */
class RegexScrape(
    val regex : Pattern,
    val group : Int,
)

/**
 * Base class for stream scrapers
 *
 * Stream scrapers are not quite puzzle parsers in that they don't parse
 * a stand-alone file. Instead they parse something like a webpage with
 * a puzzle embedded in an iframe. So they have to find the url then
 * download the puzzle from another URL.
 *
 * This is quite a common pattern for downloaders. The stream scraper
 * doesn't have the dates/source/context and can be used on streams
 * loaded from disk or streams from URLs shared with Forkyz. A
 * downloader can also use a stream scraper as its PuzzleParser.
 *
 * Provides some useful methods. Recommended to always use
 * getInputStream. If not, set a timeout of getTimeout().
 *
 * Bit janky that it's a copy-paste of AbstractDownloader..
 */
abstract class AbstractStreamScraper : StreamScraper {
    var timeout : Int = 30000

    @Throws(Exception::class)
    abstract override suspend fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle?

    @Throws(IOException::class)
    protected suspend fun getInputStream(
        url : URL,
        headers : Map<String, String>?,
    ) : BufferedInputStream {
        return getURLInputStream(url, timeout, headers)
    }

    @Throws(IOException::class, URISyntaxException::class)
    protected suspend fun getInputStream(url : String) : BufferedInputStream {
        return getInputStream((URI(url)).toURL(), null)
    }

    /**
     * From HTML pick out a particular URL to fetch and parse
     *
     * Tries first found URL, uses urlParser on the page pointed to.
     *
     * @param doc Ksoup document to search (or null, meaning null
     * returned)
     * @param selector css selector of elements to look at
     * @param attribute attribute of element to look at
     * @param regexScrape the regex scrape to use
     * @param extractString Java replacement string to turn match (of
     * full string) into the url. E.g. "$0" just to use the whole match.
     * @param urlParser used to parse the contents of the found URL
     * @return puzzle or null
     */
    protected suspend fun htmlElementScrape(
        doc : Document,
        selector : String,
        attribute : String,
        regex : Pattern,
        extractString : String,
        urlParser : PuzzleParser,
    ) : Puzzle? {
        return htmlElementFindURL(
            doc, selector, attribute, regex, extractString
        )?.let { url ->
            try {
                getInputStream(url).use { urlIS ->
                    urlParser.parseInput(urlIS)
                }
            } catch (e : Exception) {
                // we tried
                null
            }
        }
    }

    companion object {
        /**
         * From HTML pick out a particular URL
         *
         * @param doc Ksoup document to search (or null, meaning null
         * returned)
         * @param selector css selector of elements to look at
         * @param attribute attribute of element to look at
         * @param regexScrape the regex scrape to use
         * @param extractString Java replacement string to turn match (of
         * full string) into the url. E.g. "$0" just to use the whole match.
         * @return first found url or null
         */
        fun htmlElementFindURL(
            doc : Document,
            selector : String,
            attribute : String,
            regex : Pattern,
            extractString : String,
        ) : String? {
            for (ele in doc.select(selector)) {
                val att = ele.attr(attribute)
                val m = regex.matcher(att)
                if (m.find())
                    return m.replaceFirst(extractString)
            }

            return null
        }

        /**
         * Parse all of is and match against given regexes
         *
         * Return array of results, that is extract string for each match.
         * Null for those that did not match.
         *
         * Checks each line against each regex. I.e. doesn't stop at the first
         * match for a given line. Keeps first match for each regex.
         *
         * Will silently abort and return what we have so far if IOException.
         */
        fun regexScrape(
            inputStream : InputStream,
            regexScrapes : Array<RegexScrape>,
        ) : Array<String?> {
            val results = arrayOfNulls<String>(regexScrapes.size)
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))

                // stop when reaches num of scrapes
                var countFreshHits = 0

                var line : String?
                while (
                    (reader.readLine().also { line = it }) != null
                    && countFreshHits < regexScrapes.size
                ) {
                    for (i in regexScrapes.indices) {
                        if (results[i] != null)
                            continue

                        line?.let { line ->
                            val matcher = regexScrapes[i].regex.matcher(line)
                            if (matcher.find()) {
                                results[i] = matcher.group(
                                    regexScrapes[i].group,
                                )
                                countFreshHits += 1
                            }
                        }
                    }
                }
            } catch (e : IOException) {
                // fall through
            }
            return results
        }

        /**
         * Convencience method for regexScrape with one match
         */
        fun regexScrape(
            inputStream : InputStream,
            regexScrapes : RegexScrape,
        ) : String? {
            val scrapes = arrayOf<RegexScrape>(regexScrapes)
            return regexScrape(inputStream, scrapes)[0]
        }

        /**
         * Parse input as HTML, return document or null
         */
        fun getDocument(inputStream : InputStream) : Document? {
            try {
                return Ksoup.parseSource(inputStream.asSource())
            } catch (e : IOException) {
                return null
            }
        }
    }
}

