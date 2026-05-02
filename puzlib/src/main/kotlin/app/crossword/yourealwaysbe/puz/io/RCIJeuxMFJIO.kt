
package app.crossword.yourealwaysbe.puz.io

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.logging.Logger

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.JSONArray
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseHJSONObject

/**
 * Format used by RCI Jeux for Mots Fleches puzzles
 *
 * Which are the same as the Swedish crosswords for RaetselZentrale:
 * the grid, with arrows pointing to the box where the answer starts.
 *
 *
 * The basic crossword structure will be playable, but perhaps a
 * completion game is missing.
 */
class RCIJeuxMFJIO : PuzzleParser {
    class MFJFormatException(msg : String) : Exception(msg)

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return readPuzzle(inStream)
    }

    private enum class Arrow(
        val lower : Char,
        val upper : Char,
        // start position of first/second clue associated with arrow
        private val drow1 : Int,
        private val dcol1 : Int,
        // whether first/second clue is across or down
        val isAcross1 : Boolean,
        private val drow2 : Int,
        private val dcol2 : Int,
        private val isAcross2 : Boolean,
        // if arrow actually has two clues
        val hasTwoClues : Boolean
    ) {
        RIGHT('a', 'a', 0, 1, true),
        DOWN('b', 'b', 1, 0, false),
        RIGHT_DOWN('c', 'c', 0, 1, false),
        LEFT_DOWN('d', 'd', 1, 0, true),
        // guessed, not seen
        RIGHT_AND_DOWN('e', 'i', 0, 1, true, 1, 0, false),
        RIGHT_DOWN_AND_DOWN('j', 'n', 0, 1, false, 1, 0, false),
        RIGHT_AND_DOWN_RIGHT('o', 's', 0, 1, true, 1, 0, true),
        RIGHT_DOWN_AND_DOWN_RIGHT('t', 'x', 0, 1, false, 1, 0, true),
        NONE('z', 'z', 0, 0, false);

        constructor(
            lower : Char,
            upper : Char,
            drow1 : Int,
            dcol1 : Int,
            isAcross1 : Boolean,
        ) : this(lower, upper, drow1, dcol1, isAcross1, 0, 0, false, false)

        constructor(
            lower : Char,
            upper : Char,
            drow1 : Int,
            dcol1 : Int,
            isAcross1 : Boolean,
            drow2 : Int,
            dcol2 : Int,
            isAcross2 : Boolean,
        ) : this(
            lower,
            upper,
            drow1,
            dcol1,
            isAcross1,
            drow2,
            dcol2,
            isAcross2,
            true,
        )

        /**
         * Get position of first clue associated to this arrow
         *
         * Given the starting point row/col
         */
        fun getPosition1(row : Int, col : Int) : Position {
            return Position(row + drow1, col + dcol1)
        }

        fun getPosition2(row : Int, col : Int) : Position {
            require(this.hasTwoClues) {
                this.toString() + " only has one position"
            }
            return Position(row + drow2, col + dcol2)
        }

        fun getIsAcross2() : Boolean {
            require(this.hasTwoClues) {
                this.toString() + " only has one position"
            }
            return isAcross2
        }
    }

    private class ClueInfo(
        val position : Position,
        val isAcross : Boolean,
        val hint : String,
    )

    companion object {
        private val LOG
            = Logger.getLogger(RCIJeuxMFJIO::class.java.getCanonicalName())

        // Not internationalised, assume French
        private const val ACROSS_LIST = "Horiz."
        private const val DOWN_LIST = "Vert."
        private const val JOIN_DASH = '–'

        @JvmStatic
        @Throws(IOException::class)
        fun readPuzzle(inStream : InputStream) : Puzzle? {
            try {
                val hjson = getHJSON(inStream)
                if (hjson == null)
                    return null

                return readPuzzleFromHJSON(parseHJSONObject(hjson))
            } catch (e : MFJFormatException) {
                LOG.severe("Could not read RCIJeux MFJ: " + e)
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
                reader.useLines { lines ->
                    lines.forEach { line ->
                        sb.append(line)
                    }
                }

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
        @Throws(MFJFormatException::class)
        private fun readPuzzleFromHJSON(hjson : JSONObject) : Puzzle {
            val title = hjson.getString("titre")

            val builder = PuzzleBuilder(getBoxes(hjson))
                .setTitle(title)

            addClues(hjson, builder)

            return builder.puzzle
        }

        @Throws(MFJFormatException::class)
        private fun getBoxes(hjson : JSONObject) : Array<Array<Box?>> {
            val numRows = hjson.getInt("nbcaseshauteur")
            val numCols = hjson.getInt("nbcaseslargeur")

            if (numRows < 0 || numCols < 0) {
                throw MFJFormatException(
                    "Impossible grid size " + numRows + "x" + numCols + ".",
                )
            }

            val boxes = Array<Array<Box?>>(numRows) {
                arrayOfNulls<Box>(numCols)
            }
            val rows : JSONArray = hjson.getJSONArray("grille")
            val vdashes : JSONArray = hjson.getJSONArray("spountzV")
            val hdashes : JSONArray = hjson.getJSONArray("spountzH")

            for (row in 0..<numRows) {
                val cols : String = rows.getString(row)
                for (col in 0..<numCols) {
                    val cell = cols.get(col)
                    if (Character.isUpperCase(cell)) {
                        val b = Box()
                        b.solution = cell.toString()
                        boxes[row][col] = b
                        // clue numbers set with clues
                    }
                }
            }

            for (i in 0 until vdashes.size) {
                val vdash_arr = vdashes.getJSONArray(i)
                val row = vdash_arr.getInt(1) - 1
                val col = vdash_arr.getInt(0) - 1
                boxes[row][col]?.let { it.barRight = Box.Bar.DASHED }
            }

            for (i in 0 until hdashes.size) {
                val hdash_arr = hdashes.getJSONArray(i)
                val row = hdash_arr.getInt(1) - 1
                val col = hdash_arr.getInt(0) - 1
                boxes[row][col]?.let { it.barBottom = Box.Bar.DASHED }
            }

            return boxes
        }

        // this is exactly as RaetselZentrale, merge?
        @Throws(MFJFormatException::class)
        private fun addClues(hjson : JSONObject, builder : PuzzleBuilder) {
            val clueInfos = getClueInfos(hjson)

            var nextClueNumber = 1
            val curPos = Position(0, 0)

            for (row in 0..<builder.height) {
                curPos.row = row
                for (col in 0..<builder.width) {
                    curPos.col = col
                    val posClueInfos = clueInfos.get(curPos)
                    if (posClueInfos != null) {
                        for (clueInfo in posClueInfos) {
                            val box = builder.getBox(curPos)
                            if (box == null || isBlock(box)) {
                                throw MFJFormatException(
                                    "Clue has position on a block square "
                                    + curPos,
                                )
                            }

                            val clueNumber : String?
                            if (box.hasClueNumber()) {
                                clueNumber = box.clueNumber
                            } else {
                                clueNumber = nextClueNumber.toString()
                                nextClueNumber += 1
                                builder.setBoxClueNumber(
                                    row, col, clueNumber
                                )
                            }

                            if (clueInfo.isAcross) {
                                builder.addAcrossClue(
                                    ACROSS_LIST,
                                    clueNumber,
                                    clueInfo.hint,
                                )
                            } else {
                                builder.addDownClue(
                                    DOWN_LIST,
                                    clueNumber,
                                    clueInfo.hint,
                                )
                            }
                        }
                    }
                }
            }
        }

        @Throws(MFJFormatException::class)
        private fun getClueInfos(
            hjson : JSONObject,
        ) : Map<Position, MutableList<ClueInfo>> {
            val clueInfos : MutableMap<Position, MutableList<ClueInfo>>
                = mutableMapOf()

            val rows : JSONArray = hjson.getJSONArray("grille")
            val clues : JSONArray = hjson.getJSONArray("definitions")

            var clueIdx = 0

            for (row in 0..<rows.size) {
                val cols : String = rows.getString(row)
                for (col in 0..<cols.length) {
                    val cell = cols.get(col)
                    if (Character.isLowerCase(cell)) {
                        val arrow : Arrow = getArrowFromCell(cell)
                        if (arrow.upper == 'z')
                            continue

                        val position1 = arrow.getPosition1(row, col)
                        val isAcross1 = arrow.isAcross1
                        val hint1 = getHint(clues.getJSONArray(clueIdx))
                        clueIdx += 1
                        addClueInfo(clueInfos, position1, isAcross1, hint1)

                        if (arrow.hasTwoClues) {
                            val position2 = arrow.getPosition2(row, col)
                            val isAcross2 = arrow.getIsAcross2()
                            val hint2 = getHint(clues.getJSONArray(clueIdx))
                            clueIdx += 1
                            addClueInfo(clueInfos, position2, isAcross2, hint2)
                        }
                    }
                }
            }

            return clueInfos
        }

        private fun addClueInfo(
            clueInfos : MutableMap<Position, MutableList<ClueInfo>>,
            position : Position,
            isAcross : Boolean,
            hint : String,
        ) {
            if (!clueInfos.containsKey(position))
                clueInfos.put(position, mutableListOf<ClueInfo>())
            clueInfos.get(position)?.let {
                it.add(ClueInfo(position, isAcross, hint))
            }
        }

        @Throws(MFJFormatException::class)
        private fun getArrowFromCell(cell : Char) : Arrow {
            for (arrow in Arrow.entries) {
                if (arrow.lower <= cell && cell <= arrow.upper)
                    return arrow
            }
            throw MFJFormatException("Unrecognised arrow type " + cell)
        }

        @Throws(MFJFormatException::class)
        private fun getHint(clueParts : JSONArray) : String {
            val hint = StringBuilder()

            for (i in 0..<clueParts.size) {
                hint.append(clueParts.getString(i))
                val length = hint.length
                if (hint.get(length - 1) == JOIN_DASH)
                    hint.setLength(length - 1)
                else if (i < clueParts.size - 1)
                    hint.append(" ")
            }

            return hint.toString()
        }
    }
}

