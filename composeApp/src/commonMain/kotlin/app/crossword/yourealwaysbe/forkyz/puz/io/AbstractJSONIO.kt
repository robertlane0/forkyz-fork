
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle

/**
 * Base class containing convenience functions for JSON IO
 */
abstract class AbstractJSONIO : PuzzleParser {
    @Throws(Exception::class)
    abstract override suspend fun parseInput(input : Source) : Puzzle?
}
