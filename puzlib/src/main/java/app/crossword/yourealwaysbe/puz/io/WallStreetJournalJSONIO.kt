
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Logger

import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse

/**
 * Read a character stream of JSON data in the format used by the
 * Wall Street Journal
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
class WallStreetJournalJSONIO : AbstractJSONIO() {
    class WSJFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG
            = Logger.getLogger(AmuseLabsJSONIO::class.java.getCanonicalName())

        private val DATE_FORMATTER
            = DateTimeFormatter.ofPattern("EEEE, d MMMM y")

        // Not internationalised, WSJ English
        private const val DEFAULT_CLUE_LIST_NAME = "Clues"

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : WSJFormatException) {
                LOG.severe("Could not read WSJ JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read WSJ JSON: " + e)
                return null
            }
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parse(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : WSJFormatException) {
                LOG.severe("Could not read WSJ JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read WSJ JSON: " + e)
                return null
            }
        }

        @Throws(JSONException::class, WSJFormatException::class)
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
                throw WSJFormatException(
                    "Could not set grid boxes from data file: " + e.message,
                )
            }
        }

        @Throws(JSONException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val rowList = mutableListOf<Array<Box?>>()

            val rows = json.getJSONArray("grid")
            return Array(rows.length()) { row ->
                val rowArray = rows.getJSONArray(row)
                val rowBoxes = arrayOfNulls<Box?>(rowArray.length())
                for (col in 0..<rowArray.length()) {
                    val cell = rowArray.getJSONObject(col)
                    if (
                        !"blank".equals(
                            optStringNull(cell, "Blank"),
                            ignoreCase = true,
                        )
                    ) {
                        val box = Box()

                        box.clueNumber = optStringNull(cell, "Number")
                        box.solution = optStringNull(cell, "Letter")

                        val style = cell.optJSONObject("style")
                        if (style != null) {
                            val shape = optStringNull(style, "shapebg")
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
            builder.setTitle(optStringNull(copy, "title"))
            builder.setAuthor(optStringNull(copy, "setter"))
            builder.setSource(optStringNull(copy, "publisher"))
            builder.setNotes(optStringNull(copy, "description"))
            builder.setCompletionMessage(
                optStringNull(copy, "correctsolutionmessagetext"),
            )

            try {
                val date = optStringNull(copy, "date-publish")
                if (date != null)
                    builder.setDate(LocalDate.parse(date, DATE_FORMATTER))
            } catch (e : DateTimeParseException) {
                // oh well
            }
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val zones : Map<Int, Zone> = getZones(json)
            val clues = json.getJSONObject("copy").getJSONArray("clues")
            for (i in 0..<clues.length()) {
                addCluesList(clues.getJSONObject(i), zones, builder)
            }
        }

        @Throws(JSONException::class)
        private fun addCluesList(
            json : JSONObject,
            zones : Map<Int, Zone>,
            builder : PuzzleBuilder,
        ) {
            var listName = optStringNull(json, "title")
                ?: DEFAULT_CLUE_LIST_NAME

            val clues = json.getJSONArray("clues")
            for (i in 0..<clues.length()) {
                val clue = clues.getJSONObject(i)

                var zone : Zone? = null
                val zoneID = clue.optInt("word", -1)
                if (zoneID >= 0) {
                    zone = zones.get(zoneID)
                }

                val number = optStringNull(clue, "number")
                val hint = optStringNull(clue, "clue") ?: ""

                val index = builder.getNextClueIndex(listName)
                builder.addClue(
                    Clue(listName, index, number, null, hint, zone ?: Zone()),
                )
            }
        }

        @Throws(JSONException::class)
        private fun getZones(json : JSONObject) : Map<Int, Zone> {
            val copy = json.getJSONObject("copy")
            val words = copy.getJSONArray("words")

            val zones = mutableMapOf<Int, Zone>()

            for (i in 0..<words.length()) {
                val word = words.getJSONObject(i)
                val id = word.getInt("id")
                val x = optStringNull(word, "x")
                val y = optStringNull(word, "y")
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

