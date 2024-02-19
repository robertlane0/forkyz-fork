
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PAPuzzlesIO;

/**
 * Scraper for embedded PA Puzzles pages
 */
public class PAPuzzlesStreamScraper extends AbstractStreamScraper {
    private static final Pattern PA_PUZZLES_URL_RE
        = Pattern.compile(".*pa-puzzles.com.*", Pattern.CASE_INSENSITIVE);
    private static final String PA_PUZZLES_URL_EXTRACT = "$0";

    private static final String DEFAULT_SOURCE = "PA Puzzles";

    @Override
    public Puzzle parseInput(InputStream is, String url) throws Exception {
        Document doc = getDocument(is);
        Puzzle puz = htmlElementScrape(
            doc,
            "iframe", "src",
            PA_PUZZLES_URL_RE, PA_PUZZLES_URL_EXTRACT,
            new PAPuzzlesIO()
        );
        if (puz != null) {
            puz.setSource(DEFAULT_SOURCE);

            Element title = doc.selectFirst("title");
            if (title != null)
                puz.setTitle(title.text().trim());
        }
        return puz;
    }
}
