
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

import org.jetbrains.compose.resources.MissingResourceException

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.html_file_full_path
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

/**
 * Read file from files/<lang>/<unqualifiedname> or default lang.
 */
@Throws(IOException::class,CancellationException::class)
suspend fun getFileResourceData(
    utils : NativeBackendUtils,
    unqualifiedFileName : String,
) : String {
    try {
        val path = utils.getString(
            Res.string.html_file_full_path,
            unqualifiedFileName,
        )
        return Res.readBytes(path).decodeToString()
    } catch (e : MissingResourceException) {
        val path = "files/en/$unqualifiedFileName"
        return Res.readBytes(path).decodeToString()
    }
}

