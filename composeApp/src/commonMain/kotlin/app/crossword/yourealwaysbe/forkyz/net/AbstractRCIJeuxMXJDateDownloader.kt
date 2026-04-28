
package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber

import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
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
) : AbstractDateDownloader(
    downloaderID,
    downloaderName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    puzzleParser,
    null,
    shareUrlFormatPattern,
    TimeUtils.plusDays(TimeUtils.today(), -ARCHIVE_LENGTH_DAYS),
) {
    override suspend fun getSourceUrl(date : LocalDate) : String? {
        val cwNumber = getCrosswordNumber(date)
        return String.format(Locale.US, this.sourceUrlFormat, cwNumber)
    }

    override suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        return super.download(date, headers)?.also {
            it.title = getCrosswordTitle(date)
        }
    }

    private fun getCrosswordNumber(date : LocalDate) : Long {
        val daysDelta = getDaysDelta(date)
        val startDay = baseDate.dayOfWeek.isoDayNumber

        val trueDelta = days.map { weekday ->
            val offset = (7 + (weekday.isoDayNumber - startDay)) % 7
            val deltaFromDay = daysDelta - offset
            if (deltaFromDay > 0)
                1 + (deltaFromDay - 1) / 7
            else
                0
        }.sum()

        return this.baseCWNumber + trueDelta
    }

    /**
     * Get number of days between base date and argument
     */
    private fun getDaysDelta(date : LocalDate) : Long {
        return baseDate.daysUntil(date).toLong()
    }

    private fun getCrosswordTitle(date : LocalDate) : String {
        return (
            getCrosswordNumber(date).toString()
            + ", "
            + date.format(titleDateFormat)
        )
    }

    companion object {
        private const val ARCHIVE_LENGTH_DAYS = 364
        private val titleDateFormat = LocalDate.Format {
            day()
            char('/')
            monthNumber()
            char('/')
            year()
        }

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
            )
        }
    }
}

