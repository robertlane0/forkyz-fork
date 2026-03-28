
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.puz.io.IO.Companion.writeNullTerminatedString

// Add support URL
open class IOVersion7 : IOVersion6() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.supportUrl = meta.supportUrl
    }

    @Throws(IOException::class)
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)
        meta.supportUrl = readNullTerminatedString(inStream, charset)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        writeNullTerminatedString(os, puz.supportUrl, charset)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion7::class.java.getCanonicalName())
    }
}

