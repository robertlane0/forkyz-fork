
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataOutputStream
import java.util.logging.Logger
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

// Adds percentFilled
open class IOVersion4 : IOVersion3() {
    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        meta.percentFilled = input.readInt()
        return meta
    }

    @Throws(java.io.IOException::class)
    override fun writeMeta(puz : Puzzle, os : DataOutputStream) {
        super.writeMeta(puz, os)
        os.writeInt(puz.percentFilled)
    }

    companion object {
        private val LOG
            = Logger.getLogger(IOVersion4::class.java.getCanonicalName())
    }
}

