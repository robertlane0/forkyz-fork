
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataOutputStream
import java.nio.charset.Charset
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

interface IOVersion {
    @Throws(java.io.IOException::class)
    fun write(puz : Puzzle, os : DataOutputStream)

    @Throws(IOException::class)
    fun read(puz : Puzzle, input : Source)

    @Throws(IOException::class)
    fun readMeta(input : Source) : PuzzleMeta

    val charset : Charset
}
