
package app.crossword.yourealwaysbe.forkyz.net

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMCJIO
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMFJIO

/**
 * Abstract downloader for RCI Jeux puzzles.
 *
 * Build with buildMFJ or buildMCJ for arrow words or crosswords.
 *
 * Note, sourceUrlFormat is not a date format pattern, it should
 * have a %d for the crossword number.
 *
 * daysBetween is the number of days between each puzzle (e.g. 1 for daily,
 * 14 for fortnightly)
 */
class AbstractRCIJeuxMXJDateDownloader private constructor(
    downloaderID : DownloaderID,
    downloaderName : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String?,
    private val sourceUrlFormat : String,
    shareUrlFormatPattern : String?,
    puzzleParser : PuzzleParser,
    private val baseCWNumber : Int,
    private val baseDate : LocalDate,
    private val daysBetween : Int
) : AbstractDateDownloader(
    downloaderID,
    downloaderName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    puzzleParser,
    null,
    shareUrlFormatPattern,
    LocalDate.now().minusDays(ARCHIVE_LENGTH_DAYS.toLong()),
) {
    override protected val latestDateWindow : Int
        get() = max(super.latestDateWindow, daysBetween)

    override fun getSourceUrl(date : LocalDate) : String? {
        val cwNumber = getCrosswordNumber(date)
        return String.format(Locale.US, this.sourceUrlFormat, cwNumber)
    }

    public override fun isAvailable(date : LocalDate) : Boolean {
        if (!super.isAvailable(date))
            return false

        // only available on daysBetween days since base date
        return getDaysDelta(date) % daysBetween == 0L
    }


    override fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        return super.download(date, headers)?.also {
            it.setTitle(getCrosswordTitle(date))
        }
    }

    private fun getCrosswordNumber(date : LocalDate) : Long {
        val delta = Math.floorDiv(getDaysDelta(date), daysBetween)
        return this.baseCWNumber + delta
    }

    /**
     * Get number of days between base date and argument
     */
    private fun getDaysDelta(date : LocalDate) : Long {
        val diff = Duration.between(
            this.baseDate.atStartOfDay(),
            date.atStartOfDay(),
        )
        return diff.toDays()
    }

    private fun getCrosswordTitle(date : LocalDate) : String {
        return (
            getCrosswordNumber(date).toString()
            + ", "
            + titleDateFormat.format(date)
        )
    }

    companion object {
        private const val ARCHIVE_LENGTH_DAYS = 364
        private val titleDateFormat : DateTimeFormatter
            = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun buildMFJ(
            downloaderID : DownloaderID,
            downloaderName : String,
            days : Array<DayOfWeek>,
            utcAvailabilityOffset : Duration,
            supportUrl : String?,
            sourceUrlFormat : String,
            shareUrlFormatPattern : String?,
            baseCWNumber : Int,
            baseDate : LocalDate,
            daysBetween : Int
        ) : AbstractRCIJeuxMXJDateDownloader {
            return AbstractRCIJeuxMXJDateDownloader(
                downloaderID,
                downloaderName,
                days,
                utcAvailabilityOffset,
                supportUrl,
                sourceUrlFormat,
                shareUrlFormatPattern,
                RCIJeuxMFJIO(),
                baseCWNumber,
                baseDate,
                daysBetween
            )
        }

        fun buildMCJ(
            downloaderID : DownloaderID,
            downloaderName : String,
            days : Array<DayOfWeek>,
            utcAvailabilityOffset : Duration,
            supportUrl : String?,
            sourceUrlFormat : String,
            shareUrlFormatPattern : String?,
            baseCWNumber : Int,
            baseDate : LocalDate,
            daysBetween : Int
        ) : AbstractRCIJeuxMXJDateDownloader {
            return AbstractRCIJeuxMXJDateDownloader(
                downloaderID,
                downloaderName,
                days,
                utcAvailabilityOffset,
                supportUrl,
                sourceUrlFormat,
                shareUrlFormatPattern,
                RCIJeuxMCJIO(),
                baseCWNumber,
                baseDate,
                daysBetween
            )
        }
    }
}

