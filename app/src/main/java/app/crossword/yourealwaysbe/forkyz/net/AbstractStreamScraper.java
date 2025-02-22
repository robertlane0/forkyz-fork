
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import app.crossword.yourealwaysbe.forkyz.util.NetUtils;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;

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
public abstract class AbstractStreamScraper implements StreamScraper {
    private int timeoutMillis = 30000;

    /**
     * Check each line against the regex return group
     */
    protected static class RegexScrape {
        private Pattern regex;
        private int group;

        public RegexScrape(Pattern regex, int group) {
            this.regex = regex;
            this.group = group;
        }

        public Pattern getRegex() { return regex; }
        public int getGroup() { return group; }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return parseInput(is, null);
    }

    @Override
    public abstract Puzzle parseInput(
        InputStream is, String url
    ) throws Exception;

    public void setTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getTimeout() {
        return timeoutMillis;
    }

    protected BufferedInputStream getInputStream(
        URL url, Map<String, String> headers
    ) throws IOException {
        return NetUtils.getInputStream(url, headers, getTimeout());
    }

    protected BufferedInputStream getInputStream(
        String url
    ) throws IOException, URISyntaxException {
        return getInputStream((new URI(url)).toURL(), null);
    }

    /**
     * From HTML pick out a particular URL
     *
     * @param doc Jsoup document to search (or null, meaning null
     * returned)
     * @param selector css selector of elements to look at
     * @param attribute attribute of element to look at
     * @param regexScrape the regex scrape to use
     * @param extractString Java replacement string to turn match (of
     * full string) into the url. E.g. "$0" just to use the whole match.
     * @return first found url or null
     */
    protected static String htmlElementFindURL(
        Document doc,
        String selector, String attribute,
        Pattern regex, String extractString
    ) {
        if (doc == null)
            return null;
        for (Element ele : doc.select(selector)) {
            String att = ele.attr(attribute);
            Matcher m = regex.matcher(att);
            if (m.matches())
                return m.replaceFirst(extractString);
        }

        return null;
    }

    /**
     * From HTML pick out a particular URL to fetch and parse
     *
     * Tries first found URL, uses urlParser on the page pointed to.
     *
     * @param doc Jsoup document to search (or null, meaning null
     * returned)
     * @param selector css selector of elements to look at
     * @param attribute attribute of element to look at
     * @param regexScrape the regex scrape to use
     * @param extractString Java replacement string to turn match (of
     * full string) into the url. E.g. "$0" just to use the whole match.
     * @param urlParser used to parse the contents of the found URL
     * @return puzzle or null
     */
    protected Puzzle htmlElementScrape(
        Document doc,
        String selector, String attribute,
        Pattern regex, String extractString,
        PuzzleParser urlParser
    ) {
        if (doc == null)
            return null;

        String url = htmlElementFindURL(
            doc, selector, attribute, regex, extractString
        );

        Puzzle puz = null;
        if (url != null) {
            try (InputStream urlIS = getInputStream(url)) {
                puz = urlParser.parseInput(urlIS);
            } catch (Exception e) {
                // we tried
            }
        }

        return puz;
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
    public static String[] regexScrape(
        InputStream is, RegexScrape[] regexScrapes
    ) {
        String[] results = new String[regexScrapes.length];
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            // stop when reaches num of scrapes
            int countFreshHits = 0;

            String line;
            while (
                (line = reader.readLine()) != null
                    && countFreshHits < regexScrapes.length
            ) {
                for (int i = 0; i < regexScrapes.length; i++) {
                    if (results[i] != null)
                        continue;

                    Matcher matcher = regexScrapes[i].getRegex().matcher(line);
                    if (matcher.find()) {
                        results[i] = matcher.group(regexScrapes[i].getGroup());
                        countFreshHits += 1;
                    }
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return results;
    }

    /**
     * Convencience method for regexScrape with one match
     */
    public static String regexScrape(InputStream is, RegexScrape regexScrapes) {
        RegexScrape[] scrapes = { regexScrapes };
        return regexScrape(is, scrapes)[0];
    }

    /**
     * Parse input as HTML, return document or null
     */
    protected static Document getDocument(InputStream is) {
        try {
            return Jsoup.parse(is, null, "");
        } catch (IOException e) {
            return null;
        }
    }
}
