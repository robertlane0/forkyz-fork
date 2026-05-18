
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta

// Add support URL
open class IOVersion8 : IOVersion7() {
    override fun applyMeta(puz : Puzzle, meta : PuzzleMeta) {
        super.applyMeta(puz, meta)
        puz.playerNote = meta.playerNote
    }

    @Throws(IOException::class, CancellationException::class)
    override suspend fun readMeta(input : Source) : PuzzleMeta {
        val meta = super.readMeta(input)
        meta.playerNote = readNote(input)
        return meta
    }

    @Throws(IOException::class)
    override fun writeMeta(puz : Puzzle, output : Sink) {
        super.writeMeta(puz, output)
        writeNote(puz.playerNote, output)
    }
}

