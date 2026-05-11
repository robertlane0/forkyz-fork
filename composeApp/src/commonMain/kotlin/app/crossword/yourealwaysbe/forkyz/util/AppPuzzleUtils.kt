
package app.crossword.yourealwaysbe.forkyz.util

import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
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
            LocalDate.Format {
                year()
                monthNumber()
                day()
            },
        ),
        // Forkyz style file names
        FilenameFormat(
            "(\\d{4}-\\d{1,2}-\\d{1,2})-(\\w*)(?:-.*)?\\.\\w*",
            2,
            1,
            LocalDate.Format {
                year()
                char('-')
                monthNumber(Padding.NONE)
                char('-')
                day(Padding.NONE)
            },
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
            date = TimeUtils.today()

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
                val match = fmt.regex.find(filename)
                if (match != null) return match.groupValues[fmt.sourceGrp]
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
                val match = fmt.regex.find(filename)
                if (match != null) {
                    try {
                        return LocalDate.parse(
                            match.groupValues[fmt.dateGrp],
                            fmt.dateFormat,
                        )
                    } catch (e : IllegalArgumentException) {
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
        val dateFormat : DateTimeFormat<LocalDate>,
    ) {
        val regex : Regex = Regex(regex, RegexOption.IGNORE_CASE)
    }
}

