
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

// Adds percentFilled
open class IOVersion4 : IOVersion3() {
    @Throws(IOException::class)
    override fun readMeta(inStream : DataInputStream) : PuzzleMeta {
        val meta = super.readMeta(inStream)
        meta.percentFilled = inStream.readInt()
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        os.writeInt(puz.percentFilled)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion4::class.java.getCanonicalName())
    }
}

