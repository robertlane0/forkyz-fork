
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.writeNullTerminatedString

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
}

