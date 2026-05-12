
package app.crossword.yourealwaysbe.forkyz.puz.io

import java.util.logging.Logger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.parseJSONObject

/**
 * SlowDownWiseUp JSON format
 *
 * "data": {
 *   "grid": [
 *     [ { "Number": "<num>", "Letter": "<solution>", "Blank": "<blank>" } ...]
 *     ..
 *   ],
 *   "copy": {
 *     "title": "Test title",
 *     "description": "..",
 *     "setter": "..",
 *     "publisher": "..",
 *     "date-publish": "..",
 *     "type": "block",
 *     "settings": {
 *        "solution_hashed": "md5sum?"
 *     },
 *     "correctsolutionmessagetext": "Correct solution",
 *     "type": "block",
 *     "clues": [ {
 *         "title": "listname",
 *         "clues": [
 *           { "word": <id>, "number": n, "clue": ".." }, ...
 *         ]
 *       }
 *     ],
 *   },
 *   "words": [
 *      { id: n, "x": "1-5", "y": "3" }, ...
 *   ]
 * }
 */
class SlowDownWiseUpJSONIO : AbstractJSONIO() {
    class SlowDownWiseUpJSONFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(AmuseLabsJSONIO::class.java.getCanonicalName())

        private val DATE_FORMATTER = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            chars(", ")
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year(Padding.NONE)
        }

        private val DEFAULT_CLUE_LIST_NAME
            = DefaultListNames.NODIR_LIST

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val json = parseJSONObject(input)
                return readPuzzleFromJSON(json)
            } catch (e : SlowDownWiseUpJSONFormatException) {
                LOG.severe("Could not read SlowDownWiseUp JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read SlowDownWiseUp JSON: " + e)
                return null
            }
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parseJSONObject(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : SlowDownWiseUpJSONFormatException) {
                LOG.severe("Could not read SlowDownWiseUp JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read SlowDownWiseUp JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, SlowDownWiseUpJSONFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            try {
                var data = json.optJSONObject("data")
                if (data == null)
                    data = json

                val builder = PuzzleBuilder(getBoxes(data))

                addMetaData(data, builder)
                addClues(data, builder)

                return builder.puzzle
            } catch (e : IllegalArgumentException) {
                throw SlowDownWiseUpJSONFormatException(
                    "Could not set grid boxes from data file: " + e.message,
                )
            }
        }

        @Throws(JSONException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val rowList = mutableListOf<Array<Box?>>()

            val rows = json.getJSONArray("grid")
            return Array(rows.size) { row ->
                val rowArray = rows.getJSONArray(row)
                val rowBoxes = arrayOfNulls<Box?>(rowArray.size)
                for (col in 0..<rowArray.size) {
                    val cell = rowArray.getJSONObject(col)
                    if (
                        !"blank".equals(
                            cell.optStringEmptyNull("Blank"),
                            ignoreCase = true,
                        )
                    ) {
                        val box = Box()

                        box.clueNumber = cell.optStringEmptyNull("Number")
                        box.solution = cell.optStringEmptyNull("Letter")

                        val style = cell.optJSONObject("style")
                        if (style != null) {
                            val shape = style.optStringEmptyNull("shapebg")
                            if ("circle".equals(shape, ignoreCase = true)) {
                                box.shape = Box.Shape.CIRCLE
                            } else {
                                // see if it's an IPuz value why not
                                val boxShape = IPuzIO.getShape(shape)
                                if (boxShape != null)
                                    box.shape = boxShape
                            }

                            if (style.optBoolean("highlight", false)) {
                                box.color = IPuzIO.HIGHLIGHT_COLOR
                            }
                        }

                        rowBoxes[col] = box
                    }
                }
                rowBoxes
            }
        }

        @Throws(JSONException::class)
        private fun addMetaData(json : JSONObject, builder : PuzzleBuilder) {
            val copy = json.getJSONObject("copy")
            builder.setTitle(copy.optStringEmptyNull("title"))
            builder.setAuthor(copy.optStringEmptyNull("setter"))
            builder.setSource(copy.optStringEmptyNull("publisher"))
            builder.setNotes(copy.optStringEmptyNull("description"))
            builder.setCompletionMessage(
                copy.optStringEmptyNull("correctsolutionmessagetext"),
            )

            try {
                val date = copy.optStringEmptyNull("date-publish")
                if (date != null)
                    builder.setDate(DATE_FORMATTER.parse(date))
            } catch (e : IllegalArgumentException) {
                // oh well
            }
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val zones : Map<Int, Zone> = getZones(json)
            val clues = json.getJSONObject("copy").getJSONArray("clues")
            for (i in 0..<clues.size) {
                addCluesList(clues.getJSONObject(i), zones, builder)
            }
        }

        @Throws(JSONException::class)
        private fun addCluesList(
            json : JSONObject,
            zones : Map<Int, Zone>,
            builder : PuzzleBuilder,
        ) {
            var listName = json.optStringEmptyNull("title")
                ?: DEFAULT_CLUE_LIST_NAME

            val clues = json.getJSONArray("clues")
            for (i in 0..<clues.size) {
                val clue = clues.getJSONObject(i)

                var zone : Zone? = null
                val zoneID = clue.optInt("word", -1)
                if (zoneID >= 0) {
                    zone = zones.get(zoneID)
                }

                val number = clue.optStringEmptyNull("number")
                val format = clue.optStringEmptyNull("format")
                val separators = getSeparators(format)
                val hintMain = clue.getString("clue")
                val hint = hintMain + (format?.let { " ($it)" } ?: "")
                val index = builder.getNextClueIndex(listName)
                builder.addClue(
                    Clue(
                        listName,
                        index,
                        number,
                        null,
                        hint,
                        zone ?: Zone(),
                        separators,
                    ),
                )
            }
        }

        private fun getSeparators(format : String?) : Map<Int, String>? {
            if (format == null)
                return null

            val separators = mutableMapOf<Int, String>()

            var len = 0
            var sepPos = 0

            var i = 0
            while (i < format.length) {
                var c = format[i]
                if (Character.isDigit(c)) {
                    val digit = c.digitToIntOrNull() ?: -1
                    len = 10 * len + digit
                } else {
                    sepPos = sepPos + len
                    var separator = c.toString()
                    if (separator == ",")
                        separator = " "
                    separators.put(sepPos, separator)
                    len = 0
                }
                i++
            }

            return separators
        }

        @Throws(JSONException::class)
        private fun getZones(json : JSONObject) : Map<Int, Zone> {
            val copy = json.getJSONObject("copy")
            val words = copy.getJSONArray("words")

            val zones = mutableMapOf<Int, Zone>()

            for (i in 0..<words.size) {
                val word = words.getJSONObject(i)
                val id = word.getInt("id")
                val x = word.optStringEmptyNull("x")
                val y = word.optStringEmptyNull("y")
                if (x != null && y != null)
                    zones.put(id, parseCells(x, y))
            }

            return zones
        }

        /**
         * Parse cells data into zone
         *
         * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
         */
        private fun parseCells(x : String, y : String) : Zone {
            val zone = Zone()

            val xs : Array<String> = x.split("-".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val xstart = xs[0].toInt() - 1
            val xend = if (xs.size > 1)
                xs[1].toInt() - 1
            else
                xstart

            val ys : Array<String> = y.split("-".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val ystart = ys[0].toInt() - 1
            val yend = if (ys.size > 1)
                ys[1].toInt() - 1
            else
                ystart

            for (row in ystart..yend)
                for (col in xstart..xend)
                    zone.addPosition(Position(row, col))

            return zone
        }
    }
}

