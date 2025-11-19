
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.File
import java.util.Objects

import android.net.Uri

class FileHandle {
    // common to all implementations
    val uri : Uri

    override fun equals(other : Any?) : Boolean {
        if (other is FileHandle)
            return this.uri == other.uri
        return false
    }

    override fun hashCode() : Int {
        return Objects.hashCode(uri)
    }

    override fun toString() : String {
        return uri.toString()
    }

    ///////////////////////////////////////////////////////////
    // for FileHandlerJavaFile
    private var file : File? = null

    internal constructor(file : File) {
        this.uri = Uri.parse(file.toURI().toString())
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
    // for FileHandlerSAF
    var safMeta : FileHandlerSAF.Meta? = null
        private set

    internal constructor(uri : Uri, safMeta : FileHandlerSAF.Meta?) {
        this.uri = uri
        this.safMeta = safMeta
    }

    /**
     * The mime type of the file if know else null
     */
    val mimeType : String?
        get() = safMeta?.mimeType
}

