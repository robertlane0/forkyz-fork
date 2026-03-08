package app.crossword.yourealwaysbe.forkyz.net

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

import app.crossword.yourealwaysbe.forkyz.net.Downloader.DownloadResult
import app.crossword.yourealwaysbe.puz.io.UclickXMLIO

/**
 * Uclick XML Puzzles
 * URL: https://picayune.uclick.com/comics/[puzzle]/data/[puzzle]YYMMDD-data.xml
 * crnet (Newsday) = Daily
 * usaon (USA Today) = Daily
 * fcx (Universal) = Daily
 */
class UclickDownloader(
    downloaderID : DownloaderID,
    shortName : String,
    fullName : String,
    private val copyright : String?,
    supportUrl : String?,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    shareUrlPattern : String?,
) : AbstractDateDownloader(
    downloaderID,
    fullName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    UclickXMLIO(),
    String.format(
        Locale.US,
        SOURCE_URL_PATTERN_FORMAT,
        shortName,
    ),
    shareUrlPattern
) {
    public override fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult {
        val res : DownloadResult = super.download(date, existingFileNames)
        if (res.isSuccess) {
            res.puzzle?.copyright = "\u00a9 " + date.getYear() + " " + copyright
        }
        return res
    }

    companion object {
        private const val SOURCE_URL_PATTERN_FORMAT = (
            "'https://picayune.uclick.com/comics/"
            + "%1\$s/data/%1\$s'yyMMdd'-data.xml'"
        )
    }
}

