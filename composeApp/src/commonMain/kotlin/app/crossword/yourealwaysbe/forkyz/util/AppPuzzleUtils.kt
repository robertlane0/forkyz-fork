
package app.crossword.yourealwaysbe.forkyz.util

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

import app.crossword.yourealwaysbe.puz.Puzzle

object AppPuzzleUtils {
    // date, name, uuid
    private const val FILE_NAME_PATTERN = "%s-%s-%s"
    private const val FILE_NAME_REMOVE_CHARS = "[^A-Za-z0-9]"

    // source - yyyymmdd - title.puz (title optional)
    private val FILE_NAME_FORMATS = arrayOf<FilenameFormat>(
        // Default xword-dl format
        FilenameFormat(
            "(.*) - (\\d{8})(?: - .*)?\\.\\w*",
            1,
            2,
            "yyyyMMdd",
        ),
        // Forkyz style file names
        FilenameFormat(
            "(\\d{4}-\\d{1,2}-\\d{1,2})-(\\w*)(?:-.*)?\\.\\w*",
            2,
            1,
            "yyyy-M-d",
        )
    )

    /**
     * Generate a file name for the given puzzle
     */
    fun generateFileName(puzzle : Puzzle) : String {
        var name = puzzle.source
        if (name == null || name.isEmpty())
            name = puzzle.author
        if (name == null || name.isEmpty())
            name = puzzle.title
        if (name == null)
            name = ""

        var date = puzzle.date
        if (date == null)
            date = LocalDate.now()

        val normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(FILE_NAME_REMOVE_CHARS.toRegex(), "")

        return String.format(
            Locale.US,
            FILE_NAME_PATTERN,
            date,
            normalizedName,
            UUID.randomUUID(),
        )
    }

    /**
     * Derive a makeshift source for puzzles without one
     *
     * Tries to use existing data, returns null if still fails.
     *
     * Takes source as first argument to avoid needing if source == null
     * logic before use.
     */
    fun deriveSource(
        source : String?,
        filename : String?,
        author : String?,
        title : String?,
    ) : String? {
        if (source != null && !source.isEmpty())
            return source

        if (filename != null && !filename.isEmpty()) {
            for (fmt in FILE_NAME_FORMATS) {
                val matcher = fmt.regex.matcher(filename)
                if (matcher.find()) return matcher.group(fmt.sourceGrp)
            }
        }

        if (author != null && !author.isEmpty())
            return author

        if (title != null && !title.isEmpty())
            return title

        return null
    }

    /**
     * Derive a makeshift date for puzzles without one
     *
     * Tries to use existing data, returns null if still fails.
     *
     * Takes date as first argument to avoid needing if date == null
     * logic before use.
     */
    fun deriveDate(date : LocalDate?, filename : String?) : LocalDate? {
        if (date != null) return date

        if (filename != null) {
            for (fmt in FILE_NAME_FORMATS) {
                val matcher = fmt.regex.matcher(filename)
                if (matcher.find()) {
                    try {
                        return LocalDate.parse(
                            matcher.group(fmt.dateGrp), fmt.dateFormat
                        )
                    } catch (e : DateTimeParseException) {
                        // fall through
                    }
                }
            }
        }

        return null
    }

    private class FilenameFormat(
        regex : String,
        val sourceGrp : Int,
        val dateGrp : Int,
        dateFormat : String,
    ) {
        val regex : Pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val dateFormat : DateTimeFormatter
            = DateTimeFormatter.ofPattern(dateFormat, Locale.US)
    }
}

