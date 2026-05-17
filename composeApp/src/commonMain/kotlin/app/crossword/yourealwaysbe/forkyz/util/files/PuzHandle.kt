
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlinx.io.IOException

abstract class PuzHandle private constructor(
    var dirHandle : DirHandle,
    var mainFileHandle : FileHandle,
) {
    /**
     * True if the puzzle is in the given directory
     */
    fun isInDirectory(dirHandle : DirHandle): Boolean {
        return dirHandle == this.dirHandle
    }

    abstract suspend fun <R> accept(v : Visitor<R>) : R

    @Throws(IOException::class)
    abstract suspend fun <R> accept(v : VisitorIO<R>) : R

    /**
     * To update directory if puzzle moves
     *
     * Deliberately package level only
     */
    fun setDirectory(dirHandle : DirHandle) {
        this.dirHandle = dirHandle
    }

    /**
     * A Puz file handle
     *
     * @param metaFileHandle may be null if no meta file
     */
    class Puz(
        dirHandle : DirHandle,
        mainHandle : FileHandle,
        var metaFileHandle : FileHandle?,
    ) : PuzHandle(dirHandle, mainHandle) {
        override suspend fun <R> accept(v : Visitor<R>): R {
            return v.visit(this)
        }

        @Throws(IOException::class)
        override suspend fun <R> accept(v : VisitorIO<R>): R {
            return v.visit(this)
        }
    }

    class IPuz(
        dirHandle : DirHandle,
        mainHandle : FileHandle,
    ) : PuzHandle(dirHandle, mainHandle) {
        override suspend fun <R> accept(v : Visitor<R>): R {
            return v.visit(this)
        }

        @Throws(IOException::class)
        override suspend fun <R> accept(v : VisitorIO<R>) : R {
            return v.visit(this)
        }
    }

    interface Visitor<R> {
        suspend fun visit(puzHandle : Puz): R
        suspend fun visit(ipuzHandle : IPuz): R
    }

    interface VisitorIO<R> {
        @Throws(IOException::class)
        suspend fun visit(puzHandle : Puz): R

        @Throws(IOException::class)
        suspend fun visit(ipuzHandle : IPuz): R
    }
}

