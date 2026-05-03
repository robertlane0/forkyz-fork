
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataOutputStream
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta
import app.crossword.yourealwaysbe.puz.io.IO.Companion.readNullTerminatedString
import app.crossword.yourealwaysbe.puz.io.IO.Companion.writeNullTerminatedString

open class IOVersion2 : IOVersion1() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.sourceUrl = meta.sourceUrl
    }

    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        input.skip(1) // read old updatable byte
        meta.sourceUrl = readNullTerminatedString(input, charset)
        return meta
    }

    @Throws(java.io.IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        os.write(1) // old updatable byte
        writeNullTerminatedString(os, puz.sourceUrl, charset)
    }
}
