
package app.crossword.yourealwaysbe.puz.io

import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.JSONArray
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject


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
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(XWordJSONIO::class.java.getCanonicalName())

        private val DATE_FORMATTER = LocalDate.Format {
            monthNumber(Padding.NONE)
            char('/')
            day(Padding.NONE)
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
            } catch (e : IOException) {
                LOG.info("Error parsing XWord JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.info("Error parsing XWord JSON: " + e)
                return null
            } catch (e : XWordJSONFormatException) {
                LOG.info("Error parsing XWord JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, XWordJSONFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
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

        @Throws(JSONException::class, XWordJSONFormatException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val clues = json.getJSONObject("clues")
            val across = clues.getJSONArray("across")
            addCluesArray(across, builder, true)
            val down = clues.getJSONArray("down")
            addCluesArray(down, builder, false)
        }

        @Throws(XWordJSONFormatException::class)
        private fun addCluesArray(
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
                    if (across)
                        builder.addAcrossClue(ACROSS_LIST, num, hint)
                    else
                        builder.addDownClue(DOWN_LIST, num, hint)
                } catch (e : NumberFormatException) {
                    throw XWordJSONFormatException(
                        "Bad clue number " + numString,
                    )
                }
            }
        }
    }
}

