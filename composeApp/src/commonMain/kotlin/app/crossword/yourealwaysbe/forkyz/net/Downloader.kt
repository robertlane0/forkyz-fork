
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.puz.Puzzle

interface Downloader {
    class DownloadResult {
        var puzzle : Puzzle? = null
        var fileName : String? = null
        var failed : Boolean = false

        constructor(puzzle : Puzzle?, fileName : String?) {
            this.puzzle = puzzle
            this.fileName = fileName
        }

        private constructor(failed : Boolean) {
            this.failed = failed
        }

        val isFailed : Boolean
            get() { return puzzle == null && fileName == null && failed }

        val isAlreadyExists : Boolean
            get() = puzzle == null && fileName == null && !failed

        val isSuccess : Boolean
            get() = puzzle != null && fileName != null

        companion object {
            val FAILED : DownloadResult = DownloadResult(true)
            val ALREADY_EXISTS : DownloadResult = DownloadResult(false)
        }
    }

    /**
     * Constant internal identifier (for e.g. storing in prefs)
     */
    val downloaderID : DownloaderID

    /**
     * Human readable / local configurable name
     */
    val name : String

    /**
     * Returns a URL where the user may support the crossword source
     *
     * @return null if no reasonable support/source URL (discouraged)
     */
    val supportUrl : String?

    /**
     * Download the puzzle for a given date
     *
     * Existing file names passed. Don't download if the date's puzzle
     * already exists -- downloader will not save puzzles with filenames
     * that already exist.
     */
    fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult

    fun alwaysRun() : Boolean

    /**
     * If a puzzle is currently available for the date
     */
    fun isAvailable(date : LocalDate) : Boolean

    /**
     * How long since/until available
     *
     * Returns null if not known or was never available. Negative means how
     * long since.
     */
    fun getUntilAvailable(date : LocalDate) : Duration?

    /**
     * Most recent puzzle date
     *
     * May return null if there is no most recent
     */
    val latestDate : LocalDate?

    /**
     * Most recent puzzle date up to until (inclusive)
     *
     * @param until latest date until this date (or null if today)
     * @return null if there is no most recent
     */
    fun getLatestDate(until : LocalDate?) : LocalDate?

    var timeout : Int
}

