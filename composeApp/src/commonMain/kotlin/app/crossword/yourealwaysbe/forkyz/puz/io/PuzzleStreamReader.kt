
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils

class PuzzleStreamReader : PuzzleParser {
    /**
     * Read the puzzle from the input stream, try multiple formats
     *
     * The method will try known file formats until it finds one that
     * parses to completion.
     */
    override suspend fun parseInput(input : Source) : Puzzle? {
        return parseInputStatic(input)
    }

    companion object {
        private val TAG = "ForkyzPuzzleStreamRdr"

        private val PARSERS = arrayOf(
            IO(),
            JPZIO(),
            IPuzIO(),
            UclickXMLIO(),
            BrainsOnlyIO(),
            KingFeaturesPlaintextIO(),
            XWordJSONIO(),
            XDIO(),
            GuardianJSONIO(),
            AmuseLabsJSONIO(),
            RaetselZentraleSchwedenJSONIO(),
            RCIJeuxMCJIO(),
            RCIJeuxMFJIO(),
            PrzekrojIO(),
            KeesingXMLIO(),
            PAPuzzlesIO(),
            AMUniversalJSONIO(),
            SlowDownWiseUpJSONIO(),
            SpoonbillIO(),
            ExolveIO(),
            PMLIO(),
            TCAJSONIO(),
            SmartGamesJSONIO(),
            FortyTwoPuzzlesIO(),
        )

        suspend fun parseInputStatic(input : Source) : Puzzle? {
            try {
                val unzipped = StreamUtils.unzipOrPassThrough(input)
                for (parser in PARSERS) {
                    try {
                        val copy = unzipped.peek()
                        AppLogger.i(
                            TAG,
                            "Attempting parse with "
                                + parser::class,
                        )
                        val puz = parser.parseInput(copy)
                        if (puz != null)
                            return puz
                        else AppLogger.i(TAG, "Parse returned null.")
                    } catch (e : Exception) {
                        AppLogger.i(TAG, "Parse attempt failed with " + e)
                        // on to the next one
                    }
                }
            } catch (e : IOException) {
                AppLogger.i(TAG, "Could not read input stream with " + e)
            }
            return null
        }
    }
}

