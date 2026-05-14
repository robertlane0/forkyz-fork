
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta

// Adds percentFilled
open class IOVersion4 : IOVersion3() {
    @Throws(IOException::class)
    override fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        meta.percentFilled = input.readInt()
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        output.writeInt(puz.percentFilled)
    }
}

