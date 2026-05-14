package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.net.Downloader.DownloadResult
import app.crossword.yourealwaysbe.forkyz.puz.io.UclickXMLIO

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
    "'https://picayune.uclick.com/comics/$shortName/"
        + "data/$shortName'yyMMdd'-data.xml'",
    shareUrlPattern
) {
    public override suspend fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult {
        val res : DownloadResult = super.download(date, existingFileNames)
        if (res.isSuccess) {
            res.puzzle?.copyright = "\u00a9 " + date.year + " " + copyright
        }
        return res
    }
}

