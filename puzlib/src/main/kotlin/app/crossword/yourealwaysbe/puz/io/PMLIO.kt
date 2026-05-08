
package app.crossword.yourealwaysbe.puz.io

import java.util.Locale
import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.PuzzleBuilder.BasicClue
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject

/**
 * Read a character stream of JSON data in the format used by PML.
 */
class PMLIO : AbstractJSONIO() {
    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(PMLIO::class.java.getCanonicalName())

        private val ACROSS_LIST = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST = DefaultListNames.DOWN_LIST

        private val DATE_FORMATTER = LocalDate.Format {
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year(Padding.NONE)
        }

        private val CW_JSON_RE
            = "^.*starting_puzzle\\s*:\\s*(\\{.*pml_id.*\\}).*$".toRegex()
        private const val CW_JSON_GRP = 1

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            val copy = input.peek()
            var puz = readFromJSON(copy)
            if (puz == null) {
                puz = readFromHTML(input)
            }
            return puz
        }

        /**
         * Returns null if failed
         */
        fun readFromJSON(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                return null
            } catch (e : JSONException) {
                return null
            }
        }

        /**
         * Returns null if failed
         */
        fun readFromJSON(jsonString : String) : Puzzle {
            val json = parseJSONObject(jsonString)
            return readPuzzleFromJSON(json)
        }

        fun readFromHTML(input : Source) : Puzzle? {
            try {
                val cwJson = readPuzzleJSON(input)
                if (cwJson != null && !cwJson.isEmpty())
                    return readPuzzleFromJSON(parseJSONObject(cwJson))
            } catch (e : JSONException) {
                // pass through
            }
            return null
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parseJSONObject(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : JSONException) {
                LOG.severe("Could not read PML JSON: " + e)
                return null
            }
        }

        /**
         * Return JSON or null if not found
         */
        private fun readPuzzleJSON(input : Source) : String? {
            try {
                var line = input.readLine()
                while (line != null) {
                    val match = CW_JSON_RE.find(line)
                    if (match != null)
                        return match.groups[CW_JSON_GRP]?.value
                    line = input.readLine()
                }
            } catch (e : IOException) {
                // fall through
            }
            return null
        }

        /**
         * Read puzzle from PML JSON format
         */
        @Throws(JSONException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val gameData = json.getJSONObject("game_data")
            val builder = PuzzleBuilder(getBoxes(gameData))
                .setTitle(json.optStringEmptyNull("name"))
                .setDate(getDate(json))
            addClues(gameData, builder)
            return builder.puzzle
        }

        @Throws(JSONException::class)
        private fun getDate(gameData : JSONObject) : LocalDate? {
            try {
                val date = gameData.optString("rdate")
                if (date == null)
                    return null
                return DATE_FORMATTER.parse(date)
            } catch (e : IllegalArgumentException) {
                return null
            }
        }

        @Throws(JSONException::class)
        private fun getBoxes(gameData : JSONObject) : Array<Array<Box?>> {
            val numRows = gameData.getInt("rows")
            val numCols = gameData.getInt("cols")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val items = gameData.getJSONArray("items")
            for (i in 0..<items.size) {
                val item = items.getJSONObject(i)

                val position = item.getInt("start") - 1
                val row = position / numCols
                val col = position % numCols

                if (row < 0 || row >= numRows || col < 0 || col >= numCols)
                    continue

                val num = item.getInt("num").toString()
                val clueSol = item.getString("answer")
                val length = clueSol.length

                var drow = 0
                var dcol = 0
                if (isAcross(item)) dcol = 1
                else drow = 1

                var boxRow = row
                var boxCol = col
                for (j in 0..<length) {
                    if (boxRow >= numRows || boxCol >= numCols)
                        break

                    if (boxes[boxRow][boxCol] == null)
                        boxes[boxRow][boxCol] = Box()

                    if (j < clueSol.length)
                        boxes[boxRow][boxCol]?.setSolution(clueSol.get(j))

                    boxRow += drow
                    boxCol += dcol
                }

                boxes[row][col]?.let { it.clueNumber = num }
            }

            return boxes
        }

        @Throws(JSONException::class)
        private fun addClues(gameData : JSONObject, builder : PuzzleBuilder) {
            val items = gameData.getJSONArray("items")
            for (i in 0..<items.size) {
                val item = items.getJSONObject(i)
                addClue(item, builder)
            }
        }

        private fun isAcross(item : JSONObject) : Boolean {
            return item.getInt("dir") == 0
        }

        @Throws(JSONException::class)
        private fun addClue(item : JSONObject, builder : PuzzleBuilder) {
            val num = item.getInt("num").toString()
            val hint = item.getString("clue")
            if (isAcross(item))
                builder.addAcrossClue(ACROSS_LIST, BasicClue(num, hint))
            else
                builder.addDownClue(DOWN_LIST, BasicClue(num, hint))
        }
    }
}

