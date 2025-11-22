
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

import android.net.Uri
import androidx.core.net.toUri

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

class NoopFileHandler(utils : NativeBackendUtils) : FileHandler(utils) {
    override val crosswordsDirectory : DirHandle
        get() { return DirHandle(File("none")) }
    override val archiveDirectory : DirHandle
        get() { return DirHandle(File("none")) }
    override val toImportDirectory : DirHandle
        get() { return DirHandle(File("none")) }
    override val toImportDoneDirectory : DirHandle
        get() { return DirHandle(File("none")) }
    override val toImportFailedDirectory : DirHandle
        get() { return DirHandle(File("none")) }
    override val isStorageMounted = false
    override val isStorageFull = false
    override val settings = FileHandlerSettings(
        StorageLocation.SL_INTERNAL,
        "",
        "",
        "",
        "",
        "",
        "",
    )

    override fun listFiles(dir : DirHandle) : Iterable<FileHandle> {
        return listOf()
    }

    override fun getName(f : FileHandle) : String {
        return "none"
    }

    protected override fun exists(dir : DirHandle) : Boolean {
        return false
    }

    protected override fun exists(file : FileHandle) : Boolean {
        return false
    }

    override fun getUri(d : DirHandle) : Uri {
        return "content://none".toUri()
    }

    override fun getUri(f : FileHandle) : Uri {
        return "content://none".toUri()
    }

    protected override fun getLastModified(file : FileHandle) : Long {
        return 0L
    }

    protected override fun getFileHandle(uri : Uri) : FileHandle {
        return FileHandle(File("none"))
    }

    override fun getOutputStream(
        fileHandle : FileHandle
    ) : OutputStream {
        return FileOutputStream(File("none"))
    }

    override fun getInputStream(
        fileHandle : FileHandle,
    ) : InputStream {
        return FileInputStream(File("none"))
    }

    override fun createFileHandle(
        dir : DirHandle,
        fileName : String,
        mimeType : String,
    ) : FileHandle {
        return FileHandle(File("none"))
    }

    protected override fun moveToUnsync(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        // noop
    }

    protected override fun deleteUnsync(fileHandle : FileHandle) {
        // noop
    }
}
