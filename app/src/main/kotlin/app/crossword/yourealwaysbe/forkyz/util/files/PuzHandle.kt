
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.IOException

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

    abstract fun <R> accept(v : Visitor<R>) : R

    @Throws(IOException::class)
    abstract fun <R> accept(v : VisitorIO<R>) : R

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
        override fun <R> accept(v : Visitor<R>): R {
            return v.visit(this)
        }

        @Throws(IOException::class)
        override fun <R> accept(v : VisitorIO<R>): R {
            return v.visit(this)
        }
    }

    class IPuz(
        dirHandle : DirHandle,
        mainHandle : FileHandle,
    ) : PuzHandle(dirHandle, mainHandle) {
        override fun <R> accept(v : Visitor<R>): R {
            return v.visit(this)
        }

        @Throws(IOException::class)
        override fun <R> accept(v : VisitorIO<R>) : R {
            return v.visit(this)
        }
    }

    interface Visitor<R> {
        fun visit(puzHandle : Puz): R
        fun visit(ipuzHandle : IPuz): R
    }

    interface VisitorIO<R> {
        @Throws(IOException::class)
        fun visit(puzHandle : Puz): R

        @Throws(IOException::class)
        fun visit(ipuzHandle : IPuz): R
    }
}

