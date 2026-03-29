
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

// Add support URL
open class IOVersion8 : IOVersion7() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.playerNote = meta.playerNote
    }

    @Throws(IOException::class)
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)
        meta.playerNote = readNote(inStream)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        writeNote(puz.playerNote, os)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion8::class.java.getCanonicalName())
    }
}
