
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

import app.crossword.yourealwaysbe.forkyz.util.Uri

/**
 * java.io.File-like facade for preferred file mechanism
 */
class File {
    private val path : Path

    constructor(base : String, vararg parts : String) {
        path = Path(base, *parts)
    }

    constructor(base : File, vararg parts : String) {
        path = Path(base.path, *parts)
    }

    internal constructor(base : Path, vararg parts : String) {
        path = Path(base, *parts)
    }

    val isRegularFile : Boolean
        get() {
            return SystemFileSystem.metadataOrNull(path)?.isRegularFile ?: false
        }

    val isDirectory : Boolean
        get() {
            return SystemFileSystem.metadataOrNull(path)?.isDirectory ?: false
        }

    val absoluteFile : File
        get() {
            return File(SystemFileSystem.resolve(path))
        }

    val uri : Uri
        get() {
            return getUri(path)
        }

    val name : String
        get() {
            return path.name
        }

    val lastModified : Long
        get() {
            return getLastModified(path)
        }

    val pathString : String
        get() {
            return path.toString()
        }

    fun list() : Collection<File> {
        return SystemFileSystem.list(path).map(::File)
    }

    fun exists() : Boolean {
        return SystemFileSystem.exists(path)
    }

    fun mkdirs() {
        SystemFileSystem.createDirectories(path)
    }

    fun delete() {
        SystemFileSystem.delete(path)
    }

    fun move(newPath : File) {
        SystemFileSystem.atomicMove(path, newPath.path)
    }

    fun sink() : RawSink {
        return SystemFileSystem.sink(path)
    }

    fun source() : RawSource {
        return SystemFileSystem.source(path)
    }

    override fun toString() : String {
        return path.toString()
    }
}

/**
 * Get the last modified date of the Path
 *
 * (Not yet provided by kotlinx.io)
 */
expect internal fun getLastModified(path : Path) : Long

/**
 * Convert path to Uri
 *
 * May need to go through e.g. JVM URI to encode correctly.
 */
expect internal fun getUri(path : Path) : Uri

