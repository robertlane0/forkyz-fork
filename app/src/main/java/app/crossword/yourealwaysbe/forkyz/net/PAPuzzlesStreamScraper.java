
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PAPuzzlesIO;

/**
 * Scraper for embedded PA Puzzles pages
 */
public class PAPuzzlesStreamScraper extends AbstractStreamScraper {
    private static final RegexScrape PA_PUZZLES_URL_RE = new RegexScrape(
        Pattern.compile(
            "src=\\\\\"([^\"]*pa-puzzles.com[^\"]*)\\\\\"",
            Pattern.CASE_INSENSITIVE
        ),
        1
    );
    private static final RegexScrape TITLE_RE = new RegexScrape(
        Pattern.compile(
            "<title>([^<]*)<\\/title>",
            Pattern.CASE_INSENSITIVE
        ),
        1
    );

    private static final String DEFAULT_SOURCE = "PA Puzzles";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        String[] scrapes = regexScrape(
            is,
            new RegexScrape[] { PA_PUZZLES_URL_RE, TITLE_RE }
        );

        String srcUrl = scrapes[0];
        String title = scrapes[1];

        Puzzle puz = null;
        if (srcUrl != null) {
            try (InputStream urlIS = getInputStream(srcUrl)) {
                puz = PAPuzzlesIO.readPuzzle(urlIS);
            } catch (Exception e) {
                // we tried
            }
        }

        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE);
            if (title != null)
                puz.setTitle(title.trim());
        }

        return puz;
    }
}
