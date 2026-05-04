
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri

private val TAG = "ForkyzStorageExporter"

/**
 * Export the files managed by fileHandler to an external uri
 *
 * @param fileHandler the file handler managing the source data
 * @param target the uri where the storage will be copied to
 * @return false if failed
 * @throws IOException
 */
@Throws(IOException::class)
fun exportStorage(
    utils : NativeBackendUtils,
    fileHandler : FileHandler,
    target : Uri,
) : Boolean {
    AppLogger.i(TAG, "Exporting storage to " + target)
    AppLogger.i(TAG, "Creating target handler")
    val targetHandler = utils.getTargetFileHandler(target)
    if (targetHandler == null) {
        AppLogger.e(TAG, "Could not create target handler, not exporting.")
        return false
    }

    AppLogger.i(TAG, "Exporting crosswords folder")
    exportDirectory(
        fileHandler,
        fileHandler.crosswordsDirectory,
        targetHandler,
        targetHandler.crosswordsDirectory,
    )
    AppLogger.i(TAG, "Exporting archive folder")
    exportDirectory(
        fileHandler,
        fileHandler.archiveDirectory,
        targetHandler,
        targetHandler.archiveDirectory,
    )
    AppLogger.i(TAG, "Exporting to import folder")
    exportDirectory(
        fileHandler,
        fileHandler.toImportDirectory,
        targetHandler,
        targetHandler.toImportDirectory,
    )
    AppLogger.i(TAG, "Exporting to import done folder")
    exportDirectory(
        fileHandler,
        fileHandler.toImportDoneDirectory,
        targetHandler,
        targetHandler.toImportDoneDirectory,
    )
    AppLogger.i(TAG, "Exporting to import failed folder")
    exportDirectory(
        fileHandler,
        fileHandler.toImportFailedDirectory,
        targetHandler,
        targetHandler.toImportFailedDirectory,
    )

    AppLogger.i(TAG, "Export done")
    return true
}

private fun exportDirectory(
    sourceHandler : FileHandler,
    sourceDir : DirHandle,
    targetHandler : FileHandler,
    targetDir : DirHandle,
) {
    if (sourceDir == targetDir)
        return

    for (handle in sourceHandler.listFiles(sourceDir)) {
        val name = sourceHandler.getName(handle)
        targetHandler.createFileHandle(
            targetDir,
            name,
            handle.mimeType ?: FileHandler.MIME_TYPE_GENERIC,
        )?.let { newHandle ->
            sourceHandler.getBufferedInput(handle).use { input ->
                targetHandler.getBufferedOutput(newHandle).use { output ->
                    input.transferTo(output)
                }
            }
        }
    }
}

