
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Puzzle

class PuzzleStreamReader : PuzzleParser {
    /**
     * Read the puzzle from the input stream, try multiple formats
     *
     * The method will try known file formats until it finds one that
     * parses to completion.
     */
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return parseInputStatic(inStream)
    }

    companion object {
        private val LOGGER = Logger.getLogger(
            PuzzleStreamReader::class.java.getCanonicalName(),
        )

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

        @JvmStatic
        fun parseInputStatic(inStream : InputStream) : Puzzle? {
            try {
                val unzipped = StreamUtils.unzipOrPassThrough(inStream)
                for (parser in PARSERS) {
                    try {
                        unzipped.reset()
                        LOGGER.info(
                            "Attempting parse with "
                                + parser.javaClass.getName(),
                        )
                        val puz = parser.parseInput(unzipped)
                        if (puz != null)
                            return puz
                        else LOGGER.info("Parse returned null.")
                    } catch (e : Exception) {
                        LOGGER.info("Parse attempt failed with " + e)
                        // on to the next one
                    }
                }
            } catch (e : IOException) {
                LOGGER.info("Could not read input stream with " + e)
            }
            return null
        }
    }
}

