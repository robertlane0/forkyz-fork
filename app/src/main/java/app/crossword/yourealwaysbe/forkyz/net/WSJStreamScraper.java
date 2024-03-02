
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.forkyz.util.NetUtils;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.WallStreetJournalJSONIO;

/**
 * Sraper for puzzles embedded in Wall Street Journal
 *
 * Check is Wall Street Journal by looking for wsj.com.
 *
 * Get puzzle data URL by looking for puzzles/crossword/yyyymmdd/xxxxx
 * where xxxxx is a numeric puzzle ID. Data is then
 * wsj.com/puzzles/crossword/yyyymmdd/xxxxx/data.json.
 *
 * Need Apache HttpClient library to get data.json, HttpURLConnection
 * gets rejected.
 */
public class WSJStreamScraper extends AbstractStreamScraper {
    private static final RegexScrape WSJ_MATCH =
        new RegexScrape(Pattern.compile("wsj.com"), 0);
    private static final RegexScrape PUZZLE_PATH_MATCH =
        new RegexScrape(
            Pattern.compile("puzzles/crossword/\\d{8}/\\d+"), 0
        );

    private static final String DEFAULT_SOURCE = "Wall Street Journal";

    private AndroidVersionUtils utils;

    public WSJStreamScraper(AndroidVersionUtils utils) {
        this.utils = utils;
    }

    // args: puzzles/crossword/yyyymmdd/xxxxx
    private static final String DATA_URL_FORMAT
        = "https://www.wsj.com/%s/data.json";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        String jsonURL = getJSONURL(is);

        if (jsonURL == null)
            return null;

        try(
            InputStream jsonIS
                = NetUtils.apacheGetInputStream(jsonURL, null, getTimeout())
        ) {
            Puzzle puz = WallStreetJournalJSONIO.readPuzzle(jsonIS);
            if (puz != null) {
                puz.setSource(DEFAULT_SOURCE);
            }
            return puz;
        } catch (Exception e) {
            // fall through
        }

        return null;
    }

    public String getJSONURL(InputStream is) {
        RegexScrape[] matchers = { WSJ_MATCH, PUZZLE_PATH_MATCH };
        String[] results = regexScrape(is, matchers);
        String isWSJ = results[0];
        String puzzlePath = results[1];

        if (isWSJ == null || puzzlePath == null)
            return null;
        else
            return String.format(Locale.US, DATA_URL_FORMAT, puzzlePath);
    }
}
