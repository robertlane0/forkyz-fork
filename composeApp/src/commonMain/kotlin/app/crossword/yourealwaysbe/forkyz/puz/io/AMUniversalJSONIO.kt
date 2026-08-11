
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

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
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val TAG = "ForkyzAMUniversalJSON"

        private val DATE_FORMATTER = LocalDate.Format {
            year(Padding.ZERO)
            monthNumber(Padding.ZERO)
            day(Padding.ZERO)
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                AppLogger.i(TAG, "Error parsing AMUniversal JSON: " + e)
                return null
            } catch (e : JSONException) {
                AppLogger.i(TAG, "Error parsing AMUniversal JSON: " + e)
                return null
            } catch (e : AMUniversalFormatException) {
                AppLogger.i(TAG, "Error parsing AMUniversal JSON: " + e)
                return null
            }
        }

        @Throws(
            JSONException::class,
            AMUniversalFormatException::class,
            CancellationException::class,
        )
        private suspend fun readPuzzleFromJSON(
            json : JSONObject,
        ) : Puzzle {
            val builder = PuzzleBuilder(getBoxes(json))
                .setTitle(json.optStringEmptyNull("Title"))
                .setAuthor(json.optStringEmptyNull("Author"))
                .setCopyright(json.optStringEmptyNull("Copyright"))
                .setDate(getDate(json))

            addClues(json, builder)

            return builder.puzzle
        }

        private fun getDate(json : JSONObject) : LocalDate? {
            try {
                val date = json.optStringEmptyNull("Date")
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

        @Throws(
            JSONException::class,
            AMUniversalFormatException::class,
            CancellationException::class,
        )
        private suspend fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val across = json.optStringEmptyNull("AcrossClue")
            if (across != null)
                addCluesFromString(across, builder, true)
            val down = json.optStringEmptyNull("DownClue")
            if (down != null)
                addCluesFromString(down, builder, false)
        }

        @Throws(AMUniversalFormatException::class, CancellationException::class)
        private suspend fun addCluesFromString(
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
                        if (across) {
                            builder.addAcrossClue(
                                DefaultListNames.acrossList(),
                                num,
                                hint,
                            )
                        } else {
                            builder.addDownClue(
                                DefaultListNames.downList(),
                                num,
                                hint,
                            )
                        }
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

