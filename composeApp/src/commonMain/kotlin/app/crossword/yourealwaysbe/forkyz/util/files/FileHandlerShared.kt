
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils.generateFileName
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.IPuzIO

object FileHandlerShared {
    private const val TAG = "ForkyzFileHandlerShared"

    private const val LOGCAT_FILENAME = "logcat.txt"
    const val LOGCAT_SHARE_URI_MIME_TYPE = "text/plain"
    const val SHARE_URI_MIME_TYPE = FileHandler.MIME_TYPE_IPUZ

    // clear older than 1 hour
    private val CACHE_CLEAR_AGE_MILLIS = 60 * 60 * 1000L

    private val ioDispatcher
        = Dispatchers.IO.limitedParallelism(1, "File Share IO Lock")

    /**
     * Save the puzzle to cache and get Uri for sharing
     *
     * Runs off main thread to save file, calls back on main thread with
     * uriCallback.
     *
     * @param writeBlank whether to omit current play data such as
     * flagged clues and filled in letters
     * @param omitExtensions whether to omit Forkyz IPuz extensions
     */
    @Synchronized
    fun getShareUri(
        scope : CoroutineScope,
        utils : NativeBackendUtils,
        puz : Puzzle,
        writeBlank : Boolean,
        omitExtensions : Boolean,
        uriCallback : (Uri?) -> Unit,
    ) {
        scope.launch(ioDispatcher) {
            cleanCache(utils)
            val uri = getShareUriBackground(
                utils,
                puz,
                writeBlank,
                omitExtensions,
            )
            scope.launch(Dispatchers.Main) { uriCallback(uri) }
        }
    }

    /**
     * Save logcat to cache and get Uri for sharing
     *
     * Runs off main thread to save file, calls back on main thread with
     * uriCallback.
     *
     * @param lines the logcat lines
     */
    @Synchronized
    fun getLogcatShareUri(
        scope : CoroutineScope,
        utils : NativeBackendUtils,
        logcat : List<String>,
        uriCallback : (Uri?) -> Unit,
    ) {
        scope.launch(ioDispatcher) {
            cleanCache(utils)
            val uri = getLogcatShareUriBackground(utils, logcat)
            scope.launch(Dispatchers.Main) { uriCallback(uri) }
        }
    }

    @Synchronized
    private fun getShareUriBackground(
        utils : NativeBackendUtils,
        puz : Puzzle,
        writeBlank : Boolean,
        omitExtensions : Boolean,
    ) : Uri? {
        val fileName
            = generateFileName(puz) + FileHandler.Companion.FILE_EXT_IPUZ
        val shareFile = File(utils.getShareDir(), fileName)

        try {
            BufferedOutputStream(FileOutputStream(shareFile)).use { os ->
                IPuzIO.writePuzzle(puz, os, writeBlank, omitExtensions)
            }
        } catch (e : IOException) {
            AppLogger.e(TAG, "Could not create file for sharing : " + e)
            return null
        }

        return utils.getShareURIForFile(shareFile)
    }

    @Synchronized
    private fun getLogcatShareUriBackground(
        utils : NativeBackendUtils,
        logcat : List<String>,
    ) : Uri? {
        val shareFile = File(utils.getShareDir(), LOGCAT_FILENAME)

        try {
            PrintStream(
                BufferedOutputStream(FileOutputStream(shareFile))
            ).use { os ->
                for (line in logcat)
                    os.println(line)
            }
        } catch (e : IOException) {
            AppLogger.e(TAG, "Could not create file for sharing : " + e)
            return null
        }

        return utils.getShareURIForFile(shareFile)
    }

    @Synchronized
    private fun cleanCache(utils : NativeBackendUtils) {
        utils.getShareDir()?.listFiles()?.forEach { file ->
            val ageMillis = System.currentTimeMillis() - file.lastModified()
            if (ageMillis > CACHE_CLEAR_AGE_MILLIS)
                file.delete()
        }
    }
}

