
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import java.nio.charset.Charset
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta

interface IOVersion {
    @Throws(IOException::class)
    fun write(puz : Puzzle, output : Sink)

    @Throws(IOException::class)
    fun read(puz : Puzzle, input : Source)

    @Throws(IOException::class)
    fun readMeta(input : Source) : PuzzleMeta

    val charset : Charset
}
