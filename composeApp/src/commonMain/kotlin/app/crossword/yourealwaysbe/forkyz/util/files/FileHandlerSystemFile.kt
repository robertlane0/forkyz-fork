
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.File

/**
 * Base implementation of app.crossword.yourealwaysbe.forkyz.util.files.File-based file access from a given
 * root directory.
 */
abstract class FileHandlerSystemFile : FileHandler {
    private val rootDirectory : File

    constructor(
        utils : NativeBackendUtils,
        rootDirectory : File,
    ) : super(utils) {
        this.rootDirectory = rootDirectory
    }

    /**
     * For testing purposes, allow a meta cache to be passed in
     */
    constructor(
        utils : NativeBackendUtils,
        metaCache : MetaCache,
        rootDirectory : File,
    ) : super(utils, metaCache) {
        this.rootDirectory = rootDirectory
    }

    override val crosswordsDirectory : DirHandle
        get() {
            rootDirectory.mkdirs()
            return DirHandle(rootDirectory)
        }

    override val archiveDirectory : DirHandle
        get() { return getGenericDirectory("archive") }

    override val toImportDirectory : DirHandle
        get() { return getGenericDirectory("to-import") }

    override val toImportDoneDirectory : DirHandle
        get() { return getGenericDirectory("to-import-done") }

    override val toImportFailedDirectory : DirHandle
        get() { return getGenericDirectory("to-import-failed") }

    override fun listFiles(dir : DirHandle) : Iterable<FileHandle> {
        return object : Iterable<FileHandle> {
            override fun iterator() : MutableIterator<FileHandle> {
                return object : MutableIterator<FileHandle> {
                    private var pos = 0
                    private val files = dir.getFile().list().toList()
                    private var next = getNext()

                    override fun hasNext() : Boolean {
                        return next != null
                    }

                    override fun next() : FileHandle {
                        val result = next
                        next = getNext()
                        if (result == null)
                            throw NoSuchElementException()
                        else
                            return FileHandle(result)
                    }

                    override fun remove() {
                        throw UnsupportedOperationException()
                    }

                    fun getNext() : File? {
                        while (pos < files.size) {
                            next = files[pos++]
                            if (next?.isRegularFile ?: false)
                                return next
                        }
                        return null
                    }
                }
            }
        }
    }

    override fun getName(f : FileHandle) : String {
        return f.getFile().name
    }

    override fun getFileHandle(uri : Uri) : FileHandle {
        val path = uri.getPath()
        if (path == null) {
            throw IllegalArgumentException(
                "Can't get file handle for URI " + uri,
            )
        }
        return FileHandle(File(path))
    }

    override fun exists(dir : DirHandle) : Boolean {
        return dir.getFile().exists()
    }

    override fun exists(file : FileHandle) : Boolean {
        return file.getFile().exists()
    }

    override fun getUri(d : DirHandle) : Uri {
        return d.uri
    }

    override fun getUri(f : FileHandle) : Uri {
        return f.uri
    }

    override fun getLastModified(file : FileHandle) : Long {
        return file.getFile().lastModified
    }

    override fun createFileHandle(
        dir : DirHandle,
        fileName : String,
        mimeType : String,
    ) : FileHandle? {
        val file = File(dir.getFile(), fileName)
        if (file.exists())
            return null
        return FileHandle(File(dir.getFile(), fileName))
    }

    override fun deleteUnsync(fileHandle : FileHandle) {
        if (exists(fileHandle))
            fileHandle.getFile().delete()
    }

    override fun moveToUnsync(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        val file = fileHandle.getFile()
        val directory = destDirHandle.getFile()
        val newFile = getUniqueFile(directory, file.name)
        file.move(newFile)
    }

    @Throws(IOException::class)
    override fun getOutput(fileHandle : FileHandle) : RawSink {
        return fileHandle.getFile().sink()
    }

    @Throws(IOException::class)
    override fun getInput(fileHandle : FileHandle) : RawSource {
        return fileHandle.getFile().source()
    }

    private fun getGenericDirectory(path : String) : DirHandle {
        val dir = File(this.rootDirectory, path)
        dir.mkdirs()
        return DirHandle(dir)
    }

    /**
     * Get a unique file name for the directory
     *
     * E.g. dir/name.txt comes back as dir/name (1).txt if dir/name.txt
     * already exists
     */
    private fun getUniqueFile(directory : File, name : String) : File {
        var newFile = File(directory, name)

        val counter = 1
        var basename : String? = null
        var extension : String? = null

        while (newFile.exists()) {
            if (basename == null) {
                val i = name.lastIndexOf(".")
                if (i >= 0) {
                    basename = name.substring(0, i)
                    extension = name.substring(i)
                } else {
                    basename = name
                    extension = ""
                }
            }
            val newName = basename + " (" + counter + ")" + extension

            newFile = File(directory, newName)
        }

        return newFile
    }
}

