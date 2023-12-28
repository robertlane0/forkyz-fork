
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public abstract class AbstractStreamScraper implements PuzzleParser {
    private int timeoutMillis = 30000;

    @Override
    public abstract Puzzle parseInput (InputStream is) throws Exception;

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
    ) throws IOException {
        return getInputStream(new URL(url), null);
    }

    /**
     * From HTML pick out a particular URL to fetch and parse
     *
     * Tries all until one found, then uses urlParser on the page
     * pointed to.
     *
     * @param doc Jsoup document to search (or null, meaning null
     * returned)
     * @param selector css selector of elements to look at
     * @param attribute attribute of element to look at
     * @param regex regex to match attribute
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
        for (Element ele : doc.select(selector)) {
            String att = ele.attr(attribute);
            Matcher m = regex.matcher(att);
            if (m.matches()) {
                String url = m.replaceFirst(extractString);
                try (InputStream urlIS = getInputStream(url)) {
                    Puzzle puz = urlParser.parseInput(urlIS);
                    if (puz != null)
                        return puz;
                } catch (Exception e) {
                    // on to the next
                }
            }
        }

        return null;
    }

    /**
     * Parse input as HTML, return document or null
     */
    protected Document getDocument(InputStream is) {
        try {
            return Jsoup.parse(is, null, "");
        } catch (IOException e) {
            return null;
        }
    }
}
