
package app.crossword.yourealwaysbe.forkyz.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.puz.Puzzle;

public class AppPuzzleUtils {
    // date, name, uuid
    private static final String FILE_NAME_PATTERN
        = "%s-%s-%s";
    private static final String FILE_NAME_REMOVE_CHARS = "[^A-Za-z0-9]";

    // source - yyyymmdd - title.puz (title optional)
    private static final Pattern XWORD_DL_FILENAME_FORMATS
        = Pattern.compile(
            "(.*) - (\\d{8})(?: - .*)?\\.\\w*",
            Pattern.CASE_INSENSITIVE
        );
    private static final int XWORD_DL_SOURCE_GRP = 1;
    private static final int XWORD_DL_DATE_GRP = 2;
    private static final DateTimeFormatter XWORD_DL_DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);

    /**
     * Generate a file name for the given puzzle
     */
    public static String generateFileName(Puzzle puzzle) {
        String name = puzzle.getSource();
        if (name == null || name.isEmpty())
            name = puzzle.getAuthor();
        if (name == null || name.isEmpty())
            name = puzzle.getTitle();
        if (name == null)
            name = "";

        LocalDate date = puzzle.getDate();
        if (date == null)
            date = LocalDate.now();

        String normalizedName
            = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll(FILE_NAME_REMOVE_CHARS, "");

        return String.format(
            Locale.US,
            FILE_NAME_PATTERN, date, normalizedName, UUID.randomUUID()
        );
    }

    /**
     * Derive a makeshift source for puzzles without one
     *
     * Tries to use existing data, returns null if still fails.
     *
     * Takes source as first argument to avoid needing if source == null
     * logic before use.
     */
    public static String deriveSource(
        String source, String filename, String author, String title
    ) {
        if (source != null && !source.isEmpty())
            return source;

        if (filename != null && !filename.isEmpty()) {
            Matcher matcher = XWORD_DL_FILENAME_FORMATS.matcher(filename);
            if (matcher.find())
                return matcher.group(XWORD_DL_SOURCE_GRP);
        }

        if (author != null && !author.isEmpty())
            return author;

        if (title != null && !title.isEmpty())
            return title;

        return null;
    }

    /**
     * Derive a makeshift date for puzzles without one
     *
     * Tries to use existing data, returns null if still fails.
     *
     * Takes date as first argument to avoid needing if date == null
     * logic before use.
     */
    public static LocalDate deriveDate(LocalDate date, String filename) {
        if (date != null)
            return date;

        if (filename != null) {
            Matcher matcher = XWORD_DL_FILENAME_FORMATS.matcher(filename);
            if (matcher.find()) {
                try {
                    return LocalDate.parse(
                        matcher.group(XWORD_DL_DATE_GRP),
                        XWORD_DL_DATE_FORMATTER
                    );
                } catch (DateTimeParseException e) {
                    // fall through
                }
            }
        }

        return null;
    }
}
