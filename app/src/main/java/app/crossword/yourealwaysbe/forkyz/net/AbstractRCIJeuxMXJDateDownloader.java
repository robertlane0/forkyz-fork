package app.crossword.yourealwaysbe.forkyz.net;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.io.PuzzleParser;
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMCJIO;
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMFJIO;

/**
 * Abstract downloader for RCI Jeux puzzles.
 *
 * Build with buildMFJ or buildMCJ for arrow words or crosswords.
 */
public class AbstractRCIJeuxMXJDateDownloader extends AbstractDateDownloader {
    private static final int ARCHIVE_LENGTH_DAYS = 364;
    private static final DateTimeFormatter titleDateFormat
        = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String sourceUrlFormat;
    private int baseCWNumber;
    private LocalDate baseDate;
    private int daysBetween;

    /**
     * Construct an Abstract downloader
     *
     * Note, sourceUrlFormat is not a date format pattern, it should
     * have a %d for the crossword number.
     *
     * daysBetween is the number of days between each puzzle (e.g. 1 for daily,
     * 14 for fortnightly)
     */
    private AbstractRCIJeuxMXJDateDownloader(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String sourceUrlFormat,
        String shareUrlFormatPattern,
        PuzzleParser puzzleParser,
        int baseCWNumber,
        LocalDate baseDate,
        int daysBetween
    ) {
        super(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            puzzleParser,
            null,
            shareUrlFormatPattern
        );
        this.sourceUrlFormat = sourceUrlFormat;
        this.baseDate = baseDate;
        this.baseCWNumber = baseCWNumber;
        this.daysBetween = daysBetween;
    }

    public static AbstractRCIJeuxMXJDateDownloader buildMFJ(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String sourceUrlFormat,
        String shareUrlFormatPattern,
        int baseCWNumber,
        LocalDate baseDate,
        int daysBetween
    ) {
        return new AbstractRCIJeuxMXJDateDownloader(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            sourceUrlFormat,
            shareUrlFormatPattern,
            new RCIJeuxMFJIO(),
            baseCWNumber,
            baseDate,
            daysBetween
        );
    }

    public static AbstractRCIJeuxMXJDateDownloader buildMCJ(
        String internalName,
        String downloaderName,
        DayOfWeek[] days,
        Duration utcAvailabilityOffset,
        String supportUrl,
        String sourceUrlFormat,
        String shareUrlFormatPattern,
        int baseCWNumber,
        LocalDate baseDate,
        int daysBetween
    ) {
        return new AbstractRCIJeuxMXJDateDownloader(
            internalName,
            downloaderName,
            days,
            utcAvailabilityOffset,
            supportUrl,
            sourceUrlFormat,
            shareUrlFormatPattern,
            new RCIJeuxMCJIO(),
            baseCWNumber,
            baseDate,
            daysBetween
        );
    }

    @Override
    protected int getLatestDateWindow() {
        return Math.max(super.getLatestDateWindow(), daysBetween);
    }

    @Override
    protected LocalDate getGoodFrom() {
        return LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS);
    }

    @Override
    protected String getSourceUrl(LocalDate date) {
        long cwNumber = getCrosswordNumber(date);
        return String.format(Locale.US, this.sourceUrlFormat, cwNumber);
    }

    @Override
    public boolean isAvailable(LocalDate date) {
        if (!super.isAvailable(date))
            return false;

        // only available on daysBetween days since base date
        return getDaysDelta(date) % daysBetween == 0;
    }


    @Override
    protected Puzzle download(
        LocalDate date,
        Map<String, String> headers
    ){
        Puzzle puz = super.download(date, headers);
        if (puz != null) {
            puz.setTitle(getCrosswordTitle(date));
        }
        return puz;
    }

    private long getCrosswordNumber(LocalDate date) {
        long delta = Math.floorDiv(getDaysDelta(date), daysBetween);
        return this.baseCWNumber + delta;
    }

    /**
     * Get number of days between base date and argument
     */
    private long getDaysDelta(LocalDate date) {
        Duration diff = Duration.between(
            this.baseDate.atStartOfDay(), date.atStartOfDay()
        );
        return diff.toDays();
    }

    private String getCrosswordTitle(LocalDate date) {
        return getCrosswordNumber(date) + ", " + titleDateFormat.format(date);
    }
}
