
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleMeta
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetDecoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetEncoder

interface IOVersion {
    @Throws(IOException::class)
    fun write(puz : Puzzle, output : Sink)

    @Throws(IOException::class)
    suspend fun read(puz : Puzzle, input : Source)

    @Throws(IOException::class)
    suspend fun readMeta(input : Source) : PuzzleMeta

    val charsetDecoder : CharsetDecoder
    val charsetEncoder : CharsetEncoder
}
