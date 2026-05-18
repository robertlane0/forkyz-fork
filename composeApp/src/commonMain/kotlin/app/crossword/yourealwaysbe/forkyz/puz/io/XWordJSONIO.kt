
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.util.JSONArray
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject


/**
 * JSON Format used by xword
 *
 * https://www.xwordinfo.com/JSON/
 *
 * "title": ..
 * "author": ..
 * "copyright": ..
 * "publisher": ..
 * "date": "m/d/yyyy"
 * "size": { "rows": n, "cols": n }
 * "grid": [ "A", "B", ".", "REBUS" ... ]
 * "gridnums": [ n, n, 0, n, 0, 0 ... ]
 * "circles": [ 0, 1 ... ]
 * "rbars": ..
 * "bbars": ..
 * "clues": {
 * "across": [ "n. hint", ... ]
 * "down": ...
 * }
 * "notepad": "<notes>"
 * "jnotes": "<notes>"
 */
class XWordJSONIO : AbstractJSONIO() {
    class XWordJSONFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val TAG = "ForkyzXWordJSONIO"

        private val DATE_FORMATTER = LocalDate.Format {
            monthNumber(Padding.NONE)
            char('/')
            day(Padding.NONE)
            char('/')
            year(Padding.ZERO)
        }

        @Throws(IOException::class, CancellationException::class)
        suspend fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                AppLogger.i(TAG, "Error parsing XWord JSON: " + e)
                return null
            } catch (e : JSONException) {
                AppLogger.i(TAG, "Error parsing XWord JSON: " + e)
                return null
            } catch (e : XWordJSONFormatException) {
                AppLogger.i(TAG, "Error parsing XWord JSON: " + e)
                return null
            }
        }

        @Throws(
            JSONException::class,
            XWordJSONFormatException::class,
            CancellationException::class,
        )
        private suspend fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val boxes : Array<Array<Box?>> = getBoxes(json)
            val builder = PuzzleBuilder(boxes)
                .setTitle(json.optStringEmptyNull("title"))
                .setAuthor(json.optStringEmptyNull("author"))
                .setCopyright(json.optStringEmptyNull("copyright"))
                .setSource(json.optStringEmptyNull("publisher"))
                .setDate(getDate(json))
                .setNotes(getNotes(json))

            addClues(json, builder)

            return builder.puzzle
        }

        private fun getDate(json : JSONObject) : LocalDate? {
            try {
                val date = json.optStringEmptyNull("date")
                return if (date == null)
                    null
                else
                    DATE_FORMATTER.parse(date)
            } catch (e : IllegalArgumentException) {
                return null
            }
        }

        private fun getNotes(json : JSONObject) : String? {
            val notepad = json.optStringEmptyNull("notepad")
            val jnotes = json.optStringEmptyNull("jnotes")

            if (jnotes == null && notepad == null)
                return null

            if (jnotes == null)
                return notepad

            if (notepad == null)
                return jnotes

            return "<p>" + notepad + "</p><p>" + jnotes + "</p>"
        }

        @Throws(JSONException::class, XWordJSONFormatException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val size = json.getJSONObject("size")
            val numRows = size.getInt("rows")
            val numCols = size.getInt("cols")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val grid = json.getJSONArray("grid")
            val numbers = json.getJSONArray("gridnums")
            val circles = json.optJSONArray("circles")
            val rbars = json.optJSONArray("rbars")
            val bbars = json.optJSONArray("bbars")

            for (cell in 0..<grid.size) {
                val row = cell / numRows
                val col = cell % numRows

                if (row < numRows && col < numCols) {
                    val contents = grid.getString(cell)
                    if ("." != contents) {
                        val box = Box()

                        box.solution = contents

                        if (cell < numbers.size) {
                            val number = numbers.getInt(cell)
                            if (number > 0)
                                box.clueNumber = number.toString()
                        }

                        if (circles != null && cell < circles.size) {
                            val circle = circles.getInt(cell)
                            if (circle != 0)
                                box.shape = Box.Shape.CIRCLE
                        }
                        if (rbars != null && cell < rbars.size) {
                            val rbar = rbars.getInt(cell)
                            if (rbar != 0)
                                box.barRight = Box.Bar.SOLID
                        }
                        if (bbars != null && cell < bbars.size) {
                            val bbar = bbars.getInt(cell)
                            if (bbar != 0)
                                box.barBottom = Box.Bar.SOLID
                        }

                        boxes[row][col] = box
                    }
                }
            }

            return boxes
        }

        @Throws(
            JSONException::class,
            XWordJSONFormatException::class,
            CancellationException::class,
        )
        private suspend fun addClues(
            json : JSONObject,
            builder : PuzzleBuilder,
        ) {
            val clues = json.getJSONObject("clues")
            val across = clues.getJSONArray("across")
            addCluesArray(across, builder, true)
            val down = clues.getJSONArray("down")
            addCluesArray(down, builder, false)
        }

        @Throws(XWordJSONFormatException::class, CancellationException::class)
        private suspend fun addCluesArray(
            clues : JSONArray,
            builder : PuzzleBuilder,
            across : Boolean,
        ) {
            for (i in 0..<clues.size) {
                val clue = clues.getString(i)
                val split : Array<String>
                    = clue.split("\\.".toRegex(), limit = 2).toTypedArray()

                if (split.size != 2) {
                    throw XWordJSONFormatException(
                        "Clue text not in num. hint format: " + clue,
                    )
                }

                val numString = split[0].trim()
                val hint = split[1].trim()
                try {
                    val num = numString.toInt().toString()
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
                    throw XWordJSONFormatException(
                        "Bad clue number " + numString,
                    )
                }
            }
        }
    }
}

