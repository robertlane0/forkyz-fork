package app.crossword.yourealwaysbe.forkyz.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import app.crossword.yourealwaysbe.puz.io.RaetselZentraleSchwedenJSONIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Downloader for RaetselZentrale Swedish Crosswords
 *
 * This only really captures the Hamburg Abendsblatt pattern of
 * interation, so will need work later.
 *
 * Assuming archives are not available (paywall for Hamburg Abendsblatt,
 * just not available for any other i see with the same puzzle). Good
 * form is current day only.
 *
 * First URL is https://raetsel.raetselzentrale.de/l/<shortname>/schwede/
 * The above contains a puzzle ID, then we need
 * https://raetsel.raetselzentrale.de/api/r/<idnumber>
 */
public class RaetselZentraleSchwedenDownloader
        extends AbstractDateDownloader {

    private static final String ID_URL_FORMAT
        = "https://raetsel.raetselzentrale.de/l/%s/schwede";

    private String idUrl;

    public RaetselZentraleSchwedenDownloader(
        String internalName,
        String name,
        String shortName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String shareUrlPattern
    ) {
        super(
            internalName,
            name,
            days,
            utcAvailabilityOffset,
            supportUrl,
            new RaetselZentraleSchwedenJSONIO(),
            null,
            shareUrlPattern
        );
        idUrl = String.format(Locale.US, ID_URL_FORMAT, shortName);
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
        try(InputStream is = getInputStream((new URI(idUrl)).toURL(), null)) {
            return RaetselZentraleSchwedenStreamScraper.getAPIURL(is);
        } catch (IOException | URISyntaxException e) {
            // fall through
        }
        return null;
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
