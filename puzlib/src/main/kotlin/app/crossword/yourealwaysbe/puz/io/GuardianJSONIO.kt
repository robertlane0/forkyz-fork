
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
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
class GuardianJSONIO : AbstractJSONIO() {
    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    @JvmRecord
    private data class ClueInfo(
        val zone : Zone,
        val separators : Map<Int, String>?,
    )

    companion object {
        private val LOG = Logger.getLogger(
            GuardianJSONIO::class.java.getCanonicalName(),
        )

        // Not internationalised, Guardian crosswords are in English
        private const val ACROSS_LIST = "Across"
        private const val DOWN_LIST = "Down"

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            val copy = StreamUtils.makeByteArrayInputStream(inStream)
            var puz : Puzzle? = readFromJSON(copy)
            if (puz == null) {
                copy.reset()
                puz = readFromHTML(copy)
            }
            return puz
        }

        /**
         * Returns null if failed
         */
        fun readFromJSON(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : IOException) {
                return null
            } catch (e : JSONException) {
                return null
            }
        }

        fun readFromHTML(inStream : InputStream) : Puzzle? {
            try {
                val doc = Jsoup.parse(inStream, null, "")
                var cwJson : JSONObject? = null
                var err : JSONException? = null

                var cwJsonStr = doc
                    .select("[name=CrosswordComponent]")
                    .attr("props")
                if (!cwJsonStr.isEmpty()) {
                    try {
                        cwJson = parse(cwJsonStr).optJSONObject("data")
                    } catch (e : JSONException) {
                        err = e
                    }
                }

                // try old format if couldn't find above
                if (cwJson == null) {
                    cwJsonStr = doc.select(".js-crossword")
                        .attr("data-crossword-data")
                    if (!cwJsonStr.isEmpty()) cwJson = parse(cwJsonStr)
                }

                if (cwJson != null)
                    return readPuzzleFromJSON(cwJson)
                else if (err != null)
                    throw err
            } catch (e : IOException) {
                // pass through
            } catch (e : JSONException) {
            }
            return null
        }

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
         * Read puzzle from Guardian JSON format
         *
         * Does not set source or support url (this method may be moved to
         * puzlib/io at some point).
         */
        @Throws(JSONException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            val builder = PuzzleBuilder(getBoxes(json))
                .setTitle(optStringNull(json, "name"))

            val creator = json.optJSONObject("creator")
            if (creator != null)
                builder.setAuthor(optStringNull(creator, "name"))

            if (json.has("date")) {
                try {
                    val epochMillis = json.getLong("date")
                    builder.setDate(
                        LocalDate.ofEpochDay(
                            epochMillis / (1000 * 60 * 60 * 24),
                        ),
                    )
                } catch (e : JSONException) {
                    try {
                        builder.setDate(
                            LocalDate.parse(
                                json.getString("date"),
                                DateTimeFormatter.ISO_LOCAL_TIME,
                            ),
                        )
                    } catch (e2 : DateTimeParseException) {
                        // ignore
                    }
                }
            }

            addClues(json, builder)

            return builder.puzzle
        }

        @Throws(JSONException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val dimensions = json.getJSONObject("dimensions")
            val numRows = dimensions.getInt("rows")
            val numCols = dimensions.getInt("cols")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            val entries = json.getJSONArray("entries")
            for (i in 0..<entries.length()) {
                val entry = entries.getJSONObject(i)

                val position = entry.getJSONObject("position")
                val x = position.getInt("x")
                val y = position.getInt("y")

                if (x < 0 || x >= numCols || y < 0 || y >= numRows)
                    continue

                val num = entry.getInt("number").toString()
                val clueSol = optStringNull(entry, "solution")
                val direction = entry.getString("direction")
                val length = entry.getInt("length")

                var dx = 0
                var dy = 0
                if (direction == "across")
                    dx = 1
                else
                    dy = 1

                var boxX = x
                var boxY = y
                for (j in 0..<length) {
                    if (boxX >= numCols || boxY >= numRows)
                        break

                    if (boxes[boxY][boxX] == null)
                        boxes[boxY][boxX] = Box()

                    if (clueSol != null && j < clueSol.length)
                        boxes[boxY][boxX]?.setSolution(clueSol.get(j))

                    boxX += dx
                    boxY += dy
                }

                boxes[y][x]?.let { it.clueNumber = num }
            }

            return boxes
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val info = getCluesInfo(json, builder)
            val entries = json.getJSONArray("entries")
            for (i in 0..<entries.length()) {
                val entry = entries.getJSONObject(i)
                addClue(entry, info, builder)
            }
        }

        private fun addClue(
            entry : JSONObject,
            info : Map<String, ClueInfo>,
            builder : PuzzleBuilder,
        ) {
            val id = entry.getString("id")
            val num = entry.getString("humanNumber")
            val across = entry.getString("direction") == "across"
            val hint = entry.getString("clue")

            val group = entry.getJSONArray("group")
            val zone = Zone()

            val separators : MutableMap<Int, String> = mutableMapOf()

            if (group.length() > 0 && id == group.getString(0)) {
                for (i in 0..<group.length()) {
                    val otherId = group.getString(i)
                    val otherInfo = info.get(otherId)
                    if (otherInfo != null) {
                        val sepOffset = zone.size()
                        val otherSeps = otherInfo.separators
                        if (otherSeps != null) {
                            for (entry in otherSeps) {
                                val pos = entry.key
                                val sep = entry.value
                                separators.put(sepOffset + pos, sep)
                            }
                        }
                        zone.appendZone(otherInfo.zone)
                    }
                }
            }

            val listName = if (across) ACROSS_LIST else DOWN_LIST
            val index = builder.getNextClueIndex(listName)

            builder.addClue(
                Clue(listName, index, num, null, hint, zone, separators),
            )
        }

        /**
         * Gets map from clue id to info
         *
         * Info is: zone on board, separators for zone
         */
        private fun getCluesInfo(
            json : JSONObject,
            builder : PuzzleBuilder,
        ) : MutableMap<String, ClueInfo> {
            val info : MutableMap<String, ClueInfo> = mutableMapOf()
            val entries = json.getJSONArray("entries")
            for (i in 0..<entries.length()) {
                val entry = entries.getJSONObject(i)

                val id = entry.getString("id")

                val position = entry.getJSONObject("position")
                val x = position.getInt("x")
                val y = position.getInt("y")
                val across = entry.getString("direction") == "across"

                val puz = builder.puzzle
                val start = Position(y, x)

                val zone = if (across)
                    getAcrossZone(puz, start)
                else
                    getDownZone(puz, start)

                val separators = getSeparators(entry)

                info.put(id, ClueInfo(zone, separators))
            }

            return info
        }

        private fun getSeparators(entry : JSONObject) : Map<Int, String>? {
            val sepObj = entry.optJSONObject("separatorLocations")
            if (sepObj == null)
                return null

            val separators : MutableMap<Int, String> = mutableMapOf()

            sepObj.keys().forEachRemaining { sep ->
                var sep = sep
                if (sep == null || sep.isEmpty())
                    return@forEachRemaining

                val positions = sepObj.optJSONArray(sep)
                if (positions == null)
                    return@forEachRemaining

                if ("," == sep)
                    sep = " "

                for (i in 0..<positions.length()) {
                    val pos = positions.optInt(i)
                    separators.put(pos, sep)
                }
            }

            return separators
        }
    }
}

