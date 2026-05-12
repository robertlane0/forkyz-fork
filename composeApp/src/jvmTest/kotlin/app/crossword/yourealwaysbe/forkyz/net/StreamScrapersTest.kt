
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.coroutines.runBlocking
import kotlinx.io.Source

import org.junit.Assert
import org.junit.Test

import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.MockedStatic.Verification
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import app.crossword.yourealwaysbe.forkyz.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.forkyz.util.getURL

class StreamScrapersTest {
    @Test
    @Throws(Exception::class)
    fun testCrosswordNexus() {
        testScraper(
            "https://www.crosswordnexus.com/jonesin/",
            CrosswordNexusStreamScraper(),
        )
    }

    // Fails because of too many redirects, drop support for now
    // @Test
    // public void testKeesing() throws Exception {
    //     testScraper(
    //         "https://app.standaard.be/kruiswoord",
    //         new KeesingStreamScraper()
    //     );
    // }

    @Test
    @Throws(Exception::class)
    fun testPAPuzzles() {
        testScraper(
            "https://www.irishnews.com/puzzles/cryptic-crossword/",
            PAPuzzlesStreamScraper(),
        )
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
    @Throws(Exception::class)
    fun testRaetselZentraleSchweden() {
        testScraper(
            "https://www.abendblatt.de/hamburg/"
                + "Kreuzwortraetsel-Jeden-Tag-kostenlos-in-Luxusgroesse",
            RaetselZentraleSchwedenStreamScraper(),
        )
    }

    //@Test
    //@Throws(Exception::class)
    //fun testWSJ() {
    //    testScraper(
    //        "https://www.wsj.com/articles/"
    //            + "snack-time-wednesday-crossword-january-3-b09d36c8",
    //        WSJStreamScraper(),
    //    )
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
    @Throws(Exception::class)
    fun testPzzlCom() {
        testScraper(
            "https://nytsyn.pzzl.com/cwd_seattle/",
            PzzlComStreamScraper(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testMyCrossword() {
        testScraper(
            "https://mycrossword.co.uk/cryptic/2462",
            MyCrosswordStreamScraper(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPML() {
        testScraper(
            "https://metro.co.uk/puzzles/quick-crossword",
            PMLStreamScraper(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCrossweird() {
        testScraper(
            "https://crossweirdpuzzles.com/solver/?file=puzzles/forthebirds.ipuz",
            CrossweirdStreamScraper(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSlowDownWiseUp() {
        testScraper(
            "https://observer.co.uk/puzzles/everyman/article/everyman-no-4144",
            SlowDownWiseUpStreamScraper(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSquarePursuit() {
        testScraper(
            "https://puzzleme.amuselabs.com/pmm/crossword?id=30e487cd&set=1383102e08ed60ddd50b4a1517cee19c96ce58fd08d719536855e37adfdeecf4",
            PuzzleMeStreamScraper(),
        )
    }

    companion object {
        private const val TIMEOUT = 30000

        /**
         * Test scraper gets a puzzle from the URL
         */
        @Throws(Exception::class)
        private fun testScraper(
            url: String,
            scraper: StreamScraper,
        ) {
            runBlocking {
                getURL(url, TIMEOUT).use { input ->
                    val puz = scraper.parseInput(input, url)
                    Assert.assertNotNull(puz)
                }
            }
        }
    }
}

