
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import kotlinx.datetime.LocalDate

import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.JSONParser.parse

/**
 * Read a character stream of JSON data in the format used by the
 * Amuse Labs.
 */
class AmuseLabsJSONIO : AbstractJSONIO() {
    /**
     * An unfancy exception indicating error while parsing
     */
    class AmuseLabsFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG
            = Logger.getLogger(AmuseLabsJSONIO::class.java.getCanonicalName())

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val json = parse(inStream)
                return readPuzzleFromJSON(json)
            } catch (e : AmuseLabsFormatException) {
                LOG.severe("Could not read Amuse Labs JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read Amuse Labs JSON: " + e)
                return null
            }
        }

        fun readPuzzle(jsonString : String) : Puzzle? {
            try {
                val json = parse(jsonString)
                return readPuzzleFromJSON(json)
            } catch (e : AmuseLabsFormatException) {
                LOG.severe("Could not read Amuse Labs JSON: " + e)
                return null
            } catch (e : JSONException) {
                LOG.severe("Could not read Amuse Labs JSON: " + e)
                return null
            }
        }

        /**
         * Read puzzle from Amuse Labs JSON format
         */
        @Throws(JSONException::class, AmuseLabsFormatException::class)
        private fun readPuzzleFromJSON(json : JSONObject) : Puzzle {
            try {
                val builder = PuzzleBuilder(getBoxes(json))

                builder.setTitle(optStringNull(json, "title"))
                    .setAuthor(optStringNull(json, "author"))
                    .setCopyright(optStringNull(json, "copyright"))
                    .setSource(optStringNull(json, "publisher"))
                    .setCompletionMessage(optStringNull(json, "endMessage"))

                if (json.has("publishTime")) {
                    val epochMillis = json.getLong("publishTime")
                    builder.setDate(
                        LocalDate.fromEpochDays(
                            epochMillis / (1000 * 60 * 60 * 24),
                        )
                    )
                }

                addClues(json, builder)

                return builder.puzzle
            } catch (e : IllegalArgumentException) {
                throw AmuseLabsFormatException(
                    "Could not set grid boxes from data file: " + e.message,
                )
            }
        }

        @Throws(JSONException::class, AmuseLabsFormatException::class)
        private fun getBoxes(json : JSONObject) : Array<Array<Box?>> {
            val numRows = json.getInt("h")
            val numCols = json.getInt("w")

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }

            var cols = json.getJSONArray("box")
            for (col in 0..<cols.length()) {
                val rows = cols.getJSONArray(col)
                for (row in 0..<rows.length()) {
                    val entryString = rows.getString(row)

                    val isBox = (
                        entryString.isEmpty()
                        || entryString.get(0) != '\u0000'
                    )

                    if (isBox) {
                        val box = Box()
                        if (!entryString.isEmpty())
                            box.solution = entryString
                        boxes[row][col] = box
                    }
                }
            }

            cols = json.optJSONArray("clueNums")
            if (cols != null) {
                for (col in 0..<cols.length()) {
                    val rows = cols.getJSONArray(col)
                    for (row in 0..<rows.length()) {
                        val clueNum = rows.optInt(row, 0)

                        if (clueNum > 0) {
                            val box = boxes[row][col] ?: Box()
                            box.clueNumber = clueNum.toString()
                            boxes[row][col] = box
                        }
                    }
                }
            }

            val cellInfos = json.optJSONArray("cellInfos")
            if (cellInfos != null) {
                for (i in 0..<cellInfos.length()) {
                    val cellInfo = cellInfos.getJSONObject(i)

                    val row = cellInfo.getInt("y")
                    val col = cellInfo.getInt("x")
                    val circled = cellInfo.optBoolean("isCircled")

                    if (circled) {
                        val box = boxes[row][col] ?: Box()
                        box.shape = Box.Shape.CIRCLE
                        boxes[row][col] = box
                    }
                }
            }

            return boxes
        }

        @Throws(JSONException::class)
        private fun addClues(json : JSONObject, builder : PuzzleBuilder) {
            val entries = json.getJSONArray("placedWords")

            if (entries == null)
                return

            val wordLengthsEnabled = json.optBoolean("wordLengthsEnabled", true)

            for (i in 0..<entries.length()) {
                val entry = entries.getJSONObject(i)

                val num = entry.getInt("clueNum")
                if (num > 0) {
                    val number = num.toString()
                    val across = entry.getBoolean("acrossNotDown")
                    var clue = entry.getJSONObject("clue").getString("clue")
                    var listName = optStringNull(entry, "clueSection")
                    if (listName == null) listName = if (across)
                        DefaultListNames.ACROSS_LIST
                    else
                        DefaultListNames.DOWN_LIST

                    var separators : MutableMap<Int, String>? = null
                    if (wordLengthsEnabled) {
                        separators = getSeparators(entry)
                        val enumeration : String? = getEnumeration(entry)
                        if (
                            enumeration != null
                            && !clue.endsWith(enumeration)
                        ) {
                            clue += " " + enumeration
                        }
                    }

                    val zone = if (across)
                        builder.getAcrossZone(number)
                    else
                        builder.getDownZone(number)
                    val index = builder.getNextClueIndex(listName)
                    builder.addClue(
                        Clue(
                            listName,
                            index,
                            number,
                            null,
                            clue,
                            zone,
                            separators,
                        )
                    )
                }
            }
        }

        private fun getEnumeration(clue : JSONObject) : String? {
            val wordLens = clue.optJSONArray("wordLens")
            if (wordLens == null)
                return null

            val enumeration = StringBuilder()
            enumeration.append("(")
            for (i in 0..<wordLens.length()) {
                if (i > 0)
                    enumeration.append(",")
                enumeration.append(wordLens.get(i).toString())
            }
            enumeration.append(")")

            return enumeration.toString()
        }

        private fun getSeparators(
            clue : JSONObject,
        ) : MutableMap<Int, String>? {
            val wordLens = clue.optJSONArray("wordLens")
            if (wordLens == null)
                return null

            val separators : MutableMap<Int, String> = mutableMapOf()
            var totalLen = 0

            // length - 1 to remove last marker
            for (i in 0..<wordLens.length() - 1) {
                val len = wordLens.optInt(i, -1)
                if (len < 0)
                    return null

                totalLen += len
                separators.put(totalLen, " ")
            }

            return separators
        }
    }
}

