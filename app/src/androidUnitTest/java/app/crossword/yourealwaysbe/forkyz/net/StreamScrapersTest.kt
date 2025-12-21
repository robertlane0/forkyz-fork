
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream

import android.util.Base64
import org.junit.Assert
import org.junit.Test

import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.MockedStatic.Verification
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import app.crossword.yourealwaysbe.forkyz.util.apacheGetURLInputStream
import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream
import app.crossword.yourealwaysbe.puz.io.PuzzleParser

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
    //public void testWSJ() throws Exception {
    //    testScraperApache(
    //        "https://www.wsj.com/articles/"
    //            + "snack-time-wednesday-crossword-january-3-b09d36c8",
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
    fun testPuzzleMe() {
        testScraper(
            "https://observer.co.uk/puzzles/everyman/article/everyman-no-4123",
            PuzzleMeStreamScraper(),
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
            Mockito.mockStatic(Log::class.java)?.use {
                Mockito.mockStatic(Base64::class.java)?.use { mockedB64 ->
                    val inputStream : InputStream
                        = getURLInputStream(url, TIMEOUT)
                    run {
                        mockedB64.`when`<Any?>(
                            Verification {
                                Base64.decode(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyInt()
                                )
                            }
                        ).thenAnswer(
                            Answer { i: InvocationOnMock ->
                                val arg = i.getArguments()[0] as String?
                                java.util.Base64.getDecoder().decode(arg)
                            }
                        )
                        val puz = scraper.parseInput(inputStream, url)
                        Assert.assertNotNull(puz)
                    }
                }
            }
        }

        /**
         * Test scraper gets a puzzle from the URL
         *
         * Uses Apache HTTP client
         */
        @Throws(Exception::class)
        private fun testScraperApache(
            url : String,
            scraper : PuzzleParser,
        ) {
            apacheGetURLInputStream(url, TIMEOUT).use { inputStream ->
                val puz = scraper.parseInput(inputStream)
                Assert.assertNotNull(puz)
            }
        }
    }
}

