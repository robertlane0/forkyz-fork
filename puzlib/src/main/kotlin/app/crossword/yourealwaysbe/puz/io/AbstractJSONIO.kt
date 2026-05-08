
package app.crossword.yourealwaysbe.puz.io

import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle

/**
 * Base class containing convenience functions for JSON IO
 */
abstract class AbstractJSONIO : PuzzleParser {
    @Throws(Exception::class)
    abstract override fun parseInput(input : Source) : Puzzle?
}
