
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle

interface PuzzleParser {
    /**
     * Parse puzzle from input
     *
     * Implementations of this method should make a reasonable effort to
     * detect when the input is not in the right format. E.g. do not
     * simply return an empty puzzle because the right XML tags were not
     * found.
     *
     * @return parse puzzle or null if failed or not in right format
     */
    @Throws(Exception::class)
    suspend fun parseInput(input : Source) : Puzzle?
}

