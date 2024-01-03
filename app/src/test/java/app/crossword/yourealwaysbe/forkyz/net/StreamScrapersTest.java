
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

import app.crossword.yourealwaysbe.forkyz.util.NetUtils;
import app.crossword.yourealwaysbe.forkyz.versions.TiramisuUtil;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;

public class StreamScrapersTest {

    private static final int TIMEOUT = 30000;

    private static final TiramisuUtil utils = new TiramisuUtil();

    @Test
    public void testCrosswordNexus() throws Exception {
        testScraper(
            "https://www.crosswordnexus.com/jonesin/",
            new CrosswordNexusStreamScraper()
        );
    }

    @Test
    public void testKeesing() throws Exception {
        testScraper(
            "https://www.standaard.be/kruiswoordraadsel",
            new KeesingStreamScraper()
        );
    }

    @Test
    public void testPAPuzzles() throws Exception {
        testScraper(
            "https://metro.co.uk/puzzles/quick-crosswords-online-free-daily-word-puzzle/",
            new PAPuzzlesStreamScraper()
        );
    }

    //Some URL weirdness -- gets forbidden but works on phone -- perhaps
    //cookies from visiting via web first? Look for new URL or create one.
    //@Test
    //public void testPzzl() throws Exception {
    //    testScraper(
    //        "https://www.telegraaf.nl/puzzels/kruiswoord/detail-1",
    //        new PzzlStreamScraper()
    //    );
    //}

    @Test
    public void testRaetselZentraleSchweden() throws Exception {
        testScraper(
            "https://www.abendblatt.de/ratgeber/wissen/article106560367/Spielen-Sie-hier-taeglich-das-kostenlose-Kreuzwortraetsel.html",
            new RaetselZentraleSchwedenStreamScraper()
        );
    }

    @Test
    public void testWSJ() throws Exception {
        testScraperApache(
            "https://www.wsj.com/articles/snack-time-wednesday-crossword-january-3-b09d36c8",
            new WSJStreamScraper(new TiramisuUtil())
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

    /**
     * Test scraper gets a puzzle from the URL
     *
     * Uses Apache HTTP client
     */
    private static void testScraperApache(
        String url, PuzzleParser scraper
    ) throws Exception {
        try (InputStream is = utils.apacheGetInputStream(url, null, TIMEOUT)) {
            Puzzle puz = scraper.parseInput(is);
            assertNotNull(puz);
        }
    }
}
