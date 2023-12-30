
package app.crossword.yourealwaysbe.forkyz.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import app.crossword.yourealwaysbe.puz.io.KeesingXMLIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Downloader for Keesing Crosswords
 *
 * Assuming that archives are not available.
 */
public class KeesingDownloader
        extends AbstractDateDownloader {

    private String clientID;
    private String puzzleSetID;

    public KeesingDownloader(
        String internalName,
        String name,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String shareUrlPattern,
        String clientID,
        String puzzleSetID
    ) {
        super(
            internalName,
            name,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new KeesingXMLIO(),
            null,
            shareUrlPattern
        );
        this.clientID = clientID;
        this.puzzleSetID = puzzleSetID;
    }

    @Override
    protected LocalDate getGoodFrom() {
        // website has "yesterday's" puzzle up until the
        // utcAvailabilityOffset, so either return today or yesterday
        // depending on what's available
        ZonedDateTime goodFrom = ZonedDateTime.now(ZoneId.of("UTC"));
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            goodFrom =goodFrom.minus(availabilityOffset);

        return goodFrom.toLocalDate();
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        KeesingStreamScraper scraper = new KeesingStreamScraper();
        scraper.setTimeout(getTimeout());
        return scraper.getPuzzleURL(clientID, puzzleSetID);
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ) {
        Puzzle puz = super.download(date, headers);
        if (puz != null) {
            puz.setCopyright(getName());
            puz.setDate(date);
        }
        return puz;
    }
}
