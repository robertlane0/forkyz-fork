
package app.crossword.yourealwaysbe.puz.io

import java.util.logging.Logger
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.Zone

/**
 * Parser for .xwc Spoonbill
 *
 * Format url:
 * https://github.com/Tw1ddle/Blind-Crossword-3D/wiki/Spoonbill-Crossword-Compiler-(.xwc)-Format-Specification
 */
class SpoonbillIO : PuzzleParser {
    class SpoonbillFormatException(msg : String) : Exception(msg)

    @Throws(IOException::class)
    override fun parseInput(input : Source) : Puzzle? {
        return readPuzzle(input)
    }

    companion object {
        private val LOG
            = Logger.getLogger(SpoonbillIO::class.java.getCanonicalName())

        private const val MODE_SKELETON = "Skeleton"
        private const val MODE_SOLUTION = "Solution"
        private const val MODE_CLUE = "Clue"
        private const val MODE_SOLVE = "Solve"

        private val ACROSS_LIST = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST = DefaultListNames.DOWN_LIST

        @Throws(IOException::class)
        fun readPuzzle(input : Source) : Puzzle? {
            try {
                val versionLine : String = expectLine(input)

                val split = versionLine.split(" ".toRegex(), limit = 2)
                    .toTypedArray()
                if (!"TXWordGrid3".equals(split[0], ignoreCase = true)) {
                    throw SpoonbillFormatException(
                        "Unknown Spoonbill version: " + split[0],
                    )
                }
                val author = split[1]
                val title = expectLine(input)
                val mode = expectLine(input)

                val builder = PuzzleBuilder(getBoxes(input))

                builder.setTitle(title)
                    .setAuthor(author)

                if (MODE_CLUE == mode || MODE_SOLVE == mode) {
                    getClues(input, true, builder)
                    getClues(input, false, builder)
                }

                return builder.puzzle
            } catch (e : SpoonbillFormatException) {
                LOG.info("Failed to parse Spoonbill file: " + e)
                return null
            }
        }

        /**
         * Read boxes from file
         *
         * Will return a grid, throws an exception if error
         */
        @Throws(IOException::class, SpoonbillFormatException::class)
        private fun getBoxes(input : Source) : Array<Array<Box?>> {
            try {
                val height = expectLine(input).toInt()
                val width = expectLine(input).toInt()

                val boxes = Array<Array<Box?>>(height) {
                    arrayOfNulls<Box>(width)
                }

                for (row in 0..<height) {
                    val line = expectLine(input)
                    var col = 0
                    while (col < width && col < line.length) {
                        val cell = line.get(col)
                        if (cell != '1') {
                            val box = Box()
                            if (cell != '0') {
                                box.setSolution(cell.uppercaseChar())
                            }
                            boxes[row][col] = box
                        }
                        col++
                    }
                }

                return boxes
            } catch (e : NumberFormatException) {
                throw SpoonbillFormatException(
                    "Bad width or height line: " + e
                )
            }
        }

        @Throws(IOException::class, SpoonbillFormatException::class)
        private fun getClues(
            input : Source,
            across : Boolean,
            builder : PuzzleBuilder,
        ) {
            val numClues = expectLine(input).toInt()
            for (i in 0..<numClues) {
                val line : String = expectLine(input)
                val data = line.split("\\|".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                if (data.size < 6) {
                    throw SpoonbillFormatException(
                        "Unexpected clue line: " + line,
                    )
                }

                val number = data[0].trim()
                val startRow = data[1].trim().toInt() - 1
                val startCol = data[2].trim().toInt() - 1
                val length = data[3].trim().toInt()
                val zone : Zone = getZone(startRow, startCol, length, across)
                val listName = if (across)
                    SpoonbillIO.Companion.ACROSS_LIST
                else
                    SpoonbillIO.Companion.DOWN_LIST
                val index = builder.getNextClueIndex(listName)
                val hint = data[5].trim()

                builder.addClue(Clue(listName, index, number, null, hint, zone))

                if (zone.size() > 0) {
                    val box = builder.getBox(zone.getPosition(0))
                    if (box != null)
                        box.clueNumber = number
                }
            }
        }

        /**
         * Get the zone
         *
         * From (startRow, startCol) then moving length cells across or down
         * depending on across argument.
         */
        private fun getZone(
            startRow : Int,
            startCol : Int,
            length : Int,
            across : Boolean,
        ) : Zone {
            val zone = Zone()
            var row = startRow
            var col = startCol
            for (i in 0..<length) {
                zone.addPosition(Position(row, col))
                row += if (across) 0 else 1
                col += if (across) 1 else 0
            }
            return zone
        }

        /**
         * Return trimmed next line or exception
         */
        @Throws(IOException::class, SpoonbillFormatException::class)
        private fun expectLine(input : Source) : String {
            val line = input.readLine()
            if (line == null) {
                throw SpoonbillFormatException("Expected more input")
            }
            return line.trim()
        }
    }
}

