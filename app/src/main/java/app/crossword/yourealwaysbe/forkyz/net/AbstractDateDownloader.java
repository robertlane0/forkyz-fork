package app.crossword.yourealwaysbe.forkyz.net;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import app.crossword.yourealwaysbe.puz.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Base downloader class
 *
 * For downloads from sites that published puzzles on a given date. Each
 * date has one unique puzzle.
 */
public class AbstractDateDownloader extends AbstractDownloader {
    protected static final String TAG = "ForkyzDateDownloader";

    protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    private String internalName;
    private String downloaderName;
    protected PuzzleParser puzzleParser;
    private DayOfWeek[] days;
    private String supportUrl;
    private DateTimeFormatter sourceUrlFormat;
    private DateTimeFormatter shareUrlFormat;
    private LocalDate goodFrom = LocalDate.ofEpochDay(0L);
    private Duration utcAvailabilityOffset = Duration.ZERO;

    // These lists must be sorted for binary search.
    public static DayOfWeek[] DAYS_SUNDAY
        = new DayOfWeek[] { DayOfWeek.SUNDAY };
    public static DayOfWeek[] DAYS_MONDAY
        = new DayOfWeek[] { DayOfWeek.MONDAY };
    public static DayOfWeek[] DAYS_TUESDAY
        = new DayOfWeek[] { DayOfWeek.TUESDAY };
    public static DayOfWeek[] DAYS_WEDNESDAY
        = new DayOfWeek[] { DayOfWeek.WEDNESDAY };
    public static DayOfWeek[] DAYS_THURSDAY
        = new DayOfWeek[] { DayOfWeek.THURSDAY };
    public static DayOfWeek[] DAYS_FRIDAY
        = new DayOfWeek[] { DayOfWeek.FRIDAY };
    public static DayOfWeek[] DAYS_SATURDAY
        = new DayOfWeek[] { DayOfWeek.SATURDAY };
    public static DayOfWeek[] DAYS_DAILY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    };
    public static DayOfWeek[] DAYS_NO_SUNDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    };
    public static DayOfWeek[] DAYS_WEEKDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    };

    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser
    ) {
        this(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            puzzleParser,
            null,
            null,
            null
        );
    }

    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern
    ) {
        this(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            puzzleParser,
            sourceUrlFormatPattern,
            shareUrlFormatPattern,
            null
        );
    }

    /**
     * Construct a downloader
     *
     * @param internalName the name used to identify the downloader in
     * backend settings (should be constant).
     * @param downloaderName the human-readable name (can change based
     * on e.g. language settings).
     * @param days the days of the week where the puzzle is available
     * (should be sorted, recommended to use one of DAYS_DAILY,
     * DAYS_WEEKDAY, &c.). Override isPublishedOnDay if complex logic is
     * needed to determine if a puzzle is published on a given day of
     * the week. Null is interpreted as all days otherwise.
     * @param utcAvailabilityOffset the hour of the day a puzzle is
     * published as offset from UTC.
     * @param supportUrl URL for support of the puzzle source (e.g.
     * newspaper subscription page).
     * @param puzzleParser the PuzzleParser for parsing the downloaded
     * puzzle.
     * @param sourceUrlFormatPattern Java DateTimeFormatter pattern for
     * the source URL on a given date. If null, override getSourceUrl.
     * @param shareUrlFormatPattern Java DateTimeFormatter pattern for
     * the share URL (i.e. the website where the puzzle is).
     * getShareUrl can be overridden if more complex logic is needed.
     * @param goodFrom the first date the source is available. Can also
     * override getGoodFrom. Epoch 0 by default.
     */
    protected AbstractDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        PuzzleParser puzzleParser,
        String sourceUrlFormatPattern,
        String shareUrlFormatPattern,
        LocalDate goodFrom
    ) {
        this.internalName = internalName;
        this.downloaderName = downloaderName;
        this.days = days;
        this.supportUrl = supportUrl;
        this.puzzleParser = puzzleParser;
        this.sourceUrlFormat = convertUrlFormatPattern(sourceUrlFormatPattern);
        this.shareUrlFormat = convertUrlFormatPattern(shareUrlFormatPattern);
        if (goodFrom != null)
            this.goodFrom = goodFrom;
        if (utcAvailabilityOffset != null)
            this.utcAvailabilityOffset = utcAvailabilityOffset;
    }

    /**
     * A unique consistent filename for the given date
     */
    protected String createFileName(LocalDate date) {
        return (
            date.getYear() + "-" +
            date.getMonthValue() + "-" +
            date.getDayOfMonth() + "-" +
            this.downloaderName.replaceAll(" ", "")
        );
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    @Override
    public String getName() {
        return downloaderName;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getSupportUrl() {
        return supportUrl;
    }

    /**
     * Where to do the actual download from
     */
    protected String getSourceUrl(LocalDate date) {
        return (sourceUrlFormat == null)
            ? null
            : sourceUrlFormat.format(date);
    }

    /**
     * A user-facing URL for the puzzle
     *
     * I.e. go here to find an online playable version, rather than the
     * backend data file
     */
    protected String getShareUrl(LocalDate date) {
        return (shareUrlFormat == null) ? null : shareUrlFormat.format(date);
    }

    @Override
    public DownloadResult download(
        LocalDate date, Set<String> existingFileNames
    ) {
        String fileName = createFileName(date);
        if (existingFileNames.contains(fileName))
            return DownloadResult.ALREADY_EXISTS;

        Puzzle puz = download(date);
        if (puz != null)
            return new DownloadResult(puz, fileName);
        else
            return DownloadResult.FAILED;
    }

    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        if (date == null)
            return null;

        try {
            String sourceUrl = getSourceUrl(date);
            if (sourceUrl == null)
                return null;

            URL url = (new URI(sourceUrl)).toURL();
            try (InputStream is = getInputStream(url, headers)) {
                Puzzle puz = puzzleParser.parseInput(is);

                if (puz != null) {
                    puz.setDate(date);
                    puz.setSource(getName());
                    puz.setSourceUrl(sourceUrl);
                    puz.setSupportUrl(getSupportUrl());
                    puz.setShareUrl(getShareUrl(date));

                    return puz;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException | MalformedURLException e) {
            Log.e(TAG, "Malformed URL in download: " + e);
        }

        return null;
    }

    protected Puzzle download(LocalDate date) {
        return download(date, EMPTY_MAP);
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    @Override
    public boolean isAvailable(LocalDate date) {
        Duration untilAvail = getUntilAvailable(date);
        return untilAvail != null && (
            untilAvail.isZero() || untilAvail.isNegative()
        );
    }

    public Duration getUntilAvailable(LocalDate date) {
        if (date == null)
            return null;

        // check not before puzzle was available
        LocalDate goodFrom = getGoodFrom();
        if (goodFrom != null && goodFrom.isAfter(date))
            return null;

        // check not after puzzle was made unavailable
        LocalDate goodThrough = getGoodThrough();
        if (goodThrough != null && goodThrough.isBefore(date))
            return null;

        if (!isPublishedOnDay(date))
            return null;

        // check current time is before required date plus offset
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime availableFrom = ZonedDateTime.of(
            date, LocalTime.MIDNIGHT, ZoneId.of("UTC")
        );
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            availableFrom = availableFrom.plus(availabilityOffset);

        return Duration.between(now, availableFrom);
    }

    @Override
    public LocalDate getLatestDate() {
        return getLatestDate(null);
    }

    @Override
    public LocalDate getLatestDate(LocalDate until) {
        LocalDate now = LocalDate.now();

        // look a day ahead (plus offset) and in previous week for an
        // available date this relies on puzzles being weekly according
        // to our model include this day last week in case current day
        // not yet available
        long lookAhead = -1;
        Duration availabilityOffset = getUTCAvailabilityOffset();
        if (availabilityOffset != null)
            lookAhead += availabilityOffset.toDays();

        LocalDate startDate = now.plusDays(-lookAhead);
        startDate = (until == null || startDate.isBefore(until))
            ? startDate
            : until;

        int lenWindow = getLatestDateWindow();
        for (int i = 0; i <= lenWindow; i++) {
            LocalDate tryDate = startDate.plusDays(-i);
            if (isAvailable(tryDate))
                return tryDate;
        }

        // should never happen unless puzzle not available on any days
        return null;
    }

    /**
     * How far back in time to look when searching for latest puzzle
     *
     * Default is 8 days, but subclasses may want to vary. (To account for
     * slight hour differences, one week is a bit too precise.)
     */
    protected int getLatestDateWindow() {
        return 8;
    }

    /**
     * The date is a day of the week where the puzzle is published
     *
     * Does not necessarily take goodfrom/until into account. Will just
     * say e.g.. "date is Monday, puzzle is usually published on
     * Mondays".
     *
     * Can be override if more complex logic is needed. E.g. Sundays
     * until 1/1/2001, then Monday after.
     */
    protected boolean isPublishedOnDay(LocalDate date) {
        if (date == null)
            return false;
        if (days == null)
            return true;

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return Arrays.binarySearch(days, dayOfWeek) >= 0;
    }

    /**
     * Last date of availability, or null
     *
     * Null means the puzzle is ongoing.
     */
    protected LocalDate getGoodThrough(){
        return null;
    }

    protected LocalDate getGoodFrom(){
        return goodFrom;
    }

    /**
     * When the puzzle for a date is released relative to UTC midnight
     *
     * E.g. if 9am in UTC, will return +9 hours. Could be a negative
     * amount if puzzle comes from a timezone ahead of UTC. Or -24 if
     * puzzles are published at midnight one day in advance.
     */
    protected Duration getUTCAvailabilityOffset() {
        return utcAvailabilityOffset;
    }

    /**
     * Returns null if it is not a good pattern
     */
    private static DateTimeFormatter convertUrlFormatPattern(String pattern) {
        try {
            if (pattern != null)
                return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
            // fall through
        }
        return null;
    }
}
