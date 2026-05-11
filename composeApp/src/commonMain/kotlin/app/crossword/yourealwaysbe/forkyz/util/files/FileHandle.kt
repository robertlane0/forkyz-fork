
package app.crossword.yourealwaysbe.forkyz.util.files

import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.toUri

/**
 * Meta data available from some file handlers
 */
data class Meta(
    val name : String,
    val lastModified : Long,
    val mimeType : String,
)

class FileHandle {
    // common to all implementations
    val uri : Uri

    override fun equals(other : Any?) : Boolean {
        if (other is FileHandle)
            return this.uri == other.uri
        return false
    }

    override fun hashCode() : Int {
        return uri.hashCode()
    }

    override fun toString() : String {
        return uri.toString()
    }

    ///////////////////////////////////////////////////////////
    // for FileHandlerSystemFile
    private var file : File? = null

    internal constructor(file : File) {
        this.uri = file.uri
        this.file = file
    }

    fun getFile() : File {
        val path = uri.getPath()
        if (path == null) {
            throw IllegalArgumentException(
                "Can't get file from uri " + uri
            )
        }
        return file ?: File(path)
    }

    ///////////////////////////////////////////////////////////
    // E.g. for FileHandlerSAF on Android
    var meta : Meta? = null
        private set

    internal constructor(uri : Uri, meta : Meta?) {
        this.uri = uri
        this.meta = meta
    }

    /**
     * The mime type of the file if know else null
     */
    val mimeType : String?
        get() = meta?.mimeType
}

