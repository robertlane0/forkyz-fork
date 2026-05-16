
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.writeNullTerminatedString

open class IOVersion2 : IOVersion1() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.sourceUrl = meta.sourceUrl
    }

    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        input.skip(1) // read old updatable byte
        meta.sourceUrl = readNullTerminatedString(input, charsetDecoder)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        output.writeByte(1) // old updatable byte
        writeNullTerminatedString(output, puz.sourceUrl, charsetEncoder)
    }
}
