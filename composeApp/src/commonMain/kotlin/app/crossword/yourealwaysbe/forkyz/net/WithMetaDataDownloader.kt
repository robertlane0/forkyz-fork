
package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.puz.Puzzle

private const val TAG = "WithMetaDataDownloader"

/**
 * Downloader class for adding some metadata on top of IO output
 *
 * Provide non-null meta data and will be used to override IO output.
 */
class WithMetaDataDownloader(
    downloaderID : DownloaderID,
    downloaderName : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String,
    puzzleParser : PuzzleParser,
    sourceUrlFormatPattern : String,
    shareUrlFormatPattern : String,
    private val title : String? = null,
    private val author : String? = null,
) : AbstractDateDownloader(
    downloaderID,
    downloaderName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    puzzleParser,
    sourceUrlFormatPattern,
    shareUrlFormatPattern,
) {
    override protected fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        val sourceUrl = getSourceUrl(date)
        try {
            return getInputStream(
                (URI(sourceUrl)).toURL(),
                headers,
            ).use {
                puzzleParser.parseInput(it)
            }?.also { puz ->
                author?.let { puz.author = it }
                title?.let { puz.title = it }
            }
        } catch (e : IOException) {
            AppLogger.e(TAG, "Download failed with: " + e)
        } catch (e : URISyntaxException) {
            AppLogger.e(TAG, "Download failed with: " + e)
        }

        return null;
    }
}
