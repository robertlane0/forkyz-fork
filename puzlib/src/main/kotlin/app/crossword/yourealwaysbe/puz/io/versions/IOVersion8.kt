
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataOutputStream
import java.util.logging.Logger
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

// Add support URL
open class IOVersion8 : IOVersion7() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.playerNote = meta.playerNote
    }

    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        meta.playerNote = readNote(input)
        return meta
    }

    @Throws(java.io.IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        writeNote(puz.playerNote, os)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion8::class.java.getCanonicalName())
    }
}
