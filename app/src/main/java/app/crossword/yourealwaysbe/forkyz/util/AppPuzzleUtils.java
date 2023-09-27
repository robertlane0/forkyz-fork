
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
    private static final FilenameFormat[] FILE_NAME_FORMATS
        = new FilenameFormat[] {
            // Default xword-dl format
            new FilenameFormat(
                "(.*) - (\\d{8})(?: - .*)?\\.\\w*", 1, 2, "yyyyMMdd"
            ),
            // Forkyz style file names
            new FilenameFormat(
                "(\\d{4}-\\d{1,2}-\\d{1,2})-(\\w*)(?:-.*)?\\.\\w*",
                2, 1, "yyyy-M-d"
            )
        };

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
            for (FilenameFormat fmt : FILE_NAME_FORMATS) {
                Matcher matcher = fmt.regex.matcher(filename);
                if (matcher.find())
                    return matcher.group(fmt.sourceGrp);
            }
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
            for (FilenameFormat fmt : FILE_NAME_FORMATS) {
                Matcher matcher = fmt.regex.matcher(filename);
                if (matcher.find()) {
                    try {
                        return LocalDate.parse(
                            matcher.group(fmt.dateGrp), fmt.dateFormat
                        );
                    } catch (DateTimeParseException e) {
                        // fall through
                    }
                }
            }
        }

        return null;
    }

    // TODO: make record class once supported
    private static class FilenameFormat {
        final Pattern regex;
        final int dateGrp;
        final int sourceGrp;
        final DateTimeFormatter dateFormat;

        FilenameFormat(
            String regex,
            int sourceGrp,
            int dateGrp,
            String dateFormat
        ) {
            this.regex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.sourceGrp = sourceGrp;
            this.dateGrp = dateGrp;
            this.dateFormat
                = DateTimeFormatter.ofPattern(dateFormat, Locale.US);
        }
    }
}
