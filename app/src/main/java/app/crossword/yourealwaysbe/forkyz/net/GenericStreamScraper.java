
package app.crossword.yourealwaysbe.forkyz.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.StreamUtils;

/**
 * Try out all stream scrapers on an input
 *
 * Return first successful puzzle.
 */
public class GenericStreamScraper extends AbstractStreamScraper {
    private AbstractStreamScraper[] streamScrapers;

    public GenericStreamScraper(AndroidVersionUtils utils) {
        streamScrapers = new AbstractStreamScraper[] {
            // WIP
            // new AmuseLabsStreamScraper(),
            new CrosswordNexusStreamScraper(),
            new KeesingStreamScraper(),
            new PAPuzzlesStreamScraper(),
            new PzzlStreamScraper(),
            new RaetselZentraleSchwedenStreamScraper(),
            new WSJStreamScraper(utils)
        };
    }

    @Override
    public Puzzle parseInput (InputStream is) throws Exception {
        try {
            ByteArrayInputStream bis = StreamUtils.makeByteArrayInputStream(is);
            for (AbstractStreamScraper scraper : streamScrapers) {
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
