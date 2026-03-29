
package app.crossword.yourealwaysbe.puz.io.versions

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.Charset;

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleMeta

interface IOVersion {
    @Throws(IOException::class)
    fun write(puz : Puzzle, os : DataOutputStream)

    @Throws(IOException::class)
    fun read(puz : Puzzle, inStream : DataInputStream)

    @Throws(IOException::class)
    fun readMeta(inStream : DataInputStream) : PuzzleMeta

    val charset : Charset
}
