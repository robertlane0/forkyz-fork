
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;

import android.util.Base64;
import android.util.Log;

import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;

import app.crossword.yourealwaysbe.forkyz.util.NetUtilsKt;
//import app.crossword.yourealwaysbe.forkyz.versions.TiramisuUtil;
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
    public void testKeesing() throws Exception {
        testScraper(
            "https://app.standaard.be/kruiswoord",
            new KeesingStreamScraper()
        );
    }

    @Test
    public void testPAPuzzles() throws Exception {
        testScraper(
            "https://www.irishnews.com/puzzles/cryptic-crossword/",
            new PAPuzzlesStreamScraper()
        );
    }

    //Some URL weirdness -- gets forbidden but works on phone -- perhaps
    //cookies from visiting via web first? Look for new URL or create one.
    //@Test
    //public void testPzzlNet() throws Exception {
    //    testScraper(
    //        "https://www.telegraaf.nl/puzzels/kruiswoord/detail-1",
    //        new PzzlNetStreamScraper()
    //    );
    //}

    @Test
    public void testRaetselZentraleSchweden() throws Exception {
        testScraper(
            "https://www.abendblatt.de/hamburg/Kreuzwortraetsel-Jeden-Tag-kostenlos-in-Luxusgroesse",
            new RaetselZentraleSchwedenStreamScraper()
        );
    }

    //@Test
    //public void testWSJ() throws Exception {
    //    testScraperApache(
    //        "https://www.wsj.com/articles/snack-time-wednesday-crossword-january-3-b09d36c8",
    //        new WSJStreamScraper(new TiramisuUtil())
    //    );
    //}

    //403 without user agent
    //@Test
    //public void testSeattle() throws Exception {
    //    testScraper(
    //        "https://www.seattletimes.com/games-nytimes-crossword/",
    //        new PzzlComStreamScraper()
    //    );
    //}

    @Test
    public void testPzzlCom() throws Exception {
        testScraper(
            "https://nytsyn.pzzl.com/cwd_seattle/",
            new PzzlComStreamScraper()
        );
    }

    @Test
    public void testMyCrossword() throws Exception {
        testScraper(
            "https://mycrossword.co.uk/cryptic/2462",
            new MyCrosswordStreamScraper()
        );
    }

    @Test
    public void testPML() throws Exception {
        testScraper(
            "https://metro.co.uk/puzzles/quick-crossword",
            new PMLStreamScraper()
        );
    }

    @Test
    public void testPuzzleMe() throws Exception {
        testScraper(
            "https://observer.co.uk/puzzles/everyman/article/everyman-no-4109",
            new PuzzleMeStreamScraper()
        );
    }

    /**
     * Test scraper gets a puzzle from the URL
     */
    private static void testScraper(
        String url, StreamScraper scraper
    ) throws Exception {
        try (
            MockedStatic<Log> mockedLog = mockStatic(Log.class);
            MockedStatic<Base64> mockedB64 = mockStatic(Base64.class);
            InputStream is = NetUtilsKt.getURLInputStream(url, TIMEOUT)
        ) {
            mockedB64.when(() -> Base64.decode(anyString(), anyInt()))
                .thenAnswer(i -> {
                    String arg = (String) i.getArguments()[0];
                    return java.util.Base64.getDecoder().decode(arg);
                });
            Puzzle puz = scraper.parseInput(is, url);
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
        try (
            InputStream is = NetUtilsKt.apacheGetURLInputStream(url, TIMEOUT)
        ) {
            Puzzle puz = scraper.parseInput(is);
            assertNotNull(puz);
        }
    }
}
