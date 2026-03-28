
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

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
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)
        inStream.read() // read old updatable byte
        meta.sourceUrl = readNullTerminatedString(inStream, charset)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        os.write(1) // old updatable byte
        writeNullTerminatedString(os, puz.sourceUrl, charset)
    }
}
