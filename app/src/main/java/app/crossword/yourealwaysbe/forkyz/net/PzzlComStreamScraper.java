
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

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
public class PzzlComStreamScraper extends AbstractStreamScraper {
    private static final RegexScrape IFRAME_MATCH
        = new RegexScrape(
            Pattern.compile("src=\"([^\"]*pzzl.com/[^\"]*)\""),
            1
        );
    private static final RegexScrape PZZL_XWORD_ID
        = new RegexScrape(Pattern.compile("id=\"pzzl-xword\""), 0);
    private static final RegexScrape MAIN_JS_PATH
        = new RegexScrape(Pattern.compile("src=\"([^\"]*main.js)\""), 1);

    private static final Pattern MAIN_INFO
        = Pattern.compile(
            "var n=\"([^\"]*)\";n\\|\\|"
            + "\\(n=\"[^\"]*\"\\);"
            + "this.base_url=\"([^\"]*)\""
        );
    private static final RegexScrape MAIN_SET_NAME
        = new RegexScrape(MAIN_INFO, 1);
    private static final RegexScrape MAIN_BASE_URL
        = new RegexScrape(MAIN_INFO, 2);

    private static final RegexScrape DATE_URL = new RegexScrape(
        Pattern.compile(".*\\/(\\d{6})"), 1
    );
    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyMMdd", Locale.US);

    // needs baseURL then setName
    private static final String ARCHIVE_URL_FMT
        = "%s/%s?date=list&get=archivecurrent";
    // needs baseURL then setName then date in yymmdd format
    private static final String PUZZLE_URL_FMT = "%s/%s?date=%s";

    private static final String DEFAULT_SOURCE = "Pzzl.com";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);

        String embedURL = regexScrape(bis, IFRAME_MATCH);
        if (embedURL != null) {
            try (InputStream embedIS = getInputStream(embedURL)) {
                bis = StreamUtils.makeByteArrayInputStream(embedIS);
            } catch (IOException e) {
                // fall through, try old bis as embedded
            }
        } else {
            // fallback to the provided URL
            embedURL = url;
        }

        bis.reset();
        MainInfo mainInfo = getMainInfo(embedURL, bis);
        if (mainInfo == null)
            return null;

        String date = getDateString(url, mainInfo);
        if (date == null)
            return null;

        try (
            InputStream puzIS = getInputStream(makePuzzleURL(mainInfo, date))
        ) {
            Puzzle puz = BrainsOnlyIO.parse(puzIS);
            if (puz != null) {
                if (puz.getSource() == null)
                    puz.setSource(DEFAULT_SOURCE);
                if (puz.getDate() == null) {
                    try {
                        puz.setDate(LocalDate.parse(date, DATE_FORMATTER));
                    } catch (DateTimeParseException e) {
                        // oh well, we tried
                    }
                }

                return puz;
            }
        } catch (IOException e) {
            /* fall through */
        }

        return null;
    }

    private MainInfo getMainInfo(
        String embedURL, InputStream is
    ) {
        String[] mains = regexScrape(
            is, new RegexScrape[] { PZZL_XWORD_ID, MAIN_JS_PATH }
        );
        if (mains[1] == null)
            return null;

        String mainJSPath = makeMainJSPath(embedURL, mains[1]);
        try (InputStream mainIS = getInputStream(mainJSPath)) {
            String[] results = regexScrape(
                mainIS,
                new RegexScrape[] { MAIN_SET_NAME, MAIN_BASE_URL }
            );

            if (results[0] == null || results[1] == null)
                return null;
            else
                return new MainInfo(results[0], results[1]);
        } catch (IOException | URISyntaxException e) {
            return null;
        }
    }

    private String makeMainJSPath(String embedURL, String relPath) {
        return URI.create(embedURL).resolve(relPath).toString();
    }

    /**
     * Get puzzle date from URL or use latest from pzzl archive
     */
    private String getDateString(String originalURL, MainInfo mainInfo) {
        if (originalURL != null) {
            Matcher matcher = DATE_URL.getRegex().matcher(originalURL);
            if (matcher.matches())
                return matcher.group(DATE_URL.getGroup());
        }

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(getInputStream(makeArchiveURL(mainInfo)))
            )
        ) {
            String first = reader.readLine();
            if (first != null)
                return first.trim();
        } catch (IOException | URISyntaxException e) {
            /* fall through */
        }

        return null;
    }

    /**
     * Get URL for list of available puzzles from mainInfo
     */
    private String makeArchiveURL(MainInfo mainInfo) {
        return String.format(
            Locale.US,
            ARCHIVE_URL_FMT,
            mainInfo.getBaseURL(),
            mainInfo.getSetName()
        );
    }

    /**
     * Make puzzle URL from mainInfo and date
     */
    private String makePuzzleURL(MainInfo mainInfo, String date) {
        return String.format(
            Locale.US,
            PUZZLE_URL_FMT,
            mainInfo.getBaseURL(),
            mainInfo.getSetName(),
            date
        );
    }

    private class MainInfo {
        private String setName;
        private String baseURL;

        public MainInfo(String setName, String baseURL) {
            this.setName = setName;
            this.baseURL = baseURL;
        }

        public String getSetName() { return setName; }
        public String getBaseURL() { return baseURL; }
    }
}
