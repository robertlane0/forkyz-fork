
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseSource

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.forkyz.util.getURL

/**
 * Check each line against the regex return group
 */
class RegexScrape(
    val regex : Regex,
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
 * getURL. If not, set a timeout of getTimeout().
 *
 * Bit janky that it's a copy-paste of AbstractDownloader..
 */
abstract class AbstractStreamScraper : StreamScraper {
    var timeout : Int = 30000

    @Throws(Exception::class)
    abstract override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle?

    @Throws(IOException::class, CancellationException::class)
    protected suspend fun getURL(
        url : String,
        headers : Map<String, String>? = null,
    ) : Source {
        return getURL(url, timeout, headers)
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
        regex : Regex,
        extractString : String,
        urlParser : PuzzleParser,
    ) : Puzzle? {
        return htmlElementFindURL(
            doc, selector, attribute, regex, extractString
        )?.let { url ->
            try {
                getURL(url).use { urlParser.parseInput(it) }
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
            regex : Regex,
            extractString : String,
        ) : String? {
            for (ele in doc.select(selector)) {
                val att = ele.attr(attribute)
                val m = regex.find(att)
                if (m != null)
                    return regex.replaceFirst(att, extractString)
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
            input : Source,
            regexScrapes : Array<RegexScrape>,
        ) : Array<String?> {
            val results = arrayOfNulls<String>(regexScrapes.size)
            try {
                // stop when reaches num of scrapes
                var countFreshHits = 0

                var line : String?
                while (
                    (input.readLine().also { line = it }) != null
                    && countFreshHits < regexScrapes.size
                ) {
                    for (i in regexScrapes.indices) {
                        if (results[i] != null)
                            continue

                        line?.let { line ->
                            val match = regexScrapes[i].regex.find(line)
                            if (match != null) {
                                results[i] = match.groupValues[
                                    regexScrapes[i].group
                                ]
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
            input : Source,
            regexScrapes : RegexScrape,
        ) : String? {
            val scrapes = arrayOf<RegexScrape>(regexScrapes)
            return regexScrape(input, scrapes)[0]
        }

        /**
         * Parse input as HTML, return document or null
         */
        fun getDocument(input : Source) : Document? {
            try {
                return Ksoup.parseSource(input)
            } catch (e : IOException) {
                return null
            }
        }
    }
}

