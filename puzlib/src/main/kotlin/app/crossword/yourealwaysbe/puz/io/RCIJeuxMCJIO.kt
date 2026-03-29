
package app.crossword.yourealwaysbe.puz.io

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.logging.Logger

import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder

/**
 * Format used by RCI Jeux for crossword puzzles
 */
class RCIJeuxMCJIO : PuzzleParser {
    class MCJFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    companion object {
        private val LOG
            = Logger.getLogger(RCIJeuxMCJIO::class.java.getCanonicalName())

        // Not internationalised, assume French
        private const val ACROSS_LIST = "Horiz."
        private const val DOWN_LIST = "Vert."
        private const val JOIN_DASH = '–'

        private const val ARROW_WORDS_CLUE_FIELD_NAME = "definitions"
        private const val CROSSWORD_ACROSS_CLUE_FIELD_NAME = "definitionsh"
        private const val CROSSWORD_DOWN_CLUE_FIELD_NAME = "definitionsv"

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val hjson = getHJSON(inStream)
                if (hjson == null)
                    return null

                return readPuzzleFromHJSON(
                    JsonValue.readHjson(hjson).asObject(),
                )
            } catch (e : MCJFormatException) {
                LOG.severe("Could not read RCIJeux MCJ: " + e)
                return null
            }
        }

        /**
         * Extract the HJSON part from the stream
         *
         * Format is usually
         *
         * var gameData = {
         *
         * };
         *
         * just get the { ... } HJSON bit, return as a string, ignore
         * anything outside of the outer { }, doesn't have to be "var
         * gameData"
         */
        @Throws(IOException::class)
        private fun getHJSON(inStream : InputStream) : String? {
            BufferedReader(InputStreamReader(inStream)).use { reader ->
                val sb = StringBuilder()
                reader.useLines { lines -> lines.forEach { sb.append(it) } }
                val wholeFile = sb.toString()
                val start = wholeFile.indexOf('{')
                val end = wholeFile.lastIndexOf('}')
                if (start < 0 || end < 0)
                    return null
                else
                    return wholeFile.substring(start, end + 1)
            }
        }

        /**
         * Read puzzle from RaetselZentrale JSON format
         *
         * Does not include author, date, source.
         */
        @Throws(MCJFormatException::class)
        private fun readPuzzleFromHJSON(hjson : JsonObject) : Puzzle {
            val title = hjson.getString("titre", null)
            if (title == null)
                throw MCJFormatException("No titre field in HJSON.")

            val builder = PuzzleBuilder(getBoxes(hjson))
                .setTitle(title)

            addClues(hjson, builder)

            return builder.puzzle
        }

        @Throws(MCJFormatException::class)
        private fun getBoxes(hjson : JsonObject) : Array<Array<Box?>> {
            val numRows = hjson.getInt("nbcaseshauteur", -1)
            val numCols = hjson.getInt("nbcaseslargeur", -1)

            if (numRows < 0 || numCols < 0) {
                throw MCJFormatException(
                    "Impossible grid size " + numRows + "x" + numCols + ".",
                )
            }

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }
            val rows: JsonArray = asArray(hjson.get("grille"))

            for (row in 0..<numRows) {
                val cols: String = getRowString(rows.get(row))
                for (col in 0..<numCols) {
                    val cell = cols.get(col)
                    if (Character.isUpperCase(cell)) {
                        val box = Box()
                        box.solution = cell.toString()
                        boxes[row][col] = box
                        // clue numbers set with clues
                    }
                }
            }

            return boxes
        }

        @Throws(MCJFormatException::class)
        private fun getRowString(row : JsonValue?) : String {
            if (row == null) {
                throw MCJFormatException(
                    "When getting grid row, got a null value.",
                )
            } else if (row.isString()) {
                return row.asString()
            } else if (row.isArray() && row.asArray().size() == 1) {
                return asString(row.asArray().get(0))
            } else {
                throw MCJFormatException(
                    "Expected row "
                    + row
                    + " to be a string or array of one string.",
                )
            }
        }

        @Throws(MCJFormatException::class)
        private fun addClues(hjson : JsonObject, builder : PuzzleBuilder) {
            builder.autoNumberBoxes()
            addAcrossClues(hjson, builder)
            addDownClues(hjson, builder)
        }

        @Throws(MCJFormatException::class)
        private fun addAcrossClues(
            hjson : JsonObject,
            builder : PuzzleBuilder,
        ) {
            val clues = asArray(hjson.get(CROSSWORD_ACROSS_CLUE_FIELD_NAME))

            for (row in 0..<builder.height) {
                val rowClues: JsonArray = asArray(clues.get(row))
                var clueIdx = 0
                for (col in 0..<builder.width) {
                    val box = builder.getBox(row, col)
                    if (
                        box != null
                        && !isBlock(box)
                        && box.hasClueNumber()
                        && builder.isStartClue(row, col, true)
                        && clueIdx < clues.size()
                    ) {
                        val hint : String = asString(rowClues.get(clueIdx))
                        builder.addAcrossClue(
                            ACROSS_LIST,
                            box.clueNumber,
                            hint,
                        )
                        clueIdx += 1
                    }
                }
            }
        }

        @Throws(MCJFormatException::class)
        private fun addDownClues(hjson : JsonObject, builder : PuzzleBuilder) {
            val clues: JsonArray = asArray(
                hjson.get(CROSSWORD_DOWN_CLUE_FIELD_NAME),
            )

            for (col in 0..<builder.width) {
                val colClues: JsonArray = asArray(clues.get(col))
                var clueIdx = 0
                for (row in 0..<builder.height) {
                    val box = builder.getBox(row, col)
                    if (
                        box != null
                        && !isBlock(box)
                        && box.hasClueNumber()
                        && builder.isStartClue(row, col, false)
                        && clueIdx < clues.size()
                    ) {
                        val hint : String = asString(colClues.get(clueIdx))
                        builder.addDownClue(
                            DOWN_LIST,
                            box.clueNumber,
                            hint,
                        )
                        clueIdx += 1
                    }
                }
            }
        }

        @Throws(MCJFormatException::class)
        private fun getHint(clueParts: JsonArray) : String {
            val hint = StringBuilder()

            for (i in 0..<clueParts.size()) {
                hint.append(asString(clueParts.get(i)))
                val length = hint.length
                if (hint.get(length - 1) == JOIN_DASH)
                    hint.setLength(length - 1)
                else if (i < clueParts.size() - 1)
                    hint.append(" ")
            }

            return hint.toString()
        }

        @Throws(MCJFormatException::class)
        private fun asArray(value : JsonValue?) : JsonArray {
            if (value == null || !value.isArray()) {
                throw MCJFormatException(
                    "Expected " + value + " to be an array.",
                )
            }
            return value.asArray()
        }

        @Throws(MCJFormatException::class)
        private fun asString(value : JsonValue?) : String {
            if (value == null || !value.isString()) {
                throw MCJFormatException(
                    "Expect " + value + " to be a string.",
                )
            }
            return value.asString()
        }
    }
}

