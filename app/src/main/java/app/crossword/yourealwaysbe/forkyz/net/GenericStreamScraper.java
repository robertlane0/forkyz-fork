
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Try out all stream scrapers on an input
 *
 * Return first successful puzzle.
 */
public class GenericStreamScraper extends AbstractStreamScraper {
    private static AbstractStreamScraper[] STREAM_SCRAPERS
        = new AbstractStreamScraper[] {
            // WIP
            // new AmuseLabsStreamScraper(),
            new CrosswordNexusStreamScraper(),
            new PAPuzzlesStreamScraper(),
            new PzzlStreamScraper(),
            new RaetselZentraleSchwedenStreamScraper()
        };

    @Override
    public Puzzle parseInput (InputStream is) throws Exception {
        try {
            ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);
            for (AbstractStreamScraper scraper : STREAM_SCRAPERS) {
                bis.reset();
                scraper.setTimeout(getTimeout());
                try {
                    Puzzle puz = scraper.parseInput(bis);
                    if (puz != null)
                        return puz;
                } catch (Exception e) {
                    // on to the next
                }
            }
        } catch (IOException e) {
            // we tried
        }

        return null;
    }
}
