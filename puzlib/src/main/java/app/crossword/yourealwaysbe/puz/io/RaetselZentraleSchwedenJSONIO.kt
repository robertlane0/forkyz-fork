
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.Locale
import java.util.logging.Logger

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse

/**
 * Format used by RaetselZentrale for Schwedenrätsel
 *
 * Swedish crosswords are the ones where the clues themselves appear in
 * the grid, with arrows pointing to the box where the answer starts.
 */
class RaetselZentraleSchwedenJSONIO : AbstractJSONIO() {
    class RZSFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    private data class ClueInfo(
        val position : Position?,
        val isAcross : Boolean,
        val hint : String,
    )

    companion object {
        private val LOG
            = Logger.getLogger(
                RaetselZentraleSchwedenJSONIO::class.java.getCanonicalName(),
            )

        // Not internationalised, assumed German
        private const val ACROSS_LIST = "Hinüber"
        private const val DOWN_LIST = "Hinunter"
        private const val WINWORD_LIST = "Lösungswort"

        private const val WINWORD_HINT = "Lösungswort"

        private const val ARROW_DOWN_RIGHT = "1"
        private const val ARROW_UP_RIGHT = "2"
        private const val ARROW_RIGHT = "3"
        private const val ARROW_RIGHT_DOWN = "4"
        private const val ARROW_LEFT_DOWN = "5"
        private const val ARROW_DOWN = "6"
        private const val EMPTY_CELL = "+"

        private val MULTI_CLUE_CELLS = mutableSetOf(
            // seen
            "7", "8", "9", ":", ";", "@", "=",
            // supposed
            "<", ">", "?",
        )

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : JSONException) {
                LOG.severe("Could not read RaetselZentrale JSON: " + e)
                return null
            } catch (e : RZSFormatException) {
                LOG.severe("Could not read RaetselZentrale JSON: " + e)
                return null
            }
        }

        @Throws(RZSFormatException::class)
        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parse(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : JSONException) {
                LOG.severe("Could not read Guardian JSON: " + e)
                return null
            }
        }

        /**
         * Read puzzle from RaetselZentrale JSON format
         *
         * Does not include author, date, source.
         */
        @Throws(JSONException::class, RZSFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val type = json.getString("type")
            if (type.lowercase(Locale.getDefault()) != "sweden") {
                throw RZSFormatException(
                    "Unsupported puzzle type " + type + ".",
                )
            }

            if (json.optBoolean("encrypted", false)) {
                throw RZSFormatException(
                    "Encrypted puzzles not supported.",
                )
            }

            val payload = json.getJSONObject("payload")

            val builder = PuzzleBuilder(getBoxes(payload))
                .setTitle(json.getString("name"))

            addClues(payload, builder)
            addWinWord(payload, builder)

            return builder.puzzle
        }

        @Throws(RZSFormatException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val numRows = json.getInt("height")
            val numCols = json.getInt("width")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val grid = json.getJSONArray("grid")
            val solution = json.getJSONObject("solution")

            addGridToBoxes(grid, boxes)

            return boxes
        }

        private fun addGridToBoxes(
            grid : JSONArray,
            boxes : Array<Array<Box?>>,
        ) {
            for (row in 0..<grid.length()) {
                val cellRow = grid.getJSONArray(row)
                for (col in 0..<cellRow.length()) {
                    val cell = cellRow.getString(col)

                    if (!isBlankCell(cell)) {
                        val box = Box()
                        box.solution = cell
                        boxes[row][col] = box
                    }

                    // clue numbers added with clues
                }
            }
        }

        /**
         * Takes payload json, adds clues to builder
         */
        @Throws(RZSFormatException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val clueInfos = getClueInfos(json)

            var nextClueNumber = 1
            val curPos = Position(0, 0)

            for (row in 0..<builder.height) {
                curPos.row = row
                for (col in 0..<builder.width) {
                    curPos.col = col
                    val posClueInfos = clueInfos.get(curPos)
                    if (posClueInfos != null) {
                        for (clueInfo in posClueInfos) {
                            val box = builder.getBox(curPos)
                            if (box == null || isBlock(box)) {
                                throw RZSFormatException(
                                    "Clue has position on a block square "
                                    + curPos,
                                )
                            }

                            val clueNumber : String?
                            if (box.hasClueNumber()) {
                                clueNumber = box.clueNumber
                            } else {
                                clueNumber = nextClueNumber.toString()
                                nextClueNumber += 1
                                builder.setBoxClueNumber(row, col, clueNumber)
                            }

                            if (clueInfo.isAcross) {
                                builder.addAcrossClue(
                                    ACROSS_LIST, clueNumber,
                                    clueInfo.hint,
                                )
                            } else {
                                builder.addDownClue(
                                    DOWN_LIST, clueNumber,
                                    clueInfo.hint,
                                )
                            }
                        }
                    }
                }
            }
        }

        /**
         * Takes payload json, adds winword to builder
         */
        @Throws(RZSFormatException::class)
        private fun addWinWord(json : JSONObject, builder : PuzzleBuilder) {
            val boxes : Array<Array<Box?>> = builder.puzzle.boxes

            val solution = json.optJSONObject("solution")
            if (solution == null)
                return

            val positions = solution.getJSONArray("matrix")
            val zone = Zone()
            for (i in 0..<positions.length()) {
                val position = positions.getJSONArray(i)
                val row = position.getInt(1) - 1
                val col = position.getInt(0) - 1
                zone.addPosition(Position(row, col))

                boxes[row][col]?.let { box ->
                    val marks = Array<Array<String?>>(3) {
                        arrayOfNulls<String>(3)
                    }
                    marks[2][2] = (i + 1).toString()
                    box.setMarks(marks)
                }
            }

            val winWordClue = Clue(WINWORD_LIST, 0, null, WINWORD_HINT, zone)
            builder.addClue(winWordClue)
            builder.setPinnedClueID(winWordClue.clueID)
        }

        @Throws(RZSFormatException::class)
        private fun getClueInfos(
            json : JSONObject,
        ) : Map<Position, List<ClueInfo>> {
            val clueInfos : MutableMap<Position, MutableList<ClueInfo>>
                = mutableMapOf()

            val clues = json.getJSONArray("words")
            for (i in 0..<clues.length()) {
                val clue = clues.getJSONObject(i)

                val position = clue.getJSONObject("position")
                val row = position.getInt("row") - 1
                val col = position.getInt("col") - 1


                // -+ to split words across a line
                // just + to split not during a word
                val hint = clue.getString("question")
                    .replace("-+", "")
                    .replace("+", "")

                val arrowType = clue.getString("arrowtype")

                val trueStart = getTrueStart(row, col, arrowType)
                val isAcross = getIsAcross(arrowType)

                if (!clueInfos.containsKey(trueStart)) {
                    clueInfos.put(
                        trueStart,
                        mutableListOf<ClueInfo>(),
                    )
                }

                clueInfos.get(trueStart)?.let {
                    it.add(ClueInfo(trueStart, isAcross, hint))
                }
            }

            return clueInfos
        }

        /**
         * Where the word actually starts given the clue position
         */
        @Throws(RZSFormatException::class)
        private fun getTrueStart(
            row : Int,
            col : Int,
            arrowType : String?,
        ) : Position {
            if (ARROW_DOWN_RIGHT == arrowType)
                return Position(row + 1, col)
            if (ARROW_UP_RIGHT == arrowType)
                return Position(row - 1, col)
            if (ARROW_RIGHT == arrowType)
                return Position(row, col + 1)
            if (ARROW_RIGHT_DOWN == arrowType)
                return Position(row, col + 1)
            if (ARROW_LEFT_DOWN == arrowType)
                return Position(row, col - 1)
            if (ARROW_DOWN == arrowType)
                return Position(row + 1, col)

            throw RZSFormatException("Unexpected arrow type: " + arrowType)
        }

        /**
         * Whether arrow says clue is across (or down)
         */
        @Throws(RZSFormatException::class)
        private fun getIsAcross(arrowType : String?) : Boolean {
            if (ARROW_DOWN_RIGHT == arrowType)
                return true
            if (ARROW_UP_RIGHT == arrowType)
                return true
            if (ARROW_RIGHT == arrowType)
                return true
            if (ARROW_RIGHT_DOWN == arrowType)
                return false
            if (ARROW_LEFT_DOWN == arrowType)
                return false
            if (ARROW_DOWN == arrowType)
                return false

            throw RZSFormatException("Unexpected arrow type: " + arrowType)
        }

        private fun isBlankCell(cellValue : String?) : Boolean {
            if (cellValue == null)
                return true
            if (ARROW_DOWN_RIGHT == cellValue)
                return true
            if (ARROW_UP_RIGHT == cellValue)
                return true
            if (ARROW_RIGHT == cellValue)
                return true
            if (ARROW_RIGHT_DOWN == cellValue)
                return true
            if (ARROW_LEFT_DOWN == cellValue)
                return true
            if (ARROW_DOWN == cellValue)
                return true
            if (EMPTY_CELL == cellValue)
                return true
            if (MULTI_CLUE_CELLS.contains(cellValue))
                return true

            return false
        }
    }
}

