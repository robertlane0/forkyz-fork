
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

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
}
