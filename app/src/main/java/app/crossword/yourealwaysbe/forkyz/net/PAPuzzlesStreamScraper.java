
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
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

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        try {
            Document doc = Jsoup.parse(is, null, "");
            Element iframe = doc.selectFirst("iframe");

            if (iframe != null) {
                String src = iframe.attr("src");
                if (PA_PUZZLES_URL_RE.matcher(src).matches()) {
                    Puzzle puz = PAPuzzlesIO.readPuzzle(
                        getInputStream(src)
                    );
                    if (puz != null) {
                        puz.setSource("PA Puzzles");

                        Element title = doc.selectFirst("title");
                        if (title != null)
                            puz.setTitle(title.text().trim());

                        return puz;
                    }
                }
            }
        } catch (IOException e) {
            // we tried
        }

        return null;
    }
}
