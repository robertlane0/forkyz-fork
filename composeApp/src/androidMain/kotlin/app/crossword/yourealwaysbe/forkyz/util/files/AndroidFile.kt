
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.io.files.Path

import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri

actual internal fun getLastModified(path : Path) : Long {
    return java.io.File(path.toString()).lastModified()
}

actual internal fun getUri(path : Path) : Uri {
    return java.io.File(path.toString()).toURI().toString().toUri()
}

