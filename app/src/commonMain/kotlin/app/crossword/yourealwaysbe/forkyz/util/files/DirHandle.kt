
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.File

import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri

class DirHandle {
    // common to all implementations
    val uri : Uri

    override fun equals(other : Any?) : Boolean {
        if (other !is DirHandle)
            return false
        return this.uri == other.uri
    }

    override fun hashCode() : Int {
        return uri.hashCode()
    }

    override fun toString() : String {
        return uri.toString()
    }

    ///////////////////////////////////////////////////////////
    // for FileHandlerJavaFile
    private var file : File? = null

    internal constructor(file : File) {
        this.uri = file.toURI().toString().toUri()
        this.file = file
    }

    fun getFile() : File {
        val path = uri.getPath()
        if (path == null) {
            throw IllegalArgumentException(
                "Cannot get file from URI " + uri,
            )
        }
        return file ?: File(path)
    }

    ///////////////////////////////////////////////////////////
    // for FileHandlerSAF
    internal constructor(uri : Uri) {
        this.uri = uri
    }
}

