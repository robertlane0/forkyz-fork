
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

private val TAG = "USATodayJSONIO"

/**
 * Read a character stream of JSON data in the format used by USA Today
 */
class USATodayJSONIO : AbstractJSONIO() {
    class USATodayFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        // Not internationalised, USA Today crosswords are in English
        private const val ACROSS_LIST = "Across"
        private const val DOWN_LIST = "Down"

        /**
         * Returns null if failed
         */
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzle(json)
            } catch (e : IOException) {
                AppLogger.e(TAG, "Could not parse puzzle $e")
                return null
            } catch (e : JSONException) {
                AppLogger.e(TAG, "Could not parse puzzle $e")
                return null
            } catch (e : USATodayFormatException) {
                AppLogger.e(TAG, "Could not parse puzzle $e")
                return null
            }
        }

        @Throws(JSONException::class, USATodayFormatException::class)
        fun readPuzzle(json : JSONObject) : Puzzle {
            val puzJson = getPuzJSON(json)

            val date = try {
                puzJson.optString("date")?.let {
                    LocalDate.Formats.ISO.parse(it)
                }
            } catch (e : IllegalArgumentException) {
                null
            }

            var author = puzJson.optString("author") ?: ""
            if (puzJson.has("editor"))
                author += ", ed. " + puzJson.getString("editor")

            val builder = PuzzleBuilder(getBoxes(puzJson))
                .setTitle(puzJson.optStringEmptyNull("title"))
                .setAuthor(author)
                .setCopyright(puzJson.optString("copyright"))
                .setDate(date)

            addClues(puzJson, builder)

            return builder.puzzle
        }

        @Throws(JSONException::class, USATodayFormatException::class)
        private fun getPuzJSON(json : JSONObject) : JSONObject {
            var innerJson = json

            if (innerJson.has("data"))
                innerJson = innerJson.getJSONObject("data")

            if (innerJson.optString("__typename") == "Query")
                innerJson = innerJson.getJSONObject("gameData")

            if (innerJson.optString("__typename") == "CrosswordData")
                return innerJson

            throw USATodayFormatException("No game data found in JSON: $json")
        }

        @Throws(JSONException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val numRows = json.getInt("height")
            val numCols = json.getInt("width")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val rows = json.getJSONArray("layout")
            for (row in 0..<rows.size) {
                val cells = rows.getString(row)

                for ((col, cellStr) in cells.chunked(2).withIndex()) {
                    val cell = cellStr.toInt()

                    if (cell < 0)
                        continue

                    val box = Box()

                    if (cell > 0)
                        box.clueNumber = cell.toString()

                    boxes[row][col] = box
                }
            }

            json.optJSONArray("solution")?.let { solution ->
                for (row in 0 until solution.size) {
                    val rowSolution = solution.getString(row)
                    for ((col, cellSolution) in rowSolution.withIndex()) {
                        if (cellSolution != ' ') {
                            boxes[row][col]?.let {
                                it.solution = cellSolution.toString()
                            }
                        }
                    }
                }
            }

            return boxes
        }

        @Throws(JSONException::class, USATodayFormatException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            json.optString("acrossClue")?.let { across ->
                for (clue in across.split("\n"))
                    addClue(true, clue, builder)
            }
            json.optString("downClue")?.let { down ->
                for (clue in down.split("\n"))
                    addClue(false, clue, builder)
            }
        }

        @Throws(USATodayFormatException::class)
        private fun addClue(
            across : Boolean,
            clue : String,
            builder : PuzzleBuilder,
        ) {
            val clueSplit = clue.split("|", limit = 2)
            if (clueSplit.size != 2) {
                throw USATodayFormatException(
                    "Can't parse clue $clue",
                )
            }

            try {
                val listName = if (across) ACROSS_LIST else DOWN_LIST
                val number = clueSplit[0].toInt().toString()
                val hint = clueSplit[1]

                if (across)
                    builder.addAcrossClue(listName, number, hint)
                else
                    builder.addDownClue(listName, number, hint)
            } catch (e : IllegalArgumentException) {
                throw USATodayFormatException(
                    "Could not parse clue number ${clueSplit[0]}",
                )
            }
        }
    }
}

