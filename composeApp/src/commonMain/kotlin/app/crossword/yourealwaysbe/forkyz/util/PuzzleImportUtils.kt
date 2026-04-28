package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_fallback_source
import app.crossword.yourealwaysbe.forkyz.net.GenericStreamScraper
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleStreamReader
import app.crossword.yourealwaysbe.puz.io.StreamUtils

private val TAG = "ForkyzPuzzleImporter"

/** Import from a URI supported by resolver
 *
 * Currently does not use file extension or MIME type. Instead, use puzlib
 * that tries each known format in turn until one succeeds. Clunky, but
 * hopefully robust.
 *
 * Will try local content resolver first, then attempt treating uri as a
 * remote URL
 *
 * @param timeout download timeout if uri is not local
 * @return new puz handle if succeeded (will return null if failed
 * or uri is null)
 */
suspend fun importPuzzleURI(
    utils : NativeBackendUtils,
    fileHandler : FileHandler,
    uri : Uri?,
    timeout : Int,
) : PuzHandle? {
    if (uri == null)
        return null

    try {
        return utils.openInputStream(uri)?.use { inputStream ->
            importInputStream(
                utils,
                fileHandler,
                null,
                BufferedInputStream(inputStream),
                getFileName(uri),
                timeout,
            )
        }
    } catch (e : IOException) {
        val url = uri.toString()
        try {
            return getRemoteInputStream(url, timeout).use { inputStream ->
                importInputStream(
                    utils,
                    fileHandler,
                    url,
                    inputStream,
                    getFileName(uri),
                    timeout,
                )
            }
        } catch (e : IOException) {
            // we tried
            return null
        } catch (e : URISyntaxException) {
            return null
        }
    }
}

suspend fun processPuzzleToImportDirectory(
    utils : NativeBackendUtils,
    fileHandler : FileHandler,
) : List<PuzHandle> {
    val importedFiles : MutableList<PuzHandle> = mutableListOf()

    val toImportDir = fileHandler.toImportDirectory

    for (fh in fileHandler.listFiles(toImportDir)) {
        try {
            fileHandler.getBufferedInputStream(fh).use { inputStream ->
                val ph = importInputStream(
                    utils,
                    fileHandler,
                    null,
                    inputStream,
                    fileHandler.getName(fh),
                    0,
                )
                if (ph == null) {
                    // deliberately only get once needed to avoid
                    // unneeded dir creation in java file mode
                    // (currently SAF creates all dirs upfront)
                    val toImportFailedDir
                        = fileHandler.toImportFailedDirectory
                    fileHandler.moveTo(fh, toImportDir, toImportFailedDir)
                } else {
                    val toImportDoneDir
                        = fileHandler.toImportDoneDirectory
                    importedFiles.add(ph)
                    fileHandler.moveTo(fh, toImportDir, toImportDoneDir)
                }
            }
        } catch (e : IOException) {
            val toImportFailedDir
                = fileHandler.toImportFailedDirectory
            fileHandler.moveTo(fh, toImportDir, toImportFailedDir)
        }
    }

    return importedFiles.toList()
}

/**
 * Try best to make sure there is some source
 *
 * Fall back to author, title, fallback
 */
private suspend fun ensurePuzSource(
    utils : NativeBackendUtils,
    puz : Puzzle,
    fileName : String?,
) {
    var source = AppPuzzleUtils.deriveSource(
        puz.source,
        fileName,
        puz.author,
        puz.title
    )

    if (source == null || source.isEmpty())
        source = utils.getString(Res.string.import_fallback_source)

    puz.source = source
}

private fun ensurePuzDate(puz : Puzzle, fileName : String?) {
    val date
        = AppPuzzleUtils.deriveDate(puz.date, fileName) ?: TimeUtils.today()
    puz.date = date
}

private fun getFileName(uri : Uri?) : String? {
    if (uri == null)
        return null

    val path = uri.getPath()
    if (path != null)
        return File(path).getName()

    return null
}

/**
 * Import input stream
 *
 * Try first as a local file, but then see if it's a webpage that we
 * recognise (may include further web lookups).
 *
 * @param url the url the input stream came from, may be null
 */
@Throws(IOException::class)
private suspend fun importInputStream(
    utils : NativeBackendUtils,
    fileHandler : FileHandler,
    url : String?,
    inputStream : InputStream,
    fileName : String?,
    timeout : Int,
) : PuzHandle? {
    val bis = StreamUtils.makeByteArrayInputStream(inputStream)

    var puz = PuzzleStreamReader.parseInputStatic(bis)
    if (puz == null) {
        bis.reset()
        puz = importPuzzleFromWebPage(url, bis, timeout)
    }

    if (puz == null)
        return null

    ensurePuzDate(puz, fileName)
    ensurePuzSource(utils, puz, fileName)

    try {
        return fileHandler.saveNewPuzzle(
            puz, AppPuzzleUtils.generateFileName(puz)
        )
    } catch (e : IOException) {
        AppLogger.e(TAG, "Failed to save imported puzzle: " + e)
        return null
    }

}

@Throws(IOException::class,URISyntaxException::class)
private suspend fun getRemoteInputStream(
    url : String,
    timeout : Int,
) : BufferedInputStream {
    return getURLInputStream(url, timeout)
}

/**
 * Try to extract puzzle from recognised webpages
 *
 * These are usually those where the puzzle is a step or two away.
 * E.g. the metro website has an embedded iframe with a puzzle that can be
 * parsed.
 *
 * @param url the url of the webpage, may be null
 * @param timeout the timeout to use when following urls
 */
private suspend fun importPuzzleFromWebPage(
    url : String?,
    inputStream : InputStream,
    timeout : Int,
) : Puzzle? {
    try {
        val scraper = GenericStreamScraper()
        scraper.timeout = timeout
        return scraper.parseInput(inputStream, url)
    } catch (e : Exception) {
        return null
    }
}
