
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
    override fun getCrosswordsDirectory() : DirHandle {
        return DirHandle(File("none"))
    }

    override fun getArchiveDirectory() : DirHandle {
        return DirHandle(File("none"))
    }

    override fun getToImportDirectory() : DirHandle {
        return DirHandle(File("none"))
    }

    override fun getToImportDoneDirectory() : DirHandle {
        return DirHandle(File("none"))
    }

    override fun getToImportFailedDirectory() : DirHandle {
        return DirHandle(File("none"))
    }

    override fun isStorageMounted() : Boolean {
        return false
    }

    override fun isStorageFull() : Boolean {
        return false
    }

    override fun listFiles(dir : DirHandle) : Iterable<FileHandle> {
        return listOf()
    }

    override fun getName(f : FileHandle) : String {
        return "none"
    }

    override fun getSettings() : FileHandlerSettings {
        return FileHandlerSettings(
            StorageLocation.SL_INTERNAL,
            "",
            "",
            "",
            "",
            "",
            "",
        )
    }

    protected override fun exists(dir : DirHandle) : Boolean {
        return false
    }

    protected override fun exists(file : FileHandle) : Boolean {
        return false
    }

    protected override fun getUri(f : DirHandle) : Uri {
        return "content://none".toUri()
    }

    protected override fun getUri(f : FileHandle) : Uri {
        return "content://none".toUri()
    }

    protected override fun getLastModified(file : FileHandle) : Long {
        return 0L
    }

    protected override fun getFileHandle(uri : Uri) : FileHandle {
        return FileHandle(File("none"))
    }

    protected override fun getOutputStream(
        fileHandle : FileHandle
    ) : OutputStream {
        return FileOutputStream(File("none"))
    }

    protected override fun getInputStream(
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
