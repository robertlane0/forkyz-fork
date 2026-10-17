
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.io.Source

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parseSource

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getAcrossZone
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils.getDownZone
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

/**
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
class GuardianJSONIO : AbstractJSONIO() {
    @Throws(Exception::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    private data class ClueInfo(
        val zone : Zone,
        val separators : Map<Int, String>?,
    )

    companion object {
        // Not internationalised, Guardian crosswords are in English
        private const val ACROSS_LIST = "Across"
        private const val DOWN_LIST = "Down"

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            val copy = input.peek()
            var puz : Puzzle? = readFromJSON(copy)
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
                return readPuzzle(json)
            } catch (e : IOException) {
                return null
            } catch (e : JSONException) {
                return null
            }
        }

        fun readFromHTML(input : Source) : Puzzle? {
            try {
                val doc = Ksoup.parseSource(input)
                var cwJson : JSONObject? = null
                var err : JSONException? = null

                var cwJsonStr = doc
                    .select("[name=CrosswordComponent]")
                    .attr("props")

                if (!cwJsonStr.isEmpty()) {
                    try {
                        cwJson = parseJSONObject(cwJsonStr)
                            .optJSONObject("data")
                    } catch (e : JSONException) {
                        err = e
                    }
                }

                // try old format if couldn't find above
                if (cwJson == null) {
                    cwJsonStr = doc.select(".js-crossword")
                        .attr("data-crossword-data")
                    if (!cwJsonStr.isEmpty())
                        cwJson = parseJSONObject(cwJsonStr)
                }

                if (cwJson != null)
                    return readPuzzle(cwJson)
                else if (err != null)
                    throw err
            } catch (e : IOException) {
                // pass through
            } catch (e : JSONException) {
            }
            return null
        }

        /**
         * Read puzzle from Guardian JSON format
         *
         * Does not set source or support url (not necessarily from the
         * Guardian)
         */
        @Throws(JSONException::class)
        fun readPuzzle(json : JSONObject) : Puzzle {
            val builder = PuzzleBuilder(getBoxes(json))
                .setTitle(json.optStringEmptyNull("name"))
                .setIntroMessage(
                    HtmlUtil.brString(
                        json.optStringEmptyNull("instructions"),
                    ),
                )

            addAuthor(json, builder)
            addDate(json, builder)

            addClues(json, builder)

            return builder.puzzle
        }

        private fun addAuthor(json : JSONObject, builder : PuzzleBuilder) {
            val creator = json.optJSONObject("creator")
            if (creator != null)
                builder.setAuthor(creator.optStringEmptyNull("name"))
        }

        private fun addDate(json : JSONObject, builder : PuzzleBuilder) {
            if (json.has("date")) {
                try {
                    val epochMillis = json.getLong("date")
                    builder.setDate(
                        LocalDate.fromEpochDays(
                            epochMillis / (1000 * 60 * 60 * 24),
                        ),
                    )
                } catch (e : JSONException) {
                    try {
                        builder.setDate(
                            LocalDate.Formats.ISO.parse(
                                json.getString("date"),
                            ),
                        )
                    } catch (e2 : IllegalArgumentException) {
                        // ignore
                    }
                }
            }
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
            for (i in 0..<entries.size) {
                val entry = entries.getJSONObject(i)

                val position = entry.getJSONObject("position")
                val x = position.getInt("x")
                val y = position.getInt("y")

                if (x < 0 || x >= numCols || y < 0 || y >= numRows)
                    continue

                val num = entry.getInt("number").toString()
                val clueSol = entry.optStringEmptyNull("solution")
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
            for (i in 0..<entries.size) {
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

            if (group.size > 0 && id == group.getString(0)) {
                for (i in 0..<group.size) {
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
            for (i in 0..<entries.size) {
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

            sepObj.keys.forEach { sep ->
                var sep = sep
                if (sep.isEmpty())
                    return@forEach

                val positions = sepObj.optJSONArray(sep)
                if (positions == null)
                    return@forEach

                if ("," == sep)
                    sep = " "

                for (i in 0..<positions.size) {
                    positions.optInt(i)?.let { pos ->
                        separators.put(pos, sep)
                    }
                }
            }

            return separators
        }
    }
}

