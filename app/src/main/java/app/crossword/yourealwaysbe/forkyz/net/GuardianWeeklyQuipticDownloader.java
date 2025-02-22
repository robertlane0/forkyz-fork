package app.crossword.yourealwaysbe.forkyz.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Guardian Weekly Quiptic downloader
 * URL: https://www.theguardian.com/crosswords/quiptic/
 * Date = Weekly on Sundays
 */
public class GuardianWeeklyQuipticDownloader extends AbstractDateDownloader {
    private static final String INTERNAL_NAME = "guardianQuiptic";
    private static final String SUPPORT_URL = "https://support.theguardian.com";
    private static final String BASE_SOURCE_URL
        = "https://www.theguardian.com/crosswords/quiptic/";
    private static final int BASE_CW_NUMBER = 1272;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2024, 4, 7);

    public GuardianWeeklyQuipticDownloader(String internalName, String name) {
        super(
            internalName,
            name,
            DAYS_SUNDAY, // see isPublishedOnDay for logic over time
            Duration.ZERO, // TODO: availability time
            SUPPORT_URL,
            null
        );
    }

    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ) {
        String sourceUrl = getSourceUrl(date);
        try (
            InputStream is = getInputStream(
                (new URI(sourceUrl)).toURL(), headers
            )
        ) {
            Puzzle puz = GuardianJSONIO.readFromHTML(is);
            if (puz != null) {
                puz.setCopyright("Guardian / " + puz.getAuthor());
                puz.setSource(getName());
                puz.setSupportUrl(getSupportUrl());
                puz.setSourceUrl(sourceUrl);
                puz.setShareUrl(sourceUrl);
            }
            return puz;
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        return BASE_SOURCE_URL + createUrlSuffix(date);
    }

    @Override
    protected String getShareUrl(LocalDate date) {
        return getSourceUrl(date);
    }

    protected String createUrlSuffix(LocalDate date) {
        LocalDate lower = BASE_CW_DATE;
        LocalDate upper = date;
        int direction = 1;

        if (lower.isAfter(upper)) {
            lower = date;
            upper = BASE_CW_DATE;
            direction = -1;
        }

        Duration diff = Duration.between(lower.atStartOfDay(),
                                         upper.atStartOfDay());

        long daysDiff = diff.toDays();

        // change between tue/mon/sun release smoothed over by rounding
        long cwNumOffset = (daysDiff / 7);

        // 25 Dec 2023 was skipped, so adjust up until BASE_CW_DATE
        // Revisit in 2033 to see if that Christmas is skipped or not
        // (other Christmases weren't missed)
        if (date.isBefore(BASE_CW_DATE)
                && date.isAfter(LocalDate.of(2023, 12, 25)))
            cwNumOffset = cwNumOffset + 1;

        long cwNum = BASE_CW_NUMBER + direction * cwNumOffset;

        return Long.toString(cwNum);
    }

    // override default isPublishedOnDay behaviour to follow changing release date
    protected boolean isPublishedOnDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (date.isAfter(BASE_CW_DATE) || date.isEqual(BASE_CW_DATE))
            // #1272 onwards on Sundays
            return (dayOfWeek == DayOfWeek.SUNDAY);
        else if (date.isEqual(LocalDate.of(1999, 11, 23)))
            // #1 on Tuesday
            return true;
        else if (date.isEqual(LocalDate.of(2023,12,25)))
            // Skip 2024-12-25
            return false;
        else
            // #2 to #1271 on Mondays
            return (dayOfWeek == DayOfWeek.MONDAY);
    }
}
