
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.io.RaetselZentraleSchwedenJSONIO;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Downloader for RaetselZentrale Swedish Crosswords
 *
 * Three levels:
 *
 * 1) Look for embedded RaetselZentrale iframe in page
 * 2) Get ID from embedded src page
 * 3) Get puzzle data from ID
 *
 * Embedded URL is
 * https://raetsel.raetselzentrale.de/l/<shortname>/<setname>/
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 *
 * Try first or second stage as starting point.
 */
public class RaetselZentraleSchwedenStreamScraper
        extends AbstractStreamScraper {

    // Note <iframe> may be generated in javascript. Hamburger uses
    // pagespeed_iframe... So look for something that looks like it's
    // creating it.
    private static final RegexScrape SECOND_STAGE_URL_MATCH
        = new RegexScrape(
            Pattern.compile(
                "iframe[^>]*src=\"([^\"]*raetsel\\.raetselzentrale\\.de[^\"]*)\""
            ),
            1
        );

    private static final RegexScrape PUZZLE_ID_MATCH
        = new RegexScrape(Pattern.compile("window.__riddleId = (\\d*);"), 1);

    private static final String DEFAULT_SOURCE = "Raetsel Zentrale";

    private static final String JSON_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/api/r/%s";

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        // so we can reuse
        ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);
        PuzzleParser secondStage = new SecondStageParser();

        Puzzle puz = null;

        String secondStageURL = getSecondStageURL(bis);
        if (secondStageURL != null) {
            try (InputStream ssIS = getInputStream(secondStageURL)) {
                puz = secondStage.parseInput(ssIS);
            } catch (Exception e) {
                // fall through
            }
        }
        if (puz == null) {
            bis.reset();
            puz = secondStage.parseInput(bis);
        }

        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE);
        }

        return puz;
    }

    /**
     * Parse the first page and get the embedded URL, or null
     *
     * Does not close input stream. Looking for iframes in a Jsoup
     * parsed doc doesn't seem to work. Perhaps the HTML is funky.
     */
    public String getSecondStageURL(InputStream is) {
        return regexScrape(is, SECOND_STAGE_URL_MATCH);
    }

    /**
     * Parse the page containing the puzzle ID and get puzzle
     *
     * Finds ID, then gets puzzle from API URL. Does not close input
     * stream.
     */
    public Puzzle parseSecondStage(InputStream is) {
        String apiUrl = getAPIURL(is);
        if (apiUrl == null)
            return null;

        try (
            InputStream apiIS = getInputStream(apiUrl)
        ) {
            return RaetselZentraleSchwedenJSONIO.readPuzzle(apiIS);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets API URL from input stream, does not close, or null
     */
    public static String getAPIURL(InputStream is) {
        String id = regexScrape(is, PUZZLE_ID_MATCH);
        if (id != null)
            return String.format(Locale.US, JSON_URL_FORMAT, id);
        else
            return null;

    }

    private class SecondStageParser implements PuzzleParser {
        @Override
        public Puzzle parseInput(InputStream is) throws Exception {
            return parseSecondStage(is);
        }
    }
}
