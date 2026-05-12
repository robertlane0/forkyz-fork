
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle

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
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        try {
            for (scraper in streamScrapers) {
                scraper.timeout = timeout
                try {
                    val puz = scraper.parseInput(input.peek(), url)
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

