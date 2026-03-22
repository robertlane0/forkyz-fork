
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.function.Consumer
import java.util.logging.Logger

import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getAcrossZone
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils.getDownZone

/**
 * JSON Format used by slowdownwiseup
 *
 * {
 *  "copy": {
 *   "title": "..",
 *   "setter": "..",
 *   "date-publish": "Sunday, 22 March 2026",
 *   "type": "block",
 *   "gridsize": {
 *     "type": "Standard",
 *     "cols": "15",
 *     "rows": "15"
 *   },
 *   "settings": {
 *    "solution_hashed": "md5sum?"
 *   },
 *   "words": [
 *     { "id": 1, "x": "1-4", "y": "1" },
 *     ..
 *   ],
 *   "clues": [
 *    {
 *     "name": "Clues",
 *     "title": "Across",
 *     "clues": [
 *      {
 *       "word": 1,
 *       "number": 1,
 *       "clue": "hint",
 *       "format": "2,2",
 *       "length": 4
 *      },
 *      ..
 *     ]
 *    }
 *   ],
 *  }
 *  "grid": [
 *   [
 *    {
 *     "Number": "1" or "",
 *     "Blank": "" or "blank" (blank for block)
 *     "WordAcrossID": 1,
 *     "WordDownID": "",
 *     "Letter": "" (solution?)
 *    },
 *    ..
 *   ],
 *   ..
 *  ]
 * }
 *
 */
class SlowDownWiseUpJSONIO : AbstractJSONIO() {
    /**
     * An unfancy exception indicating error while parsing
     */
    class SlowDownWiseUpFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG = Logger.getLogger(
            SlowDownWiseUpJSONIO::class.java.getCanonicalName(),
        )
        private val DATE_FORMATTER
            = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu")

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                return null
            } catch (e : SlowDownWiseUpFormatException) {
                return null
            } catch (e : JSONException) {
                return null
            }
        }

        @Throws(JSONException::class, SlowDownWiseUpFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val builder = PuzzleBuilder(getBoxes(json))
            addMetaData(json, builder)
            addClues(json, builder)
            return builder.puzzle
        }

        @Throws(JSONException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val dimensions = json.getJSONObject("copy")
                .getJSONObject("gridsize")
            val numRows = Integer.valueOf(dimensions.getString("rows"))
            val numCols = Integer.valueOf(dimensions.getInt("cols"))

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val rows = json.getJSONArray("grid")
            for (row in 0..<rows.length()) {
                val rowData = rows.getJSONArray(row)
                for (col in 0..<rowData.length()) {
                    val cell = rowData.getJSONObject(col)

                    val blank = optStringNull(cell, "Blank")
                    if (
                        "blank".equals(
                            optStringNull(cell, "Blank"),
                            ignoreCase = true,
                        )
                    ) {
                        continue
                    }

                    val box = Box()

                    val number = optStringNull(cell, "Number")
                    if (number != null && !number.isEmpty())
                        box.clueNumber = number

                    val solution = optStringNull(cell, "Letter")
                    if (solution != null && !solution.isEmpty())
                        box.solution = solution

                    boxes[row][col] = box
                }
            }

            return boxes
        }

        @Throws(JSONException::class, SlowDownWiseUpFormatException::class)
        private fun addMetaData(json : JSONObject, builder : PuzzleBuilder) {
            val copyData = json.getJSONObject("copy")
            if (
                !"block".equals(
                    copyData.getString("type"),
                    ignoreCase = true,
                )
            ) {
                throw SlowDownWiseUpFormatException("Puzzle is not a block type")
            }

            optStringNull(copyData, "title")?.let { title ->
                builder.setTitle(title)
            }
            optStringNull(copyData, "date-publish")?.let { date ->
                try {
                    val parsedDate = LocalDate.parse(date, DATE_FORMATTER)
                    builder.setDate(parsedDate)
                } catch (e : DateTimeParseException) {
                    // ignore
                }
            }
            optStringNull(copyData, "setter")?.let { setter ->
                builder.setAuthor(setter)
            }
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val words = getWords(json)
            val clueLists = json.getJSONObject("copy").getJSONArray("clues")
            for (i in 0..<clueLists.length()) {
                val clueList = clueLists.getJSONObject(i)
                addClueList(clueList, words, builder)
            }
        }

        private fun addClueList(
            clueList : JSONObject,
            words : Map<Int, Zone>,
            builder : PuzzleBuilder,
        ) {
            val listName = clueList.getString("title")
            val clues = clueList.getJSONArray("clues")
            for (i in 0..<clues.length()) {
                val clue = clues.getJSONObject(i)

                val wid : Int? = clue.optInt("word")
                val zone = wid?.let { words.get(it) }
                    ?: Zone()

                val numberInt : Int? = clue.optInt("number")
                val number = numberInt?.let { it.toString() }

                val format = optStringNull(clue, "format")
                val separators = getSeparators(format)
                val hintMain = clue.getString("clue")
                val hint = hintMain + (format?.let { " ($it)" } ?: "")
                val index = builder.getNextClueIndex(listName)

                builder.addClue(
                    Clue(listName, index, number, null, hint, zone, separators),
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

        /**
         * Gets map from word ID to zones
         */
        private fun getWords(json : JSONObject) : Map<Int, Zone> {
            val wordMap : MutableMap<Int, Zone> = mutableMapOf()
            val words = json.getJSONObject("copy").getJSONArray("words")
            for (i in 0..<words.length()) {
                val word = words.getJSONObject(i)
                val wid = word.getInt("id")
                val xs = getRange(word.getString("x"))
                val ys = getRange(word.getString("y"))

                val zone = Zone()
                for (col in xs) {
                    for (row in ys) {
                        zone.addPosition(Position(row - 1, col - 1))
                    }
                }

                wordMap.put(wid, zone)
            }

            return wordMap
        }

        /**
         * Convert 3 to [3] and 3-5 to [3, 4, 5]
         */
        private fun getRange(range : String) : IntRange {
            val nums = range.split("-").map { it.toInt() }
            return when (nums.size) {
                0 -> 0..<0
                1 -> nums[0]..nums[0]
                else -> (nums[0]..nums[1])
            }
        }
    }
}

