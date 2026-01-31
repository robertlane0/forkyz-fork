
package app.crossword.yourealwaysbe.forkyz.net

import java.time.Duration

import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader

/**
 * Custom daily downloader
 * URL: customDailyLink from settings
 * Date = Daily
 *
 * Create a new custom downloader
 *
 * @param title the title of the puzzles
 * @param urlDateFormatPattern java date format string for creating URL
 */
class CustomDailyDownloader(
    internalName : String,
    title : String,
    urlDateFormatPattern : String?,
) : AbstractDateDownloader(
    internalName,
    title,
    DAYS_DAILY,  // Currently no option to configure availability time
    Duration.ZERO,
    null,
    PuzzleStreamReader(),
    urlDateFormatPattern,
    urlDateFormatPattern,
)
