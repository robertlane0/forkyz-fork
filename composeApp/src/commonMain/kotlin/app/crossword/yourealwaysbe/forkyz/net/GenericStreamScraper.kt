
package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.io.InputStream

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Try out all stream scrapers on an input
 *
 * Return first successful puzzle.
 */
class GenericStreamScraper : AbstractStreamScraper() {
    private val streamScrapers : Array<AbstractStreamScraper>

    init {
        streamScrapers = arrayOf<AbstractStreamScraper>(
            // WIP
            // AmuseLabsStreamScraper(),
            CrosswordNexusStreamScraper(),
            KeesingStreamScraper(),
            PAPuzzlesStreamScraper(),
            PzzlNetStreamScraper(),
            RaetselZentraleSchwedenStreamScraper(),
            WSJStreamScraper(),
            PzzlComStreamScraper(),
            MyCrosswordStreamScraper(),
            PMLStreamScraper(),
            PuzzleMeStreamScraper(),
            CrossweirdStreamScraper(),
            SlowDownWiseUpStreamScraper(),
        )
    }

    @Throws(Exception::class)
    public override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        try {
            val bis = StreamUtils.makeByteArrayInputStream(inputStream)
            for (scraper in streamScrapers) {
                bis.reset()
                scraper.timeout = timeout
                try {
                    val puz = scraper.parseInput(bis, url)
                    if (puz != null)
                        return puz
                } catch (e : Exception) {
                    // on to the next
                }
            }
        } catch (e : IOException) {
            // we tried
        }

        return null
    }
}

