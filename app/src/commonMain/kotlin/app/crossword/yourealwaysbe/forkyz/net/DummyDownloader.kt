
package app.crossword.yourealwaysbe.forkyz.net

import java.time.Duration
import java.time.LocalDate

import app.crossword.yourealwaysbe.forkyz.net.Downloader.DownloadResult

/**
 * Does not actually download any puzzles; just adds an "All Available"
 * option to the dropdown.
 */
class DummyDownloader : Downloader {
    override val internalName : String = "dummy"
    override val name : String = "dummy"
    override val supportUrl : String = "https://dummy"

    override fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult {
        return DownloadResult.FAILED
    }

    override fun toString() : String {
        return "All available"
    }

    override fun alwaysRun() : Boolean {
        return false
    }

    override fun isAvailable(date : LocalDate) : Boolean {
        return true
    }

    override fun getUntilAvailable(date : LocalDate) : Duration? {
        return null
    }

    override val latestDate : LocalDate? = null

    override fun getLatestDate(until : LocalDate?) : LocalDate? {
        return null
    }

    override var timeout : Int
        get() = -1
        set(timeoutMillis) {}
}

