
package app.crossword.yourealwaysbe.puz.io

import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject


/**
 * Puzzle format used by Tribune
 *
 * "metadata": {
 *  "author": ..
 *  "title": ..
 *  "date": ..
 *  "format": "TCA"
 * }
 * "data": {
 *  "clues": [
 *   {
 *    "clueNo": num,
 *    "clueType": "across/down",
 *    "shape": {
 *      "1,2,3,4,5" // list of cell IDs
 *    },
 *    "text": "clue"
 *   }
 *  ],
 *  "gridRow": num rows,
 *  "gridCol": num cols,
 *  "gCells": [
 *   {
 *    "id": cell id num,
 *    "cellNumber": string,
 *    "cellChar": char solution,
 *    "cellType": "square/circle/disable"
 *    "xchar": "Y/N" // not sure what this means, ignore for now
 *   }
 *  ]
 * }
 */
class TCAJSONIO : AbstractJSONIO() {
    class TCAJSONFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(TCAJSONIO::class.java.getCanonicalName())

        private val DATE_META_FORMATTER = LocalDate.Formats.ISO
        private val DATE_DATA_FORMATTER = LocalDate.Format {
            monthNumber(Padding.ZERO)
            char('/')
            day(Padding.ZERO)
            char('/')
            year(Padding.ZERO)
        }

        private val ACROSS_LIST = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST = DefaultListNames.DOWN_LIST

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : TCAJSONFormatException) {
                LOG.severe("Could not read TCA JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read TCA JSON: " + e)
                return null
            }
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parseJSONObject(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : TCAJSONFormatException) {
                LOG.severe("Could not read TCA JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read TCA JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, TCAJSONFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            try {
                val metaData = json.optJSONObject("metadata")
                var data = json.optJSONObject("data")
                if (data == null)
                    data = json

                val cellPositions = mutableMapOf<String, Position>()
                val builder = PuzzleBuilder(getBoxes(data, cellPositions))

                addMetaData(json, builder)
                addClues(data, cellPositions, builder)

                return builder.puzzle
            } catch (e : IllegalArgumentException) {
                throw TCAJSONFormatException(
                    "Could not set grid boxes from data file: " + e.message
                )
            }
        }

        /**
         * Get boxes
         *
         * cellPositions is a map to fill from cell id to grid position
         */
        @Throws(JSONException::class, TCAJSONFormatException::class)
        private fun getBoxes(
            json : JSONObject,
            cellPositions : MutableMap<String, Position>,
        ) : Array<Array<Box?>> {
            val numRows = json.getInt("gridRow")
            val numCols = json.getInt("gridCol")
            val cells = json.getJSONArray("gCells")

            if (numRows == 0 || numCols == 0) {
                throw TCAJSONFormatException(
                    "Grid has a bad shape: "
                    + numRows
                    + " rows and "
                    + numCols
                    + " cols.",
                )
            }

            val rowList = mutableListOf<Array<Box?>>()
            var boxes : Array<Array<Box?>> = Array(numRows) {
                arrayOfNulls<Box?>(numCols)
            }

            for (i in 0..<cells.size) {
                val row = i / numCols
                val col = i % numCols

                val cell = cells.getJSONObject(i)
                val cellType = cell.optStringEmptyNull("cellType")
                if (
                    cellType == null
                    || "disable".equals(cellType, ignoreCase = true)
                ) {
                    continue
                }

                val box = Box()

                if (cell.has("id")) {
                    cellPositions.put(
                        cell.getInt("id").toString(),
                        Position(row, col),
                    )
                }

                val clueNo = cell.optStringEmptyNull("cellNumber")
                if (clueNo != null && clueNo.length > 0)
                    box.clueNumber = clueNo

                val solution = cell.optStringEmptyNull("cellChar")
                if (solution != null && solution.length > 0)
                    box.solution = solution


                if ("square".equals(cellType, ignoreCase = true)) {
                    // normal cell
                } else if ("circle".equals(cellType, ignoreCase = true)) {
                    box.shape = Box.Shape.CIRCLE
                } else {
                    // see if it's an IPuz value why not
                    val shape = IPuzIO.getShape(cellType)
                    if (shape != null) box.shape = shape
                }

                boxes[row][col] = box

                // Does xchar need handling?
            }

            return boxes
        }

        @Throws(JSONException::class)
        private fun addMetaData(json : JSONObject, builder : PuzzleBuilder) {
            val metaData = json.optJSONObject("metaData")
            val data = json.optJSONObject("data")

            if (metaData != null) {
                builder.setTitle(metaData.optStringEmptyNull("title"))
                builder.setAuthor(metaData.optStringEmptyNull("author"))
                builder.setNotes(metaData.optStringEmptyNull("description"))
                try {
                    val date = metaData.optStringEmptyNull("date")
                    if (date != null) {
                        builder.setDate(
                            DATE_META_FORMATTER.parse(date),
                        )
                    }
                } catch (e : IllegalArgumentException) {
                    // oh well
                }
            }

            if (data != null) {
                try {
                    val date = data.optStringEmptyNull("date")
                    if (date != null) {
                        builder.setDate(
                            DATE_DATA_FORMATTER.parse(date),
                        )
                    }
                } catch (e : IllegalArgumentException) {
                    // oh well
                }
            }
        }

        @Throws(JSONException::class)
        private fun addClues(
            json : JSONObject,
            cellPositions : Map<String, Position>,
            builder : PuzzleBuilder,
        ) {
            val clues = json.getJSONArray("clues")
            for (i in 0..<clues.size) {
                val clue = clues.getJSONObject(i)

                var clueNumber : String? = null
                if (clue.has("clueNo"))
                    clueNumber = clue.getInt("clueNo").toString()

                val across = "across".equals(
                    clue.getString("clueType"),
                    ignoreCase = true,
                )

                val zone = Zone()
                val shape = clue.optJSONObject("shape")
                if (shape != null) {
                    val cellIDs = shape.getString("value")
                    for (
                        id in cellIDs.split(",".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    ) {
                        val pos = cellPositions.get(id)
                        if (pos != null)
                            zone.addPosition(pos)
                    }
                }

                val hint = clue.getString("text")

                val listName = if (across)
                    TCAJSONIO.Companion.ACROSS_LIST
                else
                    TCAJSONIO.Companion.DOWN_LIST
                val index = builder.getNextClueIndex(listName)
                builder.addClue(
                    Clue(listName, index, clueNumber, null, hint, zone),
                )
            }
        }
    }
}

