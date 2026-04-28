package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.util.Arrays
import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.number

import app.crossword.yourealwaysbe.forkyz.net.Downloader.DownloadResult
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser

/**
 * Base downloader class
 *
 * For downloads from sites that published puzzles on a given date. Each
 * date has one unique puzzle.
 *
 * @param downloaderID the identifier used to identify the downloader in
 * backend settings (should be constant).
 * @param name the human-readable name (can change based
 * on e.g. language settings).
 * @param days the days of the week where the puzzle is available
 * (should be sorted, recommended to use one of DAYS_DAILY,
 * DAYS_WEEKDAY, &c.). Override isPublishedOnDay if complex logic is
 * needed to determine if a puzzle is published on a given day of
 * the week. Null is interpreted as all days otherwise.
 * @param utcAvailabilityOffset the hour of the day a puzzle is
 * published as offset from UTC. E.g. if 9am in UTC, will return +9 hours.
 * Could be a negative amount if puzzle comes from a timezone ahead of UTC. Or
 * -24 if puzzles are published at midnight one day in advance.
 * @param supportUrl URL for support of the puzzle source (e.g.
 * newspaper subscription page).
 * @param puzzleParser the PuzzleParser for parsing the downloaded
 * puzzle.
 * @param sourceUrlFormatPattern Kotlin supported date-time format
 * pattern for the source URL on a given date. If null, override
 * getSourceUrl.
 * @param shareUrlFormatPattern Kotlin supported date-time format
 * patternfor the share URL (i.e. the website where the puzzle is).
 * getShareUrl can be overridden if more complex logic is needed.
 * @param goodFrom the first date the source is available. Can also
 * override getGoodFrom. Epoch 0 by default.
 * @param overrideEmbeddedDate always override the date embedded in the
 * puzzle download with the date provided to the download method (the
 * argument date is always used if there is no embedded date)
 */
open class AbstractDateDownloader constructor(
    override val downloaderID : DownloaderID,
    override val name : String,
    protected val days : Array<DayOfWeek>,
    protected val utcAvailabilityOffset : Duration,
    override val supportUrl : String?,
    protected var puzzleParser : StreamScraper,
    sourceUrlFormatPattern : String? = null,
    shareUrlFormatPattern : String? = null,
    val goodFrom : LocalDate = LocalDate.fromEpochDays(0L),
    private val overrideEmbeddedDate : Boolean = true,
) : AbstractDownloader() {

    /**
     * Alternative constructor with PuzzleParser not StreamScraper
     */
    constructor(
        downloaderID : DownloaderID,
        name : String,
        days : Array<DayOfWeek>,
        utcAvailabilityOffset : Duration,
        supportUrl : String?,
        puzzleParser : PuzzleParser,
        sourceUrlFormatPattern : String? = null,
        shareUrlFormatPattern : String? = null,
        goodFrom : LocalDate = LocalDate.fromEpochDays(0L),
        overrideEmbeddedDate : Boolean = true,
    ) : this(
        downloaderID,
        name,
        days,
        utcAvailabilityOffset,
        supportUrl,
        object : StreamScraper {
            override suspend fun parseInput(
                inputStream : InputStream,
                url: String?,
            ) : Puzzle? {
                return puzzleParser.parseInput(inputStream)
            }
        },
        sourceUrlFormatPattern,
        shareUrlFormatPattern,
        goodFrom,
        overrideEmbeddedDate,
    )

    private val sourceUrlFormat : DateTimeFormat<LocalDate>?
        = convertUrlFormatPattern(sourceUrlFormatPattern)
    private val shareUrlFormat : DateTimeFormat<LocalDate>?
        = convertUrlFormatPattern(shareUrlFormatPattern)

    /**
     * A unique consistent filename for the given date
     */
    protected fun createFileName(date : LocalDate) : String {
        return (
            date.year.toString() + "-" +
            date.month.number.toString() + "-" +
            date.day.toString() + "-" +
            name.replace(" ".toRegex(), "")
        )
    }

    override fun toString() : String {
        return name
    }

    /**
     * Where to do the actual download from
     */
    protected open suspend fun getSourceUrl(date : LocalDate) : String? {
        return sourceUrlFormat?.let { date.format(it) }
    }

    /**
     * A user-facing URL for the puzzle
     *
     * I.e. go here to find an online playable version, rather than the
     * backend data file
     */
    protected open suspend fun getShareUrl(date : LocalDate) : String? {
        return shareUrlFormat?.let { date.format(it) }
    }

    override suspend fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult {
        val fileName = createFileName(date)
        if (existingFileNames.contains(fileName))
            return DownloadResult.ALREADY_EXISTS

        val puz = download(date)
        if (puz != null)
            return DownloadResult(puz, fileName)
        else
            return DownloadResult.FAILED
    }

    protected open suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        try {
            val sourceUrl = getSourceUrl(date)
            if (sourceUrl == null)
                return null

            val url = (URI(sourceUrl)).toURL()
            try {
                getInputStream(url, headers).use { inputStream ->
                    val puz = puzzleParser.parseInput(inputStream)
                    if (puz != null) {
                        puz.date = if (overrideEmbeddedDate || puz.date == null)
                            date
                        else
                            puz.date
                        puz.source = name
                        puz.sourceUrl = sourceUrl
                        puz.supportUrl = supportUrl
                        puz.shareUrl = getShareUrl(date)

                        return puz
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        } catch (e : URISyntaxException) {
            AppLogger.e(TAG, "Malformed URL in download: " + e)
        } catch (e : MalformedURLException) {
            AppLogger.e(TAG, "Malformed URL in download: " + e)
        }

        return null
    }

    protected suspend fun download(date : LocalDate) : Puzzle? {
        return download(date, null)
    }

    override fun alwaysRun() : Boolean {
        return false
    }

    override fun isAvailable(date : LocalDate) : Boolean {
        val untilAvail = getUntilAvailable(date)
        return untilAvail != null && !(untilAvail.isPositive())
    }

    override fun getUntilAvailable(date : LocalDate) : Duration? {
        // check not before puzzle was available
        if (goodFrom.compareTo(date) > 0)
            return null

        // check not after puzzle was made unavailable
        if ((goodThrough?.compareTo(date) ?: 0) > 0)
            return null

        if (!isPublishedOnDay(date))
            return null

        return TimeUtils.durationUntil(date, utcAvailabilityOffset)
    }

    override val latestDate : LocalDate?
        get() = getLatestDate(null)

    override fun getLatestDate(until : LocalDate?) : LocalDate? {
        val now = TimeUtils.today()

        // look ahead offset and in previous week for an available date this
        // relies on puzzles being weekly according to our model include this
        // day last week in case current day not yet available
        var lookAhead = utcAvailabilityOffset.inWholeDays
        var startDate = TimeUtils.plusDays(now, -lookAhead)
        startDate = if (until == null || startDate.compareTo(until) < 0)
            startDate
        else
            until

        val lenWindow = this.latestDateWindow
        for (i in 0..lenWindow) {
            val tryDate = TimeUtils.plusDays(startDate, -i)
            if (isAvailable(tryDate))
                return tryDate
        }

        // should never happen unless puzzle not available on any days
        return null
    }

    /**
     * How far back in time to look when searching for latest puzzle
     *
     * Default is 8 days, but subclasses may want to vary. (To account for
     * slight hour differences, one week is a bit too precise.)
     */
    protected open val latestDateWindow : Int
        get() = 8

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
    protected open fun isPublishedOnDay(date : LocalDate) : Boolean {
        val dayOfWeek = date.dayOfWeek
        return Arrays.binarySearch(days, dayOfWeek) >= 0
    }

    /**
     * Last date of availability, or null
     *
     * Null means the puzzle is ongoing.
     */
    protected val goodThrough : LocalDate?
        get() = null

    companion object {
        protected const val TAG : String = "ForkyzDateDownloader"

        // These lists must be sorted for binary search.
        var DAYS_SUNDAY = arrayOf(DayOfWeek.SUNDAY)
        var DAYS_MONDAY = arrayOf(DayOfWeek.MONDAY)
        var DAYS_TUESDAY = arrayOf(DayOfWeek.TUESDAY)
        var DAYS_WEDNESDAY = arrayOf(DayOfWeek.WEDNESDAY)
        var DAYS_THURSDAY = arrayOf(DayOfWeek.THURSDAY)
        var DAYS_FRIDAY = arrayOf(DayOfWeek.FRIDAY)
        var DAYS_SATURDAY = arrayOf(DayOfWeek.SATURDAY)
        var DAYS_DAILY = arrayOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        var DAYS_NO_SUNDAY = arrayOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
        var DAYS_WEEKDAY = arrayOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )

        /**
         * Returns null if it is not a good pattern
         */
        private fun convertUrlFormatPattern(
            pattern : String?,
        ) : DateTimeFormat<LocalDate>? {
            try {
                return TimeUtils.convertUrlFormatPattern(pattern)
            } catch (e : IllegalArgumentException) {
                // fall through
            }
            return null
        }
    }
}

