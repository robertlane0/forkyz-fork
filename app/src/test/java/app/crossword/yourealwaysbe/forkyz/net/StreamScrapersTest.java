
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

import app.crossword.yourealwaysbe.forkyz.util.NetUtils;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;

public class StreamScrapersTest {

    private static final int TIMEOUT = 30000;

    @Test
    public void testCrosswordNexus() throws Exception {
        testScraper(
            "https://www.crosswordnexus.com/jonesin/",
            new CrosswordNexusStreamScraper()
        );
    }

    @Test
    public void testPAPuzzles() throws Exception {
        testScraper(
            "https://metro.co.uk/puzzles/quick-crosswords-online-free-daily-word-puzzle/",
            new PAPuzzlesStreamScraper()
        );
    }

    @Test
    public void testRaetselZentraleSchweden() throws Exception {
        testScraper(
            "https://www.abendblatt.de/ratgeber/wissen/article106560367/Spielen-Sie-hier-taeglich-das-kostenlose-Kreuzwortraetsel.html",
            new RaetselZentraleSchwedenStreamScraper()
        );
    }

    /**
     * Test scraper gets a puzzle from the URL
     */
    private static void testScraper(
        String url, PuzzleParser scraper
    ) throws Exception {
        try (InputStream is = NetUtils.getInputStream(url, null, TIMEOUT)) {
            Puzzle puz = scraper.parseInput(is);
            assertNotNull(puz);
        }
    }
}
