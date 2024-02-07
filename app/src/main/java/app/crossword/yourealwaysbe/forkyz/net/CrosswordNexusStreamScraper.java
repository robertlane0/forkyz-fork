
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader;

/**
 * Scraper for embedded Crossword Nexus puzzles
 */
public class CrosswordNexusStreamScraper extends AbstractStreamScraper {
    private static final Pattern PUZZLE_URL_RE
        = Pattern.compile("(.*crosswordnexus.com).*puzzle=([^&]*).*");
    private static final String PUZZLE_URL_EXTRACT = "$1/$2";

    private static final String DEFAULT_SOURCE = "Crossword Nexus";

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        Document doc = getDocument(is);
        Puzzle puz = htmlElementScrape(
            doc,
            "iframe", "src",
            PUZZLE_URL_RE, PUZZLE_URL_EXTRACT,
            new PuzzleStreamReader()
        );
        if (puz != null) {
            if (!isThere(puz.getTitle())) {
                Element title = doc.selectFirst("cw-title");
                if (title != null)
                    puz.setTitle(title.text().trim());
            }

            if (!isThere(puz.getAuthor())) {
                Element author = doc.selectFirst("cw-author");
                if (author != null)
                    puz.setAuthor(author.text().trim());
            }

            if (!isThere(puz.getSource())) {
                if (isThere(puz.getAuthor()))
                    puz.setSource(puz.getAuthor());
                else
                    puz.setSource(DEFAULT_SOURCE);
            }

            if (!isThere(puz.getCopyright())) {
                Element copyright = doc.selectFirst("cw-copyright");
                if (copyright != null)
                    puz.setCopyright(copyright.text().trim());
            }
        }
        return puz;
    }

    /**
     * s is not null or empty
     */
    private boolean isThere(String s) {
        return s != null && !s.isEmpty();
    }
}
