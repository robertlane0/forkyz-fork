
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding

import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse

/**
 * Format used by amuniversal.com
 *
 * JSON:
 *
 * Grid shape given by layout field. Each box is two characters. 00 for blank
 * cell, -1 for no cell, else number of cell. "Linei" field for ith line, 1-based.
 *
 * "Layout": { "Line1": "010203-100...", ... }
 *
 * "Solution": { "Line1": "ANSWER ANSWER ANSWER", ... }
 *
 * In solution, spaces are the blocks.
 *
 * "DownClue": "num|hint\nnum|hint\n..."
 * "AcrossClue": "num|hint\nnum|hint\n..."
 *
 * Clues are one string, one line per clue, each clue num|hint.
 *
 * Other fields:
 *
 * "Date": "yyyymmdd"
 * "Author"
 * "Editor"
 * "Copyright"
 * "Title"
 * "Width"
 * "Height"
 */
class AMUniversalJSONIO : AbstractJSONIO() {
    /**
     * An unfancy exception indicating error while parsing
     */
    class AMUniversalFormatException(msg : String?) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG
            = Logger.getLogger(AMUniversalJSONIO::class.java.getCanonicalName())

        private val DATE_FORMATTER = LocalDate.Format {
            year(Padding.ZERO)
            monthNumber(Padding.ZERO)
            day(Padding.ZERO)
        }

        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                LOG.info("Error parsing AMUniversal JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.info("Error parsing AMUniversal JSON: " + e)
                return null
            } catch (e : AMUniversalFormatException) {
                LOG.info("Error parsing AMUniversal JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, AMUniversalFormatException::class)
        private fun readPuzzleFromJSON(
            json : JSONObject,
        ) : Puzzle {
            val builder = PuzzleBuilder(getBoxes(json))
                .setTitle(optStringNull(json, "Title"))
                .setAuthor(optStringNull(json, "Author"))
                .setCopyright(optStringNull(json, "Copyright"))
                .setDate(getDate(json))

            addClues(json, builder)

            return builder.puzzle
        }

        private fun getDate(json : JSONObject) : LocalDate? {
            try {
                val date = optStringNull(json, "Date")
                return if (date == null)
                    null
                else
                    DATE_FORMATTER.parse(date)
            } catch (e : IllegalArgumentException) {
                return null
            }
        }

        @Throws(JSONException::class, AMUniversalFormatException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val numRows = json.getInt("Height")
            val numCols = json.getInt("Width")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val layout = json.getJSONObject("Layout")
            for (row in 0..<numRows) {
                val rowData = layout.getString("Line" + (row + 1))

                // split into twos
                for (col in 0..<numCols) {
                    if (2 * col + 1 < rowData.length) {
                        try {
                            val cell = (
                                ""
                                + rowData.get(2 * col)
                                + rowData.get(2 * col + 1)
                            )
                            val cellVal = cell.toInt()
                            if (cellVal >= 0) {
                                val box = Box()
                                if (cellVal > 0)
                                    box.clueNumber = cellVal.toString()
                                boxes[row][col] = box
                            }
                        } catch (e : NumberFormatException) {
                            throw AMUniversalFormatException(
                                "Bad number format for cell in " + row,
                            )
                        }
                    }
                }
            }

            val solution = json.optJSONObject("Solution")
            if (solution != null) {
                for (row in 0..<numRows) {
                    val rowData = solution.getString("Line" + (row + 1))
                    for (col in 0..<numCols) {
                        if (col < rowData.length) {
                            val answer = rowData.get(col)
                            if (answer != ' ')
                                boxes[row][col]?.setSolution(answer)
                        }
                    }
                }
            }

            return boxes
        }

        @Throws(JSONException::class, AMUniversalFormatException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val across = optStringNull(json, "AcrossClue")
            if (across != null)
                addCluesFromString(across, builder, true)
            val down = optStringNull(json, "DownClue")
            if (down != null)
                addCluesFromString(down, builder, false)
        }

        @Throws(AMUniversalFormatException::class)
        private fun addCluesFromString(
            clues : String,
            builder : PuzzleBuilder,
            across : Boolean,
        ) {
            val lines = clues.split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (clue in lines) {
                val parts = clue.split("\\|".toRegex(), limit = 2)
                    .toTypedArray()
                if (parts.size >= 2) {
                    val numString = parts[0]
                    val hint = parts[1]
                    try {
                        val num : String? = numString.toInt().toString()
                        if (across)
                            builder.addAcrossClue(ACROSS_LIST, num, hint)
                        else
                            builder.addDownClue(DOWN_LIST, num, hint)
                    } catch (e : NumberFormatException) {
                        throw AMUniversalFormatException(
                            "Bad clue number " + numString,
                        )
                    }
                }
            }
        }
    }
}

