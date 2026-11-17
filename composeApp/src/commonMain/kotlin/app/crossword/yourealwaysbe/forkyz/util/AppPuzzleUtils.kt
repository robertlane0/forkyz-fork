
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

object AppPuzzleUtils {
    private const val FILE_NAME_REMOVE_CHARS = "[^A-Za-z0-9]"

    // source - yyyymmdd - title.puz (title optional)
    private val TITLE_DATE_REGEX = Regex(
        """\b(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|"""
            + """July|Jul|August|Aug|September|Sept|Sep|October|Oct|"""
            + """November|Nov|December|Dec)\.?\s+(\d{1,2}),?\s+(\d{4})\b""",
        RegexOption.IGNORE_CASE,
    )

    private val MONTH_NUMBERS = mapOf(
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sept" to 9, "sep" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12,
    )

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
    @OptIn(ExperimentalUuidApi::class)
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

        // loses accented characters, but no KMP normalizer to use
        val normalizedName = name.replace(FILE_NAME_REMOVE_CHARS.toRegex(), "")

        return "$date-$normalizedName-${Uuid.random()}"
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

    /**
     * Derive a publication date from common crossword title formats, e.g.
     * "NY Times, Fri, Nov 13, 2009" or
     * "NY Times, Monday, August 9, 2021".
     */
    fun deriveDateFromTitle(title : String?) : LocalDate? {
        if (title == null) return null

        val match = TITLE_DATE_REGEX.find(title) ?: return null
        val month = MONTH_NUMBERS[match.groupValues[1].lowercase()]
            ?: return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val year = match.groupValues[3].toIntOrNull() ?: return null

        return try {
            LocalDate(year, month, day)
        } catch (e : IllegalArgumentException) {
            null
        }
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

