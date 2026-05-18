
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path

import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSURL
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri

@OptIn(ExperimentalForeignApi::class)
actual internal fun getLastModified(path : Path) : Long {
    val attrs = NSFileManager.defaultManager
        .attributesOfItemAtPath(path.toString(), error = null)
    val date = attrs?.get(NSFileModificationDate) as? NSDate
    return date?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0L
}

actual internal fun getUri(path : Path) : Uri {
    return (
        NSURL.fileURLWithPath(path.toString()).absoluteString ?: path.toString()
    ).toUri()
}
