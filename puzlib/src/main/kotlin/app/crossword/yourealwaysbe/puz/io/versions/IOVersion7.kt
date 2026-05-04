
package app.crossword.yourealwaysbe.puz.io.versions

import java.util.logging.Logger
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

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
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        meta.supportUrl = readNullTerminatedString(input, charset)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        writeNullTerminatedString(output, puz.supportUrl, charset)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion7::class.java.getCanonicalName())
    }
}

