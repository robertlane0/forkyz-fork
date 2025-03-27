
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.IOException

import android.content.Context
import android.net.Uri
import android.util.Log

import app.crossword.yourealwaysbe.puz.io.StreamUtils

private val TAG = "ForkyzStorageExporter"

/**
 * Export the files managed by fileHandler to an external uri
 *
 * @param context application context
 * @param fileHandler the file handler managing the source data
 * @param target the uri where the storage will be copied to
 * @return false if failed
 * @throws IOException
 */
@Throws(IOException::class)
fun exportStorage(
    context : Context,
    fileHandler : FileHandler,
    target : Uri,
) : Boolean {
    Log.i(TAG, "Exporting storage to " + target)
    Log.i(TAG, "Creating target handler")
    val targetHandler = FileHandlerSAF.initialiseSAFForRoot(
        context,
        target,
        false
    )
    if (targetHandler == null) {
        Log.e(TAG, "Could not create target handler, not exporting.")
        return false
    }

    Log.i(TAG, "Exporting crosswords folder")
    exportDirectory(
        fileHandler,
        fileHandler.getCrosswordsDirectory(),
        targetHandler,
        targetHandler.getCrosswordsDirectory(),
    )
    Log.i(TAG, "Exporting archive folder")
    exportDirectory(
        fileHandler,
        fileHandler.getArchiveDirectory(),
        targetHandler,
        targetHandler.getArchiveDirectory(),
    )
    Log.i(TAG, "Exporting to import folder")
    exportDirectory(
        fileHandler,
        fileHandler.getToImportDirectory(),
        targetHandler,
        targetHandler.getToImportDirectory(),
    )
    Log.i(TAG, "Exporting to import done folder")
    exportDirectory(
        fileHandler,
        fileHandler.getToImportDoneDirectory(),
        targetHandler,
        targetHandler.getToImportDoneDirectory(),
    )
    Log.i(TAG, "Exporting to import failed folder")
    exportDirectory(
        fileHandler,
        fileHandler.getToImportFailedDirectory(),
        targetHandler,
        targetHandler.getToImportFailedDirectory(),
    )

    Log.i(TAG, "Export done")
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
        val newHandle = targetHandler.createFileHandle(
            targetDir,
            name,
            handle.mimeType,
        )
        sourceHandler.getInputStream(handle).use { ins ->
            targetHandler.getOutputStream(newHandle).use { outs ->
                StreamUtils.copyStream(ins, outs)
            }
        }
    }
}
