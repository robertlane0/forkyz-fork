
package app.crossword.yourealwaysbe.forkyz.net

import java.io.InputStream

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.PuzzleParser

interface StreamScraper : PuzzleParser {
    /**
     * Parse puzzle from input stream from a given URL
     *
     * Implementations of this method should make a reasonable effort to
     * detect when the input is not in the right format. E.g. do not
     * simply return an empty puzzle because the right XML tags were not
     * found.
     *
     * @param is input stream data
     * @param url the url the data is coming from (may be null)
     * @return parse puzzle or null if failed or not in right format
     */
    @Throws(Exception::class)
    fun parseInput(inputStream : InputStream, url: String?): Puzzle?
}

