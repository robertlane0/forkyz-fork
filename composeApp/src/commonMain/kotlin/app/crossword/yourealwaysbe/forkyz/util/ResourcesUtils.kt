
package app.crossword.yourealwaysbe.forkyz.util

import java.util.Locale
import kotlinx.io.IOException

import org.jetbrains.compose.resources.MissingResourceException

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.html_file_full_path
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

private val DEFAULT_FILES_DIR = "files/en/%s"

/**
 * Read file from files/<lang>/<unqualifiedname> or default lang.
 */
@Throws(IOException::class)
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
        val path = String.format(
            Locale.US,
            DEFAULT_FILES_DIR,
            unqualifiedFileName,
        )
        return Res.readBytes(path).decodeToString()
    }
}

