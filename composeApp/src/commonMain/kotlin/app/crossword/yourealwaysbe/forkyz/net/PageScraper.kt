
package app.crossword.yourealwaysbe.forkyz.net

import java.util.Deque
import java.util.LinkedList
import kotlin.time.Duration
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.io.Source

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseSource

import app.crossword.yourealwaysbe.forkyz.net.Downloader.DownloadResult
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.io.IO
import app.crossword.yourealwaysbe.forkyz.puz.io.PuzzleParser
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

/**
 * A downloader that scrapes from a dateless archive
 *
 * Will return dummy values (can always download) for any date
 * functions, and just download the first found puzzle not already on
 * file.
 */
open class PageScraper(
    regexFile : String,
    private val parser : PuzzleParser,
    private val scrapeUrl : String,
    override val downloaderID : DownloaderID,
    override val name : String,
    override val supportUrl : String?,
    private val shareFileUrl : Boolean,
    private val readReverse : Boolean,
) : AbstractDownloader() {
    private val patFile = Regex(regexFile)

    class Puz(
        scrapeUrl : String,
        downloaderID : DownloaderID,
        name : String,
        supportUrl : String?,
        readReverse : Boolean,
    ) : PageScraper(
        REGEX_PUZ,
        IO(),
        scrapeUrl,
        downloaderID,
        name,
        supportUrl,
        false,
        readReverse,
    ) {
        constructor(
            scrapeUrl : String,
            downloaderID : DownloaderID,
            name : String,
            supportUrl : String?,
        ) : this(scrapeUrl, downloaderID, name, supportUrl, false)
    }

    override fun toString() : String {
        return name
    }

    override fun alwaysRun() : Boolean {
        return true
    }

    override fun isAvailable(date : LocalDate) : Boolean {
        return true
    }

    override fun getUntilAvailable(date : LocalDate) : Duration? {
        return null
    }

    override val latestDate : LocalDate
        get() { return TimeUtils.today() }

    override fun getLatestDate(until : LocalDate?) : LocalDate? {
        val now = TimeUtils.today()
        return if (until == null || now.compareTo(until) < 0) now else until
    }

    override suspend fun download(
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : DownloadResult {
        var downloadFailed = false

        try {
            val urls = getPuzzleURLs()
            val urlsToFilenames = mapURLsToFileNames(urls)
            val urlIterator = if (readReverse)
                urls.descendingIterator()
            else
                urls.iterator()

            while (urlIterator.hasNext()) {
                val url = urlIterator.next()
                val remoteFileName = urlsToFilenames.get(url)
                if (remoteFileName == null)
                    continue

                val filename = getLocalFileName(remoteFileName)
                val legacyFileName = getLegacyLocalFileName(remoteFileName)

                val exists = (
                    existingFileNames.contains(filename)
                    || existingFileNames.contains(legacyFileName)
                )

                if (!exists) {
                    try {
                        val puz = downloadPuzzle(url)
                        if (puz != null) {
                            puz.source = name
                            puz.sourceUrl = url
                            puz.supportUrl = supportUrl
                            puz.shareUrl = if (shareFileUrl) url else scrapeUrl
                            puz.date = TimeUtils.today()

                            val title = puz.title
                            if (title == null || title.isEmpty())
                                puz.title = remoteFileName

                            PuzzleBuilder.resolveImages(puz, url)

                            return DownloadResult(puz, filename)
                        }
                    } catch (e : Exception) {
                        System.err.println(
                            "Exception downloading "
                                + url + " for " + this.name,
                        )
                        e.printStackTrace()
                        downloadFailed = true
                    }
                }
            }
        } catch (e : IOException) {
            e.printStackTrace()
            downloadFailed = true
        }

        if (downloadFailed)
            return DownloadResult.FAILED
        else
            return DownloadResult.ALREADY_EXISTS
    }

    private suspend fun downloadPuzzle(url : String) : Puzzle? {
        try {
            getURL(url, null).use { input->
                return parser.parseInput(input)
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }

        return null
    }

    @Throws(IOException::class)
    private suspend fun getPuzzleURLs() : Deque<String> {
        val result = LinkedList<String>()

        val content = Ksoup.parseSource(getURL(scrapeUrl), scrapeUrl)

        for (a in content.select("a")) {
            val url : String = a.attr("abs:href")
            if (patFile.matches(url)) {
                result.add(url)
            }
        }

        return result
    }

    private fun getLocalFileName(remoteFileName : String) : String {
        return (name + "-" + remoteFileName).replace(" ".toRegex(), "")
    }

    private fun getLegacyLocalFileName(remoteFileName : String) : String {
        return remoteFileName
    }

    companion object {
        private const val REGEX_PUZ = ".*\\.puz"

        /**
         * Map URLs to names of file at url, with file extension removed
         */
        private fun mapURLsToFileNames(
            urls : Deque<String>,
        ) : Map<String, String> {
            val result = HashMap<String, String>(urls.size)
            for (url in urls) {
                var fileName : String = url
                var lastSlashIdx = fileName.lastIndexOf("/")
                // get last subdir if no filename
                if (
                    !fileName.isEmpty()
                    && lastSlashIdx == fileName.length - 1
                ) {
                    fileName = fileName.substring(0, fileName.length - 1)
                    lastSlashIdx = fileName.lastIndexOf("/")
                }

                if (lastSlashIdx > 0)
                    fileName = fileName.substring(lastSlashIdx + 1)

                val extensionIdx = fileName.lastIndexOf(".")
                if (extensionIdx > 0)
                    fileName = fileName.substring(0, extensionIdx)

                result.put(url, fileName)
            }

            return result
        }
    }
}

